(ns user
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer [refresh]]
            [figwheel-sidecar.repl-api :as ra]
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

(defn go []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))

(defn cljs-repl
  []
  (ra/cljs-repl))

(defrecord Figwheel [server]
  component/Lifecycle
  (start [this]
    (if server
      this
      (assoc this :server (ra/start-figwheel!))))
  (stop [this]
    (if-not server
      this
      (do
        (ra/stop-figwheel!)
        (assoc this :server nil)))))

(defn create-system
  [& config-options]
  (let [{:keys [port]} config-options
        port (or port 3000)]
    (component/system-map
     :database (create-database)
     :http-server (component/using
                   (map->Figwheel {})
                   [:database]))))
