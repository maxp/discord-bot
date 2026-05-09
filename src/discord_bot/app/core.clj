(ns discord-bot.app.core
  (:require
   [discord-bot.discord.jda :as jda]
   [mount.core :refer [args defstate]]
   [taoensso.telemere :refer [log!]])
  (:import
   (net.dv8tion.jda.api JDA)))


(set! *warn-on-reflection* true)


(def handlers 
  {:on-message (fn [p] (prn "on-message:" p))
   :on-button  (fn [p] (prn "on-button:" p))
   })


;; sample:
;;
;; "on-button:" {:event #object[net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent 0x283fd4c0 
;;                      "ButtonInteractionEvent(id=1502625799017201775)"], 
;;               :button-id "action:112233:5", :message-id "1502625779698106469", :user-id "128868114438811111"}
;;
;; "on-message:" {:event #object[net.dv8tion.jda.api.events.message.MessageReceivedEvent 0xed23b0c "MessageReceivedEvent"], 
;;                :user-id "128868114438811111", :content "hello"}


(defstate conn
  :start (let [cfg (args)
               _ (log! ["starting bot:" {:app-id (:discord-app-id cfg)}])
               ^JDA connection (jda/connect! cfg handlers)]
           (log! ["jda connection status:" (str (.getStatus connection))])
           connection)
  :stop (do
          (log! ["stopping bot runtime:" conn])
          (when conn
            (jda/disconnect! conn))))
