(ns user
  (:require
   [discord-bot.app.core :as app]
   [discord-bot.config :as config]
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
    (jda/send-message app/conn user-id text buttons)))



(comment

  
  (def user-id (System/getenv "USER_ID_1"))

  (send-message-with-buttons user-id "Тест кнопок.")
  

  (start)
  (stop)
  
  )
