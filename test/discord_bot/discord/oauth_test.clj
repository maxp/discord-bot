(ns discord-bot.discord.oauth-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [discord-bot.discord.oauth :as oauth]
            [discord-bot.discord.rest :as rest]
            [discord-bot.test-support :as support])
  (:import
   (com.sun.net.httpserver HttpExchange)
   (okhttp3 Credentials)))


(deftest oauth-code-exchange-posts-form-with-basic-auth
  (let [requests (atom [])]
    (support/with-test-server
      (fn [^HttpExchange exchange]
        (swap! requests conj {:method (.getRequestMethod exchange)
                              :path (.. exchange getRequestURI getPath)
                              :authorization (.getFirst (.getRequestHeaders exchange) "Authorization")
                              :content-type (.getFirst (.getRequestHeaders exchange) "Content-Type")
                              :body (support/request-body exchange)})
        (support/send-json! exchange 200 "{\"access_token\":\"access-123\",\"token_type\":\"Bearer\",\"expires_in\":604800,\"refresh_token\":\"refresh-123\",\"scope\":\"identify\"}"))
      (fn [base-url]
        (let [client (rest/create-client {:base-url base-url
                                          :client-id "client-id"
                                          :client-secret "client-secret"
                                          :redirect-uri "https://example.test/callback"})
              result (oauth/exchange-code! client "code-123")
              request (first @requests)]
          (is (= {:ok true
                  :status 200
                  :data {:access_token "access-123"
                         :token_type "Bearer"
                         :expires_in 604800
                         :refresh_token "refresh-123"
                         :scope "identify"}}
                 result))
          (is (= "POST" (:method request)))
          (is (= "/oauth2/token" (:path request)))
          (is (= (Credentials/basic "client-id" "client-secret")
                 (:authorization request)))
          (is (str/starts-with? (:content-type request) "application/x-www-form-urlencoded"))
          (is (= {"grant_type" "authorization_code"
                  "code" "code-123"
                  "redirect_uri" "https://example.test/callback"}
                 (support/parse-form-body (:body request)))))))))


(deftest oauth2-authorize-url-builds-correct-link
  (let [url (oauth/oauth2-authorize-url {:discord-app-id "12345"
                                         :discord-callback-url "https://example.com/callback"})]
    (is (str/starts-with? url "https://discord.com/oauth2/authorize?"))
    (is (str/includes? url "client_id=12345"))
    (is (str/includes? url "response_type=code"))
    (is (str/includes? url "redirect_uri=https%3A%2F%2Fexample.com%2Fcallback"))
    (is (str/includes? url "scope=identify%20applications.commands"))
    (is (str/includes? url "integration_type=1"))
    (is (not (str/includes? url "state=")))))


(deftest oauth2-authorize-url-encodes-state
  (let [url (oauth/oauth2-authorize-url {:discord-app-id "12345"
                                         :discord-callback-url "https://example.com/callback"}
                                        :state "hello world")]
    (is (str/includes? url "state=hello+world"))))
