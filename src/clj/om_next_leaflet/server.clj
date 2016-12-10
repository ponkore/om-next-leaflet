(ns om-next-leaflet.server
  (:require [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [taoensso.timbre :as timbre :refer [log trace debug info warn error fatal]]
            [taoensso.timbre.appenders.3rd-party.rotor :as rotor]
            [duct.component.hikaricp :as hcp]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [response file-response resource-response]]
            [ring.logger.timbre :refer [wrap-with-logger]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.transit :refer [wrap-transit-response wrap-transit-params]]
            [om-next-leaflet.parser :as parser]
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
   :headers {"Content-Type" "application/transit+json; charset=UTF-8"}
   :body    data})

(defn api [req]
  (generate-response
   ((om/parser {:read parser/readf :mutate parser/mutatef})
    {:state (:state req) :db (:database system)} (:remote (:transit-params req)))))

(defn index [req]
  (assoc (resource-response "index.html" {:root "public"})
         :headers {"Content-Type" "text/html; charset=UTF-8"}))

(def state (atom {:app/title "initial server title"}))

(defn handler [req]
  (let [match (bidi/match-route routes (:uri req)
                                :request-method (:request-method req))]
    (case (:handler match)
      :index (index req)
      :api (api (assoc req :state state))
      nil)))

(def app
  (-> handler
      (wrap-resource "public")
      wrap-reload
      wrap-transit-response
      wrap-transit-params
      wrap-with-logger))

(defn create-database
  []
  (let [database-uri (env :database-url)]
    (hcp/hikaricp {:uri database-uri})))

(defrecord Logger [config]
  component/Lifecycle
  (start [component]
    (timbre/merge-config! {:timestamp-opts {:pattern "yyyy/MM/dd HH:mm:ss,SSS"
                                            :timezone (java.util.TimeZone/getDefault)}})
    (timbre/merge-config!
     {:appenders
      {:rotor
       (rotor/rotor-appender
        {:path "log/om-next-leaflet.log"
         :max-size (* 512 1024)
         :backlog 10})}})
    (timbre/merge-config!
     {:appenders
      {:println
       {:enabled? true}}})
    (info "logger started.")
    component)
  (stop [component]
    ;; do nothing
    (info "logger stopped.")
    component))

(defn create-logger
  [config]
  (map->Logger config))
