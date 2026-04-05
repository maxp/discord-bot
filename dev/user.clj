(ns user
  (:require
    [discord-bot.app.core]
    [discord-bot.config :as config]
    [mount.core :as mount]))


(defn start
  []
  (mount/start-with-args (config/load-config)))



(defn stop
  []
  (mount/stop))



(defn restart
  []
  (stop)
  (start))
