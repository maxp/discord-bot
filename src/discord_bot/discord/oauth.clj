(ns discord-bot.discord.oauth
  (:require
   [discord-bot.discord.rest :as rest])
  (:import
   (java.net URLEncoder)
   (okhttp3 Credentials Request Request$Builder)))


(set! *warn-on-reflection* true)


(defn oauth2-authorize-url
  "Build the Discord OAuth2 authorization URL.
   redirect-uri is taken from opts :discord-callback-url.
   Optional state parameter is URL-encoded."
  [{:keys [discord-app-id discord-callback-url]} & {:keys [^String state]}]
  (let [client-id    (rest/require-not-empty! :client-id discord-app-id)
        redirect-uri (rest/require-not-empty! :redirect-uri discord-callback-url)
        ^String encoded-uri (URLEncoder/encode ^String redirect-uri "UTF-8")
        base         (str "https://discord.com/oauth2/authorize"
                          "?client_id=" client-id
                          "&response_type=code"
                          "&redirect_uri=" encoded-uri
                          "&scope=identify%20applications.commands"
                          "&integration_type=1")]
    (if (not-empty state)
      (str base "&state=" (URLEncoder/encode state "UTF-8"))
      base)))


(defn exchange-code!
  "Exchange an OAuth2 authorization code for a Discord access token."
  [{:keys [client-id client-secret redirect-uri] :as opts} code]
  (let [client-id     (rest/require-not-empty! :client-id client-id)
        client-secret (rest/require-not-empty! :client-secret client-secret)
        redirect-uri  (rest/require-not-empty! :redirect-uri redirect-uri)
        code          (rest/require-not-empty! :code code)
        ^String url   (rest/api-url opts "/oauth2/token")
        auth-header   (Credentials/basic client-id client-secret)
        ^Request$Builder builder (doto (Request$Builder.)
                                   (.url url)
                                   (.header "Accept" "application/json")
                                   (.header "Authorization" auth-header)
                                   (.post (rest/form-body {:grant_type "authorization_code"
                                                           :code code
                                                           :redirect_uri redirect-uri})))
        request       (.build builder)]
    (rest/execute! opts request)))
