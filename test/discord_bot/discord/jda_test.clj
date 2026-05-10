(ns discord-bot.discord.jda-test
  (:require [clojure.test :refer [deftest is]]
            [discord-bot.discord.jda :as jda]))


(deftest connect-fails-fast-on-missing-token
  (let [ex (try
             (jda/connect! {:discord-bot-token nil} {})
             nil
             (catch clojure.lang.ExceptionInfo ex
               ex))]
    (is ex)
    (is (= "discord-bot-token is empty"
           (ex-message ex)))
    (is (= {}
           (ex-data ex)))))
