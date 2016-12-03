(ns om-next-leaflet.main
  (:require [com.stuartsierra.component :as component]
            [ring.component.jetty :refer [jetty-server]]
            [taoensso.timbre :as timbre :refer [log trace debug info warn error fatal]]
            [om-next-leaflet.server :refer [system create-database app create-logger]])
  (:gen-class))

(declare create-system)

(defn init []
  (alter-var-root #'system
    (constantly (create-system))))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system
    (fn [s] (when s (component/stop s))))
  (shutdown-agents))

(defn -main [& args]
  (init)
  (start)
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop)))

(defn create-system
  [& config-options]
  (let [{:keys [port]} config-options
        port (or port 3000)]
    (component/system-map
     :database (create-database)
     :logger (create-logger {})
     :http-server (component/using
                   (jetty-server {:app {:handler app} :port port})
                   [:logger :database]))))
