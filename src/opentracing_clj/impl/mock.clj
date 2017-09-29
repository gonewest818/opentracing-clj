(ns opentracing-clj.impl.mock
  (:import [io.opentracing.mock MockTracer
                                MockTracer$Propagator]))

(def props {:printer  MockTracer$Propagator/PRINTER
            :text-map MockTracer$Propagator/TEXT_MAP})

(defn make-tracer
  "generate a mock tracer with optional propagator, currently limited to
  :printer (MockTracer$Propagator/PRINTER)
  and :text-map (MockTracer$Propagator/TEXT_MAP)"
  ([]
   (MockTracer.))
  ([propagator]
   (if-let [p (get props propagator)]
     (MockTracer. p)
     (throw (Exception. "unknown or unsupported propagator")))))
