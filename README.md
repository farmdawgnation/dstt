# The Damn Simple Test Tool

*"A nieve load tester for smart APIs.*

Imagine a very simple scenario: you want to get a rough idea of how your API performs given a
certain number of requests spread out over a certain amount of time. Enter DSTT, the Damn Simple
Test Tool.

## Introduction

DSTT is a command line utility that will make a certain number of requests to a URL and spread them
out over a certain number of seconds. So, for example, if you wanted to get a feel for how quickly
Google will respond to you if you do 10 requests in 30 seconds, you can do that:

```
$ dstt http://google.com -r 10 -t 30
Running a load test of http://google.com
10 requests spread over 30 seconds.

Average time per category:
["137ms"]
```

However, that's a bit boring. The Apache Benchmarker lets you do that (and probably does a much
better job of it). DSTT's real value add is the ability to attach custom **result handlers** to
collect other timing information too. Result handlers are simply Clojure files that define a
function of the form:

```clojure
(fn [total-time-in-ms response-body]
  ; Produce a vector of relevant times from this request.
  [total-time-in-ms something-else-you-computed ...])
```

Store this in a file and pass the filename in using `--handler` to use that result handler instead
of the boring, default one that just averages request completion times.

## Getting DSTT

To get DSTT, pop over to the [releases page](https://github.com/farmdawgnation/dstt/releases) and
download a copy. Or, if you want to build it yourself you can clone this repository and build it
using [Leiningen](http://leiningen.org).
