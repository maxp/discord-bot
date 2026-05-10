(ns discord-bot.discord.jda
  (:require
   [taoensso.telemere :refer [log!]]
   [discord-bot.discord.proxy :as proxy])
  (:import
   (java.util List Collection)
   (java.util.concurrent CompletableFuture TimeoutException TimeUnit)
   (net.dv8tion.jda.api JDA JDABuilder)
   (net.dv8tion.jda.api.entities User)
   (net.dv8tion.jda.api.events.interaction.component ButtonInteractionEvent)
   (net.dv8tion.jda.api.events.message MessageReceivedEvent)
   (net.dv8tion.jda.api.events StatusChangeEvent)
   (net.dv8tion.jda.api.events.session ReadyEvent SessionDisconnectEvent ShutdownEvent)
   (net.dv8tion.jda.api.hooks ListenerAdapter)
   (net.dv8tion.jda.api.components.actionrow ActionRow)
   (net.dv8tion.jda.api.components.buttons Button)
   (net.dv8tion.jda.api.entities.channel.middleman MessageChannel)
   (net.dv8tion.jda.api.requests.restaction CacheRestAction MessageCreateAction)))


(set! *warn-on-reflection* true)

(def ^:private send-message-timeout-seconds 10)


(defn- require-bot-token! [discord-bot-token]
  (when-not (not-empty discord-bot-token)
    (throw (ex-info "discord-bot-token is empty" {})))
  discord-bot-token)


(defn create-listener [{:keys [on-message on-button]}]
  (proxy [ListenerAdapter] []
    
    (onReady [^ReadyEvent event]
      (log! ["discord state" (.getState event)]))
    
    (onButtonInteraction [^ButtonInteractionEvent event]
      (-> (.deferEdit event)
          (.queue))
      (let [button-id  (.getComponentId event)
            message-id (.getMessageId event)
            user-id    (.. event getUser getId)]
        (on-button {:event      event
                    :button-id  button-id
                    :message-id message-id
                    :user-id    user-id})))

    (onSessionDisconnect [^SessionDisconnectEvent event]
      (log! :warn ["discord session disconnected"
                   {:close-code          (some-> (.getCloseCode event) str)
                    :service-server-close (.isClosedByServer event)}]))

    (onShutdown [^ShutdownEvent event]
      (log! :warn ["discord gateway shutdown"
                   {:close-code    (some-> (.getCloseCode event) str)
                    :shutdown-time (str (.getTimeShutdown event))}]))

    (onStatusChange [^StatusChangeEvent event]
      (log! ["discord status changed"
             {:old (str (.getOldValue event))
              :new (str (.getNewValue event))}]))

    (onMessageReceived [^MessageReceivedEvent event]
      (let [^User author (.getAuthor event)]
        (when-not (.isBot author)
          (let [content (.. event getMessage getContentRaw)
                user-id (.getId author)]
            (on-message {:event   event
                         :user-id user-id
                         :content content})))))))

;; (.. event getAuthor getId)   ;; "123456789012345678"
;; (.. event getMessage getContentRaw)   ;; "@Max hello"


(defn connect! [{:keys [discord-bot-token discord-proxy-url discord-timeout]} handlers]
  (let [builder (doto (JDABuilder/createDefault (require-bot-token! discord-bot-token))
                  (proxy/apply-to-builder discord-proxy-url discord-timeout)
                  (.addEventListeners (into-array Object [(create-listener handlers)])))]
    (.awaitReady ^JDA (.build ^JDABuilder builder))))


(defn disconnect! [^JDA jda]
  (when jda
    (.shutdown jda)))


(defn- ->button
  [{:keys [id url label style]}]
  (let [style-kw   (or style :primary)
        ^String id-str    (str id)
        ^String label-str (str label)]
    (case style-kw
      :primary   (. Button (primary id-str label-str))
      :secondary (. Button (secondary id-str label-str))
      :success   (. Button (success id-str label-str))
      :danger    (. Button (danger id-str label-str))
      :link      (if-let [^String url-str (not-empty (str url))]
                   (. Button (link url-str label-str))
                   (throw (ex-info "link button requires a non-empty :url" {:label label})))
      (. Button (primary id-str label-str)))))


(defn send-message
  "Send a text message to a user via DM.
   buttons - optional vector of button maps:
     non-link: {:id \"action:id\" :label \"Text\" :style :primary/:secondary/:success/:danger}
     link:     {:url \"https://example.com\" :label \"Text\" :style :link}
   :style defaults to :primary.
   Returns {:ok true :data msg} on success, {:ok false :error err} on failure."
  ([^JDA jda ^String user-id ^String text]
   (send-message jda user-id text nil))
  ([^JDA jda ^String user-id ^String text buttons]
   (try
     (let [^User user (.complete ^CacheRestAction (.retrieveUserById ^JDA jda user-id))
           ^CacheRestAction channel-rest (.openPrivateChannel user)
           ^MessageChannel channel (.complete channel-rest)
           action (cond-> (.sendMessage channel text)
                    (seq buttons)
                     (.setComponents ^List (java.util.Collections/singletonList
                                                     (ActionRow/of ^Collection (mapv ->button buttons)))))
           future (.submit ^MessageCreateAction action)
           msg (.get ^CompletableFuture future send-message-timeout-seconds TimeUnit/SECONDS)]
       {:ok true :data msg})
     (catch InterruptedException e
       (.interrupt (Thread/currentThread))
       {:ok false :error e})
     (catch TimeoutException e
       {:ok false :error e})
     (catch Exception e
       {:ok false :error e}))))
