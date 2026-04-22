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
- выбрана версия `JDA` для interop;
- заранее выбраны intents, `USER_INSTALL` и `BOT_DM` как единственный command context.

## Базовые шаги

1. Создать приложение в Discord Developer Portal.
2. Включить bot user и сохранить token в переменную окружения.
3. Настроить нужные intents.
4. Включить `USER_INSTALL` и убедиться, что команды доступны только в `BOT_DM`.
5. Не настраивать `GUILD_INSTALL`, `GUILD` и `PRIVATE_CHANNEL`, если для них нет отдельного документированного основания.
6. Запустить приложение локально и проверить подключение к Discord Gateway.

## Переменные окружения

Минимальный набор нужно зафиксировать после появления runtime-кода:

- `DISCORD_BOT_TOKEN`
- `DISCORD_APPLICATION_ID`
- `DISCORD_PUBLIC_KEY`

Опционально:

- `HTTPS_PROXY` для проксирования Discord REST и Gateway traffic;
- `HTTP_PROXY` как fallback, если `HTTPS_PROXY` не задан.

Локальное окружение проекта хранится в `.env`.
Если появятся дополнительные интеграции, добавлять их сюда, а не держать только в коде.

## Локальный `.env`

- локальные переменные окружения проекта должны храниться в `.env`;
- `.env` является обязательной частью локального окружения и должен присутствовать всегда;
- `.env` предназначен для локальной разработки и не должен попадать в git;
- если проекту нужен шаблон переменных, его стоит хранить отдельно, например в `.env.example`.
- текущий рекомендуемый шаблон переменных уже хранится в `.env.example`.

## Что документировать после реализации

- точную команду локального запуска;
- используемые Clojure aliases;
- как создаётся и инициализируется `JDABuilder` из Clojure;
- способ регистрации slash-команд;
- какие intents реально нужны проекту;
- какие install settings и command contexts реально нужны проекту;
- где хранится локальная конфигурация для разработки.

Текущее состояние:

- приложение подключается к Discord Gateway через `JDA`;
- минимальная команда `/ping` регистрируется как `USER_INSTALL` и `BOT_DM` only;
- если задан `HTTPS_PROXY` или `HTTP_PROXY`, прокси применяется и к JDA REST, и к Gateway/WebSocket transport через [src/discord_bot/discord/http_proxy.clj](/home/maxp/wrk/discord-bot/src/discord_bot/discord/http_proxy.clj);
- listener registration и command registration находятся в [src/discord_bot/discord/jda.clj](/home/maxp/wrk/discord-bot/src/discord_bot/discord/jda.clj).

## Полезные ссылки

- JDA getting started: https://jda.wiki/using-jda/getting-started/
- JDA interactions examples: https://jda.wiki/using-jda/interactions/
- `CommandData` builder API: https://docs.jda.wiki/net/dv8tion/jda/api/interactions/commands/build/CommandData.html
- Discord user-install tutorial: https://docs.discord.com/developers/tutorials/developing-a-user-installable-app
