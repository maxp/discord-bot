# Deployment

Деплой и операционные заметки для запуска бота вне локальной машины.

## Текущий runtime

- бот подключается к Discord Gateway через `JDA` (Java interop);
- OAuth2 callback HTTP-сервер слушает на `DISCORD_CALLBACK_HOST:DISCORD_CALLBACK_PORT` (default: `localhost:8131`);
- для приёма OAuth2 callback из внешней сети нужен reverse proxy или SSH tunnel к порту `8131`.

## SSH tunnel (vsp)

Для проброса callback на удалённый хост используется SSH reverse tunnel:

```bash
make tunnel-up      # поднять tunnel
make tunnel-down    # остановить tunnel
make tunnel-status  # проверить статус
```

Tunnel пробрасывает `localhost:8131` → `vsp:8131`.

## Secrets

- bot token и прочие secrets передаются через `.env` (не хранятся в репозитории);
- production intents, `USER_INSTALL`, install settings и command contexts должны совпадать с DM-only поведением.

## Что сюда добавить после появления полноценного runtime

- systemd unit или аналог для автоматического перезапуска;
- health checks и smoke checks после деплоя;
- мониторинг reconnect и startup failures;
- стратегия обновления приложения без downtime.

## Минимальный операционный чеклист

- bot token и прочие secrets не лежат в репозитории;
- production intents, `USER_INSTALL`, install settings и command contexts совпадают с DM-only поведением;
- после запуска бот виден online и отвечает на базовую команду;
- есть понятный способ проверить reconnect и startup failures.
