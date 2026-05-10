(ns discord-bot.app.auth
  (:require
   [discord-bot.discord.oauth :as oauth]
   [discord-bot.discord.rest :as rest]
   [discord-bot.discord.jda :as jda]
   [discord-bot.discord.users :as users]
   [taoensso.telemere :refer [log!]])
  (:import
   (net.dv8tion.jda.api JDA)))


(set! *warn-on-reflection* true)


(defn exchange-code!
  [{:keys [discord-app-id discord-app-secret discord-callback-url discord-proxy-url discord-timeout]} code]
  (let [api-opts (rest/create-client {:proxy-url discord-proxy-url
                                      :timeout   discord-timeout})]
    {:api-opts api-opts
     :result   (oauth/exchange-code!
                (assoc api-opts
                       :client-id     discord-app-id
                       :client-secret discord-app-secret
                       :redirect-uri  discord-callback-url)
                code)}))


(defn handle-code!
  [cfg on-token code]
  (let [{:keys [api-opts result]} (exchange-code! cfg code)]
    (log! ["token exchange completed" {:ok (:ok result) :status (:status result)}])
    (if (:ok result)
      (do
        (on-token api-opts result)
        {:ok true})
      {:ok false
       :status (:status result)
       :message (or (some-> result :data :message) "Unknown error")})))


(defn handle-token!
  [^JDA jda api-opts result]
  (let [access-token (or (some-> result :data :access_token)
                         (throw (ex-info "Missing access_token in OAuth2 result"
                                         {:result result})))]
    (log! ["on-token: fetching user info" {:has-token (boolean (not-empty access-token))}])
    (let [user-result (users/get-current-user! api-opts access-token)]
      (if (:ok user-result)
        (let [user-id (or (some-> user-result :data :id)
                          (throw (ex-info "Missing user id in Discord user response"
                                          {:user-result user-result})))]
          (log! ["on-token: sending welcome" {:user-id user-id}])
          (jda/send-message jda user-id "Welcome! Authorization successful."))
        (log! :warn ["on-token: failed to fetch user info" {:status (:status user-result)}])))))
