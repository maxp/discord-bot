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
- [src/discord_bot/discord/jda.clj](/home/maxp/wrk/discord-bot/src/discord_bot/discord/jda.clj) содержит `JDA` interop, listener registration и helper для отправки DM;
- [src/discord_bot/http/callback.clj](/home/maxp/wrk/discord-bot/src/discord_bot/http/callback.clj) содержит HTTP-сервер для обработки OAuth2 callback на порту `8131`;
- [src/discord_bot/discord/api.clj](/home/maxp/wrk/discord-bot/src/discord_bot/discord/api.clj) содержит прямые Discord REST-вызовы для OAuth2 и user API;
- [src/discord_bot/discord/proxy.clj](/home/maxp/wrk/discord-bot/src/discord_bot/discord/proxy.clj) содержит parsing и применение HTTP proxy settings к JDA REST, Gateway/WebSocket transport и прямому OkHttp REST client;
- [src/discord_bot/config.clj](/home/maxp/wrk/discord-bot/src/discord_bot/config.clj) читает runtime config из environment и `build-info.edn` из classpath resource.

Текущий поток запуска:

1. `discord-bot.main/-main` читает конфигурацию через `load-config`.
2. `mount/start-with-args` передает конфигурацию в mount state.
3. `discord-bot.app.core/conn` вызывает `jda/connect!`.
4. `jda/connect!` создает `JDABuilder`, применяет proxy/timeout settings, регистрирует listener и ожидает `.awaitReady`.

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
