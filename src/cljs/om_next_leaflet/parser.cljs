(ns om-next-leaflet.parser
  (:require [om.next :as om]))

(defmulti mutate om/dispatch)

(defmethod mutate 'app/update-title
  [{:keys [state]} _ {:keys [new-title]}]
  {:value {:keys [:app/title]}
   :action (fn [] (swap! state assoc :app/title new-title))})

(defmethod mutate 'app/update-mapstate
  [{:keys [state]} _ {:keys [new-mapstate]}]
  {:value {:keys [:app/mapstate]}
   :action (fn [] (swap! state assoc :app/mapstate new-mapstate))})

(defmethod mutate 'app/update-lines
  [{:keys [state]} _ {:keys [new-lines]}]
  {:value {:keys [:app/lines]}
   :action (fn [] (swap! state assoc :app/lines new-lines))})

(defmulti read om/dispatch)

(defmethod read :app/title
  [{:keys [state] :as env} k params]
  (let [st @state]
    (if-let [v (get st k)]
      {:value v}
      {})))

(defmethod read :default ;; :app/mapstate
  [{:keys [state] :as env} k params]
  (if-let [v (get @state k)]
    {:value v}
    {}))
