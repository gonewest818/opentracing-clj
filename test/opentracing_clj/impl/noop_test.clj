(ns opentracing-clj.impl.noop-test
  (:require [clojure.test :refer :all]
            [opentracing-clj.impl.noop :refer :all]))

(deftest constructor
  (is (instance? io.opentracing.noop.NoopTracer
                 (make-tracer))))
