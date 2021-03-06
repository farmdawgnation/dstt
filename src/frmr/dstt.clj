(ns frmr.dstt
  (:import [java.io PushbackReader])
  (:require [clj-http.client :as client]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.java.io :as io]
            [clojure.string :refer [join split]]
            [clojure.data.json :as json]
            [com.climate.claypoole :as cp])
  (:gen-class))

;;; Helper functions.
(defn- basic-handler
  "This is the default handler function that will just return a single category: the time taken to
  run the entire request in ms. You should probably provide your own handler function, but if this
  is all you care about - you get this functionality out of the box."
  [total-time-in-ms content]
  [total-time-in-ms])

(defn- average
  "Average a collection of numbers."
  [numbers]
  (int (/ (apply + numbers) (count numbers))))

(defn standard-deviation
  "Determine the standard deviation of a collection of numbers. Shamelessly adapted from:
  https://github.com/clojure-cookbook/clojure-cookbook/blob/master/01_primitive-data/1-20_simple-statistics.asciidoc#standard-deviation"
  [coll]
  (if (< (count coll) 2)
    0
    (let [avg (average coll)
          squares (for [x coll]
                    (let [x-avg (- x avg)]
                      (* x-avg x-avg)))
          total (count coll)]
      (-> (/ (apply + squares)
             (- total 1))
          (Math/sqrt)))))

(defn- apply-to-numbers
  "Apply fn to maybe-numbers if the first entry in maybe-numbers is a number."
  [maybe-numbers fn]
  (if (-> maybe-numbers first number?)
    (fn maybe-numbers)))

;;; Load testing implementation.
(defn- issue-timed-request
  "Issue a timed request by invoking request-invoker within a future after sleeping the future for a
  specified amount of time. Then pass the elapsed time and the content of the request through the
  `handler` to get a vector of timing results - where each member of the vector represents one
  category timing."
  [test-start-time-ns request-invoker pause-ms handler pool]
  (cp/future
    pool
    (let [current-time (System/nanoTime)
          start-time-difference-ms (int (/ (- current-time test-start-time-ns) 1000000))
          actual-pause-ms (max 0 (- pause-ms start-time-difference-ms))]
      (Thread/sleep actual-pause-ms))
    (let [start (System/nanoTime)
          response (request-invoker)
          stop (System/nanoTime)
          elapsed-time-in-ms (int (/ (- stop start) 1000000))]
      (handler elapsed-time-in-ms response))))

(defn- load-test-url
  "Execute number-of-requests requests using the request-invoker with a pause-between-requests pause between
  them. This execution happens in futures so that if we're simulating a decent amount of load that
  load should be able to happen in parallel.

  The handler provided should consume two arguments: the total time the request took and the content
  of the response. Using these two pieces of information it should generate a vector, where each
  member represents one timing category from the request. So, if you're querying a JSON API that
  surfaces a bit of timing information in the response, you might query that JSON and generate a
  vector from the information you find there in your handler."
  [request-invoker number-of-requests pause-between-requests handler]
  (cp/with-shutdown! [network-pool (cp/threadpool (min 500 number-of-requests))
                      cpu-pool (cp/threadpool (cp/ncpus))]
    (let [request-indicies (range number-of-requests)
          request-delays (map #(* % pause-between-requests) request-indicies)
          test-start-time-ns (System/nanoTime)
          request-futures (map #(issue-timed-request test-start-time-ns request-invoker % handler network-pool) request-delays)
          results (doall (cp/pmap cpu-pool deref (doall request-futures)))
          grouped-result-categories (partition (count results) (apply interleave results))
          average-per-category (cp/pmap cpu-pool #(apply-to-numbers % average) grouped-result-categories)
          min-per-category (cp/pmap cpu-pool #(apply-to-numbers % (fn [x] (apply min x))) grouped-result-categories)
          max-per-category (cp/pmap cpu-pool #(apply-to-numbers % (fn [x] (apply max x))) grouped-result-categories)
          stddev-per-category (cp/pmap cpu-pool #(apply-to-numbers % standard-deviation) grouped-result-categories)]
      {"Averages" (vec average-per-category)
       "Minimums" (vec min-per-category)
       "Maximums" (vec max-per-category)
       "StandardDeviations" (vec stddev-per-category)
       "RawResults" (vec results)})))

(defn- select-function-for-http-method
 [http-method]
 (cond
   (= "DELETE" http-method)
   client/delete

   (= "PUT" http-method)
   client/put

   (= "HEAD" http-method)
   client/head

   (= "POST" http-method)
   client/post

   :else
   client/get))

(defn- build-default-request-invoker
  "Builds a request invoker function given the name of the HTTP function you want to invoke, the url
  you want to execute requests against, and a map of options you want to provide to the underlying
  HTTP library."
  [http-method url request-options]
  (let [client-request-method (select-function-for-http-method http-method)]
    (fn [] (client-request-method url request-options))))

;;; API Functions

(defn run-load-test
  "Execute a load test against a specified URL by issuing the number-of-requests spread over the
  number-of-milliseconds using the http-method, request-options, and result-handler specified."
  [url http-method number-of-requests number-of-milliseconds request-options result-handler custom-request-invoker]
  (let [request-invoker (or custom-request-invoker
                            (build-default-request-invoker http-method url request-options))
        pause-between-requests (/ number-of-requests number-of-milliseconds)]
    (load-test-url request-invoker number-of-requests pause-between-requests result-handler)))

;;; CLI Functions
(defn- parse-header
  "Parse a header string from the command line into a map for the http library."
  [header]
  (let [[header-name header-value] (split header #": ")]
    {header-name header-value}))

(defn- parse-body
  "Parse a body string from the command line into a map for the http library."
  [body]
  {:body body})

(def cli-options
  [["-r" "--requests REQUESTS" "Number of requests"
    :id :requests
    :parse-fn #(Integer/parseInt %)
    :missing "Number of requests (-r) is required."]

   ["-t" "--time TIME" "Time in seconds"
    :id :time
    :parse-fn #(Integer/parseInt %)
    :missing "Time in seconds (-t) is required."]

   [nil "--method METHOD" "HTTP method."
    :id :method
    :default "GET"
    :validate-fn #(some #{%} ["GET" "POST"])]

   [nil "--body BODY" "HTTP post body."
    :id :body]

   [nil "--header HEADER" "HTTP header to set, multiple possible."
    :id :headers
    :default {}
    :parse-fn parse-header
    :assoc-fn (fn [map key value]
                (update-in map
                           [key]
                           #(conj % value)))]

   [nil "--handler HANDLER" "Custom handler file."
    :id :handler
    :default nil]

   [nil "--invoker-builder INVOKERFILE" "Pass in a Clojure file that constructs your request invoker function."
    :id :invoker-builder
    :default nil]

   ["-h" "--help"]

   [nil "--csv FILE" "Spit out a CSV file with raw timings."
    :id :csv]

   ["-v" "--verbose"]])

(defn -main
  "Main entry point.

  Takes in the URL that you want to test against, the number of seconds that you want to run that
  test, the number of requests to make over that time frame, and the handler to produce a vector
  of results to be averaged."

  [& args]
  (let [parsed-options (parse-opts args cli-options)]
    (cond
      (or (some-> parsed-options :options :help)
          (zero? (count (:arguments parsed-options))))
      (do (println "DSTT – The Damn Simple Test Tool\n(c)2015 Matt Farmer - Licensed under the Apache2 License\n")
          (println "Usage: dstt URL -r #REQUESTS -t SECONDS [other options]\n\nOption summary:")
          (println (:summary parsed-options))
          (System/exit 0))

      (:errors parsed-options)
      (do (mapv #(println (str "ERROR " %)) (:errors parsed-options))
          (System/exit 1))

      :else
      (let [{requests :requests
             seconds :time
             handler :handler
             invoker-builder :invoker-builder
             http-method :method
             http-headers :headers
             http-body :body
             verbose :verbose
             csv :csv} (:options parsed-options)
            [url] (:arguments parsed-options)
            milliseconds (* seconds 1000)
            pause-between-requests (/ milliseconds requests)

            request-options (merge {:headers http-headers}
                                   (some-> http-body parse-body)
                                   {:follow-redirects false
                                    :throw-exceptions false})

            [request-invoker custom-invoker-name] (if-not (nil? invoker-builder)
                                                    [(let [build-custom-request-invoker (eval (read (PushbackReader. (io/reader invoker-builder))))]
                                                       (build-custom-request-invoker http-method url request-options))
                                                     invoker-builder]
                                                    [(build-default-request-invoker http-method url request-options)
                                                     nil])

            [parsed-handler custom-handler-name] (if-not (nil? handler)
                                                   [(eval (read (PushbackReader. (io/reader handler))))
                                                    handler]
                                                   [basic-handler
                                                    nil])]
        (println (str "Running a load test of " url))
        (println (str requests " requests spread over " seconds " seconds."))
        (if verbose (println (str "Request options: " request-options)))
        (if custom-handler-name (println (str "Using custom handler: " custom-handler-name)))
        (if custom-invoker-name (println (str "Using custom request invoker: " custom-invoker-name)))
        (println "")
        (let [load-test-results (load-test-url request-invoker
                                               requests
                                               pause-between-requests
                                               parsed-handler)]
          (if csv
            (with-open [csv-writer (io/writer csv)]
              (.write csv-writer (str "Load test: " url " Requests: " requests " Seconds: " seconds "\n"))
              (let [raw-results (get load-test-results "RawResults")
                    lines (map #(join "," %) raw-results)
                    content (join "\n" lines)]
                (println (str "Writing CSV " csv))
                (.write csv-writer content))))
          (println (str "Average: " (get load-test-results "Averages")))
          (println (str "Minimum: " (get load-test-results "Minimums")))
          (println (str "Maximum: " (get load-test-results "Maximums")))
          (println (str "StdDevi: " (get load-test-results "StandardDeviations")))
        (System/exit 0))))))
