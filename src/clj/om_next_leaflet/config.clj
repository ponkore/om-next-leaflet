(ns om-next-leaflet.config
  (:require [environ.core :refer [env]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [ring.middleware.transit :refer [wrap-transit-response wrap-transit-params]]))

(defn config []
  {:http-port  (Integer. (or (env :port) 10555))
   :middleware [[wrap-defaults api-defaults]
                wrap-transit-response
                wrap-transit-params
                ; wrap-with-logger
                wrap-gzip]})
