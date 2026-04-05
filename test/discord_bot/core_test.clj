(ns discord-bot.core-test
  (:require [clojure.test :refer [deftest is]]
            [discord-bot.config :as config]
            [discord-bot.main :as main]))

(deftest main-function-exists
  (is (fn? main/-main)))



(deftest build-info-loads-from-resource
  (let [build-info (config/build-info)]
    (is (map? build-info))
    (is (= "discord-bot" (:appname build-info)))
    (is (= "dev" (:version build-info)))))
