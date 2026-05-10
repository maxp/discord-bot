(ns discord-bot.main
  (:gen-class)
  (:require
     [discord-bot.app.core]
     [discord-bot.config :refer [build-info load-config validate-config!]]
     [mount.core :as mount]
     [taoensso.telemere :refer [log!]]))

(set! *warn-on-reflection* true)


(defn -main
  [& _args]
  (log! ["init:" build-info])
  (try
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. #(try
                                  (mount/stop)
                                  (catch Throwable ex
                                    (log! :error ["shutdown hook failed" ex])))))
    (let [cfg (validate-config! (load-config))
          mnt (mount/start-with-args cfg)]
      (log! ["system started:" (:started mnt)])
      mnt)
    (catch Throwable ex
      (log! :warn ["main interrupted" ex])
      (throw ex))))
