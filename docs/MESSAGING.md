# Mensageria

O ms-ofisy-stock se comunica com os demais microsserviços por eventos, usando **Amazon SNS + SQS** ([ADR-0011](https://github.com/15SOAT-FIAP/techchallenge-ofisy/blob/master/docs/adr/0011-utilizar-sns-e-sqs-como-mensageria.md)), com a saga coordenada por coreografia ([ADR-0012](https://github.com/15SOAT-FIAP/techchallenge-ofisy/blob/master/docs/adr/0012-coordenar-a-saga-por-coreografia.md)). Os eventos publicados pelo estoque saem por um *transactional outbox* para o tópico `ofisy-stock-events.fifo`, e os eventos dos outros serviços chegam por uma única fila FIFO, `ofisy-stock.fifo`.

A estrutura é a mesma do core e do `ms-ofisy-execution`: mesmo envelope, mesmo outbox e mesmo despacho por `type`. Assim, um evento publicado por qualquer serviço tem o mesmo contrato.

---

## Topologia

```mermaid
flowchart LR
    billing[ms-ofisy-billing] -->|publica| billingTopic[(SNS<br/>ofisy-billing-events.fifo)]
    billingTopic -->|assinatura<br/>filtro type = QuoteCreated, QuoteReproved<br/>raw delivery| queue[(SQS<br/>ofisy-stock.fifo)]
    execution[ms-ofisy-execution] -->|publica| execTopic[(SNS<br/>ofisy-execution-events.fifo)]
    execTopic -->|assinatura<br/>filtro type = AllExecutionsCancelled<br/>raw delivery| queue
    queue --> app[ms-ofisy-stock]
    queue -->|após 5 recebimentos| dlq[(SQS<br/>ofisy-stock-dlq.fifo)]
    app -->|outbox + publisher| topic[(SNS<br/>ofisy-stock-events.fifo)]
    topic -.->|só local, raw delivery| inspect[(SQS<br/>ofisy-stock-events-inspect.fifo)]
```

| Recurso          | Nome                              | Observação                                                                                                   |
|------------------|-----------------------------------|--------------------------------------------------------------------------------------------------------------|
| Tópico           | `ofisy-billing-events.fifo`       | Eventos do orçamento, publicados pelo `ms-ofisy-billing` na AWS. Localmente é criado só para exercitar o fluxo |
| Tópico           | `ofisy-execution-events.fifo`     | Eventos de execução, publicados pelo `ms-ofisy-execution` na AWS. Localmente é criado só para exercitar o fluxo |
| Assinaturas      | tópicos → fila                    | Uma por tópico, com `RawMessageDelivery=true` e `FilterPolicy` aplicada ao corpo da mensagem (`MessageBody`) |
| Fila             | `ofisy-stock.fifo`                | Configurada em `ofisy.messaging.queues.stock` (`application.yaml`)                                           |
| DLQ              | `ofisy-stock-dlq.fifo`            | `RedrivePolicy` com `maxReceiveCount=5`                                                                      |
| Tópico           | `ofisy-stock-events.fifo`         | Tópico deste ms, configurado em `ofisy.messaging.topics.stock`                                               |
| Fila de inspeção | `ofisy-stock-events-inspect.fifo` | Só local. Assinada no tópico deste ms para ver o que foi publicado                                           |

Localmente, tudo é criado pelo `localstack/init/ready.d/01-messaging.sh`, que roda quando o LocalStack sobe (pelo `compose.yaml`, pelo `compose.db.yaml` e nos testes de integração). Com `queue-not-found-strategy: fail`, a aplicação não inicia se a fila não existir.

Na AWS, os recursos vêm do repositório de infraestrutura. O tópico é configurado pelo **nome**: o `SnsConfig` registra um `TopicsListingTopicArnResolver`, que descobre o ARN listando os tópicos (`sns:ListTopics`) em vez de chamar `CreateTopic`, e guarda o resultado em cache. A aplicação precisa de `sns:ListTopics`, `sns:Publish` e, na fila, `sqs:GetQueueUrl`, `sqs:ReceiveMessage`, `sqs:DeleteMessage` e `sqs:ChangeMessageVisibility`.

---

## Contrato dos eventos

Todo evento segue o mesmo envelope (`adapters/listeners/dto/EventEnvelope.java`). O `payload` varia conforme o `type`:

| Campo            | Tipo    | Descrição                                                                   |
|------------------|---------|-----------------------------------------------------------------------------|
| `eventId`        | UUID    | Identificador único do evento, usado para idempotência                      |
| `type`           | String  | Tipo do evento, usado no filtro da assinatura e no despacho                 |
| `serviceOrderId` | UUID    | Ordem de serviço à qual o evento se refere. Nulo em eventos sem OS          |
| `occurredAt`     | Instant | Momento em que o evento ocorreu no produtor                                 |
| `payload`        | JSON    | Dados específicos do tipo de evento                                         |

```json
{
  "eventId": "5e6f7a8b-9c0d-4e1f-8a2b-3c4d5e6f7a8b",
  "type": "StockConsumed",
  "serviceOrderId": "c3d4e5f6-a7b8-9012-cdef-123456780001",
  "occurredAt": "2026-10-20T10:00:00Z",
  "payload": { "quoteId": "9a2b3c4d-5e6f-7a8b-9c0d-1e2f3a4b5c6d" }
}
```

### Eventos consumidos

| Evento                   | Produtor           | Efeito                                                                                  |
|--------------------------|--------------------|-----------------------------------------------------------------------------------------|
| `QuoteCreated`           | ms-ofisy-billing   | Consome os itens do orçamento e publica o `StockConsumed` ou o `StockConsumptionFailed` |
| `QuoteReproved`          | ms-ofisy-billing   | Devolve ao estoque o que saiu para a OS e ainda não voltou, e publica o `StockRefunded` |
| `AllExecutionsCancelled` | ms-ofisy-execution | Devolve ao estoque o que saiu para a OS e ainda não voltou, e publica o `StockRefunded` |

#### `QuoteCreated`

O billing publica quando cria um orçamento com itens de estoque. Orçamento só com serviços não passa pelo estoque. O estoque usa o `quoteId` e os `stockItems` do payload e ignora os demais campos:

```json
{
  "eventId": "9b2f6c1e-4a7d-4e8b-a1c3-5d6e7f8a9b0c",
  "type": "QuoteCreated",
  "serviceOrderId": "c3d4e5f6-a7b8-9012-cdef-123456780001",
  "occurredAt": "2026-11-02T10:00:00Z",
  "payload": {
    "quoteId": "7a8b9c0d-1e2f-4a3b-8c4d-5e6f7a8b9c0d",
    "stockItems": [
      { "stockId": "a1b2c3d4-e5f6-7890-abcd-ef1234560101", "quantity": 2 }
    ]
  }
}
```

O `QuoteCreatedHandler` converte o payload e chama o `ConsumeByQuoteStockUseCase`, que, na mesma transação:

1. descarta o evento se o `eventId` já estiver em `processed_events`;
2. consome os itens pela OS (`ConsumeByServiceOrderStockUseCase`): ou todos têm saldo e todos são consumidos, ou nada é consumido;
3. grava no outbox o `StockConsumed`, se tudo foi consumido, ou o `StockConsumptionFailed`, com os itens que faltaram;
4. registra o `eventId` em `processed_events`.

Falta de saldo, ou um item que não existe, não é erro: o evento é processado e o `StockConsumptionFailed` avisa o billing, que cancela o orçamento. Já um evento sem `serviceOrderId`, `quoteId` ou `stockItems`, ou com quantidade inválida, não tem como ser atendido: a exceção impede o ack e a mensagem vai para a DLQ depois de 5 tentativas.

O consumo por OS desconta o que já saiu para ela, então um segundo `QuoteCreated` da mesma OS, com outro `eventId`, não consome os itens de novo.

#### `QuoteReproved`

O billing publica quando o cliente reprova o orçamento. O estoque devolve os itens da OS e publica o `StockRefunded`, que é o que leva o core a cancelar a OS nesse caminho.

```json
{
  "eventId": "7d8e9f0a-1b2c-4d3e-9f4a-5b6c7d8e9f0a",
  "type": "QuoteReproved",
  "serviceOrderId": "c3d4e5f6-a7b8-9012-cdef-123456780001",
  "occurredAt": "2026-11-02T11:00:00Z",
  "payload": {
    "quoteId": "7a8b9c0d-1e2f-4a3b-8c4d-5e6f7a8b9c0d",
    "reason": "Valor acima do esperado",
    "stockItems": [
      { "stockId": "a1b2c3d4-e5f6-7890-abcd-ef1234560101", "quantity": 2 }
    ]
  }
}
```

O payload é ignorado. Assim como no `AllExecutionsCancelled`, o `QuoteReprovedHandler` chama o `RefundByServiceOrderStockUseCase` só com o id da OS, e a devolução segue as movimentações (`stock_movements.service_order_id`). Elas dizem o que saiu de fato, o que evita devolver um item que nunca foi consumido.

#### `AllExecutionsCancelled`

Quando a OS é cancelada, o core publica o `ServiceOrderCancelled`, o execution cancela as execuções ativas e publica o `AllExecutionsCancelled`, e só então o estoque devolve os itens. Seguindo essa ordem, o `StockRefunded`, e o `QuoteCancelled` que o billing publica a partir dele, marcam o fim da compensação.

O payload vem vazio: o id da OS está no `serviceOrderId` do envelope, e o estoque já sabe o que saiu para cada OS pelas movimentações (`stock_movements.service_order_id`).

```json
{
  "eventId": "5e6f7a8b-9c0d-4e1f-8a2b-3c4d5e6f7a8b",
  "type": "AllExecutionsCancelled",
  "serviceOrderId": "c3d4e5f6-a7b8-9012-cdef-123456780001",
  "occurredAt": "2026-10-20T10:00:00Z",
  "payload": {}
}
```

O `AllExecutionsCancelledHandler` chama o `RefundByServiceOrderStockUseCase`, que, na mesma transação:

1. descarta o evento se o `eventId` já estiver em `processed_events`;
2. devolve, por item, o que saiu para a OS menos o que já voltou (`ReleaseByServiceOrderStockUseCase`);
3. grava o `StockRefunded` no outbox;
4. registra o `eventId` em `processed_events`.

O `StockRefunded` sai mesmo quando não há nada a devolver, como numa OS sem consumo ou que já teve os itens devolvidos por uma reprovação do orçamento. Isso vale para o `QuoteReproved` e para o `AllExecutionsCancelled`. Nesse caso a lista de itens vai vazia. O billing e o core seguem a saga a partir desse evento, então sem ele o orçamento ou a OS ficariam sem desfecho.

### Eventos publicados

| Evento                   | Quando é publicado                                                    | Quem reage          |
|--------------------------|-----------------------------------------------------------------------|---------------------|
| `StockConsumed`          | Depois de um `QuoteCreated` em que todos os itens foram consumidos    | billing             |
| `StockConsumptionFailed` | Depois de um `QuoteCreated` em que algum item não tinha saldo         | billing             |
| `StockRefunded`          | Sempre que um `QuoteReproved` ou `AllExecutionsCancelled` novo é processado | billing e core |
| `StockLow`               | A cada consumo que deixa o saldo de um item igual ou abaixo do mínimo | notification        |

O billing usa só o `serviceOrderId` do envelope no `StockConsumed`, no `StockConsumptionFailed` e no `StockRefunded`. Os payloads ficam como referência para quem precisar.

#### `StockConsumed`

Agrupado pela OS. O payload repete o orçamento e os itens consumidos:

```json
{
  "eventId": "5e6f7a8b-9c0d-4e1f-8a2b-3c4d5e6f7a8b",
  "type": "StockConsumed",
  "serviceOrderId": "c3d4e5f6-a7b8-9012-cdef-123456780001",
  "occurredAt": "2026-11-02T10:00:01Z",
  "payload": {
    "quoteId": "7a8b9c0d-1e2f-4a3b-8c4d-5e6f7a8b9c0d",
    "items": [
      { "stockId": "a1b2c3d4-e5f6-7890-abcd-ef1234560101", "quantity": 2 }
    ]
  }
}
```

#### `StockConsumptionFailed`

Agrupado pela OS. Nada foi consumido, e o payload lista os itens sem saldo suficiente. Um item que não existe aparece com `availableQuantity` igual a zero:

```json
{
  "eventId": "6f7a8b9c-0d1e-4f2a-9b3c-4d5e6f7a8b9c",
  "type": "StockConsumptionFailed",
  "serviceOrderId": "c3d4e5f6-a7b8-9012-cdef-123456780001",
  "occurredAt": "2026-11-02T10:00:01Z",
  "payload": {
    "quoteId": "7a8b9c0d-1e2f-4a3b-8c4d-5e6f7a8b9c0d",
    "missingItems": [
      { "stockId": "a1b2c3d4-e5f6-7890-abcd-ef1234560101", "requestedQuantity": 3, "availableQuantity": 1 }
    ]
  }
}
```

#### `StockLow`

Gravado pelo `ConsumeStockService`, na mesma transação da baixa, então vale tanto para o consumo pela API quanto para o consumo por OS. Como no core, o aviso sai a cada consumo enquanto o saldo estiver igual ou abaixo do mínimo, não só na primeira vez.

O aviso não pertence a uma OS: o envelope sai com `serviceOrderId` nulo e o `message-group-id` é o id do item. O payload segue o contrato que o notification já consome:

```json
{
  "eventId": "1c2d3e4f-5a6b-7c8d-9e0f-a1b2c3d4e5f6",
  "type": "StockLow",
  "serviceOrderId": null,
  "occurredAt": "2026-10-20T10:00:00Z",
  "payload": {
    "stockId": "a1b2c3d4-e5f6-7890-abcd-ef1234560101",
    "productName": "Radiador",
    "currentQuantity": 2,
    "minThreshold": 2
  }
}
```

#### `StockRefunded`

Agrupado pela OS. O payload lista o que voltou para o estoque, com `items` vazio quando nada voltou:

```json
{
  "eventId": "8b7a6f5e-4d3c-2b1a-0f9e-8d7c6b5a4f3e",
  "type": "StockRefunded",
  "serviceOrderId": "c3d4e5f6-a7b8-9012-cdef-123456780001",
  "occurredAt": "2026-10-20T10:00:01Z",
  "payload": {
    "items": [
      { "stockId": "a1b2c3d4-e5f6-7890-abcd-ef1234560101", "quantity": 2 }
    ]
  }
}
```

---

## Consumo de eventos

O `StockEventsListener` (`adapters/listeners/`) escuta a fila `ofisy-stock.fifo`. Ele não conhece os eventos: recebe todos os `EventHandler` registrados no Spring e despacha pelo `type`. Cada tipo de evento tem seu próprio handler em `adapters/listeners/handlers/`.

- **Tipo sem handler**: o evento é logado como ignorado e confirmado, sem passar pelo banco.
- **Mensagem inválida ou erro no processamento**: a exceção impede o ack, a mensagem volta para a fila depois do *visibility timeout* e, depois de 5 recebimentos, vai para a DLQ.
- **Ordenação**: as mensagens chegam com `message-group-id = serviceOrderId`, então eventos de uma mesma OS são processados em ordem e OSs diferentes em paralelo.
- **Idempotência**: a entrega do SQS é *at-least-once*. O caso de uso acionado pelo handler registra o `eventId` em **`processed_events`** (`domain/messaging/ProcessedEventRepository`) na mesma transação do seu efeito e descarta o evento se o `eventId` já existir. Na devolução por OS há uma segunda barreira: ela devolve só o que ainda não voltou, então dois eventos diferentes para a mesma OS (reprovação e cancelamento) não devolvem os itens duas vezes.
- **Concorrência no saldo**: OSs diferentes são processadas em paralelo e podem mexer no mesmo item. Toda alteração de saldo trava a linha do item com `SELECT ... FOR UPDATE` (`StockRepository.lockById`), e o consumo e a devolução por OS travam os itens em ordem de id, para duas OSs com itens em comum não se bloquearem mutuamente.

### Como adicionar um novo evento consumido

1. Criar o DTO do payload em `adapters/listeners/dto/`.
2. Criar um `@Component` em `adapters/listeners/handlers/` implementando `EventHandler`, com `type()` retornando o nome do evento.
3. No caso de uso acionado pelo handler, checar e gravar o `eventId` em `processed_events` na mesma transação.
4. Criar a assinatura da fila no tópico do produtor, com `RawMessageDelivery=true` e `FilterPolicy` pelo `type` (`FilterPolicyScope=MessageBody`), em `localstack/init/ready.d/01-messaging.sh` e na infraestrutura da AWS.
5. Cobrir com teste unitário do handler e com um cenário no `StockEventsListenerIT`.
6. Adicionar a linha na tabela de [eventos consumidos](#eventos-consumidos).

---

## Publicação de eventos (outbox)

Os casos de uso não chamam o SNS: eles gravam o evento na tabela `outbox_events`, na mesma transação da mudança de estado, e o `OutboxEventPublisher` é quem publica no tópico, de forma agendada, seguindo a [RFC-0008](https://github.com/15SOAT-FIAP/techchallenge-ofisy/blob/master/docs/rfc/0008-coordenar-a-saga-entre-os-microsservicos.md). Se a transação falhar, o evento não é gravado; se o SNS estiver fora, o evento espera no banco.

```mermaid
sequenceDiagram
    participant UC as Caso de uso
    participant DB as PostgreSQL
    participant P as OutboxEventPublisher
    participant SNS as SNS ofisy-stock-events.fifo

    UC->>DB: muda o estado + insert outbox_events (mesma transação)
    loop a cada publish-interval
        P->>DB: select pendentes for update, em ordem de occurred_at
        loop cada evento do lote
            P->>SNS: envelope (groupId = message_group_id, deduplicationId = eventId)
            P->>DB: published_at = now()
        end
    end
```

| Peça                                                    | Papel                                                                         |
|---------------------------------------------------------|-------------------------------------------------------------------------------|
| `domain/messaging/OutboxEvent`                          | O evento a publicar: tipo, OS, grupo e payload. Id e `occurredAt` são gerados |
| `domain/messaging/OutboxEventRepository`                | Porta usada pelos casos de uso. Só tem `save`                                 |
| `adapters/gateways/messaging/OutboxEventRepositoryImpl` | Serializa o payload para JSON e grava em `outbox_events`                      |
| `adapters/publishers/OutboxEventPublisher`              | Lê os pendentes, publica o envelope no SNS e marca `published_at`             |

### Eventos sem OS

Diferente do core e do execution, o estoque publica um evento que não pertence a uma OS: o aviso de estoque baixo, agrupado pelo id do estoque. Por isso o outbox tem a coluna `message_group_id`, separada do `service_order_id`:

- `OutboxEvent.create(type, serviceOrderId, payload)`: o grupo é a OS. É o caso dos eventos da saga.
- `OutboxEvent.createWithoutServiceOrder(type, messageGroupId, payload)`: o envelope sai com `serviceOrderId` nulo e o grupo é o id informado.

As entidades de `outbox_events` e `processed_events` sempre inserem no `save` (`Persistable.isNew() = true`). O id vem do domínio ou do evento, então não há consulta prévia para decidir entre persist e merge, e um evento repetido viola a PK.

### Garantias

- **Ordem**: os pendentes são publicados em ordem de `occurred_at`, com `message-group-id = message_group_id`. Quando um evento falha, o publicador loga e **pula os eventos seguintes do mesmo grupo** naquele lote, para não publicá-los fora de ordem. Os eventos dos outros grupos continuam sendo publicados. O evento que falhou é tentado de novo no próximo ciclo.
- **Evento que sempre falha**: um evento com falha permanente é tentado a cada ciclo e segura os eventos seguintes do seu grupo, mas não bloqueia os outros. Ainda não há limite de tentativas nem status de erro.
- **Mais de uma réplica**: a busca dos pendentes usa `SELECT ... FOR UPDATE`. Uma segunda réplica espera o lote da primeira terminar e já encontra os eventos marcados.
- **Duplicidade**: se a aplicação cair entre publicar e gravar `published_at`, o evento é publicado de novo. Dentro da janela de 5 minutos, a deduplicação FIFO (`deduplicationId = eventId`) descarta a cópia; depois disso, os consumidores precisam ser idempotentes pelo `eventId`.
- **Retenção**: os eventos publicados continuam na tabela, com `published_at` preenchido. Ainda não há limpeza automática.

### Configuração

| Propriedade                               | Padrão                    | Descrição                                               |
|-------------------------------------------|---------------------------|---------------------------------------------------------|
| `ofisy.messaging.queues.stock`            | `ofisy-stock.fifo`        | Fila de onde o estoque consome os eventos               |
| `ofisy.messaging.topics.stock`            | `ofisy-stock-events.fifo` | Tópico onde os eventos do estoque saem                  |
| `ofisy.messaging.outbox.publish-interval` | `1s`                      | Intervalo entre o fim de um ciclo e o início do próximo |
| `ofisy.messaging.outbox.batch-size`       | `100`                     | Máximo de eventos publicados por ciclo                  |

### Como publicar um novo evento

1. Criar um `record` com o payload do evento.
2. No caso de uso, dentro da transação que muda o estado, chamar `outboxEventRepository.save(OutboxEvent.create("NomeDoEvento", serviceOrderId, payload))`, ou `createWithoutServiceOrder` se o evento não for de uma OS.
3. Cobrir no teste do caso de uso que o evento é gravado e adicionar a linha na tabela de [eventos publicados](#eventos-publicados).

---

## Testes

O `TestcontainersConfiguration` sobe um LocalStack com o mesmo `01-messaging.sh` do compose, então a fila, a DLQ e o tópico são os mesmos do ambiente local. Todos os testes de integração usam essa configuração.

| Teste                                 | O que cobre                                                                                                                                      |
|---------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| `OutboxEventPublisherTest`            | Envelope FIFO, ordem, `published_at`, grupo dos eventos sem OS e o salto do grupo que falhou                                                      |
| `StockEventsListenerTest`             | Despacho pelo `type`, tipo sem handler, falha do handler e `type` duplicado entre handlers                                                       |
| `QuoteCreatedHandlerTest`             | Conversão do envelope e do payload no comando de consumo, campos extras ignorados e rejeição de evento sem OS, payload, `quoteId` ou `stockItems` |
| `ConsumeByQuoteStockServiceTest`      | `StockConsumed` ou `StockConsumptionFailed` conforme o resultado do consumo, registro em `processed_events`, evento repetido e falha sem efeitos |
| `QuoteReprovedHandlerTest`            | Conversão do envelope no comando de devolução                                                                                                    |
| `AllExecutionsCancelledHandlerTest`   | Conversão do envelope no comando de devolução                                                                                                    |
| `RefundByServiceOrderStockServiceTest`| Devolução, `StockRefunded` com e sem itens devolvidos, registro em `processed_events`, evento repetido e falha sem efeitos                       |
| `OutboxEventPublisherIT`              | Eventos com e sem OS gravados no outbox chegam na fila de inspeção e ficam publicados                                                             |
| `StockEventsListenerIT`               | Evento sem handler é confirmado; `QuoteCreated` repetido consome uma vez só e grava um `StockConsumed`; `QuoteCreated` sem saldo grava o `StockConsumptionFailed` e mantém o saldo; evento publicado no tópico do billing chega pela assinatura; `QuoteReproved` devolve os itens e grava o `StockRefunded`; reprovação seguida de cancelamento devolve uma vez só, com o segundo `StockRefunded` vazio; `AllExecutionsCancelled` repetido devolve uma vez só e grava um `StockRefunded`; OS sem consumo gera `StockRefunded` com `items` vazio; evento publicado no tópico do execution chega pela assinatura |

---

## Exercitando o fluxo localmente

Com `make dev-up` e `make run`:

| Comando                                                      | O que faz                                                         |
|--------------------------------------------------------------|-------------------------------------------------------------------|
| `make event-quote-created SERVICE_ORDER_ID=<uuid> STOCK_ID=<uuid> QUANTITY=<n>` | Publica um `QuoteCreated` com um item no tópico do billing, como o billing. Aceita `QUOTE_ID` |
| `make event-quote-reproved SERVICE_ORDER_ID=<uuid>` | Publica um `QuoteReproved` no tópico do billing, como o billing. Aceita `QUOTE_ID` |
| `make event-all-executions-cancelled SERVICE_ORDER_ID=<uuid>` | Publica um `AllExecutionsCancelled` no tópico de execução, como o execution |
| `make db-movements SERVICE_ORDER_ID=<uuid>`                  | Lista as movimentações de uma OS                                  |
| `make events-peek`         | Lista os eventos publicados no tópico, pela fila de inspeção      |
| `make queue-status`        | Mostra quantas mensagens há na fila e na DLQ                      |
| `make dlq-peek`            | Lista as mensagens da DLQ sem removê-las                          |
| `make dlq-redrive`         | Devolve as mensagens da DLQ para a fila principal                 |
| `make dlq-purge`           | Apaga as mensagens da DLQ                                         |
| `make db-outbox`           | Lista os últimos eventos do outbox e se já foram publicados       |
| `make db-processed-events` | Lista os últimos eventos processados                              |

> O LocalStack do estoque usa a porta `4566`, a mesma do core, então só um dos dois pode ficar no ar por vez. Para exercitar a comunicação entre os serviços, todos precisam apontar para o mesmo LocalStack, com os tópicos e as assinaturas de cada um.
