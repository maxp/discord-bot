(ns discord-bot.discord.users-test
  (:require [clojure.test :refer [deftest is]]
            [discord-bot.discord.rest :as rest]
            [discord-bot.discord.users :as users]
            [discord-bot.test-support :as support])
  (:import
   (com.sun.net.httpserver HttpExchange)))


(deftest get-current-user-sends-bearer-token
  (let [requests (atom [])]
    (support/with-test-server
      (fn [^HttpExchange exchange]
        (swap! requests conj {:method (.getRequestMethod exchange)
                              :path (.. exchange getRequestURI getPath)
                              :authorization (.getFirst (.getRequestHeaders exchange) "Authorization")})
        (support/send-json! exchange 200 "{\"id\":\"42\",\"username\":\"discord\",\"global_name\":\"Discord\"}"))
      (fn [base-url]
        (let [client (rest/create-client {:base-url base-url})
              result (users/get-current-user! client "access-123")
              request (first @requests)]
          (is (= {:ok true
                  :status 200
                  :data {:id "42"
                         :username "discord"
                         :global_name "Discord"}}
                 result))
          (is (= "GET" (:method request)))
          (is (= "/users/@me" (:path request)))
          (is (= "Bearer access-123" (:authorization request))))))))


(deftest discord-api-errors-return-result-map
  (support/with-test-server
    (fn [^HttpExchange exchange]
      (support/send-json! exchange 401 "{\"message\":\"401: Unauthorized\",\"code\":0}"))
    (fn [base-url]
      (let [client (rest/create-client {:base-url base-url})]
        (is (= {:ok false
                :status 401
                :data {:message "401: Unauthorized"
                       :code 0}}
               (users/get-current-user! client "bad-token")))))))
