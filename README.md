# ms-ofisy-stock

FIAP 15SOAT - Tech Challenge - Grupo 81

Microsserviço responsável pelo **estoque de peças e insumos** da Ofisy. Ele mantém o cadastro dos itens, o saldo de cada um e o histórico de movimentações de entrada e saída, e expõe uma API REST para consulta e manutenção do estoque.

Na saga do orçamento, o estoque consome os itens de uma ordem de serviço quando o orçamento é criado, ajusta a diferença quando ele é editado e os devolve quando o orçamento é reprovado ou a OS é cancelada. Tudo isso acontece por eventos: o estoque consome o `QuoteCreated`, o `QuoteUpdated`, o `QuoteReproved` e o `AllExecutionsCancelled`, e responde com o `StockConsumed`, o `StockConsumptionFailed` e o `StockRefunded`. Também avisa quando um item chega ao estoque mínimo, com o `StockLow` ([MESSAGING.md](docs/MESSAGING.md)).

O contexto de estoque foi extraído do core da Ofisy ([techchallenge-ofisy](https://github.com/15SOAT-FIAP/techchallenge-ofisy)), que continua existindo enquanto alguns contextos são estrangulados para microsserviços. Este ms participa da saga coordenada por coreografia, e as decisões por trás dessa arquitetura estão registradas nos ADRs do core:

- [ADR-0010 - Dividir a aplicação em microsserviços por bounded context](https://github.com/15SOAT-FIAP/techchallenge-ofisy/blob/master/docs/adr/0010-dividir-a-aplicacao-em-microsservicos-por-bounded-context.md)
- [ADR-0011 - Utilizar SNS e SQS como mensageria](https://github.com/15SOAT-FIAP/techchallenge-ofisy/blob/master/docs/adr/0011-utilizar-sns-e-sqs-como-mensageria.md)
- [ADR-0012 - Coordenar a saga por coreografia](https://github.com/15SOAT-FIAP/techchallenge-ofisy/blob/master/docs/adr/0012-coordenar-a-saga-por-coreografia.md)

---

## Estrutura do Projeto

O código segue a mesma organização em camadas do core: **Clean Architecture** com conceitos de **DDD**. Os pacotes de domínio, aplicação e adaptadores são divididos por agregado (`stock` e `stockmovement`), mais o `messaging` para a infraestrutura de eventos.

```
br.com.ofisy.ms_ofisy_stock
├── domain/                       # Entidades, portas de repositório e exceções de domínio
│   ├── stock/                        # Estoque e regras de saldo
│   ├── stockmovement/                # Movimentações de entrada e saída
│   └── messaging/                    # Evento do outbox e evento processado
├── application/                  # Casos de uso
│   ├── stock/<caso-de-uso>/          # Interface do UseCase + Service com a implementação
│   └── stockmovement/<caso-de-uso>/
├── adapters/                     # Conexão do domínio/aplicação com o mundo externo
│   ├── controllers/                  # Adaptadores de entrada: API REST e DTOs
│   ├── presenters/                   # Conversão do domínio para os DTOs de resposta
│   ├── gateways/                     # Adaptadores de saída: Entity JPA, Mapper, RepositoryImpl e interface Spring Data
│   ├── publishers/                   # Adaptadores de saída: publicação do outbox no SNS
│   └── listeners/                    # Adaptadores de entrada: consumo da fila SQS
│       └── dto/                          # Envelope do evento
├── config/                       # Configurações do Spring, segurança, JWT, Swagger, SNS e tratamento global de erros
└── shared/                       # Validação do JWT (JwtService) e filtro de autenticação (JwtAuthFilter)
```

---

## Stack do Projeto

| Tecnologia       | Versão   | Uso                                                                |
|------------------|----------|--------------------------------------------------------------------|
| Java             | 21 (LTS) | Linguagem da aplicação                                             |
| Spring Boot      | 4.1.1    | Framework base                                                     |
| Spring Cloud AWS | 4.2.0    | Integração com o Amazon SNS e o Amazon SQS                         |
| PostgreSQL       | 16       | Banco de dados do estoque                                          |
| LocalStack       | 4.13.1   | Emulação de SNS e SQS no ambiente local e nos testes de integração |
| Maven            | 3.9.x    | Build e gerenciamento de dependências                              |
| Docker           | —        | Containerização da aplicação e dos serviços dependentes            |

---

## Dependências

| Dependência              | Descrição                                                                    | Artefato                                                                                                                  |
|--------------------------|------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| Spring Cloud AWS SQS/SNS | Consumo da fila via `@SqsListener` e publicação dos eventos do outbox no SNS | `spring-cloud-aws-starter-sqs`, `spring-cloud-aws-starter-sns`                                                            |
| Spring Web MVC           | Servidor web e API REST                                                      | `spring-boot-starter-webmvc`                                                                                              |
| OpenAPI / Swagger        | Documentação da API e Swagger UI                                             | `springdoc-openapi-starter-webmvc-ui`                                                                                     |
| Spring Security          | Autorização das rotas por role                                               | `spring-boot-starter-security`                                                                                            |
| JJWT                     | Validação do JWT emitido pelo core                                           | `jjwt-api`, `jjwt-impl`, `jjwt-jackson`                                                                                   |
| Spring Data JPA          | Persistência e mapeamento ORM                                                | `spring-boot-starter-data-jpa`                                                                                            |
| Spring Validation        | Validação dos DTOs de entrada                                                | `spring-boot-starter-validation`                                                                                          |
| Spring Actuator          | Endpoint de saúde (`/actuator/health`)                                       | `spring-boot-starter-actuator`                                                                                            |
| Flyway                   | Versionamento do schema, com suporte ao dialeto do PostgreSQL                | `spring-boot-starter-flyway`, `flyway-database-postgresql`                                                                |
| PostgreSQL               | Driver JDBC                                                                  | `postgresql`                                                                                                              |
| Docker Compose           | Sobe o `compose.db.yaml` automaticamente ao rodar a aplicação localmente     | `spring-boot-docker-compose`                                                                                              |
| Lombok                   | Redução de código boilerplate                                                | `lombok`                                                                                                                  |
| Spring Boot DevTools     | Reinício automático durante o desenvolvimento                                | `spring-boot-devtools`                                                                                                    |
| Testcontainers           | PostgreSQL e LocalStack reais nos testes de integração                       | `spring-boot-testcontainers`, `testcontainers-postgresql`, `testcontainers-localstack`, `spring-cloud-aws-testcontainers` |
| Awaitility               | Asserções sobre o processamento assíncrono da fila e do outbox               | `awaitility`                                                                                                              |
| REST Assured             | Testes de integração da API contra a aplicação em porta aleatória            | `rest-assured`                                                                                                            |
| Spring Security Test     | Usuário simulado nos testes `@WebMvcTest`                                    | `spring-security-test`                                                                                                    |
| JaCoCo                   | Relatório de cobertura e gate mínimo de 80%                                  | `jacoco-maven-plugin`                                                                                                     |

---

## Arquivos de Configuração

### Aplicação e Build

- `application.yaml`: configuração base (datasource em `localhost:5434`, Flyway, região AWS, fila, tópico e outbox). O perfil ativo por padrão é `dev`;
- `application-dev.yaml`: aponta o SDK da AWS para o LocalStack em `localhost:4566`;
- `application-docker.yaml`: configuração usada quando a aplicação roda dentro do `compose.yaml`;
- `application-k8s.yaml`: configuração usada no EKS (RDS via `POSTGRES_HOST`, probes do Kubernetes e AWS real);
- `logback-spring.xml`: logs em JSON para o Datadog no perfil `k8s` e console padrão nos demais;
- `pom.xml`: dependências e plugins do Maven.

### Docker

- `Dockerfile`: build multi-stage da aplicação. A imagem inclui o `dd-java-agent` do Datadog, ativado só no k8s via `JAVA_TOOL_OPTIONS`;
- `compose.yaml`: ambiente completo (PostgreSQL, LocalStack e aplicação);
- `compose.db.yaml`: apenas PostgreSQL e LocalStack, para rodar a aplicação na IDE.

### Kubernetes

- `k8s/`: manifestos do deploy no EKS (`configmap`, `secret`, `deployment`, `service` e `hpa`). Ver [DEPLOY.md](docs/DEPLOY.md).

### Mensageria e utilitários locais

- `localstack/init/ready.d/01-messaging.sh`: cria a fila, a DLQ, o tópico e a fila de inspeção no LocalStack. É o mesmo script usado nos testes de integração;
- `scripts/generate-token.sh`: gera um JWT local no mesmo formato do core (`make token ROLE=...`), para testar a API sem subir o core;
- `Makefile`: atalhos para subir o ambiente, rodar os testes, gerar token, inspecionar a fila e a DLQ e consultar o outbox (`make help`).

### Git e Ambiente

- `.gitignore` e `.gitattributes`;
- `.env`: variáveis sensíveis. **Não é versionado**;
- `env.example`: modelo do `.env`.

### CI/CD (GitHub Actions)

- `.github/workflows/ci.yml`: build, testes e gate de cobertura (`mvn verify`) a cada push e Pull Request na `master`;
- `.github/workflows/cd.yml`: deploy manual no EKS ([DEPLOY.md](docs/DEPLOY.md));
- `.github/workflows/destroy-app.yml`: remove manualmente os recursos da aplicação do cluster;
- `.github/workflows/notify-pr.yml`: notifica abertura e fechamento de Pull Requests no Discord.

---

## Configuração do ambiente para execução

### 1. Clone o repositório

```bash
git clone git@github.com:15SOAT-FIAP/ms-ofisy-stock.git
```

### 2. Crie o arquivo `.env`

```bash
cp env.example .env
```

---

## Subindo a aplicação

### Opção 1: desenvolvimento local (dependências no Docker + aplicação na IDE)

```bash
make dev-up   # PostgreSQL em localhost:5434 e LocalStack em localhost:4566
make run      # aplicação com o perfil dev em localhost:8080
```

### Opção 2: ambiente completo via Docker

```bash
docker compose up --build
```

A aplicação fica disponível em `http://localhost:8082`.

O LocalStack usa a porta `4566`, a mesma do core, então só um dos dois pode ficar no ar por vez.

### Health check

- `GET /actuator/health`

### API

A documentação interativa fica no Swagger UI, em `http://localhost:8080/swagger-ui.html` (ou na porta `8082` pelo `compose.yaml`).

**Estoque** (`/api/v1/stocks`)

| Método | Rota                          | Descrição                                                                                                       | Roles                        |
|--------|-------------------------------|-----------------------------------------------------------------------------------------------------------------|------------------------------|
| `GET`  | `/`                           | Lista os itens, paginado (`page`, `size`, `sort`; 10 por página por padrão)                                     | `ADMIN`, `STOCKMAN`, `MECHANIC` |
| `GET`  | `/{id}`                       | Busca um item. Responde `404` se não existir                                                                    | `ADMIN`, `STOCKMAN`, `MECHANIC` |
| `GET`  | `/productName/{productName}`  | Busca um item pelo nome do produto. Responde `404` se não existir                                               | `ADMIN`, `STOCKMAN`, `MECHANIC` |
| `POST` | `/`                           | Cadastra um item. Responde `201`, `400` se algum campo for inválido e `409` se o nome do produto já existir     | `ADMIN`, `STOCKMAN`          |
| `PUT`  | `/{id}`                       | Atualiza só os campos informados (a quantidade não muda por aqui). Responde `404` ou `409` se o nome já existir | `ADMIN`, `STOCKMAN`          |
| `POST` | `/{id}/add?quantity={n}`      | Dá entrada no saldo e registra uma movimentação `IN`. Responde `400` se a quantidade não for positiva           | `ADMIN`, `STOCKMAN`          |
| `POST` | `/{id}/consume?quantity={n}`  | Baixa o saldo e registra uma movimentação `OUT`. Responde `409` se o saldo for insuficiente                     | `ADMIN`, `STOCKMAN`          |

**Movimentações** (`/api/v1/stock-movements`)

| Método | Rota                | Descrição                                       | Roles               |
|--------|---------------------|-------------------------------------------------|---------------------|
| `GET`  | `/`                 | Lista todas as movimentações, paginado          | `ADMIN`, `STOCKMAN` |
| `GET`  | `/?stockId={id}`    | Lista as movimentações de um item, paginado     | `ADMIN`, `STOCKMAN` |

As movimentações não são criadas pela API de movimentações: elas são registradas pelos casos de uso que mexem no saldo. As que vêm da saga levam o `serviceOrderId` da OS.

Todas as rotas exigem um JWT emitido pelo core. Sem token válido a resposta é `401`, e com uma role não autorizada é `403` ([SECURITY.md](docs/SECURITY.md)). Para testar localmente, gere um token com `make token ROLE=STOCKMAN`.

Erros seguem o formato `ProblemDetail` (RFC 9457), com `title` e `detail`.

Para ver todos os comandos disponíveis, rode `make help`.

---

## Variáveis de ambiente

| Variável                           | Descrição                                                    |
|------------------------------------|--------------------------------------------------------------|
| `POSTGRES_DB`                      | Nome do banco de dados                                       |
| `POSTGRES_USER`                    | Usuário do banco                                             |
| `POSTGRES_PASSWORD`                | Senha do banco                                               |
| `JWT_SECRET`                       | Chave HS256 para validar o JWT. Precisa ser a mesma do core  |
| `POSTGRES_HOST`                    | Host do banco no perfil `k8s`                                |
| `DD_METRICS_ENABLED`, `DD_API_KEY` | Exportação de métricas para o Datadog (desligada por padrão) |

---

## Documentação Adicional

### Segurança
- **[Autenticação e autorização](docs/SECURITY.md)**: contrato do JWT emitido pelo core, roles por rota, respostas 401/403 e como gerar um token local

### Mensageria
- **[Mensageria](docs/MESSAGING.md)**: topologia SNS/SQS, contrato dos eventos, outbox, idempotência, DLQ e como exercitar o fluxo localmente

### Banco de Dados
- **[Banco de Dados](docs/DATABASE.md)**: tabelas, constraints e índices
- **[Guia de Flyway](docs/FLYWAY.md)**: como criar e executar migrations

### Deploy
- **[Deploy no EKS](docs/DEPLOY.md)**: manifestos, workflows de CD e remoção, exposição pelo API Gateway, deploy manual e Datadog

### Qualidade e Testes
- **[Guia de Testes](docs/TESTING.md)**: testes unitários e de integração, Testcontainers e cobertura
