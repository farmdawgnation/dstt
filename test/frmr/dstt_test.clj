(ns frmr.dstt-test
  (:require [clojure.test :refer :all]
            [frmr.dstt]
            [clj-http.client :as client]))

(def average #'frmr.dstt/average)
(def issue-timed-request #'frmr.dstt/issue-timed-request)
(def parse-header #'frmr.dstt/parse-header)
(def select-function-for-http-method #'frmr.dstt/select-function-for-http-method)
(def parse-body #'frmr.dstt/parse-body)
(def load-test-url #'frmr.dstt/load-test-url)
(def basic-handler #'frmr.dstt/basic-handler)
(def standard-deviation #'frmr.dstt/standard-deviation)

(deftest average-averages-correctly
  (is (= 2 (average [2 2 2])))
  (is (= 15 (average [5 10 30]))))

(deftest standard-deviation-returns-zero-for-less-than-two-items
  (is (= 0 (standard-deviation nil)))
  (is (= 0 (standard-deviation [5]))))

(deftest issue-timed-request-passes-body-to-handler
  (let [last-handler-string (atom "")
        test-request-invoker (fn [] {:status 200 :body "Bacon123"})
        test-handler (fn [_ body] (reset! last-handler-string body))
        future (issue-timed-request test-request-invoker 0 test-handler)
        _ @future]
    (is (= @last-handler-string "Bacon123") "Handler receives body content.")))

(deftest load-test-url-returns-expected-keys
  (let [test-request-invoker (fn [] (Thread/sleep 10) {:status 200 :body "Bacon123"})
        load-test-results (load-test-url test-request-invoker
                                         3
                                         1
                                         basic-handler)]
    (is (contains? load-test-results "Averages"))
    (is (contains? load-test-results "Minimums"))
    (is (contains? load-test-results "Maximums"))
    (is (contains? load-test-results "StandardDeviations"))))

(deftest load-test-url-returns-correct-number-of-categories
  (let [test-request-invoker (fn [] (Thread/sleep 10) {:status 200 :body "Bacon123"})
        test-handler (fn [time body] [time 5 5])
        load-test-results (load-test-url test-request-invoker
                                         3
                                         1
                                         test-handler)
        averages (get load-test-results "Averages")
        minimums (get load-test-results "Minimums")
        maximums (get load-test-results "Maximums")
        standard-deviations (get load-test-results "StandardDeviations")]
    (is (= 3 (count averages)))
    (is (= 3 (count minimums)))
    (is (= 3 (count maximums)))
    (is (= 3 (count standard-deviations)))))

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
