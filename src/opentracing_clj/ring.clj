(ns opentracing-clj.ring
  (:require [opentracing-clj.core :as ot]))


(defn wrap-opentracing
  "ring middleware creates a span from the inbound request
  with the given name and optional tags, then leaves the tracing
  context in the request dictionary as :opentracing-context"
  ([handler tracer op-name]
   (wrap-opentracing handler tracer op-name nil))
  ([handler tracer op-name tags]
   (fn [request]
     ;;(log/debug (:headers request))
     (if-let [ctx (ot/http->context tracer (:headers request))]
       (ot/with-trace [s (ot/child-span tracer ctx op-name tags)]
         (handler (assoc request :opentracing-context s)))
       (ot/with-trace [s (ot/span tracer op-name tags)]
         (handler (assoc request :opentracing-context s)))))))
