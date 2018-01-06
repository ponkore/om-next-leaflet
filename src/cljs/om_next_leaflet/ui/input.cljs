(ns om-next-leaflet.ui.input
  (:require [om.next :as om :refer-macros [defui]]
            [sablono.core :as html :refer-macros [html]]))

;; (schema
;;  (s/optional-key :ref) s/Any
;;  (s/optional-key :class) s/Str
;;  (s/optional-key :deafult-value) s/Str
;;  :on-composition-start s/Fn
;;  :on-composition-end s/Fn
;;  (s/optional-key :on-input) s/Fn
;;  )

(defn text-change-handler
  [this e composing? old-val on-input]
  (let [v (-> e .-target .-value)]
    (when-not @composing?
      (when-not (= v @old-val)
        (reset! old-val v)
        (on-input e)))))

(defui TestInput
  Object
  (componentWillMount [this]
    (om/update-state! this assoc :composing? (atom false) :old-val (atom "")))
  (componentWillUnmount [this]
    (let [old-val (-> this om/get-state :old-val)]
      (reset! old-val "")))
  (render [this]
    (let [{:keys [ref class on-input default-value placeholder] :as init-opts} (om/props this)
          {:keys [composing? old-val]} (om/get-state this)
          opts {:on-composition-start (fn [e] (reset! composing? true))
                :on-composition-end (fn [e]
                                      (reset! composing? false)
                                      (text-change-handler this e composing? old-val on-input))
                :on-input (fn [e] (text-change-handler this e composing? old-val on-input))}
          opts (merge init-opts opts)]
      (html [:input opts]))))
