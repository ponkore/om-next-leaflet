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

(declare reconciler Root)

(defonce app-state (atom {}))

(defn render []
  (om/add-root! reconciler Root (js/document.getElementById "app")))

(def parser (om/parser {:read parser/read :mutate parser/mutate}))

(def reconciler
  (om/reconciler
    {:state app-state
     :normalize true
     ;; :merge-tree (fn [a b] (println "|merge" a b) (merge a b))
     :parser parser}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord MapState [lat lng zoom bounds])

(reset! app-state {:app/title ""
                   :app/mapstate (map->MapState {:lat 34.6964898
                                                 :lng 135.4930235
                                                 :zoom 12})})

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

(defn change-mapstate
  [this e leaflet-map]
  (let [event-type (-> e .-type keyword) ;; for debug
        {:keys [lat lng]} (-> leaflet-map .getCenter leaflet/latlng->clj)
        mapstate (map->MapState {:lat lat
                                 :lng lng
                                 :zoom (-> leaflet-map .getZoom)
                                 :bounds (-> leaflet-map .getBounds leaflet/bounds->clj)})]
    (debug (str "[" event-type "]"))
    (om/transact! this `[(app/update-mapstate {:new-mapstate ~mapstate})])))

(def leaflet-map-fn (om/factory leaflet/Leaflet))

(defmulti event-handler (fn [k this e] k))

(defmethod event-handler :root/input-on-change
  [_ this e]
  (let [v (-> e .-target .-value)]
    (om/transact! this `[(app/update-title {:new-title ~v})])))

(defmethod event-handler :root/button-click
  [_ this e]
  (let [new-title (.-value (dom/node this "title"))]
    (om/transact! this `[(app/update-title {:new-title ~new-title})])))

(defmulti channel-handler (fn [this data] (:tag data)))

(defmethod channel-handler :lines
  [this data]
  (let [layer (-> (om/react-ref this :leaflet) om/get-state :lines-layer)]
    (debug "after alts! lines")
    (leaflet/init-polylines layer (:data data))))

(defmethod channel-handler :stations
  [this data]
  (let [layer (-> (om/react-ref this :leaflet) om/get-state :stations-layer)]
    (debug "after alts! stations")
    (leaflet/init-station-markers layer (:data data))))

(defn main-channel-loop
  [this channels]
  (let [wait-channels (vals channels)
        v->k (zipmap (vals channels) (keys channels))]
    (go-loop []
      (let [[data chan-accepted] (alts! wait-channels)]
        (when (= (:result data) :success)
          (let [tag (v->k chan-accepted)]
            (channel-handler this (assoc data :tag tag))))
        (recur)))))

(defui Root
  static om/IQuery
  (query [this]
    '[:app/title
      :app/mapstate])
  Object
  (componentDidMount [this]
    (debug "did-mount")
    (let [channels {:lines (chan) :stations (chan)}]
      ;; watch channels
      (main-channel-loop this channels)
      ;; initialize
      (util/send-request! :get "/api2/lines" nil (:lines channels))
      (util/send-request! :get "/api2/lines/24/stations" nil (:stations channels))))
  (componentWillUnmount [this]
    (debug "will-unmount"))
  (render [this]
    (let [{:keys [app/title app/mapstate]} (om/props this)]
      (html
       [:div
        [:div {:id "custom-control"
               :class "leaflet-control-layers leaflet-control-layers-expanded leaflet-control"}
         [:input {:ref "title"
                  :value (if (nil? title) "" title)
                  :on-change #(event-handler :root/input-on-change this %)}]
         [:button {:on-click #(event-handler :root/button-click this %)}
          "update"]
         [:div
          [:p (str "zoom: " (:zoom mapstate))]]]
        (leaflet-map-fn {:mapid "map"
                         :ref :leaflet ;; referenced from channel-handler for look up leaflet map component.
                         :center [(:lat mapstate) (:lng mapstate)]
                         :zoom (:zoom mapstate)
                         :base-layers [osm-layer pale-layer std-layer]
                         :event-handlers {:movestart        (partial change-mapstate this)
                                          :move             (partial change-mapstate this)
                                          :moveend          (partial change-mapstate this)
                                          :zoomlevelschange (partial change-mapstate this)
                                          :viewreset        (partial change-mapstate this)
                                          :load             (partial change-mapstate this)}
                         :draw-created-handler (fn [e drawn-items] (.log js/console drawn-items))})]))))
