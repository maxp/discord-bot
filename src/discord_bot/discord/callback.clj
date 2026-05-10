(ns discord-bot.discord.callback
  (:require
   [clojure.string :as str]
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


(defn- handle-discord-callback [req on-code]
  (let [params (parse-query-string (:query-string req))
         code   (get params "code")
         state  (get params "state")]
    (log! ["discord oauth2 callback" {:state state :has-code (boolean (not-empty code))}])
    (if (not-empty code)
      (try
        (let [result (on-code code)]
          (if (:ok result)
            (html-response 200 "<h1>Authorization successful!</h1>")
            (html-response (or (:status result) 400)
                           (str "<h1>Authorization failed</h1>"
                                "<p>" (or (:message result) "Unknown error") "</p>"))))
        (catch Exception ex
          (log! :warn ["token exchange failed" ex])
          (html-response 500 "<h1>Internal error</h1>")))
      (html-response 400 "<h1>Missing authorization code</h1>"))))


(defn router
  "HTTP request router for the callback server.
   path     — the callback endpoint path, e.g. /discord/callback.
   on-code  — handler invoked as (on-code code). Should return {:ok true} on
              success or {:ok false :status n :message s} on failure."
  [path on-code]
  (fn [req]
    (if (= (:uri req) path)
      (handle-discord-callback req on-code)
      {:status 404 :body "Not found"})))


(defn start!
  "Start the HTTP callback server on the given host, port and path.
   on-code — required handler invoked as (on-code code)."
  [host port path on-code]
  (when-not (fn? on-code)
    (throw (ex-info "on-code handler is required" {})))
  (log! ["starting callback server" {:host host :port port :path path}])
  (httpkit/run-server (router path on-code) {:host host :port port}))


(defn stop!
  "Stop the HTTP callback server."
  [server]
  (when server
    (log! ["stopping callback server"])
    (server)))
