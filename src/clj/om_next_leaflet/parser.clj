(ns om-next-leaflet.parser
  (:require [om.next.server :as om]
            [om-next-leaflet.geojson :as geojson]))

(defmulti readf om/dispatch)

(defmethod readf :app/title
  [{:keys [state] :as env} k params]
  (let [st @state]
    (if-let [[_ value] (find st k)]
      {:value value}
      {:value "not-found"})))

(defmethod readf :app/stations
  [{:keys [state] :as env} k {:keys [line-id] :as params}]
  (let [line-name (->> (geojson/get-lines (fn [{:keys [id]}] (= id line-id)))
                       first
                       :line-name)
        stations (geojson/get-stations (fn [m] (= (:line-name m) line-name)))]
    {:value stations}))

(defmethod readf :app/lines
  [{:keys [state] :as env} k params]
  (let [value (->> (geojson/get-lines)
                   (map (fn [{:keys [id line-name bounding-box]}] [id line-name bounding-box])))]
    {:value value}))

(defmethod readf :loading?
  [_ _ _]
  {:value false})

(defmulti mutatef om/dispatch)

(defmethod mutatef 'app/update-title
  [{:keys [state]} _ {:keys [new-title]}]
  {:value {:keys [:app/title]}
   :action (fn [] (swap! state assoc :app/title (str new-title " server")))})
