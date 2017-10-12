(ns opentracing-clj.propagation-test
  (:require [clojure.test :refer :all]
            [opentracing-clj.propagation :refer :all]
            [opentracing-clj.core :refer [scope]]
            [opentracing-clj.impl.mock :refer [make-tracer]]))

(def tracer (atom (make-tracer :text-map)))

(use-fixtures :each
  (fn [test]
    (.reset @tracer)
    (test)))

(deftest inject-http
  (let [s (scope @tracer "foo" {"a" "1"})
        c (-> s .span .context)]
    (is (= #{"traceid" "spanid"}
           (set (keys (inject @tracer c :http)))))))

(deftest inject-text-map
  (let [s (scope @tracer "foo" {"a" "1"})
        c (-> s .span .context)]
    (is (= #{"traceid" "spanid"}
           (set (keys (inject @tracer c :text)))))))

(deftest extract-http
  (let [c (extract @tracer {"traceid" "1" "spanid" "2"} :http)]
    (is (= 1 (.traceId c)))
    (is (= 2 (.spanId c)))))

(deftest extract-text-map
  (let [c (extract @tracer {"traceid" "1" "spanid" "2"} :text)]
    (is (= 1 (.traceId c)))
    (is (= 2 (.spanId c)))))
