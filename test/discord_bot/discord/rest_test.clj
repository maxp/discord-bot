(ns discord-bot.discord.rest-test
  (:require [clojure.test :refer [deftest is]]
            [discord-bot.discord.rest :as rest]))


(deftest api-client-reuses-existing-okhttp-client
  (let [client (rest/create-client {})]
    (is (:http-client client))
    (is (identical? (:http-client client)
                    (:http-client (rest/create-client client))))))
