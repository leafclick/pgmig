(ns pgmig.logging
  (:require [alexandermann.unclogging :as unclog]
            [taoensso.timbre :as log]))

(defn configure-logging
  ([]
   (configure-logging {:output-fn log/default-output-fn}))
  ([config]
   (if (:output-fn config)
     (unclog/merge-config! config)
     (unclog/merge-config! (assoc config :output-fn log/default-output-fn)))))