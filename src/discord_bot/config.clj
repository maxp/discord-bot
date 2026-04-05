(ns discord-bot.config
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]))


(defn- env-raw
  [name]
  (some-> (System/getenv name) not-empty))


(defn- env-str
  ([name]
   (env-raw name))
  ([name default]
   (or (env-raw name) default)))


(defn- env-int
  ([name]
   (some-> (env-raw name) parse-long))
  ([name default]
   (or (env-int name) default)))


(defn build-info
  []
  (-> "build-info.edn" io/resource slurp edn/read-string))


(defn load-config
  []
  {:discord-bot-token (env-str "DISCORD_BOT_TOKEN")
   :discord-application-id (env-str "DISCORD_APPLICATION_ID")
   :discord-public-key (env-str "DISCORD_PUBLIC_KEY")
   :discord-http-host (env-str "DISCORD_HTTP_HOST" "localhost")
   :discord-http-port (env-int "DISCORD_HTTP_PORT" 8004)
   :build-info (build-info)})
