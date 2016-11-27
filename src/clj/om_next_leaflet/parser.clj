(ns om-next-leaflet.parser
  (:require [om.next.server :as om]
            [om-next-leaflet.geojson :as geojson]
            [taoensso.timbre :as timbre :refer [log trace debug info warn error fatal]]
            [clojure.java.jdbc :as j]))

(defmulti readf om/dispatch)

;; (j/with-db-connection [conn (:spec db)]
;;   (j/query conn ["select * from language"]))

(defmethod readf :app/title
  [{:keys [state] :as env} k params]
  (let [st @state]
    (if-let [[_ value] (find st k)]
      {:value value}
      {:value "not-found"})))

(defmethod readf :app/stations
  [{:keys [state] :as env} k {:keys [line-id] :as params}]
  (let [stations (geojson/get-stations (fn [m] (= (:line-id m) line-id)))]
    {:value stations}))

(defmethod readf :app/lines
  [{:keys [state] :as env} k params]
  (let [value (->> (geojson/get-lines)
                   (map (fn [{:keys [id line-name bounding-box geometry]}] [id line-name bounding-box geometry])))]
    {:value value}))

(defmethod readf :loading?
  [_ _ _]
  {:value false})

(defmulti mutatef om/dispatch)

(defmethod mutatef 'app/update-title
  [{:keys [state]} _ {:keys [new-title]}]
  {:value {:keys [:app/title]}
   :action (fn [] (swap! state assoc :app/title (str new-title " server")))})
