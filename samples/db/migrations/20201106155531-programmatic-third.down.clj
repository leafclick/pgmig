(require '[next.jdbc :as jdbc]
         '[common.util :refer [print-result]])

(println "Programmatic DOWN")

(print-result
  (:next.jdbc/update-count (jdbc/execute-one! (get-connection) ["delete from bar"])))
