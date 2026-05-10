(ns user
  (:require
   [discord-bot.app.core :as app]
   [discord-bot.config :as config]
   [discord-bot.discord.oauth :as oauth]
   [discord-bot.discord.jda :as jda]
   [mount.core :as mount]))

(set! *warn-on-reflection* true)


(defn start []
  (mount/start-with-args (config/load-config)))


(defn stop []
  (mount/stop))


(defn restart []
  (stop)
  (start))


(defn send-message-with-buttons [user-id text]
  (let [buttons (mapv (fn [n] {:id (str "action:112233:" n) :label (str n)}) (range 1 6))]
    (jda/send-message app/jda-conn user-id text buttons)))



(comment

  
  (def user-id (System/getenv "USER_ID_1"))

  (send-message-with-buttons user-id "Тест кнопок.")

  (oauth/oauth2-authorize-url (config/load-config) :state "dev")
  ;;=> "https://discord.com/oauth2/authorize?client_id=1472858289346842646&response_type=code&redirect_uri=https%3A%2F%2Fvsp.isgood.host%2Fdiscord%2Fcallback&scope=identify%20applications.commands&integration_type=1&state=dev"
  

  (oauth/oauth2-authorize-url (config/load-config))
  ;; https://discord.com/oauth2/authorize?client_id=1472858289346842646&response_type=code&redirect_uri=https%3A%2F%2Fvsp.isgood.host%2Fdiscord%2Fcallback&scope=identify%20applications.commands&integration_type=1

  
  (start)
  (stop)
  
  )
