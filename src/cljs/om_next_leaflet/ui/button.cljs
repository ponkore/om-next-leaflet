(ns om-next-leaflet.ui.button
  (:require [om.next :as om :refer-macros [defui]]
            [sablono.core :as html :refer-macros [html]]))

;; (schema
;;  (s/optional-key :ref) s/Any
;;  (s/optional-key :class) s/Str
;;  (s/optional-key :on-click) s/Fn
;;  :text s/Str)

(defui TestButton
  Object
  (render [this]
    (let [{:keys [ref class on-click text] :as init-opts} (om/props this)
          opts (assoc init-opts :on-click (fn [e] (on-click e)))]
      (html [:button opts text]))))

(def button-fn (om/factory TestButton))
