.PHONY: help dev run repl test lint outdated

help:
	@printf "%s\n" \
	"Available targets:" \
	"  make dev   - start a project REPL with .env loaded" \
	"  make run   - run the bot entrypoint" \
	"  make repl  - start a project REPL" \
	"  make test  - run the test suite" \
	"  make lint  - lint src and test with clj-kondo" \
	"  make outdated - check outdated dependencies with antq"

dev:
	set -a; . ./.env; set +a; clojure -M:dev

run:
	set -a; . ./.env; set +a; clojure -M -m discord-bot.main

repl:
	$(MAKE) dev

test:
	set -a; . ./.env; set +a; clojure -M:test:test-run

lint:
	set -a; . ./.env; set +a; clojure -M:lint --lint src test

outdated:
	set -a; . ./.env; set +a; clojure -M:outdated

