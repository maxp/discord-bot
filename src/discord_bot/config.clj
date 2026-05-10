(ns discord-bot.config
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]))


(defn- env-str
  ([name]
   (env-str name nil))
  ([name default]
   (or (System/getenv name) default)))


(def build-info
  (-> "build-info.edn" io/resource slurp edn/read-string))


(defn- require-not-empty! [cfg k]
  (let [value (get cfg k)]
    (when-not (not-empty value)
      (throw (ex-info (str (name k) " is empty") {:key k :value value})))
    cfg))


(defn- require-positive-int! [cfg k]
  (let [value (get cfg k)]
    (when-not (and (int? value) (pos? value))
      (throw (ex-info (str (name k) " must be a positive integer")
                      {:key k :value value})))
    cfg))


(defn- require-callback-path! [cfg]
  (let [value (:discord-callback-path cfg)]
    (when-not (and (not-empty value) (.startsWith ^String value "/"))
      (throw (ex-info "discord-callback-path must start with /"
                      {:key :discord-callback-path :value value})))
    cfg))


(defn load-config []
  {:discord-bot-token     (env-str "DISCORD_BOT_TOKEN")
   :discord-app-id        (env-str "DISCORD_APP_ID")
   :discord-app-secret    (env-str "DISCORD_APP_SECRET")
   :discord-proxy-url     (env-str "DISCORD_PROXY_URL")
   :discord-timeout       (parse-long (env-str "DISCORD_TIMEOUT" "20"))
   :discord-callback-url  (env-str "DISCORD_CALLBACK_URL")
   :discord-callback-host (env-str "DISCORD_CALLBACK_HOST" "localhost")
   :discord-callback-port (parse-long (env-str "DISCORD_CALLBACK_PORT" "8131"))
   :discord-callback-path (env-str "DISCORD_CALLBACK_PATH" "/discord/callback")
   :build-info build-info})


(defn validate-config!
  [cfg]
  (-> cfg
      (require-not-empty! :discord-bot-token)
      (require-not-empty! :discord-app-id)
      (require-not-empty! :discord-app-secret)
      (require-not-empty! :discord-callback-url)
      (require-not-empty! :discord-callback-host)
      (require-positive-int! :discord-timeout)
      (require-positive-int! :discord-callback-port)
      (require-callback-path!)))
