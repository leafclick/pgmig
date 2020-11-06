(ns pgmig.migration
  (:require [migratus.core :as migratus]
            [taoensso.timbre :as log]
            [pgmig.config :as config]
            [pgmig.db.store :refer [db-spec]]
            [pgmig.migration.sci]))


(defn migration-config []
  (let [migration-dir (:resource-dir config/env)]
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

(defn print-migrations [migrations]
  (doseq [[id name] migrations]
    (println (str id " " name))))

(defn pending []
  (let [migrations (->> (migratus/select-migrations (migration-config) migratus/uncompleted-migrations)
                        (sort-by first))]
    (log/info "Pending migrations:" (count migrations))
    (print-migrations migrations)))

(defn list-migrations []
  (let [migrations (->> (migratus/select-migrations (migration-config) migratus/completed-migrations)
                        (sort-by first))]
    (log/info "Completed migrations:" (count migrations))
    (print-migrations migrations)))

(defn up [arguments]
  (apply migratus/up (migration-config) arguments))

(defn down [arguments]
  (apply migratus/down (migration-config) arguments))

(defn create [arguments options]
  (migratus/create (migration-config) arguments (:format options)))
