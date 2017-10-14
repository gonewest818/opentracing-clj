(defproject org.clojars.gonewest818/opentracing-clj "0.2.0-SNAPSHOT"
  :description "OpenTracing bindings for Clojure"
  :url "http://github.com/gonewest818/opentracing-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [io.opentracing/opentracing-api  "0.31.0-RC1"]
                 [io.opentracing/opentracing-mock "0.31.0-RC1"]
                 [io.opentracing/opentracing-noop "0.31.0-RC1"]
                 [io.opentracing/opentracing-util "0.31.0-RC1"]]
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/clojars_username
                                    :password :env/clojars_password}]]
  :profiles {:dev {:plugins [[lein-cloverage "1.0.9"]]}})
