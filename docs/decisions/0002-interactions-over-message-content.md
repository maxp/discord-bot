# ADR 0002: Prefer interactions over message content

## Status

Accepted

## Context

Для нового Discord-бота обычно выгоднее строить основной UX вокруг slash commands и interactions, а не вокруг чтения обычных сообщений.

При этом проект уже принял gateway-first transport в [ADR 0001](0001-library-choice.md), поэтому нужно зафиксировать, как product-level выбор сочетается с transport-level архитектурой.

## Decision

Основной пользовательский интерфейс бота строится вокруг slash commands и interactions.

При этом interactions обрабатываются внутри gateway-based runtime, а не через interaction-only HTTP ingress как основной способ интеграции.

Бот ориентирован на использование только в direct messages, а не в guild channels или group chats.

Модель установки приложения должна быть `USER_INSTALL`, а не `GUILD_INSTALL`.

По умолчанию проект не должен требовать `MESSAGE_CONTENT` intent, если конкретный сценарий явно не зависит от чтения обычных сообщений.

## Consequences

- Базовый command surface проекта должен быть slash-first.
- Основной command context проекта должен быть DM-only.
- Основная install model проекта должна быть `USER_INSTALL`.
- Message-based команды и fallback-сценарии допустимы только по явной необходимости и должны документироваться отдельно.
- Отсутствие обязательной зависимости от `MESSAGE_CONTENT` intent упрощает permissions и установку бота.
- Gateway transport остаётся обязательным, даже если основной UX реализован через interactions.
