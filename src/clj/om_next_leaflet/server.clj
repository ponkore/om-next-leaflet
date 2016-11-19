(ns om-next-leaflet.server
  (:require [com.stuartsierra.component :as component]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [response file-response resource-response]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.transit :refer [wrap-transit-response wrap-transit-params]]
            [ring.component.jetty :refer [jetty-server]]
            [figwheel-sidecar.repl-api :as ra]
            [om-next-leaflet.parser :as parser]
            [om-next-leaflet.database :refer [db]]
            [om.next.server :as om]
            [bidi.bidi :as bidi]))

(def system nil)

(def routes
  ["" {"/" :index
       "/api"
       {:get  {[""] :api}
        :post {[""] :api}}}])

(defn generate-response [data & [status]]
  {:status  (or status 200)
   :headers {"Content-Type" "application/transit+json"}
   :body    data})

(defn api [req]
  (generate-response
   ((om/parser {:read parser/readf :mutate parser/mutatef})
    {:state (:state req)} (:remote (:transit-params req)))))

(defn index [req]
  (assoc (resource-response (str "html/index.html") {:root "public"})
         :headers {"Content-Type" "text/html"}))

(def state (atom {:app/title "initial server title"}))

(defn handler [req]
  (let [match (bidi/match-route routes (:uri req)
                                :request-method (:request-method req))]
    (case (:handler match)
      :index nil
      :api (api (assoc req :state state))
      nil)))

(defn wrap-system
  [handler system]
  (fn [req]
    (handler (assoc req
                    :db (:database system)
                    :system system))))

(def app
  (wrap-system
   (-> handler
       (wrap-resource "public")
       wrap-reload
       wrap-transit-response
       wrap-transit-params)
   system))

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
     :source-paths ["src/cljs" "src/cljc" "env/dev/cljs"]
     :compiler {:main 'om-next-leaflet.dev
                :asset-path "js"
                :output-to "resources/public/js/main.js"
                :output-dir "resources/public/js"
                :verbose true}}]})

(defn create-system
  [{:keys [port] :as config-options}]
  (component/system-map
   :database db
   :http-server (component/using
                 (jetty-server {:app app :port port})
                 [:database])))

(defn create-system-fw
  []
  (component/system-map
   :database db
   :http-server (component/using
                 (map->Figwheel {:config figwheel-config})
                 [:database])))

(defn cljs-repl
  []
  (ra/cljs-repl))
