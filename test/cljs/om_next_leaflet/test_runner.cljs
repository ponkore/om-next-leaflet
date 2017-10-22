(ns om-next-leaflet.test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [om-next-leaflet.core-test]
   [om-next-leaflet.common-test]))

(enable-console-print!)

(doo-tests 'om-next-leaflet.core-test
           'om-next-leaflet.common-test)
