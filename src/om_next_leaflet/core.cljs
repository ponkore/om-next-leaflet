(ns om-next-leaflet.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [sablono.core :as html :refer-macros [html]]
            [om-next-leaflet.util :as util]
            [om-next-leaflet.leaflet :as leaflet]))

(enable-console-print!)

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

(defn map-state-change
  [this e leaflet-map]
  (let [center (-> leaflet-map .getCenter leaflet/latlng->clj)
        mapstate (map->MapState {:lat (:lat center)
                                 :lng (:lng center)
                                 :zoom (-> leaflet-map .getZoom)
                                 :bounds (-> leaflet-map .getBounds leaflet/bounds->clj)})]
    (.log js/console (str "[" (-> e .-type) "]"))
    (om/transact! this `[(app/update-mapstate {:new-mapstate ~mapstate})])))

(defn map-callback-dispatch [this e leaflet-map] (-> e .-type keyword))

(defmulti map-callback-fn* map-callback-dispatch)
(defmethod map-callback-fn* :movestart        [this e leaflet-map] (map-state-change this e leaflet-map))
(defmethod map-callback-fn* :move             [this e leaflet-map] (map-state-change this e leaflet-map))
(defmethod map-callback-fn* :moveend          [this e leaflet-map] (map-state-change this e leaflet-map))
(defmethod map-callback-fn* :zoomlevelschange [this e leaflet-map] (map-state-change this e leaflet-map))
(defmethod map-callback-fn* :viewreset        [this e leaflet-map] (map-state-change this e leaflet-map))
(defmethod map-callback-fn* :load             [this e leaflet-map] (map-state-change this e leaflet-map))

(defn map-callback-fn
  [this e leaflet-map]
  (map-callback-fn* this e leaflet-map))

(def init-center [34.6964898 135.4930235])
(def init-zoom 15)

(def leaflet-map (om/factory leaflet/Leaflet))

(defn jump-to
  [this lat lng]
  (let [leaflet-map (get-mapobj this)
        marker (leaflet/create-marker lat lng)]
    (leaflet/set-center leaflet-map lat lng)
    (.addTo marker leaflet-map)))

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
  (componentDidMount [this]
    (.log js/console "mount"))
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
          [:button {:on-click (fn [e]
                                (let [leaflet-map (get-mapobj this)]
                                  ))} "rect"]]]
        [:div.row
         [:div.col-xs-3
          (into [] (concat [:select.custom-select
                            {:ref "sel1"
                             :on-change (fn [e]
                                          (let [line-id (-> (om/react-ref this "sel1") .-value js/parseInt)]
                                            (om/set-query! this {:params {:line-id line-id}})))}]
                           (map (fn [[id line-name]] [:option {:value (str id)} line-name]) lines)))
          ;; [:div.row
          ;;  [:button {:on-click (fn [e] (leaflet/set-center (get-mapobj this)
          ;;                                (first init-center)
          ;;                                (second init-center)))} "center"]]
          ;; [:p "zoom: " (:zoom mapstate)]
          [:div.list-group {:style {:overflow-y "scroll", :height "400px"}}
           (mapv (fn [{:keys [id station-name geometry]}]
                   (let [[lng lat] geometry
                         id-str (str id "/" lat "/" lng)]
                     [:a.list-group-item.list-group-item-action
                      {:href "#" :key id :on-click (fn [e] (jump-to this lat lng))} station-name]))
                 stations)]]
         [:div.col-xs-9
          (leaflet-map {:mapid "map"
                        :ref :leaflet
                        :center init-center
                        :zoom init-zoom
                        :base-layer (leaflet/create-tilelayer "OpenStreetMap"
                                      "http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                                      "Map data &copy; <a href=\"http://openstreetmap.org\">OpenStreetMap</a>"
                                      :maxZoom 18)
                        :optional-layer (leaflet/create-tilelayer "地理院地図"
                                          "http://cyberjapandata.gsi.go.jp/xyz/std/{z}/{x}/{y}.png"
                                          "<a href='http://www.gsi.go.jp/kikakuchousei/kikakuchousei40182.html' target='_blank'>国土地理院</a>")
                        :callback-fn (partial map-callback-fn this)})]]]))))

(def parser (om/parser {:read read :mutate mutate}))

(def reconciler
  (om/reconciler
    {:state (atom {})
     :normalize true
     ;; :merge-tree (fn [a b] (println "|merge" a b) (merge a b))
     :parser parser
     :send (util/transit-post "/api")}))

(om/add-root! reconciler Root (gdom/getElement "app"))
