.PHONY: up down restart ps logs clean

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
