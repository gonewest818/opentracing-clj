(ns opentracing-clj.ring-test
  (:require [midje.sweet :refer :all]
            [opentracing-clj.ring :refer :all]
            [opentracing-clj.core :refer :all]
            [opentracing-clj.impl.mock :refer [make-tracer]]))


(def tracer (atom nil))

(background (before :contents (reset! tracer (make-tracer :text-map))))

(defn handler
  "this mock handler returns the span's traceid"
  [request]
  (let [span (:opentracing-context request)]
    (-> (span->http @tracer span)
        (get "traceid"))))

(fact "wrap-opentracing extract traceid from request header"
  (let [f (wrap-opentracing handler @tracer "foo")]
    (f {:headers {"spanid" "123", "traceid" "456"}})) => "456"
  (let [f (wrap-opentracing handler @tracer "foo" {"tag" "test"})]
    (f {:headers {"spanid" "123", "traceid" "789"}})) => "789")

(fact "wrap-opentracing with no traceid in request header"
  (let [f (wrap-opentracing handler @tracer "foo")]
    (f {:headers {}})) => (has every? #(Character/isDigit %)))

