(defproject om-next-leaflet "0.1.0-SNAPSHOT"
  :description "My first Om program!"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.293"]
                 [org.omcljs/om "1.0.0-alpha47"]
                 [environ "1.1.0"]
                 [figwheel-sidecar "0.5.8" :scope "provided"]
                 [com.stuartsierra/component "0.3.1"]
                 [com.taoensso/timbre "4.7.4"]
                 [bidi "2.0.14"]
                 [ring/ring "1.5.0"]
                 [ring-transit "0.1.6"]
                 [ring-jetty-component "0.3.1"]
                 [com.cognitect/transit-clj "0.8.293"]
                 [com.cognitect/transit-cljs "0.8.239"]
                 [sablono "0.7.6"]
                 [cljs-http "0.1.42" :exclusions
                  [org.clojure/clojure org.clojure/clojurescript
                   com.cognitect/transit-cljs]]
                 [com.layerware/hugsql "0.4.7"]
                 [duct/hikaricp-component "0.1.0"]
                 [org.postgresql/postgresql "9.4.1207"]
                 [binaryage/devtools "0.8.3"]
                 [com.cemerick/piggieback "0.2.1"]
                 [org.clojure/tools.nrepl "0.2.12"]]
  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-environ "1.1.0"]]
  :min-lein-version "2.6.1"
  :source-paths ["src/clj" "src/cljc" "env/dev/clj" "env/prod/clj"]
  :repl-options {:init-ns user
                 :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  :test-paths ["test/clj"]
  :clean-targets ^{:protect false} [:target-path :compile-path "resources/public/js"]
  :main om-next-leaflet.server
  :cljsbuild {:builds
              {:dev {:source-paths ["src/cljs" "src/cljc" "env/dev/cljs"]
                     :figwheel true
                     :compiler
                     {:main om-next-leaflet.dev
                      :asset-path "js"
                      :output-to "resources/public/js/main.js"
                      :output-dir "resources/public/js"
                      :source-map true
                      :source-map-timestamp true
                      :verbose true
                      :optimizations :none
                      :pretty-print true}}
               :min {:source-paths ["src/cljs" "src/cljc" "src/prod/cljs"]
                     :compiler
                     {:output-to "target/cljsbuild/public/js/app.js"
                      :externs ["react/externs/react.js"]
                      :optimizations :advanced
                      :pretty-print false}}}}
  :figwheel {:http-server-root "public"       ;; serve static assets from resources/public/
             :server-port 3449                ;; default
             :server-ip "127.0.0.1"           ;; default
             :css-dirs ["resources/public/css"]
             :ring-handler om-next-leaflet.server/app
             :server-logfile "log/figwheel.log"}
  :profiles {:uberjar
             {:omit-source true
              :prep-tasks ["compile" ["cljsbuild" "once" "min"]]
              :aot :all
              :uberjar-name "om-next-leaflet.jar"
              :source-paths ["src/clj" "src/cljc" "env/prod/clj"]}
             :dev
             {:dependencies [[figwheel-sidecar "0.5.8"]]
              :plugins [[lein-figwheel "0.5.8"]]}}
)
