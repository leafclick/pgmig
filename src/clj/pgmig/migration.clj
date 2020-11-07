(ns pgmig.migration
  (:require [migratus.core :as migratus]
            [taoensso.timbre :as log]
            [pgmig.config :as config]
            [pgmig.db.store :refer [db-spec]]
            [pgmig.migration.sci]))


(defn migration-config
  [options]
  (let [migration-dir (:resource-dir config/env)
        classpath (:classpath options)]
    (log/info (str "Using migration dir '" migration-dir "'"))
    (log/info (str "Using migration classpath '" classpath "'"))
    (cond->
      {:store         :database
       :migration-dir migration-dir
       :db            db-spec}
      classpath (assoc :classpath classpath))))

(defn migrate [options]
  (migratus/migrate (migration-config options)))

(defn init [options]
  (migratus/init (migration-config options)))

(defn reset [options]
  (migratus/reset (migration-config options)))

(defn print-migrations [migrations]
  (doseq [[id name] migrations]
    (println (str id " " name))))

(defn pending [options]
  (let [migrations (->> (migratus/select-migrations (migration-config options) migratus/uncompleted-migrations)
                        (sort-by first))]
    (log/info "Pending migrations:" (count migrations))
    (print-migrations migrations)))

(defn list-migrations [options]
  (let [migrations (->> (migratus/select-migrations (migration-config options) migratus/completed-migrations)
                        (sort-by first))]
    (log/info "Completed migrations:" (count migrations))
    (print-migrations migrations)))

(defn up [arguments options]
  (apply migratus/up (migration-config options) arguments))

(defn down [arguments options]
  (apply migratus/down (migration-config options) arguments))

(defn create [arguments options]
  (migratus/create (migration-config options) arguments (:format options)))
