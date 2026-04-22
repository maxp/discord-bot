# Architecture

Этот документ должен объяснять, как устроен бот, без необходимости читать весь код подряд.

## Что здесь нужно держать

- основные namespace и их ответственность;
- поток данных: startup -> gateway connection -> event handling -> command dispatch -> response;
- где проходит граница между Discord transport и бизнес-логикой;
- как организованы обработчики команд и событий;
- как устроены logging, error handling и retry/reconnect behavior.

## Целевая структура

Текущая структура:

- entrypoint приложения;
- gateway transport слой;
- JDA interop слой;
- слой работы с Discord API и REST-вызовами;
- слой команд;
- слой доменной логики;
- слой конфигурации.

Сейчас:

- [src/discord_bot/main.clj](/home/maxp/wrk/discord-bot/src/discord_bot/main.clj) запускает mount-based lifecycle;
- [src/discord_bot/app/core.clj](/home/maxp/wrk/discord-bot/src/discord_bot/app/core.clj) поднимает и останавливает runtime;
- [src/discord_bot/discord/jda.clj](/home/maxp/wrk/discord-bot/src/discord_bot/discord/jda.clj) содержит `JDA` interop, listener registration и command registration;
- [src/discord_bot/discord/http_proxy.clj](/home/maxp/wrk/discord-bot/src/discord_bot/discord/http_proxy.clj) содержит parsing и применение HTTP proxy settings к JDA REST и Gateway/WebSocket transport.

## Критичные вопросы

Этот документ должен отвечать на вопросы:

- где начинается обработка события;
- где создаётся и поддерживается gateway session;
- где регистрируются slash-команды;
- где собираются ответы для Discord;
- где безопасно добавлять новую команду;
- где искать причины reconnect или rate-limit проблем.
