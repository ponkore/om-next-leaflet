(ns om-next-leaflet.common
  (:require [clojure.string :as str]))

#?(:clj
   (defmacro defstate
     [param-state-keyword]
     (let [[_ state-ns state-name] (str/split (str param-state-keyword) #"[:/]")
           mutate-sym (symbol (str state-ns "/update-" state-name))
           new-value-param-sym (symbol (str "new-" state-name))
           state-keyword (keyword (str state-ns "/" state-name))
           method-sym (symbol "mutate")
           state-sym (symbol "state")
           underscore (symbol "_")]
       `(do
          (defmethod ~method-sym '~mutate-sym
            [{:keys [~state-sym]} ~underscore {:keys [~new-value-param-sym]}]
            {:value {:keys [~state-keyword]}
             :action (fn [] (swap! ~state-sym assoc ~state-keyword ~new-value-param-sym))})))))

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
