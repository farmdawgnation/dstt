(ns frmr.proper-tester
  (:import [java.io PushbackReader])
  (:require [clj-http.client :as client]
            [clojure.java.io :as io]))

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

(defn- issue-timed-request
  "Issue a timed request to a url within a future after sleeping the future for a specified amount
  of time. Then pass the elapsed time and the content of the request through the `handler` to get
  a vector of timing results - where each member of the vector represents one category timing."
  [url pause handler]
  (future (let [slept (Thread/sleep pause)
                start (System/nanoTime)
                content (client/get url)
                stop (System/nanoTime)
                elapsed-time-in-ms (int (/ (- stop start) 1000000))]
            (handler elapsed-time-in-ms content))))

(defn- load-test-url
  "Execute number-of-requests requests against the url with a pause-between-requests pause between
  them. This execution happens in futures so that if we're simulating a decent amount of load that
  load should be able to happen in parallel.
  
  The handler provided should consume two arguments: the total time the request took and the content
  of the response. Using these two pieces of information it should generate a vector, where each
  member represents one timing category from the request. So, if you're querying a JSON API that
  surfaces a bit of timing information in the response, you might query that JSON and generate a
  vector from the information you find there in your handler."
  [url number-of-requests pause-between-requests handler]
  (let [request-indicies (range number-of-requests)
        request-delays (map #(* % pause-between-requests) request-indicies)
        request-futures (map #(issue-timed-request url % handler) request-delays)
        result-timings (map #(deref %) request-futures)
        grouped-result-categories (partition (count result-timings) (apply interleave result-timings))
        average-timing-per-category (map #(-> % (average) (str "ms")) grouped-result-categories)]
    (println (str "Average time per category:\n" (vec average-timing-per-category)))))

(defn -main
  "Main entry point.
  
  Takes in the URL that you want to test against, the number of seconds that you want to run that
  test, the number of requests to make over that time frame, and the handler to produce a vector
  of results to be averaged."
  ([url seconds requests] (-main url seconds requests basic-handler))

  ([url seconds requests handler]
   (let [milliseconds (* (Integer. seconds) 1000)
         pause-between-requests (/ milliseconds (Integer. requests))
         [parsed-handler custom-handler-name] (if (string? handler)
                                                [(eval (read (PushbackReader. (io/reader handler))))
                                                 handler]
                                                [handler
                                                 nil])]
     (println (str "Running a load test of " url))
     (println (str requests " requests spread over " seconds " seconds."))
     (if custom-handler-name (println (str "Using custom handler: " custom-handler-name)))
     (println "")
     (load-test-url url (Integer. requests) pause-between-requests parsed-handler)
     (System/exit 0))))
