(ns discord-bot.config
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]))


(defn- env-str
  ([name]
   (env-str name nil))
  ([name default]
   (or (System/getenv name) default)))


(defn build-info []
  (-> "build-info.edn" io/resource slurp edn/read-string))


(defn load-config []
  {:discord-bot-token (env-str "DISCORD_BOT_TOKEN")
   :discord-app-id    (env-str "DISCORD_APP_ID")
   :discord-proxy-url (env-str "DISCORD_PROXY_URL")
   :discord-timeout   (parse-long (env-str "DISCORD_TIMEOUT" "20"))
   :build-info (build-info)})
