(ns ^:figwheel-no-load om-next-leaflet.dev
  (:require [om-next-leaflet.core :as core]
            [figwheel.client :as figwheel :include-macros true]))

(enable-console-print!)

(figwheel/watch-and-reload
  :websocket-url "ws://localhost:3449/figwheel-ws"
  :on-jsload core/init!)

(core/init!)
