(ns om-next-leaflet.leaflet
  (:require [om.next :as om :refer-macros [defui]]
            [sablono.core :as html :refer-macros [html]]))

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
  [lat lng & {:keys [] :as opts}]
  (let [opts (merge {} opts)]
    (.marker js/L (.latLng js/L lat lng (clj->js opts)))))

(defn create-circle
  [lat lng radius & {:keys [] :as opts}]
  (let [opts (merge {} opts)]
    (.circle js/L (clj->js [lat lng]) radius opts)))

(defn create-polyline
  [geometry & {:keys [color weight] :as opts}]
  (let [geom (map (fn [[lng lat]] [lat lng]) geometry)
        opts (merge {} opts)]
    (.polyline js/L (clj->js geom) (clj->js opts))))

(defui Leaflet
  Object
  (componentDidMount [this]
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
      (.on leaflet-map "draw:created"     (fn [e] (let [layer (.-layer e)] (.addLayer drawn-items layer))))
      (.on leaflet-map "draw:edited"      (fn [e] (let [layers (.-layers e)] )))
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
