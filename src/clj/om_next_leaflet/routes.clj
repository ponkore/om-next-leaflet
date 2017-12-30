(ns om-next-leaflet.routes
  (:require [clojure.java.io :as io]
            [bidi.bidi :as bidi]
            [bidi.ring :refer [resources]]
            [ring.util.response :refer [response resource-response]]
            [taoensso.timbre :as timbre :refer [log trace debug info warn error fatal]]
            [cheshire.core :refer [generate-string]]
            [om-next-leaflet.geojson :as geojson]
            [om.next.server :as om]))

(defn index-handler
  [req]
  (assoc (resource-response "index.html" {:root "public"})
         :headers {"Content-Type" "text/html; charset=UTF-8"}))

(defn generate-response-json [data & [status]]
  {:status  (or status 200)
   :headers {"Content-Type" "application/json; charset=UTF-8"}
   :body    (generate-string data)})

(defn lines-handler
  [req]
  (generate-response-json
   (->> (geojson/get-lines)
        (map (fn [{:keys [id line-name bounding-box geometry]}] [id line-name bounding-box geometry])))))

(defn stations-handler
  [req]
  (generate-response-json
   (geojson/get-stations (fn [m] (= (:line-id m) (Integer/parseInt (get-in req [:params :line-id])))))))

(def route ["/" {"" {:get index-handler}
                 "index.html" {:get index-handler}
                 "api2/lines" {"" {:get lines-handler}
                               ["/" [#"\d+" :line-id]] {"/stations" {:get stations-handler}}}
                 "css" {:get (resources {:prefix "public/css/"})}
                 "js" {:get (resources {:prefix "public/js/"})}}])

(defn home-routes
  [endpoint]
  route)
