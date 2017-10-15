(ns opentracing-clj.impl.noop
  (:import [io.opentracing.noop NoopTracerFactory]))

(defn make-tracer
  "generate a noop tracer"
  []
  (NoopTracerFactory/create))
