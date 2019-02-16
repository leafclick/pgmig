(ns pgmig.config
  (:require [cprop.core :refer [load-config]]
            [cprop.source :as source]
            [mount.core :refer [args defstate]]
            [clojure.string :as str]
            [pgmig.logging :as logging])
  (:import (java.nio.file Paths Path Files LinkOption)))

(defstate env :start (let [config (load-config :merge
                                               [(args)
                                                (source/from-system-props)
                                                (source/from-env)])
                           logging-options (select-keys config [:level :output-fn :timestamp-opts])]
                       (logging/configure-logging (merge {:level :info} logging-options))
                       config))

(defn path-as-string [^Path p]
  (.toString (.toAbsolutePath p)))

(defn- append-trailing-slash [^String s]
  (if (str/ends-with? s "/")
    s
    (str s "/")))

(defn get-resource-dir
  ([env]
   (let [resource-dir (:resource-dir env)
         configured-path (when resource-dir (Paths/get resource-dir (make-array String 0)))
         default-path (Paths/get "resources" (make-array String 0))]
     (if (and configured-path
              (Files/isDirectory configured-path (make-array LinkOption 0)))
       (path-as-string configured-path)
       (path-as-string default-path))))
  ([env suffix]
   (let [resource-dir (get-resource-dir env)
         suffix (append-trailing-slash suffix)]
     (str resource-dir "/" suffix))))