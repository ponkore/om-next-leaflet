(ns om-next-leaflet.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [sablono.core :as html :refer-macros [html]]
            [om-next-leaflet.util :as util]
            [om-next-leaflet.leaflet :as leaflet]))

(defrecord MapState [lat lng zoom bounds])

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

(defmethod read :default ;; :app/mapstate
  [{:keys [state] :as env} k params]
  (if-let [v (get @state k)]
    {:value v}
    {}))

;; TODO: type、title は必須として、placeholder は optional
;; さらに on-click 他の属性を与えたい
;; あと、.form-group .row が前提(label の有無で切り替える？)
(defn input-text
  [type title placeholder]
  (let [id (str (gensym))]
    [:div.form-group.row
     [:label.form-control-label {:class "col-xs-2" :for id} title]
     [:div {:class "col-xs-4"}
      [:input.form-control.form-control-sm {:id id :type type :placeholder placeholder}]]]))

(defn get-mapobj
  [this]
  (-> (om/react-ref this :leaflet) om/get-state :mapobj))

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

(def init-center [34.6964898 135.4930235])
(def init-zoom 12)

(def leaflet-map-fn (om/factory leaflet/Leaflet))

(defn jump-to
  [this lat lng]
  (let [leaflet-map (get-mapobj this)
        marker (leaflet/create-marker lat lng)]
    (leaflet/set-center leaflet-map lat lng)
    (.addTo marker leaflet-map)))

;; add all statiions marker to 'stations-layer'
(defn init-station-markers
  [this stations]
  (let [stations-layer (get-stations-layer this)]
    (doseq [{:keys [id station-name line-name geometry]} stations]
      (let [[lng lat] geometry
            marker (leaflet/create-marker lat lng :radius 6 :fillColor "#0000ff" :fillOpacity 1.0 :weight 1)]
        (doto marker
          (.bindPopup (str "<b>" line-name "</b><br>" station-name))
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
    {:line-id 0})
  static om/IQuery
  (query [this]
    '[:app/title :loading? :app/mapstate :app/lines
      (:app/stations {:line-id ?line-id})
      ])
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
    (let [{:keys [app/title loading?
                  app/mapstate
                  app/stations
                  app/lines]} (om/props this)]
      (html
       [:div
        [:div.row
         [:div.col-xs-3
          [:p title]]
         [:div.col-xs-9
          [:input {:ref "title"}]
          [:button {:on-click (fn [e]
                                (let [new-title (.-value (dom/node this "title"))]
                                  (om/transact! this `[(app/update-title {:new-title ~new-title})
                                                       (app/loading?)
                                                       :app/title
                                                       :loading?
                                                       ])))
                    :disabled loading?} "update"]
          ]]
        [:div.row
         [:div.col-xs-12
          (leaflet-map-fn {:mapid "map"
                           :ref :leaflet ;; referenced from get-mapobj function
                           :center init-center
                           :zoom init-zoom
                           :base-layers [(leaflet/create-tilelayer "OpenStreetMap"
                                           "http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                                           "Map data &copy; <a href=\"http://openstreetmap.org\">OpenStreetMap</a>"
                                           :maxZoom 18)
                                         (leaflet/create-tilelayer "淡色地図"
                                           "http://cyberjapandata.gsi.go.jp/xyz/pale/{z}/{x}/{y}.png"
                                           "<a href='http://www.gsi.go.jp/kikakuchousei/kikakuchousei40182.html' target='_blank'>国土地理院</a>"
                                           :maxZoom 18
                                           :minZoom 12)
                                         (leaflet/create-tilelayer "地理院地図"
                                           "http://cyberjapandata.gsi.go.jp/xyz/std/{z}/{x}/{y}.png"
                                           "<a href='http://www.gsi.go.jp/kikakuchousei/kikakuchousei40182.html' target='_blank'>国土地理院</a>")
                                         ]
                           :event-handlers {:movestart        (partial change-mapstate this)
                                            :move             (partial change-mapstate this)
                                            :moveend          (partial change-mapstate this)
                                            :zoomlevelschange (partial change-mapstate this)
                                            :viewreset        (partial change-mapstate this)
                                            :load             (partial change-mapstate this)}})]]]))))

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
