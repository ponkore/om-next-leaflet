(ns om-next-leaflet.api
  (:require [clojure.string :as str]
            [taoensso.timbre :refer-macros [log trace debug info warn error fatal report]]
            [om-next-leaflet.util :as util]))

(defn get-lines
  [chan]
  (util/send-request! :get "/api2/lines" nil chan))

(defn get-stations
  [chan line-no]
  (util/send-request! :get (str "/api2/lines/" line-no "/stations") nil chan))

(defn get-lines-in-bounds
  [chan bounds zoom]
  (let [{:keys [north-west south-east]} bounds
        [nwlat nwlng] north-west
        [selat selng] south-east]
    (util/send-request! :get (str "/api2/lines-in-bounds/" zoom "/" nwlat "," nwlng "-" selat "," selng) nil chan)))

(defn get-stations-in-bounds
  [chan bounds zoom]
  (let [{:keys [north-west south-east]} bounds
        [nwlat nwlng] north-west
        [selat selng] south-east]
    (util/send-request! :get (str "/api2/stations-in-bounds/" zoom "/" nwlat "," nwlng "-" selat "," selng) nil chan)))

(defn get-line-names
  [chan]
  (util/send-request! :get "/api2/line-names" nil chan))
