; When used with the --csv option this handler will result in a CSV where the total request time
; for each individual request is associated with the time that request finished. This could be
; useful for diagnosing intermittent request latency issues.
(fn [total-time-in-ms response]
  [(str (java.util.Date.)) total-time-in-ms])
