(ns discord-bot.app.auth-test
  (:require [clojure.test :refer [deftest is]]
            [discord-bot.app.auth :as auth]
            [discord-bot.discord.oauth :as oauth]
            [discord-bot.discord.rest :as rest]))


(deftest auth-handle-code-exchanges-token-and-calls-on-token
  (let [received (atom nil)
        cfg      {:discord-app-id "id"
                  :discord-app-secret "secret"
                  :discord-callback-url "https://test/cb"
                  :discord-proxy-url nil
                  :discord-timeout 20}]
    (with-redefs [rest/create-client (fn [opts] (assoc opts :http-client ::client))
                  oauth/exchange-code! (fn [opts code]
                                         (reset! received {:opts opts :code code})
                                         {:ok true
                                          :status 200
                                          :data {:access_token "token-123"}})]
      (let [token-result (atom nil)
            result       (auth/handle-code! cfg (fn [api-opts token]
                                                  (reset! token-result {:api-opts api-opts
                                                                        :token token}))
                                            "abc")]
        (is (= {:ok true} result))
        (is (= "abc" (:code @received)))
        (is (= "id" (some-> @received :opts :client-id)))
        (is (= "secret" (some-> @received :opts :client-secret)))
        (is (= "https://test/cb" (some-> @received :opts :redirect-uri)))
        (is (= ::client (some-> @token-result :api-opts :http-client)))
        (is (= "token-123" (some-> @token-result :token :data :access_token)))))))


(deftest auth-handle-code-returns-failure-result
  (let [cfg {:discord-app-id "id"
             :discord-app-secret "secret"
             :discord-callback-url "https://test/cb"
             :discord-proxy-url nil
             :discord-timeout 20}]
    (with-redefs [rest/create-client (fn [opts] opts)
                  oauth/exchange-code! (fn [_opts _code]
                                         {:ok false
                                          :status 401
                                          :data {:message "Unauthorized"}})]
      (is (= {:ok false
              :status 401
              :message "Unauthorized"}
             (auth/handle-code! cfg (fn [_api-opts _result]
                                      (throw (ex-info "should not be called" {})))
                                "abc"))))))
