(ns om-next-leaflet.parser
  (:require [om.next :as om]))

(defmacro defstate
  [state-sym]
  (let [state-name (name state-sym)
        mutate-sym (symbol (str "app/update-" state-name))
        new-value-param-sym (symbol (str "new-" state-name))
        new-value-keyword (keyword (str "app/" state-name))
        state-sym (symbol "state")
        underscore (symbol "_")]
    `(do
       (defmethod mutate '~mutate-sym
         [{:keys [~state-sym]} ~underscore {:keys [~new-value-param-sym]}]
         {:value {:keys [~new-value-keyword]}
          :action (fn [] (swap! ~state-sym assoc ~new-value-keyword ~new-value-param-sym))}))))

#_"
;; (defstate stations)
;; =>
(defmethod mutate 'app/update-stations
  [{:keys [state]} _ {:keys [new-stations]}]
  {:value {:keys [:app/stations]}
   :action (fn [] (swap! state assoc :app/stations new-stations))})

(defmethod read :app/stations
  [{:keys [state] :as env} k params]
  (if-let [v (get @state k)]
    {:value v}
    {}))
"

;; (defstate stations
;;   :app/stations
;;   [state state-key new-value]
;;   {:mutate (swap! state assoc state-key new-value)
;;    :read (get @state state-key)})
