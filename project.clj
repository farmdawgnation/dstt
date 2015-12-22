(defproject me.frmr.tools/dstt "0.3.0"
  :description "Damn Simple Test Tool, a simple and rudamentary load testing library."
  :url "https://github.com/farmdawgnation/dstt"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [clj-http "2.0.0"]
                 [org.clojure/tools.cli "0.3.3"]
                 [org.clojure/data.json "0.2.6"]
                 [com.climate/claypoole "1.1.0"]]
  :main frmr.dstt
  :aot [frmr.dstt]

  :scm {:url "git@github.com:farmdawgnation/dstt.git"}
  :pom-addition [:developers [:developer
                              [:name "Matt Farmer"]
                              [:url "http://farmdawgnation.com"]
                              [:email "matt@frmr.me"]
                              [:timezone "-5"]]])
