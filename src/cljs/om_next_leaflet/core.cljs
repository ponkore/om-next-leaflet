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

(defrecord MapState [lat lng zoom bounds])

(defonce app-state (atom {:app/title ""
                          :app/mapstate (map->MapState {:lat 34.6964898
                                                        :lng 135.4930235
                                                        :zoom 12})}))

(def parser (om/parser {:read parser/read :mutate parser/mutate}))

(def reconciler (om/reconciler {:state app-state :parser parser}))

(declare Root)

(defn render []
  (om/add-root! reconciler Root (js/document.getElementById "app")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defmulti channel-handler (fn [this data] (:tag data)))

(defmethod channel-handler :leaflet/lines
  [this data]
  (let [leaflet-obj (om/react-ref this :leaflet)]
    (debug "after alts! lines")
    (leaflet/init-polylines leaflet-obj (:data data))))

(defmethod channel-handler :leaflet/stations
  [this data]
  (let [leaflet-obj (om/react-ref this :leaflet)]
    (debug "after alts! stations")
    (leaflet/init-station-markers leaflet-obj (:data data))))

(defmethod channel-handler :leaflet/draw-event
  [this data]
  (let [leaflet-obj (om/react-ref this :leaflet)]
    (debug "after alts! leaflet-created" data)))

(defmethod channel-handler :app/events
  [this data]
  (let [{:keys [event-id data]} data]
    (debug "after alts! :app/events :event-id " event-id ", data=" data)
    (om/transact! this `[(app/update-title {:new-title ~data})])))

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

(defn api--get-lines
  [chan]
  (util/send-request! :get "/api2/lines" nil chan))

(defn api--get-stations
  [chan line-no]
  (util/send-request! :get (str "/api2/lines/" line-no "/stations") nil chan))

(defui TestButton
  Object
  (render [this]
    (let [{:keys [event-chan input-node]} (om/props this)]
      (html
       [:button {:on-click (fn [e]
                             (let [new-title (.-value input-node)]
                               (put! event-chan {:result :success :event-id :app/on-click :data new-title})))}
        "update"]))))

(def button-fn (om/factory TestButton))

(defui TestInput
  Object
  (render [this]
    (let [{:keys [ref title event-chan]} (om/props this)]
      (html
       [:input {:ref ref
                :value (if (nil? title) "" title)
                :on-change (fn [e]
                             (let [new-title (-> e .-target .-value)]
                               (put! event-chan {:result :success :event-id :app/update-title :data new-title})))}]))))

(def input-fn (om/factory TestInput))

(defui Root
  static om/IQuery
  (query [this]
    '[:app/title
      :app/mapstate])
  Object
  (componentDidMount [this]
    (debug "did-mount")
    (let [channels {:leaflet/lines (chan)
                    :leaflet/stations (chan)
                    :leaflet/draw-event (chan)
                    :app/events (chan)}]
      ;; watch channels
      (main-channel-loop this channels)
      ;; initialize
      (api--get-lines (:leaflet/lines channels))
      (api--get-stations (:leaflet/stations channels) 24)
      (om/update-state! this assoc
                        :channels channels
                        :input-node (dom/node this "title"))))
  (componentWillUnmount [this]
    (debug "will-unmount"))
  (render [this]
    (let [{:keys [app/title app/mapstate]} (om/props this)
          draw-event-chan (-> this om/get-state :channels :leaflet/draw-event)
          event-chan (-> this om/get-state :channels :app/events)]
      (html
       [:div
        [:div {:id "custom-control"
               :class "leaflet-control-layers leaflet-control-layers-expanded leaflet-control"}
         (input-fn {:ref "title" :title title :event-chan event-chan})
         (button-fn {:event-chan event-chan :input-node (-> this om/get-state :input-node)})
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
                         :draw-event-chan draw-event-chan})]))))
