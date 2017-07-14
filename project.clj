(defproject contentserver "0.1.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [http-kit "2.1.18"]
                 [org.clojure/tools.logging "0.2.6"] ;; loggging
                 [propertea "1.2.3"]              ;; reading from .properties
                 [org.clojure/java.jdbc "0.3.6"]  ;; jdbc
                 [org.postgresql/postgresql "9.3-1101-jdbc41"] ; postgres driver
                 [korma "0.4.0"]                            ;; db library
                 [org.clojure/data.json "0.2.5"]
                 [log4j "1.2.17"]
                 [compojure "1.1.8"]                        ; routing
                 [ring/ring-codec "1.0.0"]
                 [ring/ring-mock "0.3.0"]
                 [ring "1.3.0"]
                 [cheshire "5.4.0"]
                 [clj-time "0.6.0"]
                 [midje "1.6.3"]
                 [org.slf4j/slf4j-log4j12 "1.6.4"]
                 [ring/ring-defaults "0.1.2"]
                 [com.novemberain/langohr "3.0.1"]
                 [selmer "0.8.0"]
                 [pg-hstore "3.0.3"]
                 [environ "1.0.0"]
                 [markdown-clj "0.9.64"]
                 [lib-noir "0.9.4"]]

  :main ^:skip-aot contentserver.core
  :target-path "target/%s"
  :plugins [[lein-midje "3.1.3"]
            [lein-ring "0.8.13"]]
  :profiles {:uberjar {:dependencies [[javax.servlet/servlet-api "2.5"]
                                      [ring-mock "0.1.5"]]
                       :aot :all}}
  :omit-source true)
