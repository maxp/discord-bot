(ns discord-bot.test-runner
  (:require [clojure.test :as t]
            [discord-bot.app.auth-test]
            [discord-bot.config-test]
            [discord-bot.discord.callback-test]
            [discord-bot.discord.jda-test]
            [discord-bot.discord.oauth-test]
            [discord-bot.discord.proxy-test]
            [discord-bot.discord.rest-test]
            [discord-bot.discord.users-test]
            [discord-bot.main-test]))

(defn -main
  [& _args]
  (let [{:keys [fail error]} (t/run-all-tests #"^discord-bot\..*-test$")]
    (when (pos? (+ fail error))
      (System/exit 1))))
