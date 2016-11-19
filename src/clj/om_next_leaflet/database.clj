(ns om-next-leaflet.database
  (:require [com.stuartsierra.component :as component]
            [duct.component.hikaricp :as hcp]))

(def db (hcp/hikaricp {:uri "jdbc:postgresql://localhost:5432/dvdrental"}))
