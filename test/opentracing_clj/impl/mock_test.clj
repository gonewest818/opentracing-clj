(ns opentracing-clj.impl.mock-test
  (:require [midje.sweet :refer :all]
            [opentracing-clj.impl.mock :refer :all])
  (:import [io.opentracing.mock MockTracer MockTracer$Propagator]))


(fact "create mock tracer"
  (make-tracer) => #(instance? io.opentracing.mock.MockTracer %))

(fact "create mock tracer with propagator"
  (make-tracer MockTracer$Propagator/PRINTER)
  => #(instance? io.opentracing.mock.MockTracer %)

  (make-tracer MockTracer$Propagator/TEXT_MAP)
  => #(instance? io.opentracing.mock.MockTracer %))
