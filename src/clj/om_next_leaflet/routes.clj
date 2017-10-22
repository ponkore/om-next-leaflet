(ns om-next-leaflet.routes
  (:require [clojure.java.io :as io]
            [compojure.core :refer [ANY GET PUT POST DELETE routes]]
            [compojure.route :refer [resources not-found]]
            [ring.util.response :refer [response]]
            [om-next-leaflet.parser :as parser]
            [om.next.server :as om]))

(defn generate-response [data & [status]]
  {:status  (or status 200)
   :headers {"Content-Type" "application/transit+json; charset=UTF-8"}
   :body    data})

(def state (atom {:app/title "initial server title"}))

(defn home-routes [endpoint]
  (routes
   (GET "/" _
     (-> "public/index.html"
         io/resource
         io/input-stream
         response
         (assoc :headers {"Content-Type" "text/html; charset=utf-8"})))
   (POST "/api" {req :params}
     (generate-response
      ((om/parser {:read parser/readf :mutate parser/mutatef})
       {:state state} (:remote req))))
   (resources "/")
   (not-found "Not Found")))
