(ns pgmig.config
  (:require [mount.core :refer [args defstate]]
            [pgmig.logging :as logging])
  (:import (java.nio.file Path Paths Files LinkOption)))

(defstate env :start (let [config (args)
                           logging-options (-> (select-keys config [:output-fn :timestamp-opts])
                                               (assoc :min-level (:level config :warn)))]
                       (logging/configure-logging logging-options)
                       config))

(def ^:const DEFAULT-MIGRATION-DIR "resources/migrations")

(defn get-resource-dir
  [{:keys [resource-dir]}]
  (let [configured-path (when resource-dir (Paths/get resource-dir (make-array String 0)))
        default-path (Paths/get DEFAULT-MIGRATION-DIR (make-array String 0))
        resource-path (if (and configured-path
                               (Files/isDirectory configured-path (make-array LinkOption 0)))
                        configured-path
                        default-path)]
    (when (Files/exists resource-path (make-array LinkOption 0))
      (.toString ^Path resource-path))))
