.PHONY: up down restart ps logs clean backup-db restore-db

up:
	docker compose --env-file .env up -d

down:
	docker compose down

restart:
	docker compose down && docker compose --env-file .env up -d

ps:
	docker compose ps

logs:
	docker compose logs -f --tail=100

clean:
	docker compose down -v

backup-db:
	./scripts/postgres-backup.sh

restore-db:
	@if [ -z "$(FILE)" ]; then echo "Usage: make restore-db FILE=backups/postgres/<file.sql.gz>"; exit 1; fi
	./scripts/postgres-restore.sh "$(FILE)"
