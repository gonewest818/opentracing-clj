(defproject org.clojars.gonewest818/opentracing-clj "0.1.0-SNAPSHOT"
  :description "OpenTracing bindings for Clojure"
  :url "http://github.com/gonewest818/opentracing-clj"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [io.opentracing/opentracing-api "0.20.9"]
                 [io.opentracing/opentracing-noop "0.20.9"]
                 [io.opentracing/opentracing-mock "0.20.9"]]
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/clojars_username
                                    :password :env/clojars_password}]]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[midje "1.8.3"]]
                   :plugins [[lein-midje "3.2.1"]
                             [lein-cloverage "1.0.9"]]}})
