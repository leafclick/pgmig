(ns alexandermann.unclogging
  (:require [clojure.string :as string]
            [clojure.walk :as walk]
            [taoensso.timbre :as log]
            [taoensso.timbre :as timbre])
  (:import [clojure.lang
            ArraySeq
            LazySeq]
           [java.util.logging Level
                              Logger
                              LogManager]
           [org.slf4j.bridge
            SLF4JBridgeHandler]))

(defn- deep-merge
  [& maps]
  (apply merge-with
         (fn [x y]
           (cond (map? y) (deep-merge x y)
                 (vector? y) (concat x y)
                 :else y))
         maps))

(defn- bridge-jul-to-timbre
  "java.util.Logging is more stubborn than other bridges
  to SLF4J and requires additional maintenance to get setup.

  Relevant links to this can be found:
  https://stackoverflow.com/a/9117188"
  []
  (.reset (LogManager/getLogManager))
  (SLF4JBridgeHandler/removeHandlersForRootLogger)
  (SLF4JBridgeHandler/install)
  (.setLevel (Logger/getLogger "global")
             Level/ALL))
(bridge-jul-to-timbre)

(def default-redacted-message ">>REDACTED<<")

(defn- repack
  [x]
  (into [] (take 50 x)))

(defn- ->non-lazy-print
  "Prevents an interesting case with appenders in Timbre AND
  prevents us from walking an infinite list.
  https://github.com/ptaoussanis/timbre/issues/237"
  [x]
  (cond
    (instance? ArraySeq x) (repack x)
    (instance? LazySeq x) (repack x)
    :else x))

(defn- strip-hazard
  [is-hazard? rm-hazard]
  (let [redacted-msg default-redacted-message
        clean-walk (fn inner-fn [v]
                     (if (try
                           (is-hazard? v)
                           (catch Exception _
                             false))
                       (try
                         (rm-hazard v redacted-msg)
                         (catch Exception _
                           redacted-msg))
                       (walk/walk inner-fn identity
                                  (->non-lazy-print v))))]
    (fn [{passed-data :vargs
          :as         payload}]
      (assoc payload
        :vargs (clean-walk passed-data)))))

(defn prevent-hazard
  "Sets up logging middleware which strips hazards from logging
  data.
  is-hazard? : takes a single value which identifies whether the
    data is hazardous.
  rm-hazard : optional fn which takes a value which passed is-hazard?
    and a message to use for redaction. Formats arguments to make the
    prettier in logs.
    Default is simply to replace anything matching is-hazard? with a
    redacted message."
  ([is-hazard?]
   (prevent-hazard {}
                   is-hazard?))
  ([config is-hazard?]
   (prevent-hazard config
                   is-hazard?
                   (fn [_ redact-msg]
                     redact-msg)))
  ([config is-hazard? rm-hazard]
   (deep-merge config
               {:middleware [(strip-hazard is-hazard?
                                           rm-hazard)]})))

(defn ring-logger-data [data]
  (some (fn [m] (when (and (map? m) (contains? m :ring.logger/type)) m)) data))


(def ^:private levels #{:trace
                        :debug
                        :info
                        :warn
                        :error
                        :fatal
                        :report})

(defn merge-config! [config]
  (log/merge-config! config))
