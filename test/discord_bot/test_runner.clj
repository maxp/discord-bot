(ns discord-bot.test-runner
  (:require [clojure.test :as t]
            [discord-bot.core-test]))

(defn -main
  [& _args]
  (let [{:keys [fail error]} (t/run-all-tests #"^discord-bot\..*-test$")]
    (when (pos? (+ fail error))
      (System/exit 1))))
