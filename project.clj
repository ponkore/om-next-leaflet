(defproject om-starter "0.1.0-SNAPSHOT"
  :description "My first Om program!"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.293"]
                 [org.omcljs/om "1.0.0-alpha47"]
                 [figwheel-sidecar "0.5.8" :scope "provided"]
                 [bidi "2.0.14"]
                 [ring/ring "1.5.0"]
                 [ring-transit "0.1.6"]
                 [com.cognitect/transit-clj "0.8.293"]
                 [com.cognitect/transit-cljs "0.8.239"]
                 [sablono "0.7.5"]
                 [cljs-http "0.1.42" :exclusions
                  [org.clojure/clojure org.clojure/clojurescript
                   com.cognitect/transit-cljs]]])
