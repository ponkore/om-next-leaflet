(ns user
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer [refresh]]
            [om-next-leaflet.server :as server]
            [figwheel-sidecar.repl-api :as ra]))

(def system nil)

(defrecord Figwheel [config server]
  component/Lifecycle
  (start [this]
    (if server
      this
      (let [server (ra/start-figwheel! config)]
        (assoc this :server server))))
  (stop [this]
    (if-not server
      this
      (do
        (ra/stop-figwheel!)
        (assoc this :server nil)))))

(def figwheel-config
  {:figwheel-options {:http-server-root "public"       ;; serve static assets from resources/public/
                      :server-port 3449                ;; default
                      :server-ip "127.0.0.1"           ;; default
                       :css-dirs ["resources/public/css"]
                      :ring-handler 'om-next-leaflet.server/app
                      :server-logfile "log/figwheel.log"
                      :nrepl-middleware ['cemerick.piggieback/wrap-cljs-repl]}
   :build-ids ["dev"]
   :all-builds
   [{:id "dev"
     :figwheel true
     :source-paths ["src/cljs" "dev/src/cljs"]
     :compiler {:main 'om-next-leaflet.dev
                :asset-path "js"
                :output-to "resources/public/js/main.js"
                :output-dir "resources/public/js"
                :verbose true}}]})

(defn create-system
  []
  (component/system-map
   :figwheel (map->Figwheel {:config figwheel-config})))

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

(defn browser-repl []
  (ra/cljs-repl))
