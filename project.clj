(defproject om-starter "0.1.0-SNAPSHOT"
  :description "My first Om program!"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.293"]
                 [org.omcljs/om "1.0.0-alpha47"]
                 [figwheel-sidecar "0.5.8" :scope "provided"]
                 [com.stuartsierra/component "0.3.1"]
                 [bidi "2.0.14"]
                 [ring/ring "1.5.0"]
                 [ring-transit "0.1.6"]
                 [ring-jetty-component "0.3.1"]
                 [com.cognitect/transit-clj "0.8.293"]
                 [com.cognitect/transit-cljs "0.8.239"]
                 [sablono "0.7.6"]
                 [cljs-http "0.1.42" :exclusions
                  [org.clojure/clojure org.clojure/clojurescript
                   com.cognitect/transit-cljs]]]

  :plugins [[lein-cljsbuild "1.1.1"]
            [lein-environ "1.0.1"]]

  :min-lein-version "2.6.1"

  :source-paths ["src/clj" "src/cljs"]

  :test-paths ["test/clj"]

  :profiles {:dev {
                   ;; :dependencies [[com.cemerick/piggieback "0.2.1"]
                   ;;                [org.clojure/tools.nrepl "0.2.10"]]
                   ;; :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                   :source-paths ["dev/src"]}}
)
