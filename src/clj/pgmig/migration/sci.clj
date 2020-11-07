(ns pgmig.migration.sci
  (:require [clojure.string :as string]
            [migratus.protocols :as proto]
            [next.jdbc :as njdbc]
            [next.jdbc.sql :as sql]
            [sci.core :as sci]
            [babashka.impl.pprint]
            [babashka.impl.clojure.java.io]
            [babashka.impl.cheshire]
            [clojure.java.io :as io])
  (:import (java.util UUID)
           (java.io File)))

(defn create-bindings
  [config]
  {'get-config     (fn [] config)
   'get-datasource (fn [] (some-> config :db :datasource))
   'get-connection (fn [] (some-> config :conn :connection))
   'random-uuid    (fn [] (UUID/randomUUID))})

(defn find-file-on-classpath ^File
  [classpath base-path]
  (let [separator-pattern (re-pattern (or (System/getProperty "path.separator") ":"))
        base-paths (string/split classpath separator-pattern)]
    (some (fn [cp-entry]
            (let [f (io/file cp-entry base-path)]
              (when (.exists f) f)))
          base-paths)))

(defn create-load-fn
  [config]
  (fn [{:keys [namespace]}]
    (let [base-paths (:classpath config "")
          ns-path (some-> (name namespace)
                          (munge)
                          (string/replace "." "/")
                          (str ".clj"))]
      (if-let [f (find-file-on-classpath base-paths ns-path)]
        {:file   (.getAbsolutePath f)
         :source (slurp f)}
        (binding [*out* *err*]
          (println "WARNING: file" ns-path "not found")
          nil)))))

(defn enhance-sci-opts
  [sci-opts config]
  (-> sci-opts
      (update :bindings merge (create-bindings config))
      (assoc :load-fn (create-load-fn config))))

(defrecord SciMigration [id name config sci-opts up down]
  proto/Migration
  (id [this] id)
  (name [this] name)
  (tx? [this direction] true)
  (up [this config]
    (when up
      (sci/binding
        [sci/out *out*
         sci/err *err*]
        (sci/eval-string up (enhance-sci-opts sci-opts config)))))
  (down [this config]
    (when down
      (sci/binding
        [sci/out *out*
         sci/err *err*]
        (sci/eval-string down (enhance-sci-opts sci-opts config))))))

;; stolen from babashka -- https://github.com/borkdude/babashka/blob/master/feature-jdbc/babashka/impl/jdbc.clj
(def next-ns (sci/create-ns 'next.jdbc))

(def njdbc-namespace
  {'get-datasource   (sci/copy-var njdbc/get-datasource next-ns)
   'execute!         (sci/copy-var njdbc/execute! next-ns)
   'execute-one!     (sci/copy-var njdbc/execute-one! next-ns)
   'get-connection   (sci/copy-var njdbc/get-connection next-ns)
   'plan             (sci/copy-var njdbc/plan next-ns)
   'prepare          (sci/copy-var njdbc/prepare next-ns)
   'transact         (sci/copy-var njdbc/transact next-ns)
   'with-transaction (sci/copy-var njdbc/with-transaction next-ns)})

(def sns (sci/create-ns 'next.jdbc.sql))

(def next-sql-namespace
  {'insert-multi! (sci/copy-var sql/insert-multi! sns)})

(def allowed-namespaces
  {'clojure.java.io babashka.impl.clojure.java.io/io-namespace
   'cheshire.core   babashka.impl.cheshire/cheshire-core-namespace
   'clojure.pprint  babashka.impl.pprint/pprint-namespace
   'next.jdbc       njdbc-namespace
   'next.jdbc.sql   next-sql-namespace})

(def imports
  '{ArithmeticException      java.lang.ArithmeticException
    AssertionError           java.lang.AssertionError
    BigDecimal               java.math.BigDecimal
    BigInteger               java.math.BigInteger
    Boolean                  java.lang.Boolean
    Byte                     java.lang.Byte
    Character                java.lang.Character
    Class                    java.lang.Class
    ClassNotFoundException   java.lang.ClassNotFoundException
    Comparable               java.lang.Comparable
    Double                   java.lang.Double
    Exception                java.lang.Exception
    IllegalArgumentException java.lang.IllegalArgumentException
    Integer                  java.lang.Integer
    File                     java.io.File
    Float                    java.lang.Float
    Long                     java.lang.Long
    Math                     java.lang.Math
    Number                   java.lang.Number
    NumberFormatException    java.lang.NumberFormatException
    Object                   java.lang.Object
    Runtime                  java.lang.Runtime
    RuntimeException         java.lang.RuntimeException
    Process                  java.lang.Process
    ProcessBuilder           java.lang.ProcessBuilder
    Short                    java.lang.Short
    String                   java.lang.String
    StringBuilder            java.lang.StringBuilder
    System                   java.lang.System
    Thread                   java.lang.Thread
    Throwable                java.lang.Throwable
    UUID                     java.util.UUID
    Date                     java.util.Date})

(defmethod proto/make-migration* :clj
  [_ mig-id mig-name payload config]
  (let [{:keys [up down]} payload
        sci-opts {:namespaces allowed-namespaces
                  :imports    imports}]
    (->SciMigration mig-id mig-name config sci-opts up down)))

(defmethod proto/get-extension* :clj
  [_]
  "clj")

(defmethod proto/migration-files* :clj
  [x migration-name]
  (let [ext (proto/get-extension* x)]
    [(str migration-name ".up." ext)
     (str migration-name ".down." ext)]))
