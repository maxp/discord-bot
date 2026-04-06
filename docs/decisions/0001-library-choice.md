# ADR 0001: Discord library choice

## Status

Accepted

## Context

Нужно выбрать подход для интеграции с Discord API и зафиксировать, вокруг какого transport-слоя строится runtime бота.

Для проекта рассматривались два базовых направления:

- interaction-only через входящий HTTP endpoint и REST-вызовы к Discord API;
- gateway-first runtime через постоянное WebSocket-соединение с Discord Gateway.

Для этого бота важны:

- получение событий Discord в реальном времени;
- явный контроль над reconnect, heartbeat и lifecycle соединения;
- единая event-driven модель обработки Discord-событий;
- возможность позже расшириться в сторону дополнительных gateway events без смены архитектурной основы.

## Decision

Проект принимает gateway-first подход.

Основной runtime бота должен строиться вокруг Discord Gateway, а не вокруг interaction-only HTTP webhook модели.

Для реализации gateway runtime проект использует `JDA` через Java interop из Clojure.

Это означает, что:

- слой интеграции с Discord обязан поддерживать постоянное gateway-соединение;
- transport-слой должен обрабатывать identify, heartbeat, reconnect и resume;
- обработка входящих Discord-событий должна начинаться из gateway event loop;
- slash commands и interactions остаются частью продукта, но работают поверх gateway-based runtime, а не вместо него.
- Clojure-код проекта должен взаимодействовать с Discord runtime через JDA listeners, entities и command builders, а не через отдельную Clojure-native Discord библиотеку.

## Consequences

- Архитектура проекта теперь должна исходить из event-driven потока `startup -> gateway connect -> event dispatch -> command handling -> response`.
- В документации и коде не следует моделировать bot runtime как обычное Ring-приложение, которое только принимает входящие interactions по HTTP.
- Основные технические риски смещаются в сторону reconnect behavior, shard/session lifecycle, rate limits и устойчивости long-lived соединения.
- Слой Discord transport должен быть отделен от бизнес-логики, чтобы обработчики команд не зависели от деталей gateway protocol.
- Поздняя замена gateway-first подхода на interaction-only HTTP модель будет архитектурным изменением, а не локальной заменой библиотеки.
- Проект сознательно принимает Java interop как часть технического стека ради более зрелой и актуальной Discord library surface.

## References

- JDA getting started: https://jda.wiki/using-jda/getting-started/
- JDA interactions guide: https://jda.wiki/using-jda/interactions/
- JDA `CommandData` API: https://docs.jda.wiki/net/dv8tion/jda/api/interactions/commands/build/CommandData.html
- JDA releases: https://github.com/DV8FromTheWorld/JDA/releases
