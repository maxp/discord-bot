(ns discord-bot.main
  (:gen-class)
  (:require
    [discord-bot.app.core]
    [discord-bot.config :refer [build-info load-config]]
    [mount.core :as mount]
    [taoensso.telemere :refer [log!]]))


(defn -main
  [& _args]
  (log! ["init:" (build-info)])
  (try
    (.addShutdownHook (Runtime/getRuntime) (Thread. #(mount/stop)))
    (let [cfg (load-config)
          mnt (mount/start-with-args cfg)]
      (log! ["system started:" (:started mnt)])
      mnt)
    (catch Throwable ex
      (log! :warn ["main interrupted" ex])
      (Thread/sleep 3000)
      (throw ex))))
