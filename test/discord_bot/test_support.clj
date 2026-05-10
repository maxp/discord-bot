(ns discord-bot.test-support
  (:require [clojure.string :as str])
  (:import
   (com.sun.net.httpserver HttpExchange HttpHandler HttpServer)
   (java.net InetSocketAddress URLDecoder)))


(defn decode-url [value]
  (URLDecoder/decode value "UTF-8"))


(defn parse-form-body [body]
  (into {}
        (map (fn [part]
               (let [[k v] (str/split part #"=" 2)]
                 [(decode-url k) (decode-url (or v ""))])))
        (str/split body #"&")))


(defn request-body [^HttpExchange exchange]
  (slurp (.getRequestBody exchange)))


(defn send-json! [^HttpExchange exchange status body]
  (let [bytes (.getBytes ^String body "UTF-8")]
    (.add (.getResponseHeaders exchange) "Content-Type" "application/json")
    (.sendResponseHeaders exchange status (alength bytes))
    (with-open [response-body (.getResponseBody exchange)]
      (.write response-body bytes))))


(defn with-test-server [handler f]
  (let [server (HttpServer/create (InetSocketAddress. "127.0.0.1" 0) 0)]
    (.createContext server "/" (reify HttpHandler
                                 (handle [_ exchange]
                                   (handler exchange))))
    (.start server)
    (try
      (f (str "http://127.0.0.1:" (.getPort (.getAddress server))))
      (finally
        (.stop server 0)))))
