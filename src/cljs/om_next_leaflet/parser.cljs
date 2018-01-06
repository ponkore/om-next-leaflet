(ns om-next-leaflet.parser
  (:require-macros [om-next-leaflet.parser :refer [defstate]])
  (:require [om.next :as om]))

(defmulti mutate om/dispatch)

(defstate title)
(defstate mapstate)
(defstate lines)
(defstate line-names)
(defstate current-line)
(defstate stations)

(defmulti read om/dispatch)

(defmethod read :default ;; :app/mapstate, :app/title
  [{:keys [state] :as env} k params]
  (if-let [v (get @state k)]
    {:value v}
    {}))
