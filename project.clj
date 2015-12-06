(defproject me.frmr.tools/dstt "0.2.0-SNAPSHOT"
  :description "Damn Simple Test Tool, a simple and rudamentary load testing library."
  :url "https://github.com/farmdawgnation/dstt"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [clj-http "2.0.0"]
                 [org.clojure/tools.cli "0.3.3"]
                 [org.clojure/data.json "0.2.6"]]
  :main frmr.dstt
  :aot [frmr.dstt]
  :deploy-repositories [["releases" {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
                                     :creds :gpg}
                         "snapshots" {:url "https://oss.sonatype.org/content/repositories/snapshots/"
                                      :creds :gpg}]]

  ; Required by Maven Central for publishing.
  :scm {:url "git@github.com:farmdawgnation/dstt.git"}
  :pom-addition [:developers [:developer
                              [:name "Matt Farmer"]
                              [:url "http://farmdawgnation.com"]
                              [:email "matt@frmr.me"]
                              [:timezone "-5"]]])
