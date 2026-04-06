(ns discord-bot.discord.jda
  (:require
    [clojure.string :as str])
  (:import
    (com.neovisionaries.ws.client WebSocketFactory)
    (java.net InetSocketAddress Proxy Proxy$Type URI)
    (java.util EnumSet List)
    (net.dv8tion.jda.api JDA JDABuilder)
    (net.dv8tion.jda.api.events.interaction.command SlashCommandInteractionEvent)
    (net.dv8tion.jda.api.events.session ReadyEvent)
    (net.dv8tion.jda.api.hooks ListenerAdapter)
    (net.dv8tion.jda.api.interactions IntegrationType InteractionContextType)
    (net.dv8tion.jda.api.interactions.commands.build CommandData Commands)
    (okhttp3 Authenticator Credentials OkHttpClient$Builder)))


(defn ping-command-data
  []
  (doto (Commands/slash "ping" "Basic health check for the bot")
    (.setIntegrationTypes (EnumSet/of IntegrationType/USER_INSTALL))
    (.setContexts (EnumSet/of InteractionContextType/BOT_DM))))


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


(defn- require-bot-token!
  [discord-bot-token]
  (when-not (seq discord-bot-token)
    (throw (ex-info "DISCORD_BOT_TOKEN must be set before starting JDA"
                    {:env-var "DISCORD_BOT_TOKEN"})))
  discord-bot-token)


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
                                  (InetSocketAddress. host (int port)))))]
    (when-let [authenticator (proxy-authenticator proxy-config)]
      (.proxyAuthenticator builder authenticator))
    builder))


(defn- websocket-factory
  [{:keys [uri username password]}]
  (let [factory (WebSocketFactory.)
        settings (.getProxySettings factory)]
    (.setServer settings uri)
    (when username
      (.setCredentials settings username (or password "")))
    factory))


(defn- apply-proxy
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


(defn- register-commands!
  [^JDA jda]
  (-> (.updateCommands jda)
      (.addCommands (List/of ^CommandData (ping-command-data)))
      (.queue)))


(defn create-listener
  [log-fn]
  (proxy [ListenerAdapter] []
    (onReady
      [^ReadyEvent event]
      (log-fn ["discord gateway ready"
               {:self-user (.. event getJDA getSelfUser getName)}])
      (register-commands! (.getJDA event)))

    (onSlashCommandInteraction
      [^SlashCommandInteractionEvent event]
      (case (.getName event)
        "ping" (-> (.reply event "pong")
                   (.queue))
        nil))))


(defn connect!
  [{:keys [discord-bot-token]} log-fn]
  (-> (JDABuilder/createDefault (require-bot-token! discord-bot-token))
      (apply-proxy log-fn)
      (.addEventListeners (into-array Object [(create-listener log-fn)]))
      (.build)
      (.awaitReady)))


(defn disconnect!
  [^JDA jda]
  (when jda
    (.shutdown jda)))
