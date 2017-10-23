(ns tracer.stuff
  (:require
    [clojure.core.async :refer [<! chan go-loop]]
    [puget.printer :refer [cprint]]
		[clj-http.client :as client]
    [cheshire.core :as json]
    [clojure.tools.logging :as log]
    [tracer.symbols :refer [symbols]]
    [potemkin.walk])
  
  
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [medley.core :as m]
            [metabase.util :as u]
            [metabase.util.schema :as su]
            [schema.core :as s])
  (:import java.sql.SQLException)
  
  
  
  
  
  
  )

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
  (clojure.walk/postwalk
  (fn [node]
      (if
        (and
          (sequential? node)
          ((complement vector?) node)
          ((complement empty?) node)
          (symbol? (first node))
          (is-not-reserved (first node)))
        `(let* []
           (clojure.core/push-thread-bindings
             (clojure.core/hash-map (var *tracer-level*) (inc (or *tracer-level* 0))))
           (try
              ~(cons
                `(dbgfn ~(-> node first str) ~(merge
                                                (meta node)
                                                `{:level '*tracer-level*})
                        ~(first node))
                (rest node))
             (finally (clojure.core/pop-thread-bindings))))
        ((if (-> node meta some?) #(with-meta % nil) identity) node)))
  macroexpanded-code))


(def ^:dynamic *the-macroexpand-level* nil)

(defn with-meta-or-identity [obj new-meta]
  (if (and
        (instance? clojure.lang.IMeta obj)
        (instance? clojure.lang.Obj obj))
    (with-meta obj new-meta)
    obj))

(defn macroexpand-all
  [form]
  (potemkin.walk/prewalk
    (fn [x]
      (let [r
      (if
        (seq? x)

        (with-meta-or-identity
          (try
            (macroexpand x)
            (catch Exception e
              (println "Can't macroexpand")
              (cprint x)
              ))
          (meta x))

        x)]
        ;(println (pr-str x) "return" (pr-str r))
        r))
    form))


(defmacro dbg [body]
  (let [code
  (->
    body
    macroexpand-all
    )
        _ (do
    (println "here's the code1")
    (cprint code {:print-meta true})
    (newline)
    (newline)
            )
        code (add-dbgfn code)
        ]
    (println "here's the code")
    (cprint code {:print-meta true})
    (newline)
    (newline)
    code))


(go-loop
  []
	(binding [*tracer-config* {:uri "http://127.0.0.1:5984/cljdebug/"
														 :prefix "abcdef-"}]
	  (let [data1 (<! stuff-ch)

          plus42 (fn [& args ] (apply + 42 args))]

  	  ;(println "foo" ((dbg when) true "Hello world111!"))
  	  ;(println "bar" (when true "Hello world222!"))
         (let [node '(plus42 1 (inc 10))]
    (println
      "wow"
      (and
          (list? node)
          ((complement empty?) node)
          (symbol? (first node))
          (is-not-reserved (first node)))))

    (println
      (dbg
        ^:line1 (identity ^{:line "bar"} (-> 1 ^{:bar "buz"} (plus42 ^:foo1 (inc 10))))))

(dbg
(defn- dox-for-schema
  "Look up the docstring for SCHEMA for use in auto-generated API documentation.
   In most cases this is defined by wrapping the schema with `with-api-error-message`."
  [schema]
  (if-not schema
    ""
    (or (su/api-error-message schema)
        (log/warn "We don't have a nice error message for schema:"
                  schema
                  "Consider wrapping it in `su/with-api-error-message`."))))

  )


           )
           )
  (newline)
  (newline)
  (newline)
  (newline)
  (newline)
    	(recur))
