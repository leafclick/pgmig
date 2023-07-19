(ns pgmig.main
  (:require [pgmig.migration :as migration]
            [environ.core :as environ]
            [mount.core :as mount]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [taoensso.timbre :as log]
            [pgmig.config :as config]
            [clojure.java.io :as io])
  (:gen-class))

(def env (atom {}))

(defn env-str
  [k]
  (get @env k))

(defn env-int
  [k]
  (when-let [es (env-str k)]
    (Long/parseLong es)))

(defn env-kw
  [k]
  (when-let [es (env-str k)]
    (keyword es)))

(def cli-options
  [["-a" "--adapter ADAPTER" "Database Adapter"
    :default-fn (fn [_]
                  (or (env-str :adapter)
                      "postgresql"))
    :default-desc "postgresql"]
   ["-h" "--host PGHOST" "Database Host Address"
    :default-fn (fn [_]
                  (or (env-str :pghost)
                      "localhost"))
    :default-desc "localhost"]
   ["-p" "--port PGPORT" "Database Port Number"
    :default-fn (fn [_]
                  (or (env-int :pgport)
                      5432))
    :default-desc "5432"
    :parse-fn #(Integer/parseInt %)]
   ["-d" "--dbname PGDATABASE" "Database Name"
    :default-fn (fn [_]
                  (env-str :pgdatabase))]
   ["-t" "--table TABLE_NAME" "Migration Table Name"
    :default-fn (fn [_]
                  (env-str :migtable))]
   ["-U" "--username PGUSER" "Database User"
    :default-fn (fn [_]
                  (env-str :pguser))]
   ["-P" "--password PGPASSWORD" "Database User's Password"
    :default-fn (fn [_]
                  (env-str :pgpassword))]
   ["-j" "--jdbc-url JDBC_URL" "JDBC Connection URL"
    :default-fn (fn [_]
                  (env-str :jdbc-url))]
   ["-r" "--resource-dir RESOURCE_DIR" "Resources Directory"
    :default-fn (fn [_]
                  (or (env-str :resource-dir)
                      config/DEFAULT-MIGRATION-DIR))
    :default-desc config/DEFAULT-MIGRATION-DIR]
   ["-l" "--level LEVEL" "Verbosity Level (trace/debug/info/warn/error)"
    :default-fn (fn [_]
                  (or (env-kw :level)
                      :warn))
    :default-desc "warn"
    :parse-fn (comp keyword str/lower-case)]
   ["-f" "--format " "Migration FORMAT (sql/clj/edn)"
    :default-fn (fn [_]
                  (or (env-kw :format)
                      :sql))
    :default-desc "sql"
    :parse-fn (comp keyword str/lower-case)]
   ["-c" "--classpath " "Classpath for migration support code"
    :default-fn (fn [_]
                  (or (env-str :classpath) "."))]
   ["-V" "--version" "Print PGMig version and quit"]
   ["" "--help"]])

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn exit [msg]
  (println msg)
  (System/exit 0))

(defn exit-error [status msg]
  (binding [*out* *err*]
    (println msg)
    (System/exit status)))

(defn usage [options-summary]
  (->> ["Standalone PostgreSQL Migration Runner"
        ""
        "Usage: pgmig [options] action [ids]"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "init       initialize the database"
        "list       list migrations already applied"
        "pending    list pending migrations"
        "migrate    apply pending migrations"
        "up         apply given migration ids"
        "down       rollback given migration ids"
        "reset      rollback and re-apply all migrations"
        "create     create a new migration with current timestamp"]
       (str/join \newline)))

(defn version []
  (let [pgmig-version (slurp (io/resource "PGMIG_VERSION"))]
    (str "pgmig " pgmig-version)))

(defn validate-command [{:keys [action options] :as command}]
  (if-let [resource-dir (config/get-resource-dir options)]
    (assoc-in command [:options :resource-dir] resource-dir)
    (assoc command :exit-message (str "Cannot access resources directory '" (:resource-dir options) "'"))))

(defn parse-and-validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with a error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)]
    (cond
      (:help options)                                       ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}

      (:version options)                                    ; help => exit OK version string
      {:exit-message (version) :ok? true}

      errors                                                ; errors => exit with description of errors
      {:exit-message (error-msg errors)}

      ;; custom validation on arguments
      (and (= (count arguments) 1)
           (#{"init" "list" "pending" "migrate" "reset"} (first arguments)))
      (validate-command {:action (first arguments) :options options})

      (and (> (count arguments) 1)
           (#{"create"} (first arguments)))
      (validate-command {:action (first arguments) :arguments (str/join " " (rest arguments)) :options options})

      (and (> (count arguments) 1)
           (#{"up" "down" "create"} (first arguments)))
      (validate-command {:action (first arguments) :arguments (map #(Long/parseLong %) (rest arguments)) :options options})

      :else                                                 ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))

(defn stop-app []
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped"))
  (shutdown-agents))

(defn run-action [args options]
  (let [{:keys [action arguments]} args]
    (case action
      "init" (migration/init options)
      "list" (migration/list-migrations options)
      "pending" (migration/pending options)
      "migrate" (migration/migrate options)
      "reset" (migration/reset options)
      "up" (migration/up arguments options)
      "down" (migration/down arguments options)
      "create" (migration/create arguments options))))

(defn start-app [args]
  (doseq [component (-> args
                        mount/start-with-args
                        :started)]
    (log/info component "started"))
  (.addShutdownHook (Runtime/getRuntime) (Thread. ^Runnable stop-app)))

(defn read-env []
  (let [merge-env #'environ/merge-env
        read-system-env #'environ/read-system-env
        read-system-props #'environ/read-system-props]
    (merge-env
      (read-system-env)
      (read-system-props))))

(defn -main
  [& raw-args]
  (reset! env (read-env))
  (let [{:keys [exit-message ok? options] :as args}
        (parse-and-validate-args raw-args)]
    (if-not exit-message
      (do
        (start-app options)
        (run-action args options))
      (if ok?
        (exit exit-message)
        (exit-error 1 exit-message)))))
