(ns opentracing-clj.impl.noop
  (:import [io.opentracing.noop NoopTracerFactory]))

(defn make-tracer
  []
  (NoopTracerFactory/create))
