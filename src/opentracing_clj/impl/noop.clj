(ns opentracing-clj.impl.noop
  (:import [io.opentracing NoopTracerFactory]))

(defn make-tracer
  []
  (NoopTracerFactory/create))
