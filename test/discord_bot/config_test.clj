(ns discord-bot.config-test
  (:require [clojure.test :refer [deftest is]]
            [discord-bot.config :as config]))


(deftest build-info-loads-from-resource
  (let [build-info config/build-info]
    (is (map? build-info))
    (is (= "discord-bot" (:appname build-info)))
    (is (= "dev" (:version build-info)))))


(deftest validate-config-accepts-valid-config
  (let [cfg {:discord-bot-token "bot-token"
             :discord-app-id "app-id"
             :discord-app-secret "app-secret"
             :discord-proxy-url nil
             :discord-timeout 20
             :discord-callback-url "https://example.test/discord/callback"
             :discord-callback-host "localhost"
             :discord-callback-port 8131
             :discord-callback-path "/discord/callback"
             :build-info config/build-info}]
    (is (= cfg (config/validate-config! cfg)))))


(deftest validate-config-fails-on-missing-required-values
  (let [ex (try
             (config/validate-config! {:discord-bot-token nil
                                       :discord-app-id "app-id"
                                       :discord-app-secret "app-secret"
                                       :discord-timeout 20
                                       :discord-callback-url "https://example.test/discord/callback"
                                       :discord-callback-host "localhost"
                                       :discord-callback-port 8131
                                       :discord-callback-path "/discord/callback"})
             nil
             (catch clojure.lang.ExceptionInfo ex
               ex))]
    (is ex)
    (is (= "discord-bot-token is empty" (ex-message ex)))
    (is (= {:key :discord-bot-token :value nil}
           (ex-data ex)))))


(deftest validate-config-fails-on-invalid-numeric-and-path-values
  (let [timeout-ex (try
                     (config/validate-config! {:discord-bot-token "bot-token"
                                               :discord-app-id "app-id"
                                               :discord-app-secret "app-secret"
                                               :discord-timeout 0
                                               :discord-callback-url "https://example.test/discord/callback"
                                               :discord-callback-host "localhost"
                                               :discord-callback-port 8131
                                               :discord-callback-path "/discord/callback"})
                     nil
                     (catch clojure.lang.ExceptionInfo ex
                       ex))
        path-ex (try
                  (config/validate-config! {:discord-bot-token "bot-token"
                                            :discord-app-id "app-id"
                                            :discord-app-secret "app-secret"
                                            :discord-timeout 20
                                            :discord-callback-url "https://example.test/discord/callback"
                                            :discord-callback-host "localhost"
                                            :discord-callback-port 8131
                                            :discord-callback-path "discord/callback"})
                  nil
                  (catch clojure.lang.ExceptionInfo ex
                    ex))]
    (is timeout-ex)
    (is (= "discord-timeout must be a positive integer" (ex-message timeout-ex)))
    (is (= {:key :discord-timeout :value 0}
           (ex-data timeout-ex)))
    (is path-ex)
    (is (= "discord-callback-path must start with /" (ex-message path-ex)))
    (is (= {:key :discord-callback-path :value "discord/callback"}
           (ex-data path-ex)))))
