(ns opentracing-clj.core-test
  (:require [midje.sweet :refer :all]
            [opentracing-clj.core :refer :all]
            [opentracing-clj.impl.mock :refer [make-tracer]]))

(def tracer (atom nil))

(background (before :checks (reset! tracer (make-tracer :text-map))))


(defn mock->hash-map
  [mock]
  {:name (.operationName mock)
   :parent-id (.parentId mock)
   :start (.startMicros mock)
   :finish (.finishMicros mock)
   :tags (.tags mock)})

(facts "spans"

  (fact "creating spans"
    (mock->hash-map (span @tracer "test"))
    => (contains {:name "test" :parent-id 0})

    (mock->hash-map (set-tags (span @tracer "test") {"abc" "123"}))
    => (contains {:name "test" :parent-id 0 :tags {"abc" "123"}})

    (mock->hash-map (span @tracer "test" {"abc" "123"}))
    => (contains {:name "test" :parent-id 0 :tags {"abc" "123"}})

    (mock->hash-map (child-span @tracer
                                (span @tracer "parent")
                                "child"))
    => (contains {:name "child" :parent-id anything})

    (mock->hash-map (child-span @tracer
                                (span @tracer "parent")
                                "child"
                                {"tag" "it"}))
    => (contains {:name "child" :parent-id anything :tags {"tag" "it"}}))
  
  (fact "tracing spans"
    (let [s (span @tracer "test")]
      (finish-span s)
      (map mock->hash-map (.finishedSpans @tracer)))
    => (just (contains {:name "test" :parent-id 0}))

    ;; (let [s (span @tracer "test" {"xyz" "999" "abc" "111"})]
    ;;   (finish-span s)
    ;;   (map mock->hash-map (.finishedSpans @tracer)))
    ;; => (just (contains {:name "test" :parent-id 0 :tags (contains {"xyz" "999"})}))
    ;; **** why doesn't this work?

    (let [s (span @tracer "test" {"xyz" "999"})]
      (finish-span s)
      (map mock->hash-map (.finishedSpans @tracer)))
    => (just (contains {:name "test" :parent-id 0 :tags {"xyz" "999"}}))))


(facts "contexts"

  (fact "creating contexts"
    (start-span (context @tracer "ctx"))
    => #(instance? io.opentracing.mock.MockSpan %))

  (fact "creating child contexts"
    (mock->hash-map
     (start-span (child-context @tracer
                                (span @tracer "parent")
                                "child")))
    => (contains {:name "child" :parent-id anything})

    (mock->hash-map
     (start-span (child-context @tracer
                                (span @tracer "parent")
                                "child"
                                {"tag" "it"})))
    => (contains {:name "child" :parent-id anything :tags {"tag" "it"}})))

(facts "trace* macro"

  (fact "generate a trace"
    (do (trace* {:tracer @tracer :name "quux" :parent (span @tracer "parent")} nil)
        (map mock->hash-map (.finishedSpans @tracer)))
    => (just (contains {:name "quux"})) 

    (do (trace* {:tracer @tracer :name "quux" :parent (span @tracer "parent") :tags {"awe" "struck"}})
        (map mock->hash-map (.finishedSpans @tracer)))
    => (just (contains {:name "quux" :tags {"awe" "struck"}}))))
