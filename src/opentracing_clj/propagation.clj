(ns opentracing-clj.propagation
  (:require [opentracing-clj.core :as ot])
  (:import (io.opentracing.propagation Format$Builtin
                                       TextMapExtractAdapter
                                       TextMapInjectAdapter)))


(def formats {:http   Format$Builtin/HTTP_HEADERS
              :text   Format$Builtin/TEXT_MAP
            ; :binary Format$Builtin/BINARY ; unsupported for now
              })

(defn inject
  "Serialize a tracing context and inject into a hashmap for inter-process calls,
  where 'fmt' is one of :http or :text, corresponding to the HTTP_HEADERS and
  TEXT_MAP builtins in the opentracing-java implementation. Does not support a
  BINARY carrier. Returns a Clojure hash-map containing the injected data."
  [tracer ctx fmt]
  (let [hm (java.util.HashMap.)
        tm (TextMapInjectAdapter. hm)]
    (.inject tracer ctx (get formats fmt) tm)
    (into {} hm)))

(defn extract
  "De-serialize a hashmap obtained from the header of an inter-process
  call, where the header format 'fmt' is one of :http or :text, corresponding
  to the HTTP_HEADERS and TEXT_MAP builtins in the opentracing-java
  implementation. Does not support the BINARY carrier.
  Returns the span context."
  [tracer header fmt]
  (let [hm (java.util.HashMap. header)
        tm (TextMapExtractAdapter. hm)]
    (.extract tracer (get formats fmt) tm)))

(defn ring-wrapper
  "This ring middleware creates a span from the inbound request with
  the given name and optional tags. If the tracer is able to extract a
  valid context from the inbound request header, that context then
  becomes the parent of the newly created span, else there is no
  parent.  The enclosing scope is assoc'ed into the request hash-map
  with key :opentracing-scope for downstream handlers to
  retrieve. This is in addition to the behavior of the ScopeManager
  associated with this tracer."
  ([handler tracer op-name]
   (ring-wrapper handler tracer op-name nil))
  ([handler tracer op-name tags]
   (fn [request]
     ;;(log/debug (:headers request))
     (if-let [ctx (extract tracer (:headers request) :http)]
       (with-open [s (-> tracer
                         (.buildSpan op-name)
                         (.asChildOf ctx)
                         (ot/add-tags tags)
                         (.startActive))]
         (handler (assoc request :opentracing-scope s)))
       (with-open [s (ot/scope tracer op-name tags)]
         (handler (assoc request :opentracing-scope s)))))))
