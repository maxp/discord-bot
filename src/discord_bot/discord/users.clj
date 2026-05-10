(ns discord-bot.discord.users
  (:require
   [discord-bot.discord.rest :as rest])
  (:import
   (okhttp3 Request Request$Builder)))


(set! *warn-on-reflection* true)


(defn get-current-user!
  "Fetch `GET /users/@me` using a Discord OAuth2 bearer access token."
  [opts access-token]
  (let [access-token (rest/require-not-empty! :access-token access-token)
        ^String url  (rest/api-url opts "/users/@me")
        auth-header  (str "Bearer " access-token)
        ^Request$Builder builder (doto (Request$Builder.)
                                   (.url url)
                                   (.get)
                                   (.header "Accept" "application/json")
                                   (.header "Authorization" auth-header))
        request      (.build builder)]
    (rest/execute! opts request)))
