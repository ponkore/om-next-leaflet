(ns om-next-leaflet.parser
  (:require [om.next :as om :refer-macros [defui]]))

(defmulti mutate om/dispatch)

(defmethod mutate 'app/update-title
  [{:keys [state]} _ {:keys [new-title]}]
  {:value {:keys [:app/title]}
   :action (fn [] (swap! state assoc :app/title new-title))})

(defmethod mutate 'app/update-mapstate
  [{:keys [state]} _ {:keys [new-mapstate]}]
  {:value {:keys [:app/mapstate]}
   :action (fn [] (swap! state assoc :app/mapstate new-mapstate))})

(defmulti read om/dispatch)

(defmethod read :app/title
  [{:keys [state] :as env} k params]
  (let [st @state]
    (if-let [v (get st k)]
      {:value v} ;; :remote true
      {})))

(defmethod read :app/stations
  [{:keys [state] :as env} k {:keys [line-id] :as params}]
  (let [st @state]
    (if-let [v (get st k)]
      {:value v :remote true}
      {:remote true})))

(defmethod read :app/lines
  [{:keys [state] :as env} k params]
  (let [st @state]
    (if-let [v (get st k)]
      {:value v :remote true}
      {:remote true})))

(defmethod read :app/station-info
  [{:keys [state] :as env} k _]
  (let [st @state]
    (if-let [v (get st k)]
      {:value v} ;; :remote true
      {})))

(defmethod read :default ;; :app/mapstate
  [{:keys [state] :as env} k params]
  (if-let [v (get @state k)]
    {:value v}
    {}))
