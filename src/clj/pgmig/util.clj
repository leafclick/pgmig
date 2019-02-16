(ns pgmig.util
  (:require [clojure.string :as str])
  (:import (java.util UUID)))

(defn col-format-snake [col]
  (-> col str/lower-case (str/replace \- \_)))

(defn col-format-kebab [col]
  (-> col str/lower-case (str/replace \_ \-)))

(defn snake-case [col]
  (if (keyword? col)
    (-> col name col-format-snake keyword)
    (col-format-snake col)))

(defn kebab-case [col]
  (if (keyword? col)
    (-> col name col-format-kebab keyword)
    (col-format-kebab col)))

(defn m-snake-case [m]
  (into {} (map (fn [[k v]] [(snake-case k) v])) m))

(defn m-kebab-case [m]
  (into {} (map (fn [[k v]] [(kebab-case k) v])) m))

(defn select-values [m ks]
  (reduce #(conj %1 (get m %2)) [] ks))

(defn remove-nil-values [m]
  (apply dissoc m (for [[k v] m :when (nil? v)] k)))

(defn to-uuid [uuid]
  (if (or (nil? uuid)
          (instance? java.util.UUID uuid))
    uuid
    (UUID/fromString uuid)))

(defn sanitize-whitespace [s]
  (clojure.string/replace (str/trim (str s)) #"\s{2,}" " "))
