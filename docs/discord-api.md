# Discord API Notes

Этот документ нужен как project-specific слой поверх официальной документации Discord.

## Что сюда складывать

- какие Discord API реально использует проект;
- какие части JDA используются как основной interop layer;
- как устроен gateway lifecycle: identify, heartbeat, resume, reconnect;
- какие intents включены и почему;
- какие scopes, integration types и install settings нужны для установки;
- какие interaction contexts поддерживает проект;
- как проект работает с interactions, components и permissions;
- ограничения и риски: rate limits, reconnects, interaction deadlines.

## Рекомендуемые официальные документы

- Apps Overview
- Gateway
- OAuth2
- Permissions
- Interactions Overview
- Receiving and Responding to Interactions
- Message Components / Component Reference

## JDA References

- Getting Started: https://jda.wiki/using-jda/getting-started/
- Interactions guide: https://jda.wiki/using-jda/interactions/
- Troubleshooting interactions: https://jda.wiki/using-jda/troubleshooting/
- `JDABuilder` API: https://docs.jda.wiki/net/dv8tion/jda/api/JDABuilder.html
- `ListenerAdapter` API: https://docs.jda.wiki/net/dv8tion/jda/api/hooks/ListenerAdapter.html
- `InteractionContextType` API: https://docs.jda.wiki/net/dv8tion/jda/api/interactions/InteractionContextType.html
- `IntegrationType` API: https://docs.jda.wiki/net/dv8tion/jda/api/interactions/IntegrationType.html

## Что фиксировать по мере разработки

- какие gateway events реально обрабатывает проект;
- какие JDA listeners и event types являются входной точкой Clojure-кода;
- как устроен dispatch `INTERACTION_CREATE` и других событий;
- работает ли бот только в DM и какие ограничения из этого следуют;
- используется ли только `USER_INSTALL` и как проект исключает `GUILD_INSTALL`;
- используется ли только `BOT_DM` context и как проект исключает `PRIVATE_CHANNEL` и `GUILD`;
- какие permissions нужны боту;
- используем ли message content intent;
- как обрабатываются deferred responses;
- есть ли ограничения по размеру сообщений, embeds и components;
- какие edge cases уже встречались в проде или на тестовом сервере.

## Текущее использование в коде

- Gateway connection создается через `JDABuilder/createDefault` в [src/discord_bot/discord/jda.clj](/home/maxp/wrk/discord-bot/src/discord_bot/discord/jda.clj).
- Listener реализован через `ListenerAdapter`.
- Сейчас listener обрабатывает `ReadyEvent`, `StatusChangeEvent`, `SessionDisconnectEvent`, `ShutdownEvent`, `MessageReceivedEvent` и `ButtonInteractionEvent`.
- `MessageReceivedEvent` игнорирует сообщения от bot users и читает raw content через `getContentRaw`.
- `ButtonInteractionEvent` вызывает `deferEdit` перед передачей события в app handler.
- `DISCORD_PROXY_URL`, если задан, применяется к REST HTTP client и WebSocket factory через [src/discord_bot/discord/proxy.clj](/home/maxp/wrk/discord-bot/src/discord_bot/discord/proxy.clj).
- Прямые Discord REST-вызовы разделены по namespace: общий client в [src/discord_bot/discord/rest.clj](/home/maxp/wrk/discord-bot/src/discord_bot/discord/rest.clj), OAuth2 в [src/discord_bot/discord/oauth.clj](/home/maxp/wrk/discord-bot/src/discord_bot/discord/oauth.clj), user API в [src/discord_bot/discord/users.clj](/home/maxp/wrk/discord-bot/src/discord_bot/discord/users.clj); OAuth2 credentials (`client_id`, `client_secret`, `redirect_uri`) берутся из конфигурации;
- OAuth2 callback обрабатывается HTTP-сервером [src/discord_bot/discord/callback.clj](/home/maxp/wrk/discord-bot/src/discord_bot/discord/callback.clj) на `DISCORD_CALLBACK_HOST:DISCORD_CALLBACK_PORT` (default: `localhost:8131`); endpoint `GET DISCORD_CALLBACK_PATH` (default: `/discord/callback`) принимает `code` и `state`, обменивает code на access token и возвращает HTML-ответ пользователю;
- `POST /oauth2/token` используется для обмена authorization code на access token; Discord требует `application/x-www-form-urlencoded` body и поддерживает HTTP Basic auth для `client_id:client_secret`.
- `GET /users/@me` используется с OAuth2 bearer access token и требует scope `identify`.
- JSON responses разбираются через `jsonista` с keyword keys.
