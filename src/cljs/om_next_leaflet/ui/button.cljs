(ns om-next-leaflet.ui.button
  (:require [clojure.string :as str]
            [taoensso.timbre :refer-macros [log trace debug info warn error fatal report]]
            [cljs.core.async :refer [put! chan <! go go-loop]]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [sablono.core :as html :refer-macros [html]]))

(defui TestButton
  Object
  (render [this]
    (let [{:keys [event-chan input-node]} (om/props this)]
      (html
       [:button {:on-click (fn [e]
                             (let [new-title (.-value input-node)]
                               (put! event-chan {:result :success :event-id :app/on-click :data new-title})))}
        "update"]))))
