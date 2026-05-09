(ns discord-bot.discord.api
  (:require
   [clojure.string :as str]
   [discord-bot.discord.proxy :as proxy]
   [jsonista.core :as json])
  (:import
   (okhttp3 Credentials FormBody$Builder OkHttpClient Request Request$Builder Response ResponseBody)))


(set! *warn-on-reflection* true)


(def ^:private default-base-url "https://discord.com/api/v10")
(def ^:private default-timeout-seconds 20)


(defn- require-not-empty! [field value]
  (when-not (not-empty value)
    (throw (ex-info (str (name field) " is empty") {field value})))
  value)


(defn- api-url [{:keys [base-url]} path]
  (str (str/replace (or base-url default-base-url) #"/+$" "") path))


(defn- http-client [{:keys [^OkHttpClient http-client proxy-url timeout]}]
  (or http-client
      (proxy/okhttp-client proxy-url (or timeout default-timeout-seconds))))


(defn create-client
  "Create reusable Discord API options with a shared OkHttp client."
  [opts]
  (assoc opts :http-client (http-client opts)))


(defn- form-body [params]
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


(defn- execute! [opts ^Request request]
  (let [^OkHttpClient client (http-client opts)]
    (with-open [response (.execute (.newCall client request))]
      (response->result response))))


(defn exchange-code!
  "Exchange an OAuth2 authorization code for a Discord access token."
  [{:keys [client-id client-secret redirect-uri] :as opts} code]
  (let [client-id     (require-not-empty! :client-id client-id)
        client-secret (require-not-empty! :client-secret client-secret)
        redirect-uri  (require-not-empty! :redirect-uri redirect-uri)
        code          (require-not-empty! :code code)
        ^String url   (api-url opts "/oauth2/token")
        auth-header   (Credentials/basic client-id client-secret)
        ^Request$Builder builder (doto (Request$Builder.)
                                   (.url url)
                                   (.header "Accept" "application/json")
                                   (.header "Authorization" auth-header)
                                   (.post (form-body {:grant_type "authorization_code"
                                                      :code code
                                                      :redirect_uri redirect-uri})))
        request       (.build builder)]
    (execute! opts request)))


(defn get-current-user!
  "Fetch `GET /users/@me` using a Discord OAuth2 bearer access token."
  [opts access-token]
  (let [access-token (require-not-empty! :access-token access-token)
        ^String url  (api-url opts "/users/@me")
        auth-header  (str "Bearer " access-token)
        ^Request$Builder builder (doto (Request$Builder.)
                                   (.url url)
                                   (.get)
                                   (.header "Accept" "application/json")
                                   (.header "Authorization" auth-header))
        request      (.build builder)]
    (execute! opts request)))
