(ns tracer.stuff
  (:require
    [clojure.core.async :refer [<! chan go-loop]]
    [puget.printer :refer [cprint]]
		[clj-http.client :as client]
    [cheshire.core :as json]
    [clojure.tools.logging :as log]
    [tracer.symbols :refer [symbols]]))

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
			(str uri prefix "-" (clojure.string/replace (.toString (java.util.Date.)) " " "_") "-" (rand-str 32))
		  {:body (json/generate-string
               {:prefix prefix
                :id id
                :data ((when (string? expr) identity pr-str) expr)})
		   :headers {}
  		 :socket-timeout 1000  ;; in milliseconds
		   :conn-timeout 1000}))))

(defn apply2
  ([f]
   (f))
  ([f a]
   (f a))
  ([f a b]
   (f a b))
  ([f a b c]
   (f a b c))
  ([f a b c d]
   (f a b c d))
  ([f a b c d e]
   (f a b c d e))
  ([f a b c d e f]
   (f a b c d e f))
  ([f a b c d e f & args]
   (apply f a b c d e f args)))

(defn dbgfn [name_ f]
  (fn [& args]
    (debug "you are calling" name_ "with" (pr-str args))
    (let [result (apply apply2 f args)]
      (debug "calling" name_ "with" (pr-str args) "returned" (pr-str result))
      result)))

(defn nothing [& _])

(defmacro dbg2 [body] (if (true? (:macro (meta (resolve (first body))))) ~@body 'nothing))

(defn is-not-reserved [smbl]
  (let [svalue
        (str smbl)]
  (not
    (or
      ; ...
      (#{'def 'defn 'defn- 'defmacro 'if 'do 'let 'quote 'var 'fn 'loop 'recur
         'throw 'try 'catch 'monitor-enter 'monitor-exit
         'print 'println 'pr 'prn
         'for 'ns
         ;'u/prog1
         } smbl)
      (symbols smbl)
      (clojure.string/starts-with? svalue ".")
      (clojure.string/ends-with? svalue ".")
      (clojure.string/starts-with? svalue "*")
      (clojure.string/ends-with? svalue "*")
      ))))


(defn add-dbgfn [macroexpanded-code]
  (clojure.walk/postwalk
    (fn [node]
      (let [node (cond->>
                   node
                   (and
                     (sequential? node)
                     ((complement vector?) node))
                   (apply list))]
      (if
        (and
          (list? node)
          ((complement empty?) node)
          (symbol? (first node))
          (is-not-reserved (first node)))
        (cons
          (list 'dbgfn (-> node first str) (first node))
          (rest node))
        node)))
    macroexpanded-code))


(defmacro dbg [body]
  (let [code
  (->
    body
    clojure.walk/macroexpand-all
    add-dbgfn)]
    (prn code)
    code))


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
	  (let [data1 (<! stuff-ch)

          plus42 (fn [& args ] (apply + 42 args))]

  	  ;(println "foo" ((dbg when) true "Hello world111!"))
  	  ;(println "bar" (when true "Hello world222!"))

           (println
             (dbg
               (-> 1 (plus42 10)))
             )

           )
           )
  (newline)
  (newline)
  (newline)
  (newline)
  (newline)
    	(recur))
