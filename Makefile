.PHONY: help dev-up dev-down run test verify token

help:
	@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | awk -F':.*## ' '{printf "  %-22s %s\n", $$1, $$2}'

dev-up: ## Sobe o Postgres local (compose.db.yaml)
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
