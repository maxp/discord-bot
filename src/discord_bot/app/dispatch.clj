(ns discord-bot.app.dispatch)


(set! *warn-on-reflection* true)


(defn handle-message
  [payload]
  (prn "on-message:" payload))


(defn handle-button
  [payload]
  (prn "on-button:" payload))
