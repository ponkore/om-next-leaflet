(ns om-next-leaflet.ui.button
  (:require [taoensso.timbre :refer-macros [log trace debug info warn error fatal report]]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [sablono.core :as html :refer-macros [html]]))

(defui TestButton
  Object
  (render [this]
    (let [{:keys [ref class on-click] :as init-opts} (om/props this)
          opts {:on-click (fn [e] (on-click e))}
          init-opts (dissoc init-opts :on-click)
          opts (merge opts init-opts)]
      (html [:button opts "update"]))))
