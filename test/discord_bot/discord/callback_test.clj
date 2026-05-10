(ns discord-bot.discord.callback-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [discord-bot.discord.callback :as callback]))


(deftest callback-returns-200-on-success
  (let [token-received (atom nil)
        on-code (fn [code]
                  (reset! token-received code)
                  {:ok true})
        handler (callback/router "/discord/callback" on-code)
        resp    (handler {:uri "/discord/callback"
                          :query-string "code=abc&state=xyz"})]
    (is (= 200 (:status resp)))
    (is (str/includes? (:body resp) "Authorization successful"))
    (is (= "abc" @token-received))))


(deftest callback-returns-400-when-code-missing
  (let [handler (callback/router "/discord/callback" nil)
        resp    (handler {:uri "/discord/callback"
                          :query-string "state=xyz"})]
    (is (= 400 (:status resp)))
    (is (str/includes? (:body resp) "Missing authorization code"))))


(deftest callback-returns-404-for-unknown-path
  (let [handler (callback/router "/discord/callback" nil)
        resp    (handler {:uri "/other"})]
    (is (= 404 (:status resp)))))
