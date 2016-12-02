(ns user
  (:require [com.stuartsierra.component :as component]
            [ring.component.jetty :refer [jetty-server]]
            [clojure.tools.namespace.repl :refer [refresh]]
            [taoensso.timbre :as timbre]
            [figwheel-sidecar.repl-api :as ra]
            [om-next-leaflet.server :refer [system create-database app create-logger]]))

(timbre/refer-timbre)

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
  (start [component]
    (if (:server component)
      component
      (assoc component :server (ra/start-figwheel!))))
  (stop [component]
    (if-let [server (:server component)]
      (do
        (ra/stop-figwheel!)
        (dissoc component :server)))))

(defn create-system
  [& config-options]
  (let [{:keys [port]} config-options
        port (or port 3000)]
    (component/system-map
     :database (create-database)
     :logger (create-logger {})
     :http-server (component/using
                   (jetty-server {:app {:handler app} :port port})
                   [:logger :database])
     :figwheel (component/using
                (map->Figwheel {})
                [:logger :database :http-server]))))
