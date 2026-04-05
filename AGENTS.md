# AGENTS.md

## Working Rules

- This repository contains a Discord bot written in Clojure.
- Use `deps.edn` for dependency management and aliases.
- Use `Makefile` as the primary entrypoint for recurring developer commands.
- `.env` is mandatory in the local working copy and is always loaded for dev commands.
- In development and test flows, `build-info.edn` is expected on the `dev/` classpath.

## Change Rules

- Do not introduce Leiningen or alternative build tooling unless explicitly requested.
- Do not bypass `Makefile` for recurring developer workflows. If a command becomes standard, expose it through `Makefile`.
- If you add a new required environment variable, update `.env.example` and `docs/setup.md`.
- Do not change `src/discord_bot/config.clj` unless the task is specifically about configuration behavior.
- Keep documentation in sync when changing startup, config, build, or developer workflows.

## Code Style

- Keep two blank lines between top-level functions.
- Prefer small, explicit functions over unnecessary abstractions.
- Preserve existing namespace and file naming conventions.

## Validation

- After changing Clojure code, run `make test`.
- After changing startup, mount lifecycle, or configuration loading, also run `make run`.
- After changing lint workflow, run `make lint`.
- If you change developer commands or setup assumptions, review `README.md`, `docs/project-rules.md`, and `docs/setup.md`.

## Current Constraints

- The current `mount` startup is a scaffold and not yet the final Discord runtime.
