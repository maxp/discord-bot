(ns discord-bot.discord.jda
  (:require
   [discord-bot.discord.http-proxy :as http-proxy])
  (:import
   (java.util EnumSet List Collection)
   (net.dv8tion.jda.api JDA JDABuilder)
   (net.dv8tion.jda.api.entities User)
   (net.dv8tion.jda.api.events.interaction.command SlashCommandInteractionEvent)
   (net.dv8tion.jda.api.events.message MessageReceivedEvent)
   (net.dv8tion.jda.api.events.session ReadyEvent)
   (net.dv8tion.jda.api.hooks ListenerAdapter)
   (net.dv8tion.jda.api.interactions IntegrationType InteractionContextType)
   (net.dv8tion.jda.api.interactions.commands.build CommandData Commands)
   (net.dv8tion.jda.api.components.actionrow ActionRow)
   (net.dv8tion.jda.api.components.buttons Button)
   (net.dv8tion.jda.api.entities.channel.middleman MessageChannel)
   (net.dv8tion.jda.api.requests.restaction CacheRestAction MessageCreateAction)
   (java.util.concurrent TimeoutException TimeUnit)))


(set! *warn-on-reflection* true)

(def ^:private send-message-timeout-seconds 10)


(defn ping-command-data
  []
  (doto (Commands/slash "ping" "Basic health check for the bot")
    (.setIntegrationTypes (EnumSet/of IntegrationType/USER_INSTALL))
    (.setContexts (EnumSet/of InteractionContextType/BOT_DM))))


(defn- require-bot-token!
  [discord-bot-token]
  (when-not (seq discord-bot-token)
    (throw (ex-info "DISCORD_BOT_TOKEN must be set before starting JDA"
                    {:env-var "DISCORD_BOT_TOKEN"})))
  discord-bot-token)


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
        nil))

    (onMessageReceived
      [^MessageReceivedEvent event]
      (when-not (.. event getAuthor isBot)
        (let [content (.. event getMessage getContentRaw)]
          (log-fn ["message received"
                   {:author (.. event getAuthor getName)
                    :content content}]))))))

;; (.. event getAuthor getId)   ;; "123456789012345678"
;; (.. event getMessage getContentRaw)   ;; "@Max hello"



(defn connect!
  [{:keys [discord-bot-token]} log-fn]
  (let [builder (doto (JDABuilder/createDefault (require-bot-token! discord-bot-token))
                  #(http-proxy/apply-to-builder % log-fn)
                  #(.addEventListeners ^JDABuilder % (into-array Object [(create-listener log-fn)])))]
    (.awaitReady ^JDA (.build ^JDABuilder builder))))


(defn disconnect!
  [^JDA jda]
  (when jda
    (.shutdown jda)))


(defn- ->button
  [{:keys [id label style]}]
  (let [style-kw (or style :primary)
        ^String id-str (name id)
        ^String label-str (str label)]
    (case style-kw
      :primary   (. Button (primary id-str label-str))
      :secondary (. Button (secondary id-str label-str))
      :success   (. Button (success id-str label-str))
      :danger    (. Button (danger id-str label-str))
      :link      (. Button (link id-str label-str))
      (. Button (primary id-str label-str)))))


(defn send-message
  "Send a text message to a user via DM.
   buttons - optional vector of button maps with keys: :id, :label, :style (optional, defaults to :primary).
   Returns {:ok true :data msg} on success, {:ok false :error err} on failure."
  ([^JDA jda ^String user-id ^String text]
   (send-message jda user-id text nil))
  ([^JDA jda ^String user-id ^String text buttons]
    (try
      (let [^User user (.retrieveUserById ^JDA jda user-id)
            ^CacheRestAction channel-rest (.openPrivateChannel user)
            ^MessageChannel channel (.complete channel-rest)
            action (cond-> (.sendMessage channel text)
                     (seq buttons)
                     (.setComponents ^java.util.List (java.util.Collections/singletonList
                                                      (ActionRow/of ^Collection (mapv ->button buttons)))))
            future (.submit ^MessageCreateAction action)
            msg (.get future send-message-timeout-seconds TimeUnit/SECONDS)]
        {:ok true :data msg})
      (catch TimeoutException e
        {:ok false :error e})
      (catch Exception e
        {:ok false :error e}))))
