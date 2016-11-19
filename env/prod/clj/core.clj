(ns om-next-leaflet.core
  (:require [com.stuartsierra.component :as component]
            [om-next-leaflet.server :refer [system create-system]]))

(defn -main
  ""
  [& args]
  (alter-var-root #'system (constantly (create-system))))
