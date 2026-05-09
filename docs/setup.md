# Setup

Этот документ описывает локальный запуск Discord-бота и базовую настройку окружения.

## Prerequisites

Для локальной работы с проектом должны быть установлены:

- `make`;
- Clojure CLI (`clojure`);
- POSIX-совместимый shell (`/bin/sh`) для выполнения команд из `Makefile`.

Отдельно устанавливать не нужно:

- `clj-kondo`, так как `make lint` запускает его через alias в `deps.edn`;
- `antq`, так как `make outdated` запускает его через alias в `deps.edn`.

## Что должно быть готово

- установлен Clojure CLI;
- создано приложение в Discord Developer Portal;
- создан bot user для приложения;
- получен bot token;
- выбрана версия `JDA` для interop.

## Базовые шаги

1. Создать приложение в Discord Developer Portal.
2. Включить bot user и сохранить token в переменную окружения.
3. Настроить intents под текущий сценарий запуска.
4. Запустить приложение локально и проверить подключение к Discord Gateway.

## Переменные окружения

Текущий runtime читает переменные через [src/discord_bot/config.clj](/home/maxp/wrk/discord-bot/src/discord_bot/config.clj):

- `DISCORD_BOT_TOKEN` — токен bot user.
- `DISCORD_APP_ID` — ID приложения (используется как OAuth2 `client_id`).
- `DISCORD_APP_SECRET` — секрет приложения (используется как OAuth2 `client_secret`).
- `DISCORD_CALLBACK_URL` — публичный redirect URI для OAuth2 callback (например, `https://vsp.isgood.host/discord/callback`).

Опционально:

- `DISCORD_PROXY_URL` для проксирования Discord REST и Gateway traffic через `http` или `https` URL.
- `DISCORD_TIMEOUT` для HTTP/WebSocket timeout в секундах, значение по умолчанию `20`.
- `DISCORD_CALLBACK_HOST` для адреса, на котором слушает callback HTTP-сервер (default: `localhost`).
- `DISCORD_CALLBACK_PORT` для порта callback HTTP-сервера (default: `8131`).

Локальное окружение проекта хранится в `.env`.
Если появятся дополнительные интеграции, добавлять их сюда, а не держать только в коде.

## Локальный `.env`

- локальные переменные окружения проекта должны храниться в `.env`;
- `.env` является обязательной частью локального окружения и должен присутствовать всегда;
- `.env` предназначен для локальной разработки и не должен попадать в git;
- если проекту нужен шаблон переменных, его стоит хранить отдельно, например в `.env.example`.
- текущий рекомендуемый шаблон переменных уже хранится в `.env.example`.

## Что еще нужно документировать после реализации

- какие intents реально нужны проекту;
- какие install settings и command contexts реально нужны проекту;
- где будет жить основной dispatch входящих событий.

Текущее состояние:

- приложение подключается к Discord Gateway через `JDA`;
- [Makefile](/home/maxp/wrk/discord-bot/Makefile) является основным интерфейсом для `run`, `dev`, `test`, `lint` и `outdated`;
- [deps.edn](/home/maxp/wrk/discord-bot/deps.edn) содержит aliases `:dev`, `:test`, `:test-run`, `:lint` и `:outdated`;
- `JDABuilder` создается в [src/discord_bot/discord/jda.clj](/home/maxp/wrk/discord-bot/src/discord_bot/discord/jda.clj);
- текущий listener принимает `MessageReceivedEvent` и `ButtonInteractionEvent` и передает данные в scaffold-handlers из [src/discord_bot/app/core.clj](/home/maxp/wrk/discord-bot/src/discord_bot/app/core.clj);
- если задан `DISCORD_PROXY_URL`, прокси применяется и к JDA REST, и к Gateway/WebSocket transport через [src/discord_bot/discord/proxy.clj](/home/maxp/wrk/discord-bot/src/discord_bot/discord/proxy.clj);
- прямые Discord REST-вызовы `oauth2/token` и `users/@me` находятся в [src/discord_bot/discord/api.clj](/home/maxp/wrk/discord-bot/src/discord_bot/discord/api.clj) и получают OAuth2 client credentials через аргументы вызова, а не через глобальный config;
- listener registration находится в [src/discord_bot/discord/jda.clj](/home/maxp/wrk/discord-bot/src/discord_bot/discord/jda.clj).

## Полезные ссылки

- JDA getting started: https://jda.wiki/using-jda/getting-started/
- JDA interactions examples: https://jda.wiki/using-jda/interactions/
- Discord user-install tutorial: https://docs.discord.com/developers/tutorials/developing-a-user-installable-app
