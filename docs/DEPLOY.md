# Deploy no EKS

O ms-ofisy-stock roda em um cluster EKS próprio, com banco RDS próprio, e é exposto pelo API Gateway compartilhado. Os manifestos ficam em `k8s/` e o deploy é feito pelos workflows `cd.yml` e `destroy-app.yml`.

---

## Infraestrutura

A infraestrutura vem de outros repositórios:

- `techchallenge-ofisy-eks-infra` (`eks/stock`): cluster `ms-ofisy-stock-cluster`, ECR `ms-ofisy-stock-ecr` e Datadog Agent;
- `techchallenge-ofisy-db-infra` (`stock`): RDS `ms-ofisy-stock-db`, banco `ms_ofisy_stock_db`.

---

## Manifestos

| Arquivo          | Recurso                                                                                                        |
|------------------|----------------------------------------------------------------------------------------------------------------|
| `configmap.yml`  | `ms-ofisy-stock-config`: host e nome do banco, perfil `k8s`, `JAVA_TOOL_OPTIONS` e variáveis `DD_*`            |
| `secret.yml`     | `ms-ofisy-stock-secret`: usuário e senha do banco, `JWT_SECRET` e `DD_API_KEY`                                 |
| `deployment.yml` | `ms-ofisy-stock-deployment`: container na porta 8080, probes do Actuator e tags do Datadog                     |
| `service.yml`    | `ms-ofisy-stock-service`: `LoadBalancer` que cria um NLB interno na porta 8080                                 |
| `hpa.yml`        | `ms-ofisy-stock-hpa`: de 2 a 4 réplicas, escalando com 50% de CPU                                              |

---

## Pelo workflow (padrão)

Rode o workflow **Deploy Manual na AWS (CD)** (`cd.yml`) informando a branch, tag ou commit. Ele:

1. espera o RDS ficar disponível e busca o endereço dele. Se o banco não existir, o deploy para com erro;
2. instala o Metrics Server, usado pelo HPA;
3. builda a imagem e envia ao ECR com a tag do commit curto. Se a tag já existir, reaproveita a imagem, porque o ECR não aceita sobrescrever tags;
4. substitui os placeholders dos manifestos, aplica no cluster e espera o rollout. A annotation `checksum/config` recebe o hash do ConfigMap e do Secret, então os pods são recriados quando a imagem ou a configuração mudam, mesmo rodando o CD de novo no mesmo commit.

Usa o environment `AWS_ACADEMY` com os secrets `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`, `AWS_ACCOUNT_ID`, `DB_PASSWORD`, `JWT_SECRET` e `DD_API_KEY`.

Para remover a aplicação do cluster (por exemplo, antes de destruir o EKS), rode **Destruir Aplicação no Kubernetes (EKS)** (`destroy-app.yml`).

---

## Exposição pelo API Gateway

O `Service` cria um NLB interno, e o API Gateway do `techchallenge-ofisy-eks-infra` encontra esse NLB pela tag `kubernetes.io/service-name`. Depois do primeiro deploy, altere o `api-gateway/` desse repositório:

1. em `variables.tf`, inclua o serviço no default de `nlb_service_names`:
   ```hcl
   stock = "default/ms-ofisy-stock-service"
   ```
2. em `main.tf`, inclua as rotas do stock em `local.service_routes`. Sem elas, as requisições continuam caindo no `ANY /{proxy+}`, que vai para o core:
   ```hcl
   "ANY /api/v1/stocks"                   = { service = "stock", protected = false }
   "ANY /api/v1/stocks/{proxy+}"          = { service = "stock", protected = false }
   "ANY /api/v1/stock-movements"          = { service = "stock", protected = false }
   "ANY /api/v1/stock-movements/{proxy+}" = { service = "stock", protected = false }
   ```
   `protected = false` porque o JWT é validado pela própria aplicação, e não pelo authorizer de clientes do Gateway;
3. rode o deploy do API Gateway.

Cada vez que o `Service` é recriado (por exemplo, depois do `destroy-app.yml`), o NLB muda e o deploy do API Gateway precisa rodar de novo. Antes de destruir a aplicação sem recriá-la, tire a entrada `stock` de `nlb_service_names`, senão o próximo deploy do Gateway falha ao procurar o NLB.

---

## Manualmente

Os manifestos em `k8s/` têm placeholders que precisam ser substituídos antes do `kubectl apply`:

| Arquivo          | Placeholder                                                                            | Valor                                                                |
|------------------|----------------------------------------------------------------------------------------|----------------------------------------------------------------------|
| `configmap.yml`  | `<RDS_ADDRESS>`                                                                        | Endereço do RDS `ms-ofisy-stock-db`                                  |
| `configmap.yml`  | `<IMAGE_TAG>`                                                                          | Tag da imagem, usada como `DD_VERSION`                               |
| `deployment.yml` | `<IMAGE_TAG>` (label `tags.datadoghq.com/version`)                                     | Mesma tag da imagem                                                  |
| `deployment.yml` | `<ECR_REPOSITORY_URL>:<IMAGE_TAG>`                                                     | `<account>.dkr.ecr.us-east-1.amazonaws.com/ms-ofisy-stock-ecr:<tag>` |
| `deployment.yml` | `<CONFIG_HASH>` (annotation `checksum/config`)                                         | SHA-256 do `configmap.yml` e do `secret.yml` já substituídos         |
| `secret.yml`     | `<BASE64_USERNAME>`, `<BASE64_PASSWORD>`, `<BASE64_JWT_SECRET>`, `<BASE64_DD_API_KEY>` | Valores em base64                                                    |

```bash
aws eks update-kubeconfig --region us-east-1 --name ms-ofisy-stock-cluster
kubectl apply -f k8s/secret.yml -f k8s/configmap.yml -f k8s/deployment.yml -f k8s/service.yml -f k8s/hpa.yml
kubectl rollout status deployment/ms-ofisy-stock-deployment --timeout=300s
```

O acesso ao SNS/SQS usa as credenciais da `LabRole` dos nós, sem chaves no manifesto. O HPA depende do Metrics Server no cluster (o workflow instala); sem ele, o deployment fica nas 2 réplicas mínimas.

---

## Observabilidade (Datadog)

- **Tracer**: a imagem inclui o `dd-java-agent` com versão e checksum fixos. Ele só é ativado no k8s, pelo `JAVA_TOOL_OPTIONS` do ConfigMap, e envia os traces ao Agent do nó (`DD_AGENT_HOST` recebe o IP do host). Com `DD_TRACE_OTEL_ENABLED=true`, o trace da requisição também segue pelo outbox até a publicação no SNS ([MESSAGING.md](MESSAGING.md#rastreamento-distribuído-pelo-outbox));
- **Logs**: no perfil `k8s`, o `logback-spring.xml` escreve em JSON com os campos `dd.trace_id` e `dd.span_id`, o que liga cada log ao trace. Nos demais perfis o log continua no console padrão;
- **Métricas**: o Micrometer exporta para o Datadog quando `DD_METRICS_ENABLED=true` (só no ConfigMap do k8s), com as tags `service` e `env`;
- **Health check**: a annotation `ad.datadoghq.com/ms-ofisy-stock-app.checks` configura um `http_check` em `/actuator/health`.
