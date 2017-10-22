(ns cljs.user
  (:require [om-next-leaflet.core]
            [om-next-leaflet.system :as system]
            [dirac.runtime]))

(def go system/go)
(def reset system/reset)
(def stop system/stop)
(def start system/start)

(dirac.runtime/install!)
