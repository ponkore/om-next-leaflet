(defproject om-next-leaflet "0.1.0-SNAPSHOT"
  :description "My first Om program!"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.293" :scope "provided"]
                 ;; component, environment, logging
                 [environ "1.1.0"]
                 [com.stuartsierra/component "0.3.1"]
                 [com.taoensso/timbre "4.7.4"]
                 ;; http server related dependencies
                 [bidi "2.0.14"]
                 [ring/ring "1.5.0"]
                 [ring-transit "0.1.6"]
                 [ring-jetty-component "0.3.1"]
                 ;; database related dependencies
                 [com.layerware/hugsql "0.4.7"]
                 [duct/hikaricp-component "0.1.0"]
                 [org.postgresql/postgresql "9.4.1207"]
                 ;; http data transmission related dependencies
                 [com.cognitect/transit-clj "0.8.293"]
                 [com.cognitect/transit-cljs "0.8.239"]
                 [cljs-http "0.1.42" :exclusions
                  [org.clojure/clojure org.clojure/clojurescript
                   com.cognitect/transit-cljs]]
                 ;; UI layer dependencies
                 [org.omcljs/om "1.0.0-alpha47"]
                 [sablono "0.7.6"]
                 [cljsjs/leaflet "0.7.7-5"]
                 [cljsjs/leaflet-draw "0.2.3-2"]]
  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-environ "1.1.0"]]
  :min-lein-version "2.6.1"
  :source-paths ["src/clj" "src/cljc"]
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s/"
  :clean-targets ^{:protect false} [:target-path :compile-path "resources/public/js"]
  :figwheel {:http-server-root "public"
             :css-dirs ["resources/public/css"]
             :server-logfile "log/figwheel.log"
             :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  :main om-next-leaflet.main
  :profiles
  {:dev [:project/dev :profiles/dev]
   :test [:project/dev :project/test :profiles/test]
   :uberjar {:omit-source true
             :prep-tasks ["compile" ["cljsbuild" "once" "min"]]
             :aot :all
             :uberjar-name "om-next-leaflet.jar"
             :source-paths ["env/prod/clj"]
             :resource-paths ["env/prod/resources"]
             :cljsbuild
             {:builds
              {:min
               {:source-paths ["src/cljc" "src/cljs" "env/prod/cljs"]
                :compiler
                {:output-to "target/cljsbuild/public/js/app.js"
                 :externs ["react/externs/react.js"]
                 :optimizations :advanced
                 :pretty-print false
                 :closure-warnings {:externs-validation :off :non-standard-jsdoc :off}}}}}}
   :project/dev {:dependencies [[figwheel-sidecar "0.5.8"]
                                [binaryage/devtools "0.8.3"]
                                [com.cemerick/piggieback "0.2.1"]
                                [doo "0.1.7"]]
                 :plugins [[lein-figwheel "0.5.8"]
                           [lein-doo "0.1.7"]
                           [org.clojure/clojurescript "1.9.293"]]
                 :source-paths ["env/dev/clj" "test/clj"]
                 :resource-paths ["env/dev/resources"]
                 :repl-options {:init-ns user}
                 :cljsbuild
                 {:builds
                  {:app
                   {:source-paths ["src/cljs" "src/cljc" "env/dev/cljs"]
                    :compiler
                    {:main "om-next-leaflet.dev"
                     :asset-path "js"
                     :output-to "resources/public/js/main.js"
                     :output-dir "resources/public/js"
                     :source-map true
                     :source-map-timestamp true
                     :optimizations :none
                     :pretty-print true}}}}
                 :doo {:build "test"}}
   :project/test {:resource-paths ["env/test/resources"]
                  :cljsbuild
                  {:builds
                   {:test
                    {:source-paths ["src/cljc" "src/cljs" "test/cljs"]
                     :compiler
                     {:output-to "target/test.js"
                      :main "om-next-leaflet.doo-runner"
                      :optimizations :whitespace
                      :pretty-print true}}}}}
   :profiles/dev {}
   :profiles/test {}})
