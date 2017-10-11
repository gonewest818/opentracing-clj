(ns opentracing-clj.core)


(defn add-tags
  "Add contents of map 'm' as tags to a span context"
  [span-builder m]
  (if (map? m)
    (doseq [[k v] (seq m)]
      (.withTag span-builder k v)))
  span-builder)

(defn scope
  "Convenience wrapper to create a span with optional tags and return the scope"
  ([tracer op-name]
   (scope tracer op-name nil true))
  ([tracer op-name tags]
   (scope tracer op-name tags true))
  ([tracer op-name tags finish-span]
   (-> tracer
       (.buildSpan op-name)
       (add-tags tags)
       (.startActive finish-span))))

(defn log-kv
  "Convenience wrapper to log a hashmap of structured data on the scope's span"
  ([s h]
   (-> s .span (.log (java.util.HashMap. h))))
  ([s h ts]
   (-> s .span (.log ts (java.util.HashMap. h)))))

(defn log-string
  "Convenience wrapper to log a event (string) on the scope's span"
  ([s e]
   (-> s .span (.log e)))
  ([s e ts]
   (-> s .span (.log ts e))))

(defn add-baggage-item
  "Convenience wrapper to add baggage to a scope's span, where baggage is
  a key and a value both strings."
  [s k v]
  (-> s .span (.setBaggageItem k v)))
