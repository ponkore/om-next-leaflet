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
