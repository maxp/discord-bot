# ADR 0002: Prefer interactions over message content

## Status

Proposed

## Context

Для нового Discord-бота обычно выгоднее строить основной UX вокруг slash commands и interactions, а не вокруг чтения обычных сообщений.

## Decision

TBD.

## Consequences

После принятия решения здесь нужно зафиксировать:

- нужен ли `MESSAGE_CONTENT` intent;
- какие команды будут slash-only;
- где останутся message-based fallback сценарии, если они вообще нужны;
- как это упрощает permissions, безопасность и установку бота.
