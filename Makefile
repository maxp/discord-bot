.PHONY: help dev run repl test lint outdated tunnel-up tunnel-down tunnel-status

help:
	@printf "%s\n" \
	"Available targets:" \
	"  make dev   - start a project REPL with .env loaded" \
	"  make run   - run the bot entrypoint" \
	"  make repl  - start a project REPL" \
	"  make test  - run the test suite" \
	"  make lint  - lint src and test with clj-kondo" \
	"  make outdated - check outdated dependencies with antq" \
	"  make tunnel-up   - start SSH reverse tunnel to vsp" \
	"  make tunnel-down - stop SSH reverse tunnel" \
	"  make tunnel-status - check SSH tunnel status"

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

tunnel-up:
	ssh -fN -o ControlMaster=yes -o ControlPath=/tmp/ssh-tunnel-vsp.sock -R 8131:localhost:8131 vsp

tunnel-down:
	ssh -O exit -o ControlPath=/tmp/ssh-tunnel-vsp.sock vsp 2>/dev/null || true

tunnel-status:
	@ssh -O check -o ControlPath=/tmp/ssh-tunnel-vsp.sock vsp 2>&1 && echo "Tunnel is active" || echo "Tunnel is down"

