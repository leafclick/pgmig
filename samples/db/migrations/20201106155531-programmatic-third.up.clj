(require '[next.jdbc.sql :as sql]
         '[common.util :as util])

(defn prepare-ids [n]
  (mapv vector (repeatedly n random-uuid)))

(println "Programmatic UP")

(let [data (prepare-ids 5)]
  (util/print-result
    (count (sql/insert-multi! (get-connection) :bar [:id] data))))
