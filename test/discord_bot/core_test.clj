(ns discord-bot.core-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [discord-bot.config :as config]
            [discord-bot.discord.api :as api]
            [discord-bot.discord.proxy :as proxy]
            [discord-bot.discord.jda :as jda]
            [discord-bot.http.callback :as callback]
            [discord-bot.main :as main])
  (:import
   (com.sun.net.httpserver HttpExchange HttpHandler HttpServer)
   (java.net InetSocketAddress URLDecoder)
   (okhttp3 Credentials)))


(defn- decode-url [value]
  (URLDecoder/decode value "UTF-8"))


(defn- parse-form-body [body]
  (into {}
        (map (fn [part]
               (let [[k v] (str/split part #"=" 2)]
                 [(decode-url k) (decode-url (or v ""))])))
        (str/split body #"&")))


(defn- request-body [^HttpExchange exchange]
  (slurp (.getRequestBody exchange)))


(defn- send-json! [^HttpExchange exchange status body]
  (let [bytes (.getBytes ^String body "UTF-8")]
    (.add (.getResponseHeaders exchange) "Content-Type" "application/json")
    (.sendResponseHeaders exchange status (alength bytes))
    (with-open [response-body (.getResponseBody exchange)]
      (.write response-body bytes))))


(defn- with-test-server [handler f]
  (let [server (HttpServer/create (InetSocketAddress. "127.0.0.1" 0) 0)]
    (.createContext server "/" (reify HttpHandler
                                 (handle [_ exchange]
                                   (handler exchange))))
    (.start server)
    (try
      (f (str "http://127.0.0.1:" (.getPort (.getAddress server))))
      (finally
        (.stop server 0)))))

(deftest main-function-exists
  (is (fn? main/-main)))



(deftest build-info-loads-from-resource
  (let [build-info config/build-info]
    (is (map? build-info))
    (is (= "discord-bot" (:appname build-info)))
    (is (= "dev" (:version build-info)))))



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



(deftest oauth-code-exchange-posts-form-with-basic-auth
  (let [requests (atom [])]
    (with-test-server
      (fn [^HttpExchange exchange]
        (swap! requests conj {:method (.getRequestMethod exchange)
                              :path (.. exchange getRequestURI getPath)
                              :authorization (.getFirst (.getRequestHeaders exchange) "Authorization")
                              :content-type (.getFirst (.getRequestHeaders exchange) "Content-Type")
                              :body (request-body exchange)})
        (send-json! exchange 200 "{\"access_token\":\"access-123\",\"token_type\":\"Bearer\",\"expires_in\":604800,\"refresh_token\":\"refresh-123\",\"scope\":\"identify\"}"))
      (fn [base-url]
        (let [client (api/create-client {:base-url base-url
                                         :client-id "client-id"
                                         :client-secret "client-secret"
                                         :redirect-uri "https://example.test/callback"})
              result (api/exchange-code! client
                                         "code-123")
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
                 (parse-form-body (:body request)))))))))


(deftest get-current-user-sends-bearer-token
  (let [requests (atom [])]
    (with-test-server
      (fn [^HttpExchange exchange]
        (swap! requests conj {:method (.getRequestMethod exchange)
                              :path (.. exchange getRequestURI getPath)
                              :authorization (.getFirst (.getRequestHeaders exchange) "Authorization")})
        (send-json! exchange 200 "{\"id\":\"42\",\"username\":\"discord\",\"global_name\":\"Discord\"}"))
      (fn [base-url]
        (let [client (api/create-client {:base-url base-url})
              result (api/get-current-user! client "access-123")
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
  (with-test-server
    (fn [^HttpExchange exchange]
      (send-json! exchange 401 "{\"message\":\"401: Unauthorized\",\"code\":0}"))
    (fn [base-url]
      (let [client (api/create-client {:base-url base-url})]
        (is (= {:ok false
                :status 401
                :data {:message "401: Unauthorized"
                       :code 0}}
               (api/get-current-user! client "bad-token")))))))


(deftest api-client-reuses-existing-okhttp-client
  (let [client (api/create-client {})]
    (is (:http-client client))
    (is (identical? (:http-client client)
                    (:http-client (api/create-client client))))))



(deftest callback-returns-200-on-success
  (with-redefs [config/load-config (fn [] {:discord-app-id "id"
                                           :discord-app-secret "secret"
                                           :discord-callback-url "https://test/cb"
                                           :discord-proxy-url nil
                                           :discord-timeout 20
                                           :discord-callback-host "localhost"
                                           :discord-callback-port 8131})
                api/exchange-code! (fn [_opts _code]
                                     {:ok true
                                      :status 200
                                      :data {:access_token "token-123"}})]
    (let [resp (callback/router {:uri "/discord/callback"
                                 :query-string "code=abc&state=xyz"})]
      (is (= 200 (:status resp)))
      (is (str/includes? (:body resp) "Authorization successful")))))



(deftest callback-returns-400-when-code-missing
  (let [resp (callback/router {:uri "/discord/callback"
                               :query-string "state=xyz"})]
    (is (= 400 (:status resp)))
    (is (str/includes? (:body resp) "Missing authorization code"))))



(deftest callback-returns-404-for-unknown-path
  (let [resp (callback/router {:uri "/other"})]
    (is (= 404 (:status resp)))))



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
