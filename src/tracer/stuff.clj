(ns tracer.stuff
  (:require
    [clojure.core.async :refer [<! chan go-loop]]
    [puget.printer :refer [cprint]]
		[clj-http.client :as client]
    [cheshire.core :as json]
    [clojure.tools.logging :as log]))

(def stuff-ch (chan))

(def ^:dynamic *tracer-config* nil)

(defn rand-str [len]
  (apply str (take len (repeatedly #(char (+ (rand 26) 65))))))

(comment
  {:uri "http://localhost:3451/api/debug/"
	 :prefix "123abc"
   :number 16})

(defn debug [& words]
  (let [expr (clojure.string/join " " words)]
    #_(println expr)
	(let [{:keys [uri prefix id]} (merge
                                 {:uri "http://127.0.0.1:5984/cljdebug/"
														 :prefix "abcdef-"}
                                  
                                 *tracer-config*
                                  )
                                 ]
		(client/put
			(str uri prefix "-" (clojure.string/replace (.toString (java.util.Date.)) " " "_") "-" (rand-str 4))
		  {:body (json/generate-string
               {:prefix prefix
                :id id
                :data ((when (string? expr) identity pr-str) expr)})
		   :headers {}
  		 :socket-timeout 1000  ;; in milliseconds
		   :conn-timeout 1000}))))

(defn dbgfn [name_ f]
  (fn [& args]
    (debug "you are calling" name_ "with" (pr-str args))
    (let [result (apply f args)]
      (debug "calling" name_ "with" (pr-str args) "returned" (pr-str result))
      result)))

(defn nothing [& _])

(defmacro dbg2 [body] (if (true? (:macro (meta (resolve (first body))))) ~@body 'nothing))

(defmacro dbg [body] (clojure.walk/macroexpand-all body))



#_(defmacro dbg [x]
  (let [is-fn?
        (and
          (symbol? x)
          (try
            (do
              (println "we're ready to eval" (str \" (pr-str x) \"))
              (true? (:macro (meta (resolve x))))
              (println "eval done" (str \" (pr-str x) \"))
              true)
            (catch Exception e
              false)))]
  (if is-fn?
    `(dbgfn '~x ~x)
    (do
      ;(debug "macro called" (str \" x \") "on the way")
      x))))

(go-loop
  []
	(binding [*tracer-config* {:uri "http://127.0.0.1:5984/cljdebug/"
														 :prefix "abcdef-"}]
	  (let [data1 (<! stuff-ch)]

  	  ;(println "foo" ((dbg when) true "Hello world111!"))
  	  ;(println "bar" (when true "Hello world222!"))
         )
           
           (println ((dbg +) 1 2))
           (println
             (dbg (-> 1 (+ 10)))
             )

           )
  (newline)
  (newline)
  (newline)
  (newline)
  (newline)
    	(recur))
