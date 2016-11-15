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
  (ra/start-figwheel!
   {:figwheel-options {:ring-handler 'om-next-leaflet.server/app}
    :build-ids ["dev"]
    :all-builds
    [{:id "dev"
      :figwheel true
      :source-paths ["src/cljs"]
      :compiler {:main 'om-next-leaflet.core
                 :asset-path "js"
                 :output-to "resources/public/js/main.js"
                 :output-dir "resources/public/js"
                 :verbose true}}]})
  (ra/cljs-repl))
