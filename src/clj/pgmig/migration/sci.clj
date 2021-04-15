(ns pgmig.migration.sci
  (:require [clojure.string :as string]
            [migratus.protocols :as proto]
            [next.jdbc :as njdbc]
            [next.jdbc.datafy :as ndatafy]
            [next.jdbc.date-time :as ndate-time]
            [next.jdbc.optional :as noptional]
            [next.jdbc.plan :as nplan]
            [next.jdbc.prepare :as nprepare]
            [next.jdbc.protocols :as nprotocols]
            [next.jdbc.quoted :as nquoted]
            [next.jdbc.result-set :as nresult-set]
            [next.jdbc.sql :as nsql]
            [next.jdbc.sql.builder :as nbuilder]
            [next.jdbc.transaction :as ntransaction]
            [next.jdbc.types :as ntypes]
            [sci.core :as sci]
            [babashka.impl.pprint]
            [babashka.impl.clojure.java.io]
            [babashka.impl.cheshire]
            [clojure.java.io :as io])
  (:import (java.util UUID)
           (java.io File)
           (java.sql PreparedStatement)
           (org.postgresql.util PGobject PGBinaryObject)))

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
   'with-options     (sci/copy-var njdbc/with-options next-ns)
   'with-transaction (sci/copy-var njdbc/with-transaction next-ns)
   })

(def next-datafy-ns (sci/create-ns 'next.jdbc.datafy))

(def ndatafy-namespace
  {'*datafy-failure* (sci/copy-var ndatafy/*datafy-failure* next-datafy-ns)})

(def next-date-time-ns (sci/create-ns 'next.jdbc.date-time))

(def ndate-time-namespace
  {'read-as-default (sci/copy-var ndate-time/read-as-default next-date-time-ns)
   'read-as-instant (sci/copy-var ndate-time/read-as-instant next-date-time-ns)
   'read-as-local   (sci/copy-var ndate-time/read-as-local next-date-time-ns)})

(def noptional-ns (sci/create-ns 'next.jdbc.optional))

(def noptional-namespace
  {'as-lower-maps                 (sci/copy-var noptional/as-lower-maps noptional-ns)
   'as-maps                       (sci/copy-var noptional/as-maps noptional-ns)
   'as-maps-adapter               (sci/copy-var noptional/as-maps-adapter noptional-ns)
   'as-modified-maps              (sci/copy-var noptional/as-modified-maps noptional-ns)
   'as-unqualified-lower-maps     (sci/copy-var noptional/as-unqualified-lower-maps noptional-ns)
   'as-unqualified-maps           (sci/copy-var noptional/as-unqualified-maps noptional-ns)
   'as-unqualified-modified-maps  (sci/copy-var noptional/as-unqualified-modified-maps noptional-ns)
   '->MapResultSetOptionalBuilder (sci/copy-var noptional/->MapResultSetOptionalBuilder noptional-ns)})

(def nplan-ns (sci/create-ns 'next.jdbc.plan))

(def nplan-namespace
  {'select!     (sci/copy-var nplan/select! nplan-ns)
   'select-one! (sci/copy-var nplan/select-one! nplan-ns)})

(def nprepare-ns (sci/create-ns 'next.jdbc.prepare))

(def nprepare-namespace
  {'create            (sci/copy-var nprepare/create nprepare-ns)
   'execute-batch!    (sci/copy-var nprepare/execute-batch! nprepare-ns)
   'set-parameters    (sci/copy-var nprepare/set-parameters nprepare-ns)
   'SettableParameter (sci/copy-var nprepare/SettableParameter nprepare-ns)
   'statement         (sci/copy-var nprepare/statement nprepare-ns)})

(def nprotocols-ns (sci/create-ns 'next.jdbc.protocols))

(def nprotocols-namespace
  {'Connectable  (sci/copy-var nprotocols/Connectable nprotocols-ns)
   'Executable   (sci/copy-var nprotocols/Executable nprotocols-ns)
   'Preparable   (sci/copy-var nprotocols/Preparable nprotocols-ns)
   'Sourceable   (sci/copy-var nprotocols/Sourceable nprotocols-ns)
   'Transactable (sci/copy-var nprotocols/Transactable nprotocols-ns)})

(def nquoted-ns (sci/create-ns 'next.jdbc.quoted))

(def nquoted-namespace
  {'ansi       (sci/copy-var nquoted/ansi nquoted-ns)
   'mysql      (sci/copy-var nquoted/mysql nquoted-ns)
   'oracle     (sci/copy-var nquoted/oracle nquoted-ns)
   'postgres   (sci/copy-var nquoted/postgres nquoted-ns)
   'schema     (sci/copy-var nquoted/schema nquoted-ns)
   'sql-server (sci/copy-var nquoted/sql-server nquoted-ns)})

(def nresult-set-ns (sci/create-ns 'next.jdbc.result-set))

(def nresult-set-namespace
  {'->ArrayResultSetBuilder               (sci/copy-var nresult-set/->ArrayResultSetBuilder nresult-set-ns)
   'as-arrays                             (sci/copy-var nresult-set/as-arrays nresult-set-ns)
   'as-arrays-adapter                     (sci/copy-var nresult-set/as-arrays-adapter nresult-set-ns)
   'as-lower-arrays                       (sci/copy-var nresult-set/as-lower-arrays nresult-set-ns)
   'as-lower-maps                         (sci/copy-var nresult-set/as-lower-maps nresult-set-ns)
   'as-maps                               (sci/copy-var nresult-set/as-maps nresult-set-ns)
   'as-maps-adapter                       (sci/copy-var nresult-set/as-maps-adapter nresult-set-ns)
   'as-modified-arrays                    (sci/copy-var nresult-set/as-modified-arrays nresult-set-ns)
   'as-modified-maps                      (sci/copy-var nresult-set/as-modified-maps nresult-set-ns)
   'as-unqualified-arrays                 (sci/copy-var nresult-set/as-unqualified-arrays nresult-set-ns)
   'as-unqualified-lower-arrays           (sci/copy-var nresult-set/as-unqualified-lower-arrays nresult-set-ns)
   'as-unqualified-lower-maps             (sci/copy-var nresult-set/as-unqualified-lower-maps nresult-set-ns)
   'as-unqualified-maps                   (sci/copy-var nresult-set/as-unqualified-maps nresult-set-ns)
   'as-unqualified-modified-arrays        (sci/copy-var nresult-set/as-unqualified-modified-arrays nresult-set-ns)
   'as-unqualified-modified-maps          (sci/copy-var nresult-set/as-unqualified-modified-maps nresult-set-ns)
   'builder-adapter                       (sci/copy-var nresult-set/builder-adapter nresult-set-ns)
   'clob->string                          (sci/copy-var nresult-set/clob->string nresult-set-ns)
   'clob-column-reader                    (sci/copy-var nresult-set/clob-column-reader nresult-set-ns)
   'datafiable-result-set                 (sci/copy-var nresult-set/datafiable-result-set nresult-set-ns)
   'DatafiableRow                         (sci/copy-var nresult-set/DatafiableRow nresult-set-ns)
   'foldable-result-set                   (sci/copy-var nresult-set/foldable-result-set nresult-set-ns)
   'get-column-names                      (sci/copy-var nresult-set/get-column-names nresult-set-ns)
   'get-lower-column-names                (sci/copy-var nresult-set/get-lower-column-names nresult-set-ns)
   'get-modified-column-names             (sci/copy-var nresult-set/get-modified-column-names nresult-set-ns)
   'get-unqualified-column-names          (sci/copy-var nresult-set/get-unqualified-column-names nresult-set-ns)
   'get-unqualified-lower-column-names    (sci/copy-var nresult-set/get-unqualified-lower-column-names nresult-set-ns)
   'get-unqualified-modified-column-names (sci/copy-var nresult-set/get-unqualified-modified-column-names nresult-set-ns)
   'InspectableMapifiedResultSet          (sci/copy-var nresult-set/InspectableMapifiedResultSet nresult-set-ns)
   '->MapResultSetBuilder                 (sci/copy-var nresult-set/->MapResultSetBuilder nresult-set-ns)
   'reducible-result-set                  (sci/copy-var nresult-set/reducible-result-set nresult-set-ns)
   'ResultSetBuilder                      (sci/copy-var nresult-set/ResultSetBuilder nresult-set-ns)
   'RowBuilder                            (sci/copy-var nresult-set/RowBuilder nresult-set-ns)})

;(def nspecs-ns (sci/create-ns 'next.jdbc.specs))
;
;(def nspecs-namespace
;  {'jdbc-url-format? (sci/copy-var nspecs/jdbc-url-format? nspecs-ns)})

(def nbuilder-ns (sci/create-ns 'next.jdbc.sql.builder))
(def nbuilder-namespace
  {'as-?             (sci/copy-var nbuilder/as-? nbuilder-ns)
   'as-cols          (sci/copy-var nbuilder/as-cols nbuilder-ns)
   'as-keys          (sci/copy-var nbuilder/as-keys nbuilder-ns)
   'by-keys          (sci/copy-var nbuilder/by-keys nbuilder-ns)
   'for-delete       (sci/copy-var nbuilder/for-delete nbuilder-ns)
   'for-insert       (sci/copy-var nbuilder/for-insert nbuilder-ns)
   'for-insert-multi (sci/copy-var nbuilder/for-insert-multi nbuilder-ns)
   'for-order        (sci/copy-var nbuilder/for-order nbuilder-ns)
   'for-order-col    (sci/copy-var nbuilder/for-order-col nbuilder-ns)
   'for-query        (sci/copy-var nbuilder/for-query nbuilder-ns)
   'for-update       (sci/copy-var nbuilder/for-update nbuilder-ns)})

(def ntransaction-ns (sci/create-ns 'next.jdbc.transaction))
(def ntransaction-namespace
  {'*nested-tx* (sci/copy-var ntransaction/*nested-tx* ntransaction-ns)})

(def ntypes-ns (sci/create-ns 'next.jdbc.types))
(def ntypes-namespace
  {'as-array        (sci/copy-var ntypes/as-array ntypes-ns)
   'as-bigint       (sci/copy-var ntypes/as-bigint ntypes-ns)
   'as-binary       (sci/copy-var ntypes/as-binary ntypes-ns)
   'as-bit          (sci/copy-var ntypes/as-bit ntypes-ns)
   'as-blob         (sci/copy-var ntypes/as-blob ntypes-ns)
   'as-boolean      (sci/copy-var ntypes/as-boolean ntypes-ns)
   'as-char         (sci/copy-var ntypes/as-char ntypes-ns)
   'as-clob         (sci/copy-var ntypes/as-clob ntypes-ns)
   'as-datalink     (sci/copy-var ntypes/as-datalink ntypes-ns)
   'as-date         (sci/copy-var ntypes/as-date ntypes-ns)
   'as-decimal      (sci/copy-var ntypes/as-decimal ntypes-ns)
   'as-distinct     (sci/copy-var ntypes/as-distinct ntypes-ns)
   'as-double       (sci/copy-var ntypes/as-double ntypes-ns)
   'as-float        (sci/copy-var ntypes/as-float ntypes-ns)
   'as-integer      (sci/copy-var ntypes/as-integer ntypes-ns)
   'as-java-object  (sci/copy-var ntypes/as-java-object ntypes-ns)
   'as-longnvarchar (sci/copy-var ntypes/as-longnvarchar ntypes-ns)})


(def nsql-ns (sci/create-ns 'next.jdbc.sql))

(def nsql-namespace
  {'delete!       (sci/copy-var nsql/delete! nsql-ns)
   'find-by-keys  (sci/copy-var nsql/find-by-keys nsql-ns)
   'get-by-id     (sci/copy-var nsql/get-by-id nsql-ns)
   'insert!       (sci/copy-var nsql/insert! nsql-ns)
   'insert-multi! (sci/copy-var nsql/insert-multi! nsql-ns)
   'query         (sci/copy-var nsql/query nsql-ns)
   'update!       (sci/copy-var nsql/update! nsql-ns)})


(def allowed-namespaces
  {'clojure.java.io       babashka.impl.clojure.java.io/io-namespace
   'cheshire.core         babashka.impl.cheshire/cheshire-core-namespace
   'clojure.pprint        babashka.impl.pprint/pprint-namespace
   'next.jdbc             njdbc-namespace
   'next.jdbc.datafy      ndatafy-namespace
   'next.jdbc.date-time   ndate-time-namespace
   'next.jdbc.optional    noptional-namespace
   'next.jdbc.plan        nplan-namespace
   'next.jdbc.prepare     nprepare-namespace
   'next.jdbc.protocols   nprotocols-namespace
   'next.jdbc.quoted      nquoted-namespace
   'next.jdbc.result-set  nresult-set-namespace
   ;'next.jdbc.specs       nspecs-namespace
   'next.jdbc.sql.builder nbuilder-namespace
   'next.jdbc.sql         nsql-namespace
   'next.jdbc.transaction ntransaction-namespace
   'next.jdbc.types       ntypes-namespace})

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

(def classes
  {'java.util.UUID                     java.util.UUID
   'java.sql.PreparedStatement         java.sql.PreparedStatement
   'org.postgresql.util.PGobject       org.postgresql.util.PGobject
   'org.postgresql.util.PGBinaryObject org.postgresql.util.PGBinaryObject})

(defmethod proto/make-migration* :clj
  [_ mig-id mig-name payload config]
  (let [{:keys [up down]} payload
        sci-opts {:namespaces allowed-namespaces
                  :imports    imports
                  :classes    classes}]
    (->SciMigration mig-id mig-name config sci-opts up down)))

(defmethod proto/get-extension* :clj
  [_]
  "clj")

(defmethod proto/migration-files* :clj
  [x migration-name]
  (let [ext (proto/get-extension* x)]
    [(str migration-name ".up." ext)
     (str migration-name ".down." ext)]))
