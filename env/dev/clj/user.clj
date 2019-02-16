(ns user
  (:require [mount.core :as mount]))

(defn start []
  (do
    (require 'pgmig.core)
    (let [repl-server (ns-resolve 'pgmig.core 'repl-server)]
      (mount/start-without repl-server))))

(defn stop []
  (let [repl-server (ns-resolve 'pgmig.core 'repl-server)]
    (mount/stop-except repl-server)))

(defn restart []
  (stop)
  (start))