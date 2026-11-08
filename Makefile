.PHONY: help dev-up dev-down run test verify token \
	event-all-executions-cancelled event-quote-created event-quote-updated event-quote-reproved \
	queue-status dlq-peek dlq-redrive dlq-purge events-peek \
	db-movements db-processed-events db-outbox

LOCALSTACK_CONTAINER ?= ms-ofisy-stock-local-localstack
DB_CONTAINER ?= ms-ofisy-stock-local-db

AWS := docker exec $(LOCALSTACK_CONTAINER) awslocal
QUEUE_NAME := ofisy-stock.fifo
DLQ_NAME := ofisy-stock-dlq.fifo
INSPECT_QUEUE_NAME := ofisy-stock-events-inspect.fifo
# Só é calculado se o target usar e o QUOTE_ID não for informado
QUOTE_ID ?= $(shell od -An -N16 -tx1 /dev/urandom | tr -d ' \n' | sed -E 's/(.{8})(.{4})(.{4})(.{4})(.{12})/\1-\2-\3-\4-\5/')
QUEUE_ARN := arn:aws:sqs:us-east-1:000000000000:$(QUEUE_NAME)
DLQ_ARN := arn:aws:sqs:us-east-1:000000000000:$(DLQ_NAME)
# Vírgula literal dentro do $(if ...), onde ela separaria os argumentos
COMMA := ,

queue_url = $$($(AWS) sqs get-queue-url --queue-name $(1) --query QueueUrl --output text)
psql = set -a && . ./.env && set +a && \
	docker exec $(DB_CONTAINER) psql -U "$$POSTGRES_USER" -d "$$POSTGRES_DB" -c

help:
	@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | awk -F':.*## ' '{printf "  %-22s %s\n", $$1, $$2}'

dev-up: ## Sobe Postgres e LocalStack (compose.db.yaml)
	@docker compose -f compose.db.yaml up -d --wait

dev-down: ## Derruba o ambiente local
	@docker compose -f compose.db.yaml down

run: ## Roda a aplicação com o perfil dev
	@./mvnw spring-boot:run

test: ## Roda os testes unitários
	@./mvnw test

verify: ## Roda testes unitários, de integração e o gate de cobertura
	@./mvnw verify

token: ## Gera um JWT local no formato do core (ROLE obrigatório: ADMIN, ATTENDANT, MECHANIC ou STOCKMAN)
	@ROLE=$(ROLE) EMAIL=$(EMAIL) scripts/generate-token.sh

event-all-executions-cancelled: ## Publica um AllExecutionsCancelled no tópico de execução (SERVICE_ORDER_ID obrigatório, EVENT_ID opcional)
	@test -n "$(SERVICE_ORDER_ID)" || (echo "Informe SERVICE_ORDER_ID=<uuid>" && exit 1)
	@LOCALSTACK_CONTAINER=$(LOCALSTACK_CONTAINER) EVENT_ID=$(EVENT_ID) SERVICE_ORDER_ID=$(SERVICE_ORDER_ID) \
	TOPIC=ofisy-execution-events.fifo TYPE=AllExecutionsCancelled scripts/publish-event.sh

event-quote-created: ## Publica um QuoteCreated com um item no tópico do billing (SERVICE_ORDER_ID, STOCK_ID e QUANTITY obrigatórios, QUOTE_ID e EVENT_ID opcionais)
	@test -n "$(SERVICE_ORDER_ID)" || (echo "Informe SERVICE_ORDER_ID=<uuid>" && exit 1)
	@test -n "$(STOCK_ID)" || (echo "Informe STOCK_ID=<uuid>" && exit 1)
	@test -n "$(QUANTITY)" || (echo "Informe QUANTITY=<quantidade>" && exit 1)
	@LOCALSTACK_CONTAINER=$(LOCALSTACK_CONTAINER) EVENT_ID=$(EVENT_ID) SERVICE_ORDER_ID=$(SERVICE_ORDER_ID) \
	PAYLOAD='{"quoteId":"$(QUOTE_ID)","stockItems":[{"stockId":"$(STOCK_ID)","quantity":$(QUANTITY)}]}' \
	TOPIC=ofisy-billing-events.fifo TYPE=QuoteCreated scripts/publish-event.sh

event-quote-updated: ## Publica um QuoteUpdated no tópico do billing com a lista final de itens (SERVICE_ORDER_ID obrigatório; STOCK_ID e QUANTITY para um item, sem eles a lista vai vazia; QUOTE_ID e EVENT_ID opcionais)
	@test -n "$(SERVICE_ORDER_ID)" || (echo "Informe SERVICE_ORDER_ID=<uuid>" && exit 1)
	@test -z "$(STOCK_ID)" || test -n "$(QUANTITY)" || (echo "Informe QUANTITY=<quantidade> junto com o STOCK_ID" && exit 1)
	@LOCALSTACK_CONTAINER=$(LOCALSTACK_CONTAINER) EVENT_ID=$(EVENT_ID) SERVICE_ORDER_ID=$(SERVICE_ORDER_ID) \
	PAYLOAD='{"quoteId":"$(QUOTE_ID)","stockItems":[$(if $(STOCK_ID),{"stockId":"$(STOCK_ID)"$(COMMA)"quantity":$(QUANTITY)})]}' \
	TOPIC=ofisy-billing-events.fifo TYPE=QuoteUpdated scripts/publish-event.sh

event-quote-reproved: ## Publica um QuoteReproved no tópico do billing (SERVICE_ORDER_ID obrigatório, QUOTE_ID e EVENT_ID opcionais)
	@test -n "$(SERVICE_ORDER_ID)" || (echo "Informe SERVICE_ORDER_ID=<uuid>" && exit 1)
	@LOCALSTACK_CONTAINER=$(LOCALSTACK_CONTAINER) EVENT_ID=$(EVENT_ID) SERVICE_ORDER_ID=$(SERVICE_ORDER_ID) \
	PAYLOAD='{"quoteId":"$(QUOTE_ID)","reason":"Teste local"}' \
	TOPIC=ofisy-billing-events.fifo TYPE=QuoteReproved scripts/publish-event.sh

queue-status: ## Mostra quantas mensagens há na fila e na DLQ
	@for q in $(QUEUE_NAME) $(DLQ_NAME); do \
		url=$$($(AWS) sqs get-queue-url --queue-name $$q --query QueueUrl --output text); \
		$(AWS) sqs get-queue-attributes --queue-url $$url \
			--attribute-names ApproximateNumberOfMessages ApproximateNumberOfMessagesNotVisible \
			--query 'Attributes.[ApproximateNumberOfMessages,ApproximateNumberOfMessagesNotVisible]' --output text \
		| awk -v q=$$q '{printf "  %-28s visíveis=%s  em processamento=%s\n", q, $$1, $$2}'; \
	done

dlq-peek: ## Lista as mensagens da DLQ sem removê-las
	@$(AWS) sqs receive-message --queue-url $(call queue_url,$(DLQ_NAME)) \
		--max-number-of-messages 10 --visibility-timeout 0 --attribute-names All \
		--query 'Messages[].{body:Body,receiveCount:Attributes.ApproximateReceiveCount}' --output json

dlq-redrive: ## Devolve as mensagens da DLQ para a fila principal
	@$(AWS) sqs start-message-move-task --source-arn $(DLQ_ARN) --destination-arn $(QUEUE_ARN) > /dev/null
	@echo "Redrive iniciado. Acompanhe com 'make queue-status'"
	@echo "Atenção: a mensagem volta com o mesmo deduplication id; se faz menos de 5 min do envio original, a fila FIFO a descarta"

dlq-purge: ## Apaga todas as mensagens da DLQ
	@$(AWS) sqs purge-queue --queue-url $(call queue_url,$(DLQ_NAME))
	@echo "DLQ esvaziada"

events-peek: ## Lista os eventos publicados no tópico do ms, pela fila de inspeção local
	@$(AWS) sqs receive-message --queue-url $(call queue_url,$(INSPECT_QUEUE_NAME)) \
		--max-number-of-messages 10 --visibility-timeout 0 \
		--query 'Messages[].Body' --output json

db-movements: ## Lista as movimentações de uma OS (SERVICE_ORDER_ID obrigatório)
	@test -n "$(SERVICE_ORDER_ID)" || (echo "Informe SERVICE_ORDER_ID=<uuid>" && exit 1)
	@$(psql) "select stock_id, movement_type, quantity, previous_quantity, new_quantity, created_at from stock_movements where service_order_id = '$(SERVICE_ORDER_ID)' order by created_at;"

db-processed-events: ## Lista os últimos eventos processados
	@$(psql) "select * from processed_events order by processed_at desc limit 10;"

db-outbox: ## Lista os últimos eventos do outbox e se já foram publicados
	@$(psql) "select event_id, event_type, service_order_id, message_group_id, occurred_at, published_at from outbox_events order by occurred_at desc limit 10;"
