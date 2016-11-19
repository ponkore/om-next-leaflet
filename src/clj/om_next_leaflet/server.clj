(ns om-next-leaflet.server
  (:require [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [duct.component.hikaricp :as hcp]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [response file-response resource-response]]
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
   :headers {"Content-Type" "application/transit+json"}
   :body    data})

(defn api [req]
  (generate-response
   ((om/parser {:read parser/readf :mutate parser/mutatef})
    {:state (:state req) :db (:db req)} (:remote (:transit-params req)))))

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

(defn create-database
  []
  (let [database-uri (env :database-url)]
    (hcp/hikaricp {:uri database-uri})))
