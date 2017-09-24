(ns tracer.stuff
  (:require
    [clojure.core.async :refer [<! chan go-loop]]
    [puget.printer :refer [cprint]]))

(def stuff-ch (chan))

(go-loop
  []
  (let [data1 (<! stuff-ch)]
    (newline)
    (println ">>>" (.toString (new java.util.Date)))
    (cprint "Hello world!")
    (recur)))
