(ns om-next-leaflet.leaflet
  (:require [om.next :as om :refer-macros [defui]]
            [sablono.core :as html :refer-macros [html]]))

(defn create-tilelayer
  [title url attribution & {:keys [maxZoom] :as opts}]
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
  [lat lng]
  (.marker js/L (.latLng js/L lat lng)))

(defui Leaflet
  Object
  (componentDidMount [this]
    (let [{:keys [mapid center zoom base-layer optional-layer event-handlers]} (om/props this)
          leaflet-map (.map js/L mapid (clj->js {:center center :zoom zoom}))
          drawn-items (.addTo (js/L.FeatureGroup.) leaflet-map)
          ext-layer (fn [{:keys [title layer]}] {title layer})]
      (.addTo (:layer base-layer) leaflet-map)
      (.addTo (.layers (.-control js/L)
                       (clj->js (merge (ext-layer base-layer)
                                       (ext-layer optional-layer)))
                       (clj->js { "drawnItems" drawn-items })
                       (clj->js { :collapsed false }))
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
      (om/update-state! this assoc :mapobj leaflet-map)))
  (render [this]
    (html
     [:div {:id (:mapid (om/props this))}])))
