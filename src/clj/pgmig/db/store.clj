(ns pgmig.db.store
  (:require [mount.core :refer [args defstate]]
            [hikari-cp.core :as hikari]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [clojure.java.jdbc :as jdbc]
            [pgmig.config :refer [env]])
  (:import (org.postgresql.ds PGSimpleDataSource)
           (com.zaxxer.hikari HikariConfig)))

(defn jdbc-adapter [jdbc-uri]
  (if (str/starts-with? jdbc-uri "jdbc:")
    (if-let [adapter (second (str/split jdbc-uri #":"))]
      (keyword adapter)
      :unknown)
    :unknown))

(defmulti create-datasource jdbc-adapter)
(defmethod create-datasource :default [jdbc-uri]
  (throw (IllegalArgumentException. "Unknown datasource type.")))

(defmethod create-datasource :postgresql [jdbc-uri]
  (doto (PGSimpleDataSource.)
    (.setUrl jdbc-uri)))

(defn to-jdbc-uri [datasource-config]
  (cond
    (:jdbc-url datasource-config) (:jdbc-url datasource-config)
    (:url datasource-config) (:url datasource-config)
    :else (let [{:keys [adapter server-host server-port database-name dbuser dbpass]} datasource-config
                port (when server-port (str ":" server-port))
                password (when dbpass (str "&password=" dbpass))
                userauth (when dbuser (str "?user=" dbuser password))]
            (str "jdbc:" adapter "://" server-host port "/" database-name userauth))))

(defstate db-spec :start (let [jdbc-uri (to-jdbc-uri env)
                               dbpool (hikari/make-datasource {:datasource
                                                               (create-datasource jdbc-uri)
                                                               :maximum-pool-size 1})]
                           (log/info "Using jdbc-uri" jdbc-uri)
                           {:datasource dbpool})
  :stop (do
          (when-let [ds (:datasource db-spec)]
            (hikari/close-datasource ds))))

(defn is-postgresql? [db-spec]
  (let [^HikariConfig hikari-config (:datasource db-spec)]
    (instance? PGSimpleDataSource (.getDataSource hikari-config))))

(defn health-check
  "Test the database connection. Return true if valid; falsy value on connection error or timeout"
  [timeout-in-s]
  (try
    (with-open [connection (jdbc/get-connection db-spec)]
      (.isValid connection timeout-in-s))
    (catch Exception e
      (log/error "Database connection error: " (.getMessage e)))))
