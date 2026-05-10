(ns discord-bot.app.handlers
  (:require
   [discord-bot.app.auth :as auth]
   [discord-bot.app.dispatch :as dispatch]))


(set! *warn-on-reflection* true)


(defn create-gateway-handlers
  []
  {:on-message dispatch/handle-message
   :on-button  dispatch/handle-button})


(defn create-handlers
  [{:keys [jda]}]
  (assoc (create-gateway-handlers)
         :on-token (fn [api-opts result]
                     (auth/handle-token! jda api-opts result))))
