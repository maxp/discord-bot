(ns discord-bot.http.core
  (:require
   [clojure.string :as str])
  (:import
   (com.neovisionaries.ws.client WebSocketFactory)
   (java.net InetSocketAddress Proxy Proxy$Type URI URISyntaxException)
   (java.util.concurrent TimeUnit)
   (net.dv8tion.jda.api JDABuilder)
   (okhttp3 Authenticator Credentials OkHttpClient$Builder Response)))


(set! *warn-on-reflection* true)


(defn- default-port
  [scheme]
  (case (some-> scheme str/lower-case)
    "https" 443
    "http" 80
    nil))


(defn- redact-proxy-url
  "Removes user:password from a proxy URL for safe logging/error reporting."
  [^String proxy-url]
  (str/replace proxy-url #"://[^@]+@" "://<redacted>@"))


(defn parse-proxy-url [^String proxy-url]
  (when (not-empty proxy-url)
    (let [^URI uri (try (URI. proxy-url)
                        (catch URISyntaxException e
                          (throw (ex-info "Proxy URL is malformed"
                                          {:proxy-url (redact-proxy-url proxy-url)} e))))
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
                        {:proxy-url (redact-proxy-url proxy-url)})))
      (when-not host
        (throw (ex-info "Proxy URL must include a host"
                        {:proxy-url (redact-proxy-url proxy-url)})))
      (when-not port
        (throw (ex-info "Proxy URL must include a port or use a default for http/https"
                        {:proxy-url (redact-proxy-url proxy-url)})))
      {:url proxy-url
       :uri uri
       :scheme scheme
       :host host
       :port port
       :username username
       :password password})))


(defn- proxy-authenticator
  [{:keys [username password]}]
  (when username
    (reify Authenticator
      (authenticate
        [_ _ response]
        ;; Check the *request* that triggered this 407, not the response headers.
        ;; If we already sent Proxy-Authorization and still got 407, credentials
        ;; are wrong — return nil to stop retrying and avoid an infinite loop.
        (when-not (.header (.request ^Response response) "Proxy-Authorization")
          (-> (.request ^Response response)
              (.newBuilder)
              (.header "Proxy-Authorization"
                       (Credentials/basic ^String username (or ^String password "")))
              (.build)))))))


(defn- base-http-client-builder [timeout]
  (doto (OkHttpClient$Builder.)
    (.connectTimeout timeout TimeUnit/SECONDS)
    (.readTimeout    timeout TimeUnit/SECONDS)
    (.writeTimeout   timeout TimeUnit/SECONDS)))


(defn- http-client-builder [{:keys [host port] :as proxy-config} timeout]
  (let [^OkHttpClient$Builder builder (doto ^OkHttpClient$Builder (base-http-client-builder timeout)
                                        (.proxy (Proxy. Proxy$Type/HTTP
                                                        (InetSocketAddress. ^String host ^int port))))]
    (when-let [authenticator (proxy-authenticator proxy-config)]
      (.proxyAuthenticator builder authenticator))
    builder))


(defn- websocket-factory [{:keys [^URI uri ^String username ^String password]}]
  (let [factory (WebSocketFactory.)
        settings (.getProxySettings factory)]
    (.setServer settings uri)
    (when username
      (.setCredentials settings username (or password "")))
    factory))


(defn apply-to-builder [^JDABuilder builder ^String proxy-url timeout]
  (if-let [proxy-config (parse-proxy-url proxy-url)]
    (doto builder
      (.setHttpClientBuilder (http-client-builder proxy-config timeout))
      (.setWebsocketFactory (websocket-factory proxy-config)))
    (doto builder
      (.setHttpClientBuilder (base-http-client-builder timeout)))))
