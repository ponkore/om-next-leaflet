(ns om-next-leaflet.core
  (:require [clojure.string :as str]
            [taoensso.timbre :refer-macros [log trace debug info warn error fatal report]]
            [cljs.core.async :refer [put! chan <! go go-loop]]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [sablono.core :as html :refer-macros [html]]
            [om-next-leaflet.parser :as parser]
            [om-next-leaflet.util :as util]
            [om-next-leaflet.leaflet :as leaflet]))

(enable-console-print!)

(defonce app-state (atom {}))

(declare reconciler Root)

(defn render []
  (om/add-root! reconciler Root (js/document.getElementById "app")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def init-center [34.6964898 135.4930235])
(def init-zoom 12)
(defrecord MapState [lat lng zoom bounds])

(def osm-layer (leaflet/create-tilelayer "OpenStreetMap"
                 "http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                 "Map data &copy; <a href=\"http://openstreetmap.org\">OpenStreetMap</a>"
                 :maxZoom 18))
(def pale-layer (leaflet/create-tilelayer "淡色地図"
                  "http://cyberjapandata.gsi.go.jp/xyz/pale/{z}/{x}/{y}.png"
                  "<a href='http://www.gsi.go.jp/kikakuchousei/kikakuchousei40182.html' target='_blank'>国土地理院</a>"
                  :maxZoom 18
                  :minZoom 12))
(def std-layer (leaflet/create-tilelayer "地理院地図"
                 "http://cyberjapandata.gsi.go.jp/xyz/std/{z}/{x}/{y}.png"
                 "<a href='http://www.gsi.go.jp/kikakuchousei/kikakuchousei40182.html' target='_blank'>国土地理院</a>"))

(defn get-stations-layer
  [this]
  (-> (om/react-ref this :leaflet) om/get-state :stations-layer))

(defn get-lines-layer
  [this]
  (-> (om/react-ref this :leaflet) om/get-state :lines-layer))

(defn change-mapstate
  [this e leaflet-map]
  (let [event-type (-> e .-type keyword) ;; for debug
        {:keys [lat lng]} (-> leaflet-map .getCenter leaflet/latlng->clj)
        mapstate (map->MapState {:lat lat
                                 :lng lng
                                 :zoom (-> leaflet-map .getZoom)
                                 :bounds (-> leaflet-map .getBounds leaflet/bounds->clj)})]
    (.log js/console (str "[" event-type "]"))
    (om/transact! this `[(app/update-mapstate {:new-mapstate ~mapstate})])))

(def leaflet-map-fn (om/factory leaflet/Leaflet))

;; (fn [e] (let [station (filter (fn [station] (= (:id station) id)) stations)
;;               new-station-info (-> station
;;                                    first
;;                                    (dissoc :geometry))]
;;           (om/transact! this `[(app/update-station-info {:new-station-info ~new-station-info})])))

(defmulti event-handler (fn [k this e] k))

(defmethod event-handler :root/input-on-change
  [_ this e]
  (let [v (-> e .-target .-value)]
    (om/transact! this `[(app/update-title {:new-title ~v})])))

(defmethod event-handler :root/button-click
  [_ this e]
  (let [new-title (.-value (dom/node this "title"))]
    (om/transact! this `[(app/update-title {:new-title ~new-title})])))

(defui Root
  static om/IQueryParams
  (params [_]
    {:line-id 24})
  static om/IQuery
  (query [this]
    '[:app/title
      :app/mapstate
      (:app/stations {:line-id ?line-id}) ;; can be removed?
      :app/station-info])
  Object
  (componentWillMount [this]
    (.log js/console "will-mount"))
  (componentDidMount [this]
    (.log js/console "did-mount")
    (let [{:keys [app/stations]} (om/props this)
          stations-layer (get-stations-layer this)
          lines-layer (get-lines-layer this)
          lines-chan (chan)
          stations-chan (chan)]
      (go-loop []
        (let [data (<! lines-chan)]
          (if (= (:result data) :success)
            (let [lines-data (:data data)]
              (leaflet/init-polylines lines-layer lines-data))
            ;; TODO: error handling
            )
          (recur)))
      (go-loop []
        (let [data (<! stations-chan)]
          (if (= (:result data) :success)
            (let [stations-data (:data data)]
              (leaflet/init-station-markers stations-layer stations-data))
            ;; TODO: error handling
            )
          (recur)))
      (util/send-request! :get "/api2/lines" nil lines-chan)
      (util/send-request! :get "/api2/lines/24/stations" nil stations-chan)))
  (componentWillUnmount [this]
    (.log js/console "will-unmount"))
  (render [this]
    (let [{:keys [app/title
                  app/mapstate
                  app/stations
                  app/lines
                  app/station-info]} (om/props this)]
      (html
       [:div
        [:div {:id "custom-control"
               :class "leaflet-control-layers leaflet-control-layers-expanded leaflet-control"}
         [:input {:ref "title"
                  :value (if (nil? title) "" title)
                  :on-change (fn [e] (event-handler :root/input-on-change this e))}]
         [:button {:on-click (fn [e] (event-handler :root/button-click this e))}
          "update"]
         (vec (concat [:select {:value 24}] (mapv (fn [[id line-name]] [:option {:value id} line-name]) lines)))
         [:div
          [:p (str "zoom: " (:zoom mapstate init-zoom))]]]
        (leaflet-map-fn {:mapid "map"
                         :ref :leaflet ;; referenced from get-xxx-layer function
                         :center init-center
                         :zoom init-zoom
                         :base-layers [osm-layer pale-layer std-layer]
                         :event-handlers {:movestart        (partial change-mapstate this)
                                          :move             (partial change-mapstate this)
                                          :moveend          (partial change-mapstate this)
                                          :zoomlevelschange (partial change-mapstate this)
                                          :viewreset        (partial change-mapstate this)
                                          :load             (partial change-mapstate this)}})]))))

(def parser (om/parser {:read parser/read :mutate parser/mutate}))

(def reconciler
  (om/reconciler
    {:state app-state
     :normalize true
     ;; :merge-tree (fn [a b] (println "|merge" a b) (merge a b))
     :parser parser
     :send (util/transit-post "/api")}))

;; (defn save-binary
;;   [binary filename]
;;   ;; http://kuroeveryday.blogspot.jp/2016/05/file-download-from-browser.html
;;   ;; http://qiita.com/blackawa/items/c83d3f08b71a02db9348
;;   (let [link (.createElement js/document "a")
;;         blob (js/Blob. #js [binary] #js {"type" "application/octet-binary"})
;;         url (.createObjectURL (.-URL js/window) blob)]
;;     (set! (.-download link) filename)
;;     (set! (.-target link) "_blank")
;;     (set! (.-href link) url)
;;     (.appendChild (.-body js/document) link)
;;     (try
;;       (.click link) ;; blocking
;;       (finally
;;         (.removeChild (.-body js/document) link)))))

;; (defn attachment-on-click
;;   [this link filename]
;;   (util/download-file link
;;                       :on-success (fn [event] (let [binary (-> event .-target .getResponse)]
;;                                                 (save-binary binary filename)))
;;                       :on-error   (fn [event] (let [errorcode (.getStatus (-> event .-target))]
;;                                                 (if (= errorcode 404)
;;                                                   (js/alert "file not found")
;;                                                   (js/alert "download error code=" errorcode))))))
