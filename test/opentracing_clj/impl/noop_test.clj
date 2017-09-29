(ns opentracing-clj.impl.noop-test
  (:require [midje.sweet :refer :all]
            [opentracing-clj.impl.noop :refer :all]))


(fact "create noop tracer"
  (make-tracer) => #(instance? io.opentracing.noop.NoopTracer %))

