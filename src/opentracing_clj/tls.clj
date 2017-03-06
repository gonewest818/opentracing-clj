(ns opentracing-clj.tls
  (:require [opentracing-clj.core :as core :refer [add-tags set-tags]])
  (:import [io.opentracing.propagation Format$Builtin
                                       TextMapExtractAdapter
                                       TextMapInjectAdapter]
           [clojure.lang IDeref]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Manage thread local context containing tracer and span info

(defn- thread-local*
  "Thread local storage clojure impl, taken from useful:
   https://github.com/flatland/useful/blob/develop/src/flatland/useful/utils.clj"
  [init]
  (let [generator (proxy [ThreadLocal] []
                    (initialValue [] (init)))]
    (reify IDeref
      (deref [this]
        (.get generator)))))

(defmacro thread-local
  [& body]
  `(#'opentracing-clj.tls/thread-local* (fn [] ~@body)))


(def ^{:doc "Thread local state containing tracer and current span."} 
  context (thread-local (atom {:tracer nil :spans nil})))

(defn reset-tracer!
  "Reset the tracer in a thread local context and discard any existing span."
  [tracer]
  (reset! @context {:tracer tracer :spans nil}))

(defn push-span!
  "Push span onto a stack stored in a thread local context."
  [span]
  (swap! @context update-in [:spans] conj span))

(defn pop-span!
  "Pop most recent span from a stack in a thread-local-context."
  []
  ;; http://stackoverflow.com/questions/5727611/threadsafe-pop-in-clojure
  (let [{[head & rest] :spans :as ctx} @@context]
    (if (compare-and-set! @context ctx
                          (assoc-in ctx [:spans] rest))
      head
      (recur))))

(defn peek-span
  "Peek at the top of the stack of spans in this thread."
  []
  (peek (:spans @@context)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; TLS versions of core functions
;;; These are simplified versions of the opentracing-clj.core functions
;;; since we are now tracking state on the caller's behalf

(defn span-context-from-parent
  "Create a new span context with the given name and optional tags,
  and make it a child of the supplied parent, but do not start the
  span. Parent can be a span or a span context. Ordinarily you would
  just call span! instead."
  ([parent op-name]
   (span-context-from-parent parent op-name nil))
  ([parent op-name tags]
   (-> (:tracer @@context)
       (.buildSpan op-name)
       (.asChildOf parent)
       (add-tags tags))))

(defn span-context
  "Create a new span context with the given name and optional tags,
  and make it a child of the existing span if any, but do not start
  the context. Ordinarily you will just call span! instead."
  ([op-name]
   (span-context op-name nil))
  ([op-name tags]
   (if-let [spn (peek-span)]
     ;; make it a child of the existing span
     (span-context-from-parent spn op-name tags)
     ;; or just make a new one
     (-> (:tracer @@context)
         (.buildSpan op-name)
         (add-tags tags)))))

(defn start-span-context!
  "Start the given span context and swap into the current thread
  local context."
  [ctx]
  (push-span! (.start ctx)))

(defn span!
  "Create a new span with the given name and optional tags, and
  make it a child of the existing span if any, and swap! into
  the current thread local context"
  ([op-name]
   (span! op-name nil))
  ([op-name tags]
   (start-span-context! (span-context op-name tags))))

(defn set-tags!
  "Set additional tags on the topmost span in the stack."
  [tags]
  (set-tags (peek-span) tags))

(defn finish-span!
  "Pop the top span from the thread local stack, and call finish on
  the span such that trace data will be logged to the APM server.
  
  If a hash-map of tags are provided, set those on the span before
  finishing. (These additional tags could be metadata about the trace
  that is only known after the traced code was executed.)"
  ([]
   (.finish (pop-span!)))
  ([tags]
   (let [spn (pop-span!)]
     (set-tags spn tags)
     (.finish spn))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; propagation

(defn to-http
  "Serialize the thread local context into http headers suitable
  for inter-process calls. If there is no current span then return
  an empty hash-map."
  []
  (if-let [spn (peek-span)]
    ;; if there is a current span, serialize it
    (let [hm (java.util.HashMap.)
          tm (TextMapInjectAdapter. hm)
          tracer (:tracer @@context)
          ctx (.context spn)]
      (.inject tracer ctx Format$Builtin/HTTP_HEADERS tm)
      (into {} hm))
    ;; else return an empty hash-map
    {}))

(defn span-from-http!
  "De-serialize http headers into the thread local span context"
  ([http-header op-name]
   (span-from-http! http-header op-name nil))
  ([http-header op-name tags]
   (let [hm (java.util.HashMap. http-header)
         tm (TextMapExtractAdapter. hm)
         tracer (:tracer @@context)]
     (if-let [ctx (.extract tracer Format$Builtin/HTTP_HEADERS tm)]
       (push-span! (.start (span-context-from-parent ctx op-name tags)))
       (span! op-name tags)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; TLS versions of trace macro

(defmacro trace
  "Trace execution of body"
  [{:keys [op-name tags]} & body]
  `(try
     (#'opentracing-clj.tls/span! ~op-name ~tags)
     (do ~@body)
     (finally (#'opentracing-clj.tls/finish-span!))))

