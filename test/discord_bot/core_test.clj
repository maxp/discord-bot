(ns discord-bot.core-test
  (:require [clojure.test :refer [deftest is]]
            [discord-bot.config :as config]
            [discord-bot.http.core :as http]
            [discord-bot.discord.jda :as jda]
            [discord-bot.main :as main]))

(deftest main-function-exists
  (is (fn? main/-main)))



(deftest build-info-loads-from-resource
  (let [build-info config/build-info]
    (is (map? build-info))
    (is (= "discord-bot" (:appname build-info)))
    (is (= "dev" (:version build-info)))))



(deftest ping-command-is-user-install-bot-dm-only
  (let [command (jda/ping-command-data)]
    (is (= #{"USER_INSTALL"}
           (into #{} (map #(.name %)) (.getIntegrationTypes command))))
    (is (= #{"BOT_DM"}
           (into #{} (map #(.name %)) (.getContexts command))))))



(deftest proxy-url-parsing-supports-defaults-and-credentials
  (is (= {:scheme "http"
          :host "proxy.local"
          :port 8080
          :username "alice"
          :password "secret"}
         (select-keys (http/parse-proxy-url "http://alice:secret@proxy.local:8080")
                      [:scheme :host :port :username :password])))
  (is (= {:scheme "https"
          :host "secure-proxy.local"
          :port 443
          :username nil
          :password nil}
         (select-keys (http/parse-proxy-url "https://secure-proxy.local")
                      [:scheme :host :port :username :password])))
  (let [ex (try
             (http/parse-proxy-url "socks5://proxy.local:1080")
             nil
             (catch clojure.lang.ExceptionInfo ex
               ex))]
    (is ex)
    (is (= "Proxy URL must use http or https scheme"
           (ex-message ex)))
    (is (= {:proxy-url "socks5://proxy.local:1080"}
           (ex-data ex)))))



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
