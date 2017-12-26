(ns om-next-leaflet.leaflet
  (:require [om.next :as om :refer-macros [defui]]
            [taoensso.timbre :refer-macros [log trace debug info warn error fatal report]]
            [sablono.core :as html :refer-macros [html]]
            [cljsjs.leaflet]
            [cljsjs.leaflet-draw]))

(def mouseover-style {:color "#ff0000" :weight 8 :opacity 0.7})
(def default-style {:color "#666666" :weight 6 :opacity 0.7})
(def polyline-default-style [:color "#666666" :weight 6 :opacity 0.7])
(def marker-default-style [:radius 6 :fillColor "#0000ff" :fillOpacity 1.0 :weight 1])
(def station-marker-default-style {:fillColor "#0000ff"})
(def station-marker-mouseover-style {:fillColor "#ff0000"})

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

(defn set-center
  [leaflet-map lat lng]
  (.setView leaflet-map (.latLng js/L lat lng)))

(defn create-marker
  [lat lng & {:keys [radius] :as opts}]
  (let [opts (merge {} opts)]
    (.circleMarker js/L (.latLng js/L lat lng) (clj->js opts))))

(defn create-circle
  [lat lng radius & {:keys [] :as opts}]
  (let [opts (merge {} opts)]
    (.circle js/L (clj->js [lat lng]) radius opts)))

(defn create-polyline
  [geometry & {:keys [color weight] :as opts}]
  (let [geom (map (fn [[lng lat]] [lat lng]) geometry)
        opts (merge {} opts)]
    (.polyline js/L (clj->js geom) (clj->js opts))))

(defn init-station-markers
  [stations-layer stations]
  (doseq [{:keys [id station-name line-name geometry]} stations]
    (let [[lng lat] geometry
          marker (apply create-marker lat lng marker-default-style)]
      (doto marker
        (.bindPopup (str "<b>" line-name "</b><br>" station-name))
        (.on "mouseover" (fn [e] (.setStyle marker (clj->js station-marker-mouseover-style))))
        (.on "mouseout" (fn [e] (.setStyle marker (clj->js station-marker-default-style))))
        (.addTo stations-layer)))))

(defn init-polylines
  [lines-layer lines]
  (doseq [[id name bounding-box geometry] lines]
    (let [polyline (apply create-polyline geometry polyline-default-style)]
      (doto polyline
        (.bindTooltip (str "<b>" name "[" id "]</b>"))
        (.on "mouseover" (fn [e]
                           (.setStyle polyline (clj->js mouseover-style))
                           (.openTooltip polyline (.-latlng e))))
        (.on "mouseout" (fn [e]
                          (.setStyle polyline (clj->js default-style))
                          (.closeTooltip polyline)))
        (.addTo lines-layer)))))

(defn draw-created!
  [drawn-items]
  (fn [e]
    (let [layer (.-layer e)]
      (.setStyle layer (clj->js default-style))
      (.on layer "mouseover" (fn [e] (.setStyle layer (clj->js mouseover-style))))
      (.on layer "mouseout" (fn [e] (.setStyle layer (clj->js default-style))))
      (.addLayer drawn-items layer))))

(defui Leaflet
  Object
  (componentWillMount [this]
    (.log js/console "will-mount @leaflet.cljs"))
  (componentDidMount [this]
    (.log js/console "did-mount @leaflet.cljs")
    (let [{:keys [mapid center zoom base-layers event-handlers]} (om/props this)
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
      (.on leaflet-map "draw:created" (draw-created! drawn-items))
      ;; (.on leaflet-map "draw:edited" (fn [e] (let [layers (.-layers e)] )))
      ;; event-handlers expected:
      ;;   movestart, move, moveend, zoomlevelschange, viewreset, load
      (doseq [k (keys event-handlers)
              :let [callback (get event-handlers k)
                    event-name (name k)]]
        (.on leaflet-map event-name (fn [e] (callback e leaflet-map))))
      (om/update-state! this assoc :mapobj leaflet-map :stations-layer stations-layer :lines-layer lines-layer)))
  (render [this]
    (html
     [:div {:id (:mapid (om/props this))}])))
