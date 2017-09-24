(ns tracer.stuff
  (:require
    [clojure.core.async :refer [<! chan go-loop]]
    [puget.printer :refer [cprint]]
		[clj-http.client :as client]))

(def stuff-ch (chan))

(def ^:dynamic *tracer-config* nil)

(comment
  {:uri "http://localhost:3451/api/debug"
   :id "123abc"})

(defn debug [& args]
	(let [{:keys [uri id]} *tracer-config*]
		(client/post 
		  {:body (pr-str {:id id :data args})
		   :headers {"X-Api-Version" "2"}
  		 :socket-timeout 1000  ;; in milliseconds
		   :conn-timeout 1000}))

(defmacro dbgfn [f & args]
  `(do
    (debug (str '~f) ~@args)
    (~f ~@args)))

(go-loop
  []
  (let [data1 (<! stuff-ch)]
    (newline)
    (println ">>>" (.toString (new java.util.Date)))
    (dbgfn cprint "Hello world!")
    (recur)))
