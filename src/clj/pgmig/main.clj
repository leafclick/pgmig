(ns pgmig.main
  (:require [pgmig.migration :as migration]
            [mount.core :as mount]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [taoensso.timbre :as log])
  (:gen-class))


(def cli-options
  [["-a" "--adapter ADAPTER" "Database Adapter"
    :default "postgresql"]
   ["-h" "--server-host SERVER_HOST" "Database Host Address"
    :default "localhost"]
   ["-p" "--server-port SERVER_PORT" "Database Port Number"
    :default 5432
    :parse-fn #(Integer/parseInt %)]
   ["-d" "--database-name DATABASE_NAME" "Database Name"]
   ["-u" "--dbuser DBUSER" "Database User"]
   ["-P" "--dbpass DBPASS" "Database User's Password"]
   ["-j" "--jdbc-url JDBC_URL" "JDBC Connection URL"]
   ["-r" "--resource-dir RESOURCE_DIR" "Resources Directory"
    :default "db/migrations"]
   ["-l" "--level LEVEL" "Verbosity Level (trace/debug/info/warn/error)"
    :default :info
    :parse-fn (comp keyword str/lower-case)]
   ["" "--help"]])

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

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

(defn parse-and-validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with a error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)]
    (cond
      (:help options)                                       ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}

      errors                                                ; errors => exit with description of errors
      {:exit-message (error-msg errors)}

      ;; custom validation on arguments
      (and (= (count arguments) 1)
           (#{"init" "list" "pending" "migrate" "reset"} (first arguments)))
      {:action (first arguments) :options options}

      (and (> (count arguments) 1)
           (#{"create"} (first arguments)))
      {:action (first arguments) :arguments (str/join " " (rest arguments)) :options options}

      (and (> (count arguments) 1)
           (#{"up" "down" "create"} (first arguments)))
      {:action (first arguments) :arguments (map #(Long/parseLong %) (rest arguments)) :options options}

      :else                                                 ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))

(defn stop-app []
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped"))
  (shutdown-agents))

(defn run-action [args]
  (let [{:keys [action arguments]} args]
    (case action
      "init" (migration/init)
      "list" (migration/list-migrations)
      "pending" (migration/pending)
      "migrate" (migration/migrate)
      "reset" (migration/reset)
      "up" (migration/up arguments)
      "down" (migration/down arguments)
      "create" (migration/create arguments))))

(defn start-app [args]
  (doseq [component (-> args
                        mount/start-with-args
                        :started)]
    (log/info component "started"))
  (.addShutdownHook (Runtime/getRuntime) (Thread. ^Runnable stop-app)))

(defn -main
  [& raw-args]
  (let [{:keys [exit-message ok?] :as args}
        (parse-and-validate-args raw-args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (do
        (start-app (:options args))
        (run-action args)))))
