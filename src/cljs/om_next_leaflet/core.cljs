(ns om-next-leaflet.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [sablono.core :as html :refer-macros [html]]
            [om-next-leaflet.util :as util]
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

(defrecord StationInfo [id name line-info])
(defrecord StationLineInfo [station-id line-id kilotei])

(defmulti mutate om/dispatch)

(defmethod mutate 'app/update-title
  [{:keys [state]} _ {:keys [new-title]}]
  {:remote true
   :value {:keys [:app/title]}
   :action (fn [] (swap! state assoc :app/title new-title))})

(defmethod mutate 'app/loading?
  [{:keys [state]} _ _]
  {:value {:keys [:loading?]}
   :action (fn [] (swap! state assoc :loading? true))})

(defmethod mutate 'app/update-mapstate
  [{:keys [state]} _ {:keys [new-mapstate]}]
  {:value {:keys [:app/mapstate]}
   :action (fn [] (swap! state assoc :app/mapstate new-mapstate))})

(defmethod mutate 'app/update-station-info
  [{:keys [state]} _ {:keys [new-station-info]}]
  {;; :remote true
   :value {:keys [:app/station-info]}
   :action (fn [] (swap! state assoc :app/station-info new-station-info))})

(defmulti read om/dispatch)

(defmethod read :app/title
  [{:keys [state] :as env} k params]
  (let [st @state]
    (if-let [v (get st k)]
      {:value v :remote true}
      {:remote true})))

(defmethod read :app/stations
  [{:keys [state] :as env} k {:keys [line-id] :as params}]
  (let [st @state]
    (if-let [v (get st k)]
      {:value v :remote true}
      {:remote true})))

(defmethod read :app/lines
  [{:keys [state] :as env} k params]
  (let [st @state]
    (if-let [v (get st k)]
      {:value v :remote true}
      {:remote true})))

(defmethod read :loading?
  [{:keys [state] :as env} k _]
  (let [st @state]
    (let [v (get st :loading? false)]
      (if v
        {:value v :remote true}
        {:remote true}))))

(defmethod read :app/station-info
  [{:keys [state] :as env} k _]
  (let [st @state]
    (if-let [v (get st k)]
      {:value v} ;; :remote true
      {})))

(defmethod read :default ;; :app/mapstate
  [{:keys [state] :as env} k params]
  (if-let [v (get @state k)]
    {:value v}
    {}))

(defn get-stations-layer
  [this]
  (-> (om/react-ref this :leaflet) om/get-state :stations-layer))

(defn get-lines-layer
  [this]
  (-> (om/react-ref this :leaflet) om/get-state :lines-layer))

(defn change-mapstate
  [this e leaflet-map]
  (let [event-type (-> e .-type keyword) ;; for debug
        center (-> leaflet-map .getCenter leaflet/latlng->clj)
        mapstate (map->MapState {:lat (:lat center)
                                 :lng (:lng center)
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
          (.on "click" (fn [e] (let [new-station-info (map->StationInfo {:id id
                                                                         :name station-name
                                                                         :line-info
                                                                         (list
                                                                          (map->StationLineInfo {:station-id id
                                                                                                 :line-id line-name
                                                                                                 :kilotei 10}))
                                                                         })]
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
    {:line-id 25})
  static om/IQuery
  (query [this]
    '[:app/title
      :loading?
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
                  loading?
                  app/mapstate
                  app/stations
                  app/lines
                  app/station-info]} (om/props this)]
      (html
       [:div
        [:div.leaflet-control-layers.leaflet-control-layers-expanded.leaflet-control
         {:style {:position "absolute"
                  :top "10px"
                  :left "45px"
                  :width "200px"
                  :bottom "40px"
                  :background "#ffffff"
                  :font-size "12px"
                  :box-shadow "1px 1px 5px rgba(0,0,0,0,4)"
                  :border-radius "5px"
                  :padding "6px 10px 6px 6px"
                  :color "#333"}}
         [:input {:ref "title"}]
         [:p title]
         [:p (str "zoom: " (:zoom mapstate init-zoom))]
         [:p (str "station-id:" (:id station-info))]
         [:p (str "station-name:" (:name station-info))]
         [:p (str "line-id: " (:line-id (first (:line-info station-info))))]
         [:input {:ref "kilotei" :value (str (:kilotei (first (:line-info station-info))))
                  :on-change (fn [e] (let [new-text (-> e .-target .-value)
                                           li (first (:line-info station-info))
                                           li (list (update li :kilotei (fn [_] constantly new-text)))
                                           new-value (update station-info :line-info (fn [_] constantly li))]
                                       (om/transact! this `[(app/update-station-info {:new-station-info ~new-value})])))}]
         [:button {:on-click (fn [e]
                                (let [new-title (.-value (dom/node this "title"))]
                                  (om/transact! this `[(app/update-title {:new-title ~new-title})
                                                       (app/loading?)
                                                       :app/title
                                                       :loading?
                                                       ])))
                   :disabled loading?} "update"]]
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

(def parser (om/parser {:read read :mutate mutate}))

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
