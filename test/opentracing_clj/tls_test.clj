(ns opentracing-clj.tls-test
  (:require [midje.sweet :refer :all]
            [opentracing-clj.tls :refer :all]
            [opentracing-clj.impl.mock :refer [make-tracer]])
  (:import [clojure.lang IDeref]))


(background (before :checks (reset-tracer! (make-tracer :text-map))))

(defn mock->hash-map
  [mock]
  {:name (.operationName mock)
   :parent-id (.parentId mock)
   :start (.startMicros mock)
   :finish (.finishMicros mock)
   :tags (.tags mock)})


(facts "generic thread-local tests"
  
  (fact "make a thread-local atom"
    (thread-local (atom 1)) => #(instance? IDeref %))

  (fact "deref thread-local yields the atom"
    @(thread-local (atom 1)) => #(instance? clojure.lang.Atom %))

  (fact "double deref thread-local yields the value"
    @@(thread-local (atom 1)) => 1))


(facts "tls tracer and span tests"

  (fact "reset-tracer! sets the tracer in context"
    (do (reset-tracer! :z)
        @@context) => (just {:tracer :z :spans nil}))

  (fact @@context => (just {:tracer #(instance? io.opentracing.mock.MockTracer %)
                            :spans nil}))

  (fact "reset-tracer! throws away existing spans"
    (do (push-span! :a)
        (reset-tracer! :z)
        @@context) => (just {:tracer :z :spans nil}))
  
  (fact "push span onto stack"
    (do (push-span! :a) @@context) => (contains {:spans (just :a)}))
  
  (fact "push spans onto stack"
    (do (push-span! :a)
        (push-span! :b)
        @@context) => (contains {:spans (just [:b :a])}))

  (fact "pop span from stack"
    (do (push-span! :a)
        (push-span! :b)
        (pop-span!)) => :b)

  (fact "peek at stack"
    (do (push-span! :a)
        (push-span! :b)
        (peek-span)) => :b))
  

(facts "propagation functions"

  (fact "to-http with no current span"
    (to-http) => {})

  (fact "to-http with a span active"
    (do (span! "test to-http")
        (to-http)) => (just {"traceid" (has every? #(Character/isDigit %))
                             "spanid" (has every? #(Character/isDigit %))}))

  (fact "span-from-http! with empty header"
    (span-from-http! {} "from header")
    => (just {:tracer #(instance? io.opentracing.mock.MockTracer %)
              :spans (just #(instance? io.opentracing.mock.MockSpan %))}))

  (fact "span-from-http! with a serialized trace in the header"
    (span-from-http! {"traceid" "100" "spanid" "999"} "from header")
    ;; results in a tracer and a single span in the list of spans
    => (just {:tracer #(instance? io.opentracing.mock.MockTracer %)
              :spans (just #(instance? io.opentracing.mock.MockSpan %))})))


(facts "working with spans"

  (fact "span-context-from-parent"
    (do (span! "active")
        (type (span-context-from-parent (peek-span) "from parent")))
    => io.opentracing.mock.MockTracer$SpanBuilder)
  
  (fact "span-context-from-parent with tags"
    (do (span! "active")
        (type (span-context-from-parent (peek-span) "from parent" {"tag" "me"})))
    => io.opentracing.mock.MockTracer$SpanBuilder)

  (fact "span-context with no current span"
    (type (span-context "no current span"))
    => io.opentracing.mock.MockTracer$SpanBuilder)

  (fact "span-context with no current span and tags"
    (type (span-context "no current span w/ tags" {"tag" "me"}))
    => io.opentracing.mock.MockTracer$SpanBuilder)

  (fact "span-context with active span"
    (do (span! "active")
        (type (span-context "span active")))
    => io.opentracing.mock.MockTracer$SpanBuilder)
  
  (fact "span-context with active span and tags"
    (do (span! "active")
        (type (span-context "span active" {"tag" "me"})))
    => io.opentracing.mock.MockTracer$SpanBuilder)

  (fact "start-span-context! with no other span active"
    (do (start-span-context! (span-context "start me"))
        @@context)
    => (just {:tracer #(instance? io.opentracing.mock.MockTracer %)
              :spans (just #(instance? io.opentracing.mock.MockSpan %))}))

  (fact "start-span-context! with no other span active"
    (do (span! "active")
        (start-span-context! (span-context "start me"))
        @@context)
    => (just {:tracer #(instance? io.opentracing.mock.MockTracer %)
              :spans (two-of #(instance? io.opentracing.mock.MockSpan %))})))


(facts "full tracing lifecycle"
  
  (fact "single span lifecycle"
    (do
      (span! "foo" {"starting" "number"})
      (let [spn (peek-span)]
        (set-tags! {"bar" "baz"})
        (finish-span! {"final" "value"})
        (mock->hash-map spn)))
    => (contains {:name "foo" :parent-id 0
                  :tags {"starting" "number"
                         "bar" "baz"
                         "final" "value"}}))

  (fact "nested span lifecycle"
    (do
      (span! "pre" {"prior" "span"})
      (span! "foo" {"starting" "number"})
      (let [spn (peek-span)]
        (set-tags! {"bar" "baz"})
        (finish-span!)
        (mock->hash-map spn)))
    => (contains {:name "foo"
                  :parent-id #(> % 0)
                  :tags {"starting" "number"
                         "bar" "baz"}}))

  (fact "single trace macro"
    (mock->hash-map
     (trace
      {:op-name "single"}
      (let [s (peek-span)]
        s)))
    => (contains {:name "single"
                  :parent-id 0}))

  (fact "nested trace macro"
    (mock->hash-map
     (trace
      {:op-name "single"}
      (trace
       {:op-name "nested"}
       (let [s (peek-span)]
         s))))
    => (contains {:name "nested"
                  :parent-id #(> % 0)}))
  
  (fact "single trace macro with tags"
    (mock->hash-map
     (trace
      {:op-name "single" :tags {"one" 1}}
      (let [s (peek-span)]
        s)))
    => (contains {:name "single"
                  :tags {"one" 1}
                  :parent-id 0}))

  (fact "nested trace macro with tags"
    (mock->hash-map
     (trace
      {:op-name "single" :tags {"one" 1}}
      (trace
       {:op-name "nested" :tags {"two" 2}}
       (let [s (peek-span)]
         s))))
    => (contains {:name "nested"
                  :tags {"two" 2}
                  :parent-id #(> % 0)})))
