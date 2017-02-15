(ns opentracing-clj.core
  (:import [io.opentracing.propagation Format$Builtin
                                       TextMapExtractAdapter
                                       TextMapInjectAdapter]))


(defn context->http
  "serialize a span context into http headers suitable for inter-process calls"
  [tracer ctx]
  (let [hm (java.util.HashMap.)
        tm (TextMapInjectAdapter. hm)]
    (.inject tracer ctx Format$Builtin/TEXT_MAP tm)
    (into {} hm)))

(defn span->http
  "serialize a span into http headers suitable for inter-process calls"
  [tracer span]
  (let [ctx (.context span)]
    (context->http tracer ctx)))

(defn http->context
  "de-serialize http headers into a span context"
  [tracer http-header]
  (let [hm (java.util.HashMap. http-header)
        tm (TextMapExtractAdapter. hm)]
    (.extract tracer Format$Builtin/TEXT_MAP tm)))

(defn add-tags
  "add tags to a span context"
  [span-builder tags]
  (when-let [td tags]
    (doseq [[k v] td]
      (.withTag span-builder k v)))
  span-builder)

(defn set-tags
  "set tags on a span"
  [span tags]
  (when-let [td tags]
    (doseq [[k v] td]
      (.setTag span k v))))

(defn context
  "create a new span context with the given name and optional tags"
  ([tracer op-name]
   (context tracer op-name nil))
  ([tracer op-name tags]
   (-> tracer
       (.buildSpan op-name)
       (add-tags tags))))

(defn span
  "create a new span with the given name and optional tags"
  ([tracer op-name]
   (span tracer op-name nil))
  ([tracer op-name tags]
   (.start (context tracer op-name tags))))

(defn child-context
  "create a new span context as a child of the given span, with name and optional tags"
  ([tracer spn op-name]
   (child-context tracer spn op-name nil))
  ([tracer spn op-name tags]
   (-> tracer
       (.buildSpan op-name)
       (.asChildOf spn)
       (add-tags tags))))

(defn child-span
  "create a new span as a child of the given span, with name and optional tags"
  ([tracer spn op-name]
   (child-span tracer spn op-name nil))
  ([tracer spn op-name tags]
   (.start (child-context tracer spn op-name tags))))

(defn start-span
  "start span from span context"
  [ctx]
  (.start ctx))

(defn finish-span
  "finish span so trace can be sent to server"
  [spn]
  (.finish spn))

(defmacro trace*
  "trace execution of the body"
  [{:keys [tracer parent name tags]} & body]
  `(let [span# (#'opentracing-clj.core/child-span ~tracer ~parent ~name ~tags)]
     (try
       (do ~@body)
       (finally (#'opentracing-clj.core/finish-span span#)))))

(defmacro with-trace
  "trace execution of body using the span declared in the body"
  [binding & body]
  `(let ~binding
     (try
       (do ~@body)
       (finally (#'opentracing-clj.core/finish-span ~(binding 0))))))
