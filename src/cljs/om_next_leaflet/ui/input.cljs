(ns om-next-leaflet.ui.input
  (:require [clojure.string :as str]
            [taoensso.timbre :refer-macros [log trace debug info warn error fatal report]]
            [cljs.core.async :refer [put! chan <! go go-loop]]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [sablono.core :as html :refer-macros [html]]))

(defn text-change-handler
  [this e composing? old-val event-chan]
  (let [v (-> e .-target .-value)]
    (when-not @composing?
      (when-not (= v @old-val)
        (reset! old-val v)
        (put! event-chan {:result :success :event-id :app/update-title :data v})
        (debug "text-change-handler old-val=" old-val ", v=" v)))))

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
    (let [{:keys [ref title event-chan]} (om/props this)
          {:keys [composing? old-val]} (om/get-state this)]
      (html
       [:input {:ref ref
                :on-composition-start (fn [e] (reset! composing? true))
                :on-composition-end (fn [e]
                                      (reset! composing? false)
                                      (text-change-handler this e composing? old-val event-chan))
                :on-input (fn [e] (text-change-handler this e composing? old-val event-chan))}]))))
