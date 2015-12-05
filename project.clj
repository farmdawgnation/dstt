(defproject dstt "0.2.0-SNAPSHOT"
  :description "The Damn Simple Test Tool"
  :url "https://github.com/farmdawgnation/dstt"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [clj-http "2.0.0"]
                 [org.clojure/tools.cli "0.3.3"]
                 [org.clojure/data.json "0.2.6"]]
  :main frmr.dstt
  :aot [frmr.dstt])
