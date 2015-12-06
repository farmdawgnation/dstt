(ns frmr.dstt-test
  (:require [clojure.test :refer :all]
            [frmr.dstt]
            [clj-http.client :as client]))

(def average #'frmr.dstt/average)
(def issue-timed-request #'frmr.dstt/issue-timed-request)
(def parse-header #'frmr.dstt/parse-header)
(def select-function-for-http-method #'frmr.dstt/select-function-for-http-method)
(def parse-body #'frmr.dstt/parse-body)

(deftest average-averages-correctly
  (is (= 2 (average [2 2 2])))
  (is (= 15 (average [5 10 30]))))

(deftest issue-timed-request-passes-body-to-handler
  (let [last-handler-string (atom "")
        test-request-invoker (fn [] {:status 200 :body "Bacon123"})
        test-handler (fn [_ body] (reset! last-handler-string body))
        future (issue-timed-request test-request-invoker 0 test-handler)
        _ @future]
    (is (= @last-handler-string "Bacon123") "Handler receives body content.")))

(deftest parse-header-parses-http-headers
  (let [expected-header {"User-Agent" "Barney Stinson"}
        input "User-Agent: Barney Stinson"]
    (is (= expected-header (parse-header input)))))

(deftest select-function-for-http-method-makes-correct-selections
  (is (= client/get (select-function-for-http-method "GET")))
  (is (= client/post (select-function-for-http-method "POST")))
  (is (= client/get (select-function-for-http-method "PATCH")))
  (is (= client/get (select-function-for-http-method "wroifjwofij"))))

(deftest parse-body-returns-map
  (is (= {:body "bacon"} (parse-body "bacon"))))
