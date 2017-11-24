(defproject tracer "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories [["bintray" "https://dl.bintray.com/crate/crate"]]    ; Repo for Crate JDBC driver
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.3.442"]
                 [org.clojure/tools.logging "0.3.1"]                  ; logging framework
                 [clojure-watch "0.1.11"]
                 [mvxcvi/puget "1.0.1"]
                 [clj-http "3.7.0"]
                 [cheshire "5.8.0"]
                 [potemkin "0.4.4"]
                
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.3.442"]
                 [org.clojure/core.match "0.3.0-alpha4"]              ; optimized pattern matching library for Clojure
                 [org.clojure/core.memoize "0.5.9"]                   ; needed by core.match; has useful FIFO, LRU, etc. caching mechanisms
                 [org.clojure/data.csv "0.1.3"]                       ; CSV parsing / generation
                 [org.clojure/java.classpath "0.2.3"]                 ; examine the Java classpath from Clojure programs
                 [org.clojure/java.jdbc "0.7.0"]                      ; basic JDBC access from Clojure
                 [org.clojure/math.numeric-tower "0.0.4"]             ; math functions like `ceil`
                 [org.clojure/tools.logging "0.3.1"]                  ; logging framework
                 [org.clojure/tools.namespace "0.2.10"]
                 [amalloy/ring-buffer "1.2.1"
                  :exclusions [org.clojure/clojure
                               org.clojure/clojurescript]]            ; fixed length queue implementation, used in log buffering
                 [amalloy/ring-gzip-middleware "0.1.3"]               ; Ring middleware to GZIP responses if client can handle it
                 [aleph "0.4.3"]                                      ; Async HTTP library; WebSockets
                 [bigml/histogram "4.1.3"]                            ; Streaming one-pass Histogram data structure
                 [buddy/buddy-core "1.2.0"]                           ; various cryptograhpic functions
                 [buddy/buddy-sign "1.5.0"]                           ; JSON Web Tokens; High-Level message signing library
                 [cheshire "5.7.0"]                                   ; fast JSON encoding (used by Ring JSON middleware)
                 [clj-http "3.4.1"                                    ; HTTP client
                  :exclusions [commons-codec
                               commons-io
                               slingshot]]
                 [clj-time "0.13.0"]                                  ; library for dealing with date/time
                 [clojurewerkz/quartzite "2.0.0"]                     ; scheduling library
                 [colorize "0.1.1" :exclusions [org.clojure/clojure]] ; string output with ANSI color codes (for logging)
                 ;[com.amazon.redshift/redshift-jdbc41 "1.2.8.1005"]   ; Redshift JDBC driver
                 [com.cemerick/friend "0.2.3"                         ; auth library
                  :exclusions [commons-codec
                               org.apache.httpcomponents/httpclient
                               net.sourceforge.nekohtml/nekohtml
                               ring/ring-core]]
                 [com.draines/postal "2.0.2"]                         ; SMTP library
                 [com.github.brandtg/stl-java "0.1.1"]                ; STL decomposition
                 [com.google.apis/google-api-services-analytics       ; Google Analytics Java Client Library
                  "v3-rev139-1.22.0"]
                 [com.google.apis/google-api-services-bigquery        ; Google BigQuery Java Client Library
                  "v2-rev342-1.22.0"]
                 [com.jcraft/jsch "0.1.54"]                           ; SSH client for tunnels
                 [com.h2database/h2 "1.4.194"]                        ; embedded SQL database
                 [com.mattbertolini/liquibase-slf4j "2.0.0"]          ; Java Migrations lib
                 [com.mchange/c3p0 "0.9.5.2"]                         ; connection pooling library
                 [com.microsoft.sqlserver/mssql-jdbc "6.2.1.jre7"]    ; SQLServer JDBC driver. TODO - Switch this to `.jre8` once we officially switch to Java 8
                 [com.novemberain/monger "3.1.0"]                     ; MongoDB Driver
                 [com.taoensso/nippy "2.13.0"]                        ; Fast serialization (i.e., GZIP) library for Clojure
                 [compojure "1.5.2"]                                  ; HTTP Routing library built on Ring
                 [crypto-random "1.2.0"]                              ; library for generating cryptographically secure random bytes and strings
                 [dk.ative/docjure "1.11.0"]                          ; Excel export
                 [environ "1.1.0"]                                    ; easy environment management
                 [hiccup "1.0.5"]                                     ; HTML templating
                 [honeysql "0.8.2"]                                   ; Transform Clojure data structures to SQL
                 [io.crate/crate-jdbc "2.1.6"]                        ; Crate JDBC driver
                 [kixi/stats "0.3.10"                                 ; Various statistic measures implemented as transducers
                  :exclusions [org.clojure/test.check                 ; test.check and AVL trees are used in kixi.stats.random. Remove exlusion if using.
                               org.clojure/data.avl]]
                 [log4j/log4j "1.2.17"                                ; logging framework
                  :exclusions [javax.mail/mail
                               javax.jms/jms
                               com.sun.jdmk/jmxtools
                               com.sun.jmx/jmxri]]
                 [medley "0.8.4"]                                     ; lightweight lib of useful functions
                 [metabase/throttle "1.0.1"]                          ; Tools for throttling access to API endpoints and other code pathways
                 [mysql/mysql-connector-java "5.1.39"]                ;  !!! Don't upgrade to 6.0+ yet -- that's Java 8 only !!!
                 [jdistlib "0.5.1"                                    ; Distribution statistic tests
                  :exclusions [com.github.wendykierp/JTransforms]]
                 [net.cgrand/xforms "0.13.0"                          ; Additional transducers
                  :exclusions [org.clojure/clojurescript]]
                 [net.sf.cssbox/cssbox "4.12"                         ; HTML / CSS rendering
                  :exclusions [org.slf4j/slf4j-api]]
                 [com.clearspring.analytics/stream "2.9.5"            ; Various sketching algorithms
                  :exclusions [org.slf4j/slf4j-api
                               it.unimi.dsi/fastutil]]
                 [org.clojars.pntblnk/clj-ldap "0.0.12"]              ; LDAP client
                 [org.liquibase/liquibase-core "3.5.3"]               ; migration management (Java lib)
                 [org.postgresql/postgresql "42.1.4.jre7"]            ; Postgres driver
                 [org.slf4j/slf4j-log4j12 "1.7.25"]                   ; abstraction for logging frameworks -- allows end user to plug in desired logging framework at deployment time
                 [org.tcrawley/dynapath "0.2.5"]                      ; Dynamically add Jars (e.g. Oracle or Vertica) to classpath
                 [org.xerial/sqlite-jdbc "3.16.1"]                    ; SQLite driver
                 [org.yaml/snakeyaml "1.18"]                          ; YAML parser (required by liquibase)
                 [prismatic/schema "1.1.5"]                           ; Data schema declaration and validation library
                 [puppetlabs/i18n "0.8.0"]                            ; Internationalization library
                 [redux "0.1.4"]                                      ; Utility functions for building and composing transducers
                 [ring/ring-core "1.6.0"]
                 [ring/ring-jetty-adapter "1.6.0"]                    ; Ring adapter using Jetty webserver (used to run a Ring server for unit tests)
                 [ring/ring-json "0.4.0"]                             ; Ring middleware for reading/writing JSON automatically
                 [stencil "0.5.0"]                                    ; Mustache templates for Clojure
                 [toucan "1.0.3"                                      ; Model layer, hydration, and DB utilities
                  :exclusions [honeysql]]
                 [metabase "metabase-SNAPSHOT"]

                 
                 ]
  :main tracer.core)
