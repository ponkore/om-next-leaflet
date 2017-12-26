(ns om-next-leaflet.routes
  (:require [clojure.java.io :as io]
            [compojure.core :refer [ANY GET PUT POST DELETE routes]]
            [compojure.route :refer [resources not-found]]
            [ring.util.response :refer [response]]
            [cheshire.core :refer [generate-string]]
            [om-next-leaflet.geojson :as geojson]
            [om-next-leaflet.parser :as parser]
            [om.next.server :as om]))

(defn generate-response [data & [status]]
  {:status  (or status 200)
   :headers {"Content-Type" "application/transit+json; charset=UTF-8"}
   :body    data})

(defn generate-response-json [data & [status]]
  {:status  (or status 200)
   :headers {"Content-Type" "application/json; charset=UTF-8"}
   :body    (generate-string data)})

(def state (atom {:app/title "initial server title"}))

(defn home-routes [endpoint]
  (routes
   (GET "/" _
     (-> "public/index.html"
         io/resource
         io/input-stream
         response
         (assoc :headers {"Content-Type" "text/html; charset=utf-8"})))
   (GET "/api2/lines" _
     (generate-response-json
      (->> (geojson/get-lines)
           (map (fn [{:keys [id line-name bounding-box geometry]}] [id line-name bounding-box geometry])))))
   (GET "/api2/lines/:line-id/stations" [line-id]
     (generate-response-json
      (geojson/get-stations (fn [m] (= (:line-id m) (Integer/parseInt line-id))))))
   (POST "/api" {req :params}
     (generate-response
      ((om/parser {:read parser/readf :mutate parser/mutatef})
       {:state state} (:remote req))))
   (resources "/")
   (not-found "Not Found")))
