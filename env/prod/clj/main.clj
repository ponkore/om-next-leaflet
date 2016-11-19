(ns om-next-leaflet.main
  (:require [com.stuartsierra.component :as component]
            [ring.component.jetty :refer [jetty-server]]
            [om-next-leaflet.server :refer [system create-database app]]))

(declare create-system)

(defn init []
  (alter-var-root #'system
    (constantly (create-system))))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system
    (fn [s] (when s (component/stop s)))))

;; TODO add-shutdown-hook
(defn -main [& args]
  (init)
  (start))

(defn create-system
  [& config-options]
  (let [{:keys [port]} config-options
        port (or port 3000)]
    (component/system-map
     :database (create-database)
     :http-server (component/using
                   (jetty-server {:app app :port port})
                   [:database]))))
