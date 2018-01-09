(ns om-next-leaflet.routes
  (:require [clojure.java.io :as io]
            [bidi.bidi :as bidi]
            [bidi.ring :refer [resources]]
            [ring.util.response :refer [response resource-response]]
            [taoensso.timbre :as timbre :refer [log trace debug info warn error fatal]]
            [cheshire.core :refer [generate-string]]
            [om-next-leaflet.geojson :as geojson]))

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

(defn line-names-handler
  [req]
  (generate-response-json
   (->> (geojson/get-lines)
        (map (fn [{:keys [id line-name]}] [id line-name])))))

(defn stations-handler
  [req]
  (generate-response-json
   (geojson/get-stations (fn [m] (= (:line-id m) (Integer/parseInt (get-in req [:params :line-id])))))))

(defn lines-in-bounds-handler
  [req]
  (generate-response-json
   (let [{:keys [params]} req
         {:keys [zoom nwlat nwlng selat selng]} params]
     {}))) ;; TODO

(defn stations-in-bounds-handler
  [req]
  (generate-response-json
   (let [{:keys [params]} req
         {:keys [zoom nwlat nwlng selat selng]} params]
     {}))) ;; TODO

(defn save-object-handler
  [req]
  (let [{:keys [body]} req]
    (debug "save-object-handler: body=" body)
    (generate-response-json
     {}))) ;; TODO: return object id

(defn get-objects-handler
  [req]
  (let [{:keys [params]} req]
    (debug "get-objects-handler: params=" params)
    (generate-response-json
     {}))) ;; TODO: return stored objects

(def route ["/" {"" {:get index-handler}
                 "index.html" {:get index-handler}
                 "css" {:get (resources {:prefix "public/css/"})}
                 "js" {:get (resources {:prefix "public/js/"})}
                 "api2/" {"lines" {:get lines-handler}
                          ["lines/" [#"\d+" :line-id] "/stations"] {:get stations-handler}
                          "line-names" {:get line-names-handler}
                          ["lines-in-bounds/" :zoom "/" :nwlat "," :nwlng "-" :selat "," :selng] {:get lines-in-bounds-handler}
                          ["stations-in-bounds/" :zoom "/" :nwlat "," :nwlng "-" :selat "," :selng] {:get stations-in-bounds-handler}
                          "objects" {:get get-objects-handler}
                          "object" {:post save-object-handler}
                          }}])

(defn home-routes
  [endpoint]
  route)
