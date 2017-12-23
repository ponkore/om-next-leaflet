(ns om-next-leaflet.components.shell-component
  (:require [com.stuartsierra.component :as component]
            [clojure.string :as str]))

(defrecord ShellComponent [command]
  component/Lifecycle
  (start [this]
    (when-not (:running this)
      (println "Shell command:" (str/join " " command))
      #_(future (apply clojure.java.shell/sh command)))
    (assoc this :running true))
  (stop [this]
    (when (:running this)
      (println "Stopping (fake).")
      (dissoc this :running))))

(defn shell-component [& cmd]
  (->ShellComponent cmd))
