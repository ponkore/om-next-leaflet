(ns om-next-leaflet.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [om-next-leaflet.core-test]))

(doo-tests 'om-next-leaflet.core-test)

