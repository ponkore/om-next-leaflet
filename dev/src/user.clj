(ns user
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer (refresh)]
            [om-next-leaflet.server :as server]
            [figwheel-sidecar.repl-api :as ra]))

(def system nil)

(defn init []
  (alter-var-root #'system
    (constantly (om-next-leaflet.server/create-system {:port 3000}))))

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

(defn browser-repl []
  (ra/start-figwheel!)
  (ra/cljs-repl))
