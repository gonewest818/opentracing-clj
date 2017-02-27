(ns opentracing-clj.impl.mock
  (:import [io.opentracing.mock MockTracer
                                MockTracer$Propagator]))

(def props {:printer  MockTracer$Propagator/PRINTER
            :text-map MockTracer$Propagator/TEXT_MAP})

(defn make-tracer
  "generate a mock tracer with optional propagator, currently limited to
  :printer and :text-map
  MockTracer$Propagator/PRINTER and MockTracer$Propagator/TEXT_MAP"
  ([]
   (MockTracer.))
  ([propagator]
   (condp = propagator
     :text-map (MockTracer. MockTracer$Propagator/TEXT_MAP)
     :printer  (MockTracer. MockTracer$Propagator/PRINTER)
     (throw (Exception. "unknown or unsupported propagator")))))
