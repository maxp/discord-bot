(ns discord-bot.discord.proxy-test
  (:require [clojure.test :refer [deftest is]]
            [discord-bot.discord.proxy :as proxy]))


(deftest proxy-url-parsing-supports-defaults-and-credentials
  (is (= {:scheme "http"
          :host "proxy.local"
          :port 8080
          :username "alice"
          :password "secret"}
         (select-keys (proxy/parse-proxy-url "http://alice:secret@proxy.local:8080")
                      [:scheme :host :port :username :password])))
  (is (= {:scheme "https"
          :host "secure-proxy.local"
          :port 443
          :username nil
          :password nil}
         (select-keys (proxy/parse-proxy-url "https://secure-proxy.local")
                      [:scheme :host :port :username :password])))
  (let [ex (try
             (proxy/parse-proxy-url "socks5://proxy.local:1080")
             nil
             (catch clojure.lang.ExceptionInfo ex
               ex))]
    (is ex)
    (is (= "Proxy URL must use http or https scheme"
           (ex-message ex)))
    (is (= {:proxy-url "socks5://proxy.local:1080"}
           (ex-data ex)))))
