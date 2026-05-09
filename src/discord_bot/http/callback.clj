(ns discord-bot.http.callback
  (:require
   [clojure.string :as str]
   [discord-bot.config :as config]
   [discord-bot.discord.api :as api]
   [org.httpkit.server :as httpkit]
   [taoensso.telemere :refer [log!]])
  (:import
   (java.net URLDecoder)))


(set! *warn-on-reflection* true)


(defn- decode-url [^String value]
  (URLDecoder/decode value "UTF-8"))


(defn- parse-query-string [query-string]
  (when (not-empty query-string)
    (into {}
          (map (fn [part]
                 (let [[k v] (str/split part #"=" 2)]
                   [(decode-url k) (decode-url (or v ""))])))
          (str/split query-string #"&"))))


(defn- html-response [status body]
  {:status  status
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body    body})


(defn- handle-discord-callback [req]
  (let [params (parse-query-string (:query-string req))
        code   (get params "code")
        state  (get params "state")
        cfg    (config/load-config)]
    (log! ["discord oauth2 callback" {:state state :has-code (boolean (not-empty code))}])
    (if (not-empty code)
      (try
        (let [api-opts (api/create-client {:proxy-url (:discord-proxy-url cfg)
                                           :timeout   (:discord-timeout cfg)})
              result   (api/exchange-code!
                         (assoc api-opts
                                :client-id     (:discord-app-id cfg)
                                :client-secret (:discord-app-secret cfg)
                                :redirect-uri  (:discord-callback-url cfg))
                         code)]
          (log! ["token exchange completed" {:ok (:ok result) :status (:status result)}])
          (if (:ok result)
            (html-response 200 "<h1>Authorization successful!</h1>")
            (html-response 400
                           (str "<h1>Authorization failed</h1>"
                                "<p>" (get-in result [:data :message] "Unknown error") "</p>"))))
        (catch Exception ex
          (log! :warn ["token exchange failed" ex])
          (html-response 500 "<h1>Internal error</h1>")))
      (html-response 400 "<h1>Missing authorization code</h1>"))))


(defn router
  "HTTP request router for the callback server."
  [req]
  (case (:uri req)
    "/discord/callback" (handle-discord-callback req)
    {:status 404 :body "Not found"}))


(defn start!
  "Start the HTTP callback server on the given host and port."
  [host port]
  (log! ["starting callback server" {:host host :port port}])
  (httpkit/run-server router {:host host :port port}))


(defn stop!
  "Stop the HTTP callback server."
  [server]
  (when server
    (log! ["stopping callback server"])
    (server)))
