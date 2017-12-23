(ns om-next-leaflet.parser
  (:require [om.next.server :as om]
            [om-next-leaflet.geojson :as geojson]
            [taoensso.timbre :as timbre :refer [log trace debug info warn error fatal]]))

(defmulti readf om/dispatch)

(defmethod readf :app/stations
  [{:keys [state] :as env} k {:keys [line-id] :as params}]
  (let [stations (geojson/get-stations (fn [m] (= (:line-id m) line-id)))]
    {:value stations}))

(defmethod readf :app/lines
  [{:keys [state] :as env} k params]
  (let [value (->> (geojson/get-lines)
                   (map (fn [{:keys [id line-name bounding-box geometry]}] [id line-name bounding-box geometry])))]
    {:value value}))

(defmulti mutatef om/dispatch)
