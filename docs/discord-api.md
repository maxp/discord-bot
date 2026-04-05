# Discord API Notes

Этот документ нужен как project-specific слой поверх официальной документации Discord.

## Что сюда складывать

- какие Discord API реально использует проект;
- какие intents включены и почему;
- какие scopes нужны для установки;
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

## Что фиксировать по мере разработки

- guild vs global command registration;
- какие permissions нужны боту;
- используем ли message content intent;
- как обрабатываются deferred responses;
- есть ли ограничения по размеру сообщений, embeds и components;
- какие edge cases уже встречались в проде или на тестовом сервере.
