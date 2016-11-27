(ns om-next-leaflet.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [sablono.core :as html :refer-macros [html]]
            [om-next-leaflet.util :as util]
            [om-next-leaflet.parser :as parser]
            [om-next-leaflet.leaflet :as leaflet]))

(def init-center [34.6964898 135.4930235])
(def init-zoom 12)

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

(defrecord MapState [lat lng zoom bounds])

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

;; add all statiions marker to 'stations-layer'
(defn init-station-markers
  [this stations]
  (let [stations-layer (get-stations-layer this)]
    (doseq [{:keys [id station-name line-name geometry]} stations]
      (let [[lng lat] geometry
            marker (leaflet/create-marker lat lng :radius 6 :fillColor "#0000ff" :fillOpacity 1.0 :weight 1)]
        (doto marker
          ;; (.bindPopup (str "<b>" line-name "</b><br>" station-name))
          (.on "click" (fn [e] (let [station (filter (fn [station] (= (:id station) id)) stations)
                                     new-station-info (-> station
                                                          first
                                                          (dissoc :geometry))]
                                 (om/transact! this `[(app/update-station-info {:new-station-info ~new-station-info})]))))
          (.on "mouseover" (fn [e] (.setStyle marker (clj->js {:fillColor "#ff0000"}))))
          (.on "mouseout" (fn [e] (.setStyle marker (clj->js {:fillColor "#0000ff"}))))
          (.addTo stations-layer))))))

;; add all line polygons to 'lines-layer'
(defn init-all-lines
  [this lines]
  (let [lines-layer (get-lines-layer this)]
    (doseq [[id name bounding-box geometry] lines]
      (let [line-color "#666666"
            polyline (leaflet/create-polyline geometry :color line-color :weight 6 :opacity 0.7)]
        (doto polyline
          (.bindTooltip (str "<b>" name "[" id "]</b>"))
          (.on "mouseover" (fn [e]
                             (.setStyle polyline (clj->js {:color "#ff0000" :weight 8}))
                             (.openTooltip polyline (.-latlng e))))
          (.on "mouseout" (fn [e]
                            (.setStyle polyline (clj->js {:color line-color :weight 6}))
                            (.closeTooltip polyline)))
          (.addTo lines-layer))))))

(defui Root
  static om/IQueryParams
  (params [_]
    {:line-id 24})
  static om/IQuery
  (query [this]
    '[:app/title
      :app/mapstate
      :app/lines
      (:app/stations {:line-id ?line-id})
      :app/station-info])
  Object
  (componentWillMount [this]
    (.log js/console "will-mount"))
  (componentDidMount [this]
    (.log js/console "did-mount")
    (let [{:keys [app/lines app/stations]} (om/props this)]
      (init-all-lines this lines)
      (init-station-markers this stations)))
  (componentWillUnmount [this]
    (.log js/console "will-unmount"))
  (render [this]
    (let [{:keys [app/title
                  app/mapstate
                  app/stations
                  app/lines
                  app/station-info]} (om/props this)
          {:keys [kilotei]} station-info]
      (html
       [:div
        [:div {:id "custom-control"
               :class "leaflet-control-layers leaflet-control-layers-expanded leaflet-control"}
         #_[:input {:ref "title"
                  :value title
                  :on-change (fn [e] (let [v (-> e .-target .-value)]
                                       (om/transact! this `[(app/update-title {:new-title ~v})])))}]
         #_[:button {:on-click (fn [e] (let [new-title (.-value (dom/node this "title"))]
                                       (om/transact! this `[(app/update-title {:new-title ~new-title})
                                                            :app/title])))
                     } "update"]
         (into [] (concat [:select {:value 24}] (mapv (fn [[id line-name]] [:option {:value id} line-name]) lines)))
         (let [{:keys [id station-name line-id line-name]} station-info]
           [:div
            [:p (str "zoom: " (:zoom mapstate init-zoom))]
            [:p (str "[" line-id "] " line-name "/ [" id "] " station-name)]
            [:input {:value kilotei
                     :on-change (fn [e] (let [new-kilotei (-> e .-target .-value)]
                                          (om/transact! this `[(app/update-station-info {:id ~id
                                                                                         :line-id ~line-id
                                                                                         :kilotei ~new-kilotei})])))}]])]
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
    {:state (atom {})
     :normalize true
     ;; :merge-tree (fn [a b] (println "|merge" a b) (merge a b))
     :parser parser
     :send (util/transit-post "/api")}))

(defn init!
  []
  (om/add-root! reconciler Root (gdom/getElement "app")))
