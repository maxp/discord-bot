(ns discord-bot.app.core
  (:require
    [mount.core :refer [args defstate]]
    [taoensso.telemere :refer [log!]]))


(declare bot-runtime)


(defstate bot-runtime
  :start
  (let [cfg (args)
        build-info (:build-info cfg)]
    (log! ["starting bot runtime" {:appname (:appname build-info)
                                   :version (:version build-info)
                                   :http-host (:discord-http-host cfg)
                                   :http-port (:discord-http-port cfg)}])
    {:config cfg})
  :stop
  (log! ["stopping bot runtime"]))
