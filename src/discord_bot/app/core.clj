(ns discord-bot.app.core
  (:require
   [discord-bot.app.auth :as auth]
   [discord-bot.app.handlers :as handlers]
   [discord-bot.discord.callback :as callback]
   [discord-bot.discord.jda :as jda]
   [mount.core :refer [args defstate]]
   [taoensso.telemere :refer [log!]])
  (:import
   (net.dv8tion.jda.api JDA)))


(set! *warn-on-reflection* true)


;; sample:
;;
;; "on-button:" {:event #object[net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent 0x283fd4c0
;;                      "ButtonInteractionEvent(id=1502625799017201775)"],
;;               :button-id "action:112233:5", :message-id "1502625779698106469", :user-id "128868114438811111"}
;;
;; "on-message:" {:event #object[net.dv8tion.jda.api.events.message.MessageReceivedEvent 0xed23b0c "MessageReceivedEvent"],
;;                :user-id "128868114438811111", :content "hello"}


(defn- validate-handlers! [handlers]
  (doseq [k [:on-message :on-button :on-token]]
    (when-not (fn? (get handlers k))
      (throw (ex-info (str (name k) " handler is required") {})))))


(defstate jda-conn
  :start (let [cfg (args)
                gateway-handlers (handlers/create-gateway-handlers)
                _ (log! ["starting jda:" {:app-id (:discord-app-id cfg)}])
                ^JDA connection (jda/connect! cfg gateway-handlers)]
            (log! ["jda connection status:" (str (.getStatus connection))])
            connection)
  :stop (do
          (log! ["stopping jda:" jda-conn])
          (when jda-conn
            (jda/disconnect! jda-conn))))


(defstate app-handlers
  :start (let [created-handlers (handlers/create-handlers {:jda jda-conn})]
           (validate-handlers! created-handlers)
           created-handlers))


(defstate callback-server
  :start (let [cfg      (args)
                host     (:discord-callback-host cfg)
                port     (:discord-callback-port cfg)
                path     (:discord-callback-path cfg)
                on-token (:on-token app-handlers)
                on-code  (fn [code]
                           (auth/handle-code! cfg on-token code))]
            (callback/start! host port path on-code))
  :stop (callback/stop! callback-server))
