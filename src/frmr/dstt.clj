(ns frmr.dstt
  (:import [java.io PushbackReader])
  (:require [clj-http.client :as client]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.java.io :as io]
            [clojure.string :refer [split]])
  (:gen-class))

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
  "Issue a timed request by invoking request-invoker within a future after sleeping the future for a
  specified amount of time. Then pass the elapsed time and the content of the request through the
  `handler` to get a vector of timing results - where each member of the vector represents one
  category timing."
  [request-invoker pause handler]
  (future (let [slept (Thread/sleep pause)
                start (System/nanoTime)
                content (request-invoker)
                stop (System/nanoTime)
                elapsed-time-in-ms (int (/ (- stop start) 1000000))]
            (handler elapsed-time-in-ms content))))

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
  (let [request-indicies (range number-of-requests)
        request-delays (map #(* % pause-between-requests) request-indicies)
        request-futures (map #(issue-timed-request request-invoker % handler) request-delays)
        result-timings (map #(deref %) request-futures)
        grouped-result-categories (partition (count result-timings) (apply interleave result-timings))
        average-timing-per-category (map #(-> % (average) (str "ms")) grouped-result-categories)]
    (println (str "Average time per category:\n" (vec average-timing-per-category)))))

(defn- parse-header
  "Parse a header string from the command line into a map for the http library."
  [header]
  (let [[header-name header-value] (split header #": ")]
    {header-name header-value}))

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

   [nil "--header HEADER" "HTTP header to set"
    :id :headers
    :default {}
    :parse-fn parse-header
    :assoc-fn (fn [map key value] (update-in map [key] #(conj % value)))]

   [nil "--handler HANDLER" "Custom handler file."
    :id :handler
    :default nil]

   ["-h" "--help"]

   ["-v" "--verbose"]])

(defn- select-function-for-http-method
  [http-method]
  (cond
    (= "POST" http-method)
    client/post

    :else
    client/get))

(defn- parse-body
  "Parse a body string from the command line into a map for the http library."
  [body]
  {:body body})

(defn -main
  "Main entry point.
  
  Takes in the URL that you want to test against, the number of seconds that you want to run that
  test, the number of requests to make over that time frame, and the handler to produce a vector
  of results to be averaged."

  [& args]
  (let [parsed-options (parse-opts args cli-options)]
    (cond
      (some-> parsed-options :options :help)
      (do (println (:summary parsed-options))
          (System/exit 0))

      (:errors parsed-options)
      (do (mapv #(println (str "ERROR " %)) (:errors parsed-options))
          (System/exit 1))

      :else
      (let [{requests :requests
             seconds :time
             handler :handler
             http-method :method
             http-headers :headers
             http-body :body
             verbose :verbose} (:options parsed-options)
            [url] (:arguments parsed-options)
            milliseconds (* seconds 1000)
            pause-between-requests (/ milliseconds requests)

            client-request-method (select-function-for-http-method http-method)
            request-options (merge {:headers http-headers}
                                   (some-> http-body parse-body)
                                   {:follow-redirects false})
            request-invoker (fn [] (client-request-method url request-options))

            [parsed-handler custom-handler-name] (if-not (nil? handler)
                                                   [(eval (read (PushbackReader. (io/reader handler))))
                                                    handler]
                                                   [basic-handler
                                                    nil])]
        (println (str "Running a load test of " url))
        (println (str requests " requests spread over " seconds " seconds."))
        (if verbose (println (str "Request options: " request-options)))
        (if custom-handler-name (println (str "Using custom handler: " custom-handler-name)))
        (println "")
        (load-test-url request-invoker requests pause-between-requests parsed-handler)
        (System/exit 0)))))
