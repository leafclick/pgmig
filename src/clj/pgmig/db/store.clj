(ns pgmig.db.store
  (:require [mount.core :refer [args defstate]]
            [hikari-cp.core :as hikari]
            [taoensso.timbre :as log]
            [clojure.java.jdbc :as jdbc]
            [pgmig.config :as config]))

(def default-datasource-options
  {:minimum-idle       1
   :maximum-pool-size  2})

(defn datasource-options [config]
  (let [{:keys [adapter host port dbname username password jdbc-url]} config
        ext-options (select-keys config [:adapter :datasource :datasource-classname :auto-commit :configure
                                        :connection-test-query :connection-timeout :validation-timeout :idle-timeout
                                        :max-lifetime :maximum-pool-size :minimum-idle :password :pool-name
                                        :read-only :username :leak-detection-threshold :register-mbeans :jdbc-url
                                        :driver-class-name :connection-init-sql :metric-registry :health-check-registry])
        pool-options (merge default-datasource-options ext-options)]
    (cond-> pool-options
            adapter (conj [:adapter adapter])
            host (conj [:server-name host])
            port (conj [:port-number port])
            dbname (conj [:database-name dbname])
            username (conj [:username username])
            password (conj [:password password])
            jdbc-url (conj [:jdbc-url jdbc-url]))))

(defstate db-spec
  :start (let [opt (datasource-options config/env)
               dbpool (try (hikari/make-datasource opt)
                           (catch Exception e (prn e) (throw e)))]
           {:datasource dbpool})
  :stop (when-let [ds (:datasource db-spec)]
          (hikari/close-datasource ds)))

(defn health-check
  "Test the database connection. Return true if valid; falsy value on connection error or timeout"
  [timeout-in-s]
  (try
    (with-open [connection (jdbc/get-connection db-spec)]
      (.isValid connection timeout-in-s))
    (catch Exception e
      (log/error "Database connection error: " (.getMessage e)))))
