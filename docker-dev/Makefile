COMPOSE ?= docker compose

.PHONY: help init build up watch down logs logs-builder restart psql shell reset-db reset-all ps mail-settings mail-test mail-dev

help:
	@echo "make init        - create .env from .env.example"
	@echo "make build       - full Maven build of the monorepo (first run, 20-40 min)"
	@echo "make up          - start postgres + install/demo data + backend"
	@echo "make watch       - start the builder in watch mode (own terminal)"
	@echo "make logs        - follow backend logs"
	@echo "make logs-builder- follow builder logs"
	@echo "make restart     - restart the backend container"
	@echo "make psql        - open psql on the dev database"
	@echo "make down        - stop everything"
	@echo "make reset-db    - wipe the database and re-run install + demo data"
	@echo "make reset-all   - wipe database, build output and the Maven cache"
	@echo "make mail-settings - push .env Mailtrap settings into the running backend"
	@echo "make mail-test   - send a test email through Mailtrap"
	@echo "make mail-dev TO=you@example.com - call the custom /api/dev/mail/test endpoint"

init:
	@test -f .env || cp .env.example .env
	@echo ".env ready - put your Mailtrap password in it"

build:
	$(COMPOSE) run --rm builder full

up:
	$(COMPOSE) up -d postgres
	$(COMPOSE) up tb-init
	$(COMPOSE) up -d thingsboard
	@echo "API:     http://localhost:8080"
	@echo "Swagger: http://localhost:8080/swagger-ui.html"
	@echo "WS:      ws://localhost:8080/api/ws"

watch:
	$(COMPOSE) up builder

logs:
	$(COMPOSE) logs -f thingsboard

logs-builder:
	$(COMPOSE) logs -f builder

restart:
	$(COMPOSE) restart thingsboard

ps:
	$(COMPOSE) ps

psql:
	$(COMPOSE) exec postgres psql -U postgres -d thingsboard

shell:
	$(COMPOSE) exec thingsboard bash

down:
	$(COMPOSE) down

reset-db:
	$(COMPOSE) down
	docker volume rm -f tb-dev-postgres-data tb-dev-data
	@echo "database wiped - next 'make up' reinstalls the schema and demo data"

mail-settings:
	./scripts/test-mail.sh settings

mail-test:
	./scripts/test-mail.sh

mail-dev:
	./scripts/test-mail.sh dev $(TO)

reset-all: reset-db
	docker volume rm -f tb-dev-build tb-dev-m2-cache
