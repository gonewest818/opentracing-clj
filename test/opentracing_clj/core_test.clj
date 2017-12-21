(ns opentracing-clj.core-test
  (:require [clojure.test :refer :all]
            [opentracing-clj.core :refer :all]
            [opentracing-clj.impl.mock :refer [make-tracer]]))


(def tracer (atom (make-tracer :text-map)))

(use-fixtures :each
  (fn [t]
    (.reset @tracer)
    (t)))

(defn mock->hash-map
  "convert mock span into a hashmap for simpler comparisons"
  [mock]
  {:name (.operationName mock)
   :parent-id (.parentId mock)
   :start (.startMicros mock)
   :finish (.finishMicros mock)
   :tags (into {} (.tags mock))
   :log-entries (map #(.fields %) (.logEntries mock))
   :baggage (into {} (-> mock .context .baggageItems))})

(deftest interop-create-with-no-tags
  (let [_ (with-open [s (-> @tracer
                            (.buildSpan "foo")
                            (add-tags nil)
                            (.startActive true))])
        spans (map mock->hash-map (.finishedSpans @tracer))]
    (is (= 1 (count spans)))
    (is (= {} (:tags (first spans))))))

(deftest interop-create-with-one-tag
  (let [_ (with-open [s (-> @tracer
                            (.buildSpan "foo")
                            (add-tags {"a" "1"})
                            (.startActive true))])
        spans (map mock->hash-map (.finishedSpans @tracer))]
    (is (= 1 (count spans)))
    (is (= {"a" "1"} (:tags (first spans))))))

(deftest interop-create-with-three-tags
  (let [_ (with-open [s (-> @tracer
                            (.buildSpan "foo")
                            (add-tags {"a" "1" "b" "2" "c" "3"})
                            (.startActive true))])
        spans (map mock->hash-map (.finishedSpans @tracer))]
    (is (= 1 (count spans)))
    (is (= 3 (count (:tags (first spans)))))))

(deftest scope-create
  (let [sc (scope @tracer "foo" nil)
        sp (.span sc)]
    (is (= io.opentracing.mock.MockSpan (type sp)))
    (is (= "foo" (.operationName sp)))))

(deftest scope-create-with-one-tag
  (let [sc (scope @tracer "foo" {"a" "1"})
        sp (.span sc)]
    (is (= io.opentracing.mock.MockSpan (type sp)))
    (is (= {"a" "1"} (.tags sp)))
    (is (= "foo" (.operationName sp)))))

(deftest scope-create-finishspan
  (let [sc (scope @tracer "foo" nil false)
        sp (.span sc)]
    (is (= io.opentracing.mock.MockSpan (type sp)))
    (is (= "foo" (.operationName sp)))))

(deftest scope-create-finishspan-with-one-tag
  (let [sc (scope @tracer "foo" {"a" "1"} false)
        sp (.span sc)]
    (is (= io.opentracing.mock.MockSpan (type sp)))
    (is (= {"a" "1"} (.tags sp)))
    (is (= "foo" (.operationName sp)))))

(deftest log-event-to-scope
  (let [_ (with-open [s (scope @tracer "foo")]
            (log-string s "and then this happened"))
        spans (map mock->hash-map (.finishedSpans @tracer))]
    (is (= 1 (count spans)))
    (is (= 1 (count (:log-entries (first spans)))))
    (is (= {"event" "and then this happened"}
           (-> spans first :log-entries first)))))

(deftest log-event-with-ts-to-scope
  (let [_ (with-open [s (scope @tracer "foo")]
            (log-string s "and then this happened" 100))
        spans (map mock->hash-map (.finishedSpans @tracer))]
    (is (= 1 (count spans)))
    (is (= 1 (count (:log-entries (first spans)))))
    (is (= {"event" "and then this happened"}
           (-> spans first :log-entries first)))))

(deftest log-kv-to-scope
  (let [_ (with-open [s (scope @tracer "bar")]
            (log-kv s {"foo" 1 "bar" 2 "baz" false}))
        spans (map mock->hash-map (.finishedSpans @tracer))]
    (is (= 1 (count spans)))
    (is (= 1 (count (:log-entries (first spans)))))
    (is (= {"foo" 1 "bar" 2 "baz" false}
           (-> spans first :log-entries first)))))

(deftest log-kvs-to-scope
  (let [_ (with-open [s (scope @tracer "bar")]
            (log-kv s {"foo" 1 "bar" 2})
            (log-kv s {"baz" false}))
        spans (map mock->hash-map (.finishedSpans @tracer))]
    (is (= 1 (count spans)))
    (is (= 2 (count (:log-entries (first spans)))))
    (is (= {"foo" 1 "bar" 2}
           (-> spans first :log-entries first)))
    (is (= {"baz" false}
           (-> spans first :log-entries second)))))

(deftest log-kvs-with-ts-to-scope
  (let [_ (with-open [s (scope @tracer "bar")]
            (log-kv s {"foo" 1 "bar" 2} 200)
            (log-kv s {"baz" false} 300))
        spans (map mock->hash-map (.finishedSpans @tracer))]
    (is (= 1 (count spans)))
    (is (= 2 (count (:log-entries (first spans)))))
    (is (= {"foo" 1 "bar" 2}
           (-> spans first :log-entries first)))
    (is (= {"baz" false}
           (-> spans first :log-entries second)))))

(deftest baggage-item
  (let [_ (with-open [s (scope @tracer "bar")]
            (add-baggage-item s "bag" "value"))
        spans (map mock->hash-map (.finishedSpans @tracer))]
    (is (= 1 (count spans)))
    (is (= {"bag" "value"} (-> spans first :baggage)))))
