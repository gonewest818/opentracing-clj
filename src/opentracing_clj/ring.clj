(ns opentracing.ring
  (:require [opentracing.core :as ot]))


(defn wrap-opentracing
  "ring middleware creates a span from the inbound request
  with the given name and optional tags, then leaves the tracing
  context in the request dictionary as :opentracing-context"
  ([handler op-name]
   (wrap-opentracing handler op-name nil))
  ([handler op-name tags]
   (fn [request]
     ;;(log/debug (:headers request))
     (let [ctx (ot/http->context (:headers request))]
       (apm/with-trace [s (apm/child-span ctx op-name tags)]
         (handler (assoc request :opentracing-context s)))))))
