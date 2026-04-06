# Discord API Notes

Этот документ нужен как project-specific слой поверх официальной документации Discord.

## Что сюда складывать

- какие Discord API реально использует проект;
- какие части JDA используются как основной interop layer;
- как устроен gateway lifecycle: identify, heartbeat, resume, reconnect;
- какие intents включены и почему;
- какие scopes, integration types и install settings нужны для установки;
- какие interaction contexts поддерживает проект;
- как проект работает с slash commands, components и permissions;
- ограничения и риски: rate limits, reconnects, interaction deadlines.

## Рекомендуемые официальные документы

- Apps Overview
- Gateway
- OAuth2
- Permissions
- Interactions Overview
- Application Commands
- Receiving and Responding to Interactions
- Message Components / Component Reference

## JDA References

- Getting Started: https://jda.wiki/using-jda/getting-started/
- Interactions guide: https://jda.wiki/using-jda/interactions/
- Troubleshooting interactions: https://jda.wiki/using-jda/troubleshooting/
- `JDABuilder` API: https://docs.jda.wiki/net/dv8tion/jda/api/JDABuilder.html
- `ListenerAdapter` API: https://docs.jda.wiki/net/dv8tion/jda/api/hooks/ListenerAdapter.html
- `SlashCommandInteractionEvent` API: https://docs.jda.wiki/net/dv8tion/jda/api/events/interaction/command/SlashCommandInteractionEvent.html
- `CommandData` API: https://docs.jda.wiki/net/dv8tion/jda/api/interactions/commands/build/CommandData.html
- `InteractionContextType` API: https://docs.jda.wiki/net/dv8tion/jda/api/interactions/InteractionContextType.html
- `IntegrationType` API: https://docs.jda.wiki/net/dv8tion/jda/api/interactions/IntegrationType.html

## Что фиксировать по мере разработки

- какие gateway events реально обрабатывает проект;
- какие JDA listeners и event types являются входной точкой Clojure-кода;
- как устроен dispatch `INTERACTION_CREATE` и других событий;
- работает ли бот только в DM и какие ограничения из этого следуют;
- используется ли только `USER_INSTALL` и как проект исключает `GUILD_INSTALL`;
- используется ли только `BOT_DM` context и как проект исключает `PRIVATE_CHANNEL` и `GUILD`;
- используются ли guild-specific registration или проект полностью избегает guild command surface;
- какие permissions нужны боту;
- используем ли message content intent;
- как обрабатываются deferred responses;
- есть ли ограничения по размеру сообщений, embeds и components;
- какие edge cases уже встречались в проде или на тестовом сервере.
