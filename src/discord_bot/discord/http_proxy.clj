(ns discord-bot.discord.http-proxy
  (:require
   [clojure.string :as str])
  (:import
   (com.neovisionaries.ws.client WebSocketFactory)
   (java.net InetSocketAddress Proxy Proxy$Type URI)
   (net.dv8tion.jda.api JDABuilder)
   (okhttp3 Authenticator Credentials OkHttpClient$Builder)))


(defn- default-port
  [scheme]
  (case (some-> scheme str/lower-case)
    "https" 443
    "http" 80
    nil))


(defn parse-proxy-url
  [proxy-url]
  (when (seq proxy-url)
    (let [uri (URI. proxy-url)
          scheme (.getScheme uri)
          host (.getHost uri)
          port (let [raw-port (.getPort uri)]
                 (if (neg? raw-port)
                   (default-port scheme)
                   raw-port))
          [username password] (when-let [user-info (.getUserInfo uri)]
                                (str/split user-info #":" 2))]
      (when-not (#{"http" "https"} (some-> scheme str/lower-case))
        (throw (ex-info "Proxy URL must use http or https scheme"
                        {:proxy-url proxy-url})))
      (when-not host
        (throw (ex-info "Proxy URL must include a host"
                        {:proxy-url proxy-url})))
      (when-not port
        (throw (ex-info "Proxy URL must include a port or use a default for http/https"
                        {:proxy-url proxy-url})))
      {:url proxy-url
       :uri uri
       :scheme scheme
       :host host
       :port port
       :username username
       :password password})))


(defn proxy-config-from-env
  []
  (or (some-> (System/getenv "HTTPS_PROXY") parse-proxy-url)
      (some-> (System/getenv "https_proxy") parse-proxy-url)
      (some-> (System/getenv "HTTP_PROXY") parse-proxy-url)
      (some-> (System/getenv "http_proxy") parse-proxy-url)))


(defn- proxy-authenticator
  [{:keys [username password]}]
  (when username
    (reify Authenticator
      (authenticate
        [_ _ response]
        (when-not (.header response "Proxy-Authorization")
          (-> response
              (.request)
              (.newBuilder)
              (.header "Proxy-Authorization"
                       (Credentials/basic username (or password "")))
              (.build)))))))


(defn- http-client-builder
  [{:keys [host port] :as proxy-config}]
  (let [builder (doto (OkHttpClient$Builder.)
                  (.proxy (Proxy. Proxy$Type/HTTP
                                  (InetSocketAddress. ^String host ^int port))))]
    (when-let [authenticator (proxy-authenticator proxy-config)]
      (.proxyAuthenticator builder authenticator))
    builder))


(defn- websocket-factory
  [{:keys [uri username password]}]
  (let [factory (WebSocketFactory.)
        settings (.getProxySettings factory)]
    (.setServer settings ^URI uri)
    (when username
      (.setCredentials settings username (or password "")))
    factory))


(defn apply-to-builder
  [^JDABuilder builder log-fn]
  (if-let [proxy-config (proxy-config-from-env)]
    (do
      (log-fn ["configuring proxy for Discord transport"
               {:host (:host proxy-config)
                :port (:port proxy-config)
                :scheme (:scheme proxy-config)}])
      (doto builder
        (.setHttpClientBuilder (http-client-builder proxy-config))
        (.setWebsocketFactory (websocket-factory proxy-config))))
    builder))
