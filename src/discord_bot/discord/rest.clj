(ns discord-bot.discord.rest
  (:require
   [clojure.string :as str]
   [discord-bot.discord.proxy :as proxy]
   [jsonista.core :as json])
  (:import
   (okhttp3 FormBody$Builder OkHttpClient Request Response ResponseBody)))


(set! *warn-on-reflection* true)


(def ^:private default-base-url "https://discord.com/api/v10")
(def ^:private default-timeout-seconds 20)


(defn require-not-empty!
  [field value]
  (when-not (not-empty value)
    (throw (ex-info (str (name field) " is empty") {field value})))
  value)


(defn api-url
  [{:keys [base-url]} path]
  (str (str/replace (or base-url default-base-url) #"/+$" "") path))


(defn- http-client
  [{:keys [^OkHttpClient http-client proxy-url timeout]}]
  (or http-client
      (proxy/okhttp-client proxy-url (or timeout default-timeout-seconds))))


(defn create-client
  "Create reusable Discord API options with a shared OkHttp client."
  [opts]
  (assoc opts :http-client (http-client opts)))


(defn form-body
  [params]
  (let [builder (FormBody$Builder.)]
    (doseq [[k v] params]
      (when (some? v)
        (.add builder (name k) (str v))))
    (.build builder)))


(defn- parse-response-body [body]
  (when (not-empty body)
    (try
      (json/read-value body json/keyword-keys-object-mapper)
      (catch Exception _
        body))))


(defn- response->result [^Response response]
  (let [status (.code response)
        ^ResponseBody body (.body response)
        body-text (when body (.string body))]
    {:ok (<= 200 status 299)
     :status status
     :data (parse-response-body body-text)}))


(defn execute!
  [opts ^Request request]
  (let [^OkHttpClient client (http-client opts)]
    (with-open [response (.execute (.newCall client request))]
      (response->result response))))
