(ns om-next-leaflet.ui.input
  (:require [taoensso.timbre :refer-macros [log trace debug info warn error fatal report]]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [sablono.core :as html :refer-macros [html]]))

(defn text-change-handler
  [this e composing? old-val on-input]
  (let [v (-> e .-target .-value)]
    (when-not @composing?
      (when-not (= v @old-val)
        (reset! old-val v)
        (debug "text-change-handler old-val=" old-val ", v=" v)
        (on-input e)))))

(defui TestInput
  Object
  (componentWillMount [this]
    (debug "will-mount@TestInput")
    (om/update-state! this assoc :composing? (atom false) :old-val (atom "")))
  (componentWillUnmount [this]
    (debug "will-unmount@TestInput")
    (let [old-val (-> this om/get-state :old-val)]
      (reset! old-val "")))
  (render [this]
    (let [{:keys [ref class on-input] :as init-opts} (om/props this)
          {:keys [composing? old-val]} (om/get-state this)
          opts {:on-composition-start (fn [e] (reset! composing? true))
                :on-composition-end (fn [e]
                                      (reset! composing? false)
                                      (text-change-handler this e composing? old-val on-input))
                :on-input (fn [e] (text-change-handler this e composing? old-val on-input))}
          init-opts (dissoc init-opts :on-input)
          opts (merge opts init-opts)]
      (html [:input opts]))))
