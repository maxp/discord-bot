(ns discord-bot.app.core
  (:require
    [discord-bot.discord.jda :as jda]
    [mount.core :refer [args defstate]]
    [taoensso.telemere :refer [log!]]))


(declare bot-runtime)


(defn- log-entry!
  [entry]
  (log! entry))


(defstate bot-runtime
  :start
  (let [cfg (args)
        build-info (:build-info cfg)
        gateway (jda/connect! cfg log-entry!)]
    (log! ["starting bot runtime" {:appname (:appname build-info)
                                   :version (:version build-info)
                                   :jda-status (str (.getStatus gateway))}])
    {:config cfg
     :gateway gateway})
  :stop
  (do
    (when-let [gateway (:gateway bot-runtime)]
      (jda/disconnect! gateway))
    (log! ["stopping bot runtime"])))
