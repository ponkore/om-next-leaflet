(ns om-next-leaflet.ui
  (:require [om.next :as om]
            [om-next-leaflet.ui.button :refer [TestButton]]
            [om-next-leaflet.ui.input :refer [TestInput]]
            [om-next-leaflet.ui.leaflet :refer [Leaflet]]))

(def leaflet-map-fn (om/factory Leaflet))

(def button-fn (om/factory TestButton))

(def input-fn (om/factory TestInput))
