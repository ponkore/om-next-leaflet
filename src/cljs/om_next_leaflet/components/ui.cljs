(ns om-next-leaflet.components.ui
  (:require [com.stuartsierra.component :as component]
            [om-next-leaflet.core :refer [render]]))

(defrecord UIComponent []
  component/Lifecycle
  (start [component]
    (render)
    component)
  (stop [component]
    component))

(defn new-ui-component []
  (map->UIComponent {}))
