(ns om-next-leaflet.server
  (:require [com.stuartsierra.component :as component]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [response file-response resource-response]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.transit :refer [wrap-transit-response wrap-transit-params]]
            [ring.component.jetty :refer [jetty-server]]
            [om-next-leaflet.parser :as parser]
            [om.next.server :as om]
            [bidi.bidi :as bidi]))

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

(def app
  (-> handler
      (wrap-resource "public")
      wrap-reload
      wrap-transit-response
      wrap-transit-params))

(defn create-system
  [{:keys [port] :as config-options}]
  (component/system-map
   :http-server (jetty-server {:app app :port port})))
