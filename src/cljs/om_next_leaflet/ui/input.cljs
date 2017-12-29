(ns om-next-leaflet.ui.input
  (:require [clojure.string :as str]
            [taoensso.timbre :refer-macros [log trace debug info warn error fatal report]]
            [cljs.core.async :refer [put! chan <! go go-loop]]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [sablono.core :as html :refer-macros [html]]))

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
