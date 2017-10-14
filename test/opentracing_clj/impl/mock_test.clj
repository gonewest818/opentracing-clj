(ns opentracing-clj.impl.mock-test
  (:require [clojure.test :refer :all]
            [opentracing-clj.impl.mock :refer :all])
  (:import [io.opentracing.mock MockTracer MockTracer$Propagator]))


(deftest constructor
  (is (instance? io.opentracing.mock.MockTracer
                 (make-tracer)))
  (is (instance? io.opentracing.mock.MockTracer
                 (make-tracer :printer)))
  (is (instance? io.opentracing.mock.MockTracer
                 (make-tracer :text-map)))
  (is (thrown-with-msg? Exception #"unknown or unsupported propagator"
                        (make-tracer :unknown))))
