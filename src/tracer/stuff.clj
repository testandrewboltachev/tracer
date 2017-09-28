(ns tracer.stuff
  (:require
    [clojure.core.async :refer [<! chan go-loop]]
    [puget.printer :refer [cprint]]
		[clj-http.client :as client]
    [cheshire.core :as json]
    [clojure.tools.logging :as log]
    [tracer.symbols :refer [symbols]]
    [potemkin.walk]))

(def stuff-ch (chan))

(def ^:dynamic *tracer-config* nil)
(def ^:dynamic *tracer-level* nil)

(defn rand-str [len]
  (apply str (take len (repeatedly #(char (+ (rand 26) 65))))))

(comment
  {:uri "http://localhost:3451/api/debug/"
	 :prefix "123abc"
   :number 16})

(defn map-vals [f m]
  (zipmap (keys m) (map f (vals m))))

(defn debug [data]
	(let [{:keys [uri prefix id]} (merge
                                 {:uri "http://127.0.0.1:5984/cljdebug/"
														 :prefix "abcdef-"}

                                 *tracer-config*
                                  )
                                 ]
		(client/put
			(str uri prefix "-" (clojure.string/replace (.toString (java.util.Date.)) " " "_") "-" (rand-str 32))
		  {:body (json/generate-string
               (merge
                 {:prefix prefix
                  :id id
                  }
                 (map-vals
                   pr-str
                   #_(fn [v]
                     (if
                       (or
                         (nil? v)
                         (true? v)
                         (false? v)
                         (integer? v)
                         (string? v)
                         )
                       ))
                   data)))
		   :headers {}
  		 :socket-timeout 1000  ;; in milliseconds
		   :conn-timeout 1000})))

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
  ([f a b c d e & args]
   (apply f a b c d e args)))

(defn dbgfn [name_ meta_ f]
  (fn [& args]
    (debug {:type :call
            :name name_
            :meta meta_
            :args args})
    (let [result (apply apply2 f args)]
      (debug {:type :result
              :name name_
              :meta meta_
              :args args
              :result result})
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
  (potemkin.walk/prewalk
  (fn [node]
      (if
        (and
          (list? node)
          ((complement empty?) node)
          (symbol? (first node))
          (is-not-reserved (first node))
          )
        `(let* []
           (clojure.core/push-thread-bindings
             (clojure.core/hash-map (var *tracer-level*) (inc (or *tracer-level* 0))))
           (try
              ~(cons
                `(dbgfn ~(-> node first str) ~(merge
                                                   (meta node)
                                                   `{:level ~'*tracer-level*}) (first node))
                (rest node))
             (finally (clojure.core/pop-thread-bindings))))
       (cond->
         node
         (some? (meta node))
         (vary-meta
           (fn [meta_]
             (let [original-meta_ (dissoc meta_ :column :line)]
               (when-not (empty? original-meta_)
                 original-meta_)))))))
  macroexpanded-code))

(defn macroexpand-all
  [form]
  (potemkin.walk/prewalk
    (fn [x]
      (if
        (seq? x)
        (with-meta (macroexpand x) (meta x))
        x))
    form))


(defmacro dbg [body]
  (let [code
  (->
    body
    macroexpand-all
    )
        code (add-dbgfn code)
        ]
    (cprint code {:print-meta true})
    (newline)
    (newline)
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
               ^:line1 (identity ^{:line "bar"} (-> 1 ^{:bar "buz"} (plus42 ^:foo1 (inc 10))))))

           )
           )
  (newline)
  (newline)
  (newline)
  (newline)
  (newline)
    	(recur))
