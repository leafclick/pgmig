(ns pgmig.migration
  (:require [migratus.core :as migratus]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [pgmig.config :refer [env]]
            [pgmig.db.store :refer [db-spec]]))


(defn migration-config []
  (let [migration-dir (:resource-dir env)]
    (log/info (str "Using migration dir '" migration-dir "'"))
    {:store         :database
     :migration-dir migration-dir
     :db            db-spec}))

(defn migrate []
  (migratus/migrate (migration-config)))

(defn init []
  (migratus/init (migration-config)))

(defn reset []
  (migratus/reset (migration-config)))

(defn pending []
  (let [migrations (migratus/select-migrations (migration-config) migratus/uncompleted-migrations)]
    (log/info (apply str "You have " (count migrations) " PENDING migration(s)"
                     (if-not (empty? migrations) ":\n" ".")
                     (str/join "\n" migrations)))))

(defn list-migrations []
  (let [migrations (migratus/select-migrations (migration-config) migratus/completed-migrations)]
    (log/info (apply str "You have " (count migrations) " completed migration(s)"
                     (if-not (empty? migrations) ":\n" ".")
                     (str/join "\n" migrations)))))

(defn up [arguments]
  (apply migratus/up (migration-config) arguments))

(defn down [arguments]
  (apply migratus/down (migration-config) arguments))

(defn create [arguments]
  (migratus/create (migration-config) arguments))
