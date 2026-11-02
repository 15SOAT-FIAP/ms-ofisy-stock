# Guia de Testes

## Pré-requisitos

- Docker rodando localmente. Os testes que sobem o contexto do Spring usam **Testcontainers** para criar um PostgreSQL e um LocalStack reais, sem precisar do `compose`.

---

## Executando

| Comando                         | O que roda                                                                  |
|---------------------------------|-----------------------------------------------------------------------------|
| `make test` / `./mvnw test`     | Testes unitários (Surefire)                                                 |
| `make verify` / `./mvnw verify` | Unitários + integração (Failsafe) + relatório e gate de cobertura do JaCoCo |

A separação é feita pelo nome: classes terminadas em `IT` são excluídas do Surefire e executadas pelo Failsafe.

> O `MsOfisyStockApplicationTests` (context load) roda no Surefire, mas também usa Testcontainers. Por isso o Docker é necessário inclusive para `make test`.

---

## Estrutura dos testes

| Camada       | Testes                                                                                       | Tipo                                                                        |
|--------------|----------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------|
| Domínio      | `StockTest`, `StockMovementTest`, `ProcessedEventTest`, `OutboxEventTest`                    | Unitário                                                                    |
| Casos de uso | Um `*ServiceTest` por caso de uso em `application/`                                          | Unitário, com repositórios e casos de uso dependentes mockados             |
| Casos de uso | `ConsumeByServiceOrderStockServiceIT`, `ReleaseByServiceOrderStockServiceIT`                 | Integração (PostgreSQL real)                                                |
| Gateways     | `*MapperTest`, `*RepositoryImplTest`                                                         | Unitário                                                                    |
| Listeners    | `StockEventsListenerTest`                                                                    | Unitário                                                                    |
| Listeners    | `StockEventsListenerIT`                                                                      | Integração (SQS real)                                                       |
| Publishers   | `OutboxEventPublisherTest`                                                                   | Unitário, com SNS e JPA mockados                                            |
| Publishers   | `OutboxEventPublisherIT`                                                                     | Integração (SNS + SQS + PostgreSQL reais)                                   |
| Controllers  | `StockControllerTest`, `StockMovementControllerTest`                                         | `@WebMvcTest` com `MockMvc` e casos de uso mockados                         |
| Controllers  | `StockControllerIT`, `StockMovementControllerIT`                                             | Integração com RestAssured (aplicação em porta aleatória + PostgreSQL real) |
| Segurança    | `JwtServiceTest`, `JwtAuthFilterTest`                                                        | Unitário                                                                    |
| Aplicação    | `MsOfisyStockApplicationTests`                                                               | Context load                                                                |

### Testes de integração

O `StockControllerIT` sobe a aplicação em porta aleatória e chama a API com **RestAssured**, como os `*ControllerIT` do core. Cenários cobertos:

- listar usa 10 por página por padrão e respeita o `size` informado;
- buscar por id devolve um item da massa do V3 e `404` se não existir; buscar por nome do produto devolve o item;
- cadastrar persiste o item, devolve `409` se o nome já existir e `400` se faltar campo obrigatório;
- dar entrada soma a quantidade e registra uma movimentação `IN`, e uma quantidade não positiva devolve `400`;
- consumir baixa a quantidade e registra uma movimentação `OUT`, e um consumo maior que o saldo devolve `409` sem alterar o saldo;
- atualizar muda só os campos informados e devolve `409` se o novo nome for de outro item;
- autorização: sem token, com token assinado por outra chave, expirado ou emitido para cliente (`techchallenge-ofisy-auth`) devolve `401`; role sem permissão ou token sem role devolve `403`, inclusive um `MECHANIC` tentando consumir, sem alterar o saldo; as roles autorizadas recebem `200`; o Swagger e o `/actuator/health` respondem sem token.

O `StockMovementControllerIT` cobre a listagem de todas as movimentações, a listagem por item (só as do item, ou página vazia se ele não tiver movimentações) e a autorização: `401` sem token, `403` para `MECHANIC` e `ATTENDANT` e `200` para `ADMIN` e `STOCKMAN`.

Todas as chamadas autenticadas enviam um token gerado pelo `JwtTestTokens`, assinado com o `jwt.secret` passado no `@SpringBootTest(properties = ...)`. Os `*ControllerTest` (`@WebMvcTest`) seguem o core: rodam com `addFilters = false` e `@WithMockUser`, então as regras de role são verificadas só nos IT.

O `ConsumeByServiceOrderStockServiceIT` valida no PostgreSQL real o consumo dos itens de uma OS:

- com saldo em todos os itens, baixa cada um e grava as movimentações com o `service_order_id`;
- se faltar saldo em um item, nenhum saldo muda, nenhuma movimentação é criada e o retorno traz o item faltante.

O `ReleaseByServiceOrderStockServiceIT` valida a devolução por OS:

- devolve o que foi consumido e grava a movimentação `IN` com o `service_order_id`;
- repetir a devolução não devolve os itens de novo;
- a devolução de uma OS não mexe no que foi consumido por outra.

O `OutboxEventPublisherIT` grava eventos pelo `OutboxEventRepository`, com e sem OS, e espera recebê-los na fila de inspeção `ofisy-stock-events-inspect.fifo`, assinada no tópico do ms. Ele confere o envelope recebido e que o registro foi marcado como publicado.

O `StockEventsListenerIT` envia direto na fila do LocalStack um evento sem handler e usa **Awaitility** para esperar que ele seja confirmado e saia da fila.

Como o banco é compartilhado entre os testes e tem a massa do V3, cada cenário cria os próprios itens com nomes e IDs aleatórios e só faz asserções sobre eles.

---

## Testcontainers

A configuração fica em `TestcontainersConfiguration` e é importada com `@Import(TestcontainersConfiguration.class)`:

- **PostgreSQL** via `@ServiceConnection`: o datasource é apontado para o container e o Flyway aplica as migrations ao subir o contexto.
- **LocalStack** (`sns`, `sqs`) via `@ServiceConnection`: o container recebe uma cópia do `localstack/init/ready.d/01-messaging.sh`, então a fila, a DLQ e o tópico são **os mesmos do ambiente local**. O container só é considerado pronto quando o init script termina.

### Rodando a aplicação com os containers de teste

O `TestMsOfisyStockApplication` (em `src/test`) sobe a aplicação usando os containers do Testcontainers no lugar do `compose`, o que é útil para depurar pela IDE sem precisar do `make dev-up`.

---

## Cobertura (JaCoCo)

- O relatório é gerado no `verify` em `target/site/jacoco/index.html`.
- O build **falha** se a cobertura do projeto ficar abaixo de **80% de linhas** ou **80% de branches**.
- Ficam fora da contagem: `MsOfisyStockApplication` e o pacote `config`.

---

## CI

O workflow `.github/workflows/ci.yml` roda `mvn -B verify` a cada push e Pull Request na `master`, com o mesmo gate de cobertura. O relatório do JaCoCo é publicado como artefato `jacoco-report` da execução, mesmo quando o build falha.
