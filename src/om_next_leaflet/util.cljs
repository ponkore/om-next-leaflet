(ns om-next-leaflet.util
  (:require [cognitect.transit :as t])
  (:import [goog.net XhrIo]))

(defn transit-post [url]
  (fn [edn cb]
    (.send XhrIo url
      (fn [e]
        (this-as this
          (cb (t/read (t/reader :json) (.getResponseText this)))))
      "POST" (t/write (t/writer :json) edn)
      #js {"Content-Type" "application/transit+json"})))
