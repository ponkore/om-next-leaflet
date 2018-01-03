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

(defmethod mutate 'app/update-line-names
  [{:keys [state]} _ {:keys [new-line-names]}]
  {:value {:keys [:app/line-names]}
   :action (fn [] (swap! state assoc :app/line-names new-line-names))})

(defmethod mutate 'app/update-current-line
  [{:keys [state]} _ {:keys [new-line]}]
  {:value {:keys [:app/current-line]}
   :action (fn [] (swap! state assoc :app/current-line new-line))})

(defmulti read om/dispatch)

(defmethod read :default ;; :app/mapstate, :app/title
  [{:keys [state] :as env} k params]
  (if-let [v (get @state k)]
    {:value v}
    {}))
