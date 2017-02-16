(ns opentracing-clj.impl.mock
  (:import [io.opentracing.mock MockTracer
                                MockTracer$Propagator]))

(defn make-tracer
  "generate a mock tracer with optional propagator (for now only MockTracer$Propagator/PRINTER)"
  ([]
   (MockTracer.))
  ([propagator]
   (MockTracer. propagator)))
