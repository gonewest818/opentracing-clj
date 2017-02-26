(ns opentracing-clj.impl.mock
  (:import [io.opentracing.mock MockTracer
                                MockTracer$Propagator]))

(defn make-tracer
  "generate a mock tracer with optional propagator, currently limited to
  MockTracer$Propagator/PRINTER and MockTracer$Propagator/TEXT_MAP"
  ([]
   (MockTracer.))
  ([propagator]
   (MockTracer. propagator)))
