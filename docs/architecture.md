# Architecture

Этот документ должен объяснять, как устроен бот, без необходимости читать весь код подряд.

## Что здесь нужно держать

- основные namespace и их ответственность;
- поток данных: startup -> gateway connection -> event handling -> response;
- где проходит граница между Discord transport и бизнес-логикой;
- как организованы обработчики событий;
- как устроены logging, error handling и retry/reconnect behavior.

## Целевая структура

Целевая структура:

- entrypoint приложения;
- gateway transport слой;
- JDA interop слой;
- слой работы с Discord API и REST-вызовами;
- слой доменной логики;
- слой конфигурации.

Сейчас:

- [src/discord_bot/main.clj](/home/maxp/wrk/discord-bot/src/discord_bot/main.clj) запускает mount-based lifecycle;
- [src/discord_bot/app/core.clj](/home/maxp/wrk/discord-bot/src/discord_bot/app/core.clj) поднимает и останавливает runtime;
- [src/discord_bot/app/auth.clj](/home/maxp/wrk/discord-bot/src/discord_bot/app/auth.clj) содержит прикладной OAuth2 flow: code exchange, загрузку user info и welcome DM;
- [src/discord_bot/app/dispatch.clj](/home/maxp/wrk/discord-bot/src/discord_bot/app/dispatch.clj) содержит scaffold-dispatch для message и button событий;
- [src/discord_bot/app/handlers.clj](/home/maxp/wrk/discord-bot/src/discord_bot/app/handlers.clj) собирает обработчики runtime с уже связанными зависимостями;
- [src/discord_bot/discord/jda.clj](/home/maxp/wrk/discord-bot/src/discord_bot/discord/jda.clj) содержит `JDA` interop, listener registration и helper для отправки DM;
- [src/discord_bot/discord/callback.clj](/home/maxp/wrk/discord-bot/src/discord_bot/discord/callback.clj) содержит HTTP-сервер для обработки OAuth2 callback на порту `8131` и не знает о runtime config или Discord REST;
- [src/discord_bot/discord/rest.clj](/home/maxp/wrk/discord-bot/src/discord_bot/discord/rest.clj) содержит общий OkHttp client и общие REST helper-функции;
- [src/discord_bot/discord/oauth.clj](/home/maxp/wrk/discord-bot/src/discord_bot/discord/oauth.clj) содержит OAuth2 authorize URL и token exchange;
- [src/discord_bot/discord/users.clj](/home/maxp/wrk/discord-bot/src/discord_bot/discord/users.clj) содержит user-related Discord REST вызовы;
- [src/discord_bot/discord/proxy.clj](/home/maxp/wrk/discord-bot/src/discord_bot/discord/proxy.clj) содержит parsing и применение HTTP proxy settings к JDA REST, Gateway/WebSocket transport и прямому OkHttp REST client;
- [src/discord_bot/config.clj](/home/maxp/wrk/discord-bot/src/discord_bot/config.clj) читает runtime config из environment, валидирует обязательные поля и загружает `build-info.edn` из classpath resource.

Текущий поток запуска:

1. `discord-bot.main/-main` читает конфигурацию через `load-config` и валидирует ее через `validate-config!`.
2. `mount/start-with-args` передает конфигурацию в mount state.
3. `discord-bot.app.core/conn` вызывает `jda/connect!`.
4. `jda/connect!` создает `JDABuilder`, применяет proxy/timeout settings, регистрирует listener и ожидает `.awaitReady`.
5. `discord-bot.discord.callback/router` извлекает OAuth2 `code` из HTTP callback и передает его в прикладной handler.
6. `discord-bot.app.auth/handle-code!` обменивает code на token и передает успешный результат в `:on-token` handler.

Текущий поток событий:

- `MessageReceivedEvent` игнорирует bot authors и передает `:user-id` и `:content` в `:on-message`;
- `ButtonInteractionEvent` делает `deferEdit` и передает `:button-id`, `:message-id` и `:user-id` в `:on-button`;
- `ReadyEvent`, `SessionDisconnectEvent`, `ShutdownEvent` и `StatusChangeEvent` сейчас только логируются.

## Критичные вопросы

Этот документ должен отвечать на вопросы:

- обработка события начинается в `discord-bot.discord.jda/create-listener`;
- gateway session создается и поддерживается внутри `JDA`;
- отправка DM-сообщений находится в `discord-bot.discord.jda/send-message`;
- причины reconnect и shutdown нужно начинать смотреть по логам `onSessionDisconnect`, `onShutdown` и `onStatusChange`.
