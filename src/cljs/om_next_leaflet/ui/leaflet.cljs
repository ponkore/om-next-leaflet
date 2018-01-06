(ns om-next-leaflet.ui.leaflet
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [taoensso.timbre :refer-macros [log trace debug info warn error fatal report]]
            [cljs.core.async :refer [put!]]
            [sablono.core :as html :refer-macros [html]]
            [cljsjs.leaflet]
            [cljsjs.leaflet-draw]))

(def custom-styles {:polyline-default {:color "#666666" :weight 6 :opacity 0.7}
                    :polyline-selected {:color "#0000dd" :weight 6 :opacity 0.7}
                    :polyline-mouseover {:color "#ff0000" :weight 6 :opacity 0.7}
                    :marker-default {:radius 6 :fillColor "#0000ff" :fillOpacity 1.0 :weight 1}
                    :station-marker-default {:fillColor "#0000ff"}
                    :station-marker-mouseover {:fillColor "#ff0000"}})

(defn create-tilelayer
  [title url attribution & {:keys [minZoom maxZoom] :as opts}]
  (let [opts (merge {:attribution attribution} opts)]
    {:title title :layer (.tileLayer js/L url (clj->js opts))}))

(defn latlng->clj
  [latlng]
  {:lat (.-lat latlng) :lng (.-lng latlng)})

(defn bounds->clj
  [bounds]
  (let [nw (-> bounds .getNorthWest latlng->clj)
        se (-> bounds .getSouthEast latlng->clj)]
    {:north-west nw :south-east se}))

(defn leaflet-bounds
  [leaflet-map]
  (-> leaflet-map .getBounds bounds->clj))

(defn set-center
  [leaflet-map lat lng]
  (.setView leaflet-map (.latLng js/L lat lng)))

(defn create-marker
  [target-layer line-name station-name geometry]
  (let [[lng lat] geometry
        marker (.circleMarker js/L (.latLng js/L lat lng) (clj->js (:marker-default custom-styles)))]
    (doto marker
      (.bindPopup (str "<b>" line-name "</b><br>" station-name))
      (.on "mouseover" (fn [e] (.setStyle marker (clj->js (:station-marker-mouseover custom-styles)))))
      (.on "mouseout" (fn [e] (.setStyle marker (clj->js (:station-marker-default custom-styles))))))
    marker))

(defn create-polyline
  [target-layer id name geometry selected]
  (let [geom (map (fn [[lng lat]] [lat lng]) geometry)
        default-attr (if selected (:polyline-selected custom-styles) (:polyline-default custom-styles))
        ;; default-attr (:polyline-default custom-styles)
        polyline (.polyline js/L (clj->js geom) (clj->js default-attr))]
    (doto polyline
      (.bindTooltip (str "<b>" name "[" id "]</b>"))
      (.on "mouseover" (fn [e]
                         (.setStyle polyline (clj->js (:polyline-mouseover custom-styles)))
                         (.openTooltip polyline (.-latlng e))))
      (.on "mouseout" (fn [e]
                        (.setStyle polyline (clj->js default-attr))
                        (.closeTooltip polyline))))
    polyline))

(defn init-station-markers
  [leaflet-obj stations]
  (let [target-layer (-> leaflet-obj om/get-state :stations-layer)
        markers (map (fn [{:keys [id station-name line-name geometry]}]
                       (create-marker target-layer line-name station-name geometry))
                     stations)]
    (when-let [old-markers (-> leaflet-obj om/get-state :markers)]
      (doseq [marker old-markers]
        (.removeFrom marker target-layer)))
    (doseq [m markers]
      (.addTo m target-layer))
    (om/update-state! leaflet-obj assoc :markers markers)))

(defn init-polylines
  [leaflet-obj lines]
  (let [target-layer (-> leaflet-obj om/get-state :lines-layer)
        current-line (-> leaflet-obj om/props :current-line)
        polylines (map (fn [[id name bounding-box geometry]]
                         (create-polyline target-layer id name geometry (= id current-line)))
                       lines)]
    (when-let [old-lines (-> leaflet-obj om/get-state :polylines)]
      (doseq [line old-lines]
        (.removeFrom line target-layer)))
    (doseq [l polylines]
      (.addTo l target-layer))
    (om/update-state! leaflet-obj assoc :polylines polylines)))

(defn draw-created-fn
  [this]
  (fn [e]
    (let [layer (.-layer e)
          drawn-items (-> this om/get-state :drawn-items)
          draw-event-chan (-> this om/props :draw-event-chan)]
      (.setStyle layer (clj->js (:polyline-default custom-styles)))
      (.on layer "mouseover" (fn [e] (.setStyle layer (clj->js (:polyline-mouseover custom-styles)))))
      (.on layer "mouseout" (fn [e] (.setStyle layer (clj->js (:polyline-default custom-styles)))))
      (.addLayer drawn-items layer)
      (put! draw-event-chan {:result :success :event e :item (js->clj (.toGeoJSON layer))}))))

(defui Leaflet
  Object
  (componentWillMount [this]
    (debug "will-mount @leaflet.cljs"))
  (componentDidMount [this]
    (debug "did-mount @leaflet.cljs")
    (let [{:keys [mapid center zoom base-layers event-handler current-line]} (om/props this)
          leaflet-map (.map js/L mapid (clj->js {:center center :zoom zoom}))
          drawn-items (.addTo (js/L.FeatureGroup.) leaflet-map)
          stations-layer (.addTo (js/L.FeatureGroup.) leaflet-map)
          lines-layer (.addTo (js/L.FeatureGroup.) leaflet-map)]
      (doseq [{:keys [layer]} base-layers]
        (.addTo layer leaflet-map))
      (.addTo (.layers (.-control js/L)
                       (clj->js (apply merge (map (fn [{:keys [title layer]}] {title layer}) base-layers)))
                       (clj->js {"drawnItems" drawn-items
                                 "stations" stations-layer
                                 "lines" lines-layer})
                       (clj->js { :collapsed false }))
              leaflet-map)
      (.addTo (.scale (.-control js/L)
                      (clj->js { "imperial" false }))
              leaflet-map)
      (.addControl leaflet-map
                   (js/L.Control.Draw. (clj->js {:edit { :featureGroup drawn-items }
                                                 :draw { }})))
      (.on leaflet-map "draw:created" (draw-created-fn this))
      (doseq [event-name ["movestart" "move" "moveend" "zoomlevelschange" "viewreset" "load"]]
        (.on leaflet-map event-name (fn [e] (event-handler e leaflet-map))))
      (om/update-state! this assoc
                        :mapobj leaflet-map
                        :drawn-items drawn-items
                        :stations-layer stations-layer
                        :lines-layer lines-layer)))
  (render [this]
    (html
     [:div {:id (:mapid (om/props this))}])))
