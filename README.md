# Discord echo bot in Clojure

This repository contains a Discord bot project in Clojure.

## Quick Start

- Требуются установленные `make` и `clojure` CLI.
- Локальный файл `.env` обязателен; стартовые имена переменных см. в `.env.example`.
- `make run`: запускает текущий entrypoint приложения.
- `make dev`: поднимает проект в dev mode.
- `make test`: запускает тесты.
- `make lint`: запускает `clj-kondo` для `src` и `test`.
- `make outdated`: проверяет устаревшие зависимости через `antq`.
- `make repl`: алиас для `make dev`.
- `make tunnel-up`: поднимает SSH reverse tunnel к `vsp` (порт `8131`).
- `make tunnel-down`: останавливает SSH tunnel.
- `make tunnel-status`: проверяет статус SSH tunnel.

## Documentation

- [docs/project-rules.md](docs/project-rules.md): базовые технические правила проекта.
- [docs/prompting.md](docs/prompting.md): рекомендации по промптам для Clojure-задач.
- [docs/setup.md](docs/setup.md): локальный запуск и настройка окружения.
- [docs/architecture.md](docs/architecture.md): устройство проекта и поток обработки событий.
- [docs/discord-api.md](docs/discord-api.md): project-specific заметки по Discord API.
- [docs/commands.md](docs/commands.md): каталог команд бота.
- [docs/deployment.md](docs/deployment.md): деплой и операционные заметки.
- [docs/troubleshooting.md](docs/troubleshooting.md): типовые сбои и способы диагностики.
- [docs/decisions/0001-library-choice.md](docs/decisions/0001-library-choice.md): ADR по выбору библиотеки.
- [docs/decisions/0002-interactions-over-message-content.md](docs/decisions/0002-interactions-over-message-content.md): ADR по выбору interactions как основного интерфейса.

## Stack Notes

- Discord runtime строится на `JDA` через Java interop из Clojure.
- Runtime подключается к Discord Gateway через `JDA`.
