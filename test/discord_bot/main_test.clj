(ns discord-bot.main-test
  (:require [clojure.test :refer [deftest is]]
            [discord-bot.main :as main]))


(deftest main-function-exists
  (is (fn? main/-main)))
