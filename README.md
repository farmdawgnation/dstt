# The Damn Simple Test Tool

[![Build Status](https://travis-ci.org/farmdawgnation/dstt.svg?branch=master)](https://travis-ci.org/farmdawgnation/dstt)

*"A naive load tester for smart APIs."*

Imagine a very simple scenario: you want to get a rough idea of how your API performs given a
certain number of requests spread out over a certain amount of time. Enter DSTT, the Damn Simple
Test Tool.

## Getting DSTT

To get the CLI version of DSTT, pop over to the [releases page](https://github.com/farmdawgnation/dstt/releases)
and download a copy.

Or, if you're interested in using DSTT in your project (say, in an automated test) just include a
reference to the dependency in your project.clj:

```clojure
[me.frmr.tools/dstt "0.3.0"]
```

## Introduction

DSTT is a command line utility that will make a certain number of requests to a URL and spread them
out over a certain number of seconds. So, for example, if you wanted to get a feel for how quickly
Google will respond to you if you do 10 requests in 30 seconds, you can do that:

```
$ dstt http://google.com -r 10 -t 30
Running a load test of http://google.com
10 requests spread over 30 seconds.

Average: [111]
Minimum: [91]
Maximum: [255]
StdDevi: [50.81885039584776]
```

The above gives you information about the total time it took to complete 10 GET requests to Google
over 30 seconds.

### Result Handlers and Timing Categories

However, that's a bit boring. The Apache Benchmarker lets you do that (and probably does a much
better job of it). DSTT's real value add is the ability to attach custom **result handlers** to
collect other timing information too.

DSTT is based on the concept of timing categories. Result handlers are Clojure functions that
take in the total time of the request and the response from the HTTP server on the other end
and return a vector of the timing categories that you're interested in. You could pull those numbers
from timing information reported in a JSON response, timing information in headers, etc, etc.

They look somewhat like:

```clojure
(fn [total-time-in-ms response]
  ; Produce a vector of relevant times from this request.
  [total-time-in-ms category-two category-three ...])
```

So, you could, for example:

* Pull timing information from a JSON API response and pair that with the timing information
  for the overall request.
* Determine the length of the entire request body and return that as a category.
* Return arbitrary data - such as a new `java.util.Date` representing the time the request was
  completed.

Store your code in a clojure and pass that to DSTT like so:

```
$ dstt http://google.com -r 10 -t 30 --handler my_handler.clj
```

Result categories can be of any type, but only categories that are numeric will get the nice
average/minimum/maximum/stddev output in the summary.

There are a few example handlers in the [samples folder](https://github.com/farmdawgnation/dstt/tree/master/samples).

### Raw CSV Output

DSTT will also let you spit out a raw CSV that contains the individual timing information for each
request in the test, complete with all of your categories. You can do that using the csv option:

```
$ dstt http://google.com -r 10 -t 30 --csv google.csv
```

## Use DSTT Programatically

If you're interested in using DSTT programatically, you can do that. `frmr.dstt` exposes a
public function named `run-load-test` that can be invoked like so:

```clojure
(run-load-test "http://google.com"
               "GET"
               10
               3000 # Note this takes milliseconds, not seconds
               {}   # Request options like headers, cookies, etc. See clj-http docs.
               result-handler)
```

## About the Author

Matt Farmer is a Senior Software Engineer with [Domino Data Lab](http://dominodatalab.com) changing
how data science is done for the better.
