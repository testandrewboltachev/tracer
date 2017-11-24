(ns tracer.stuff
  (:require
    [clojure.core.async :refer [<! chan go-loop]]
    [puget.printer :refer [cprint]]
		[clj-http.client :as client]
    [cheshire.core :as json]
    [clojure.tools.logging :as log]
    [tracer.symbols :refer [symbols]]
    [potemkin.walk])
  

  (:require [cheshire.core :as json]
            [clojure.data.csv :as csv]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [compojure.core :refer [POST]]
            [dk.ative.docjure.spreadsheet :as spreadsheet]
            [metabase
             [middleware :as middleware]
             [query-processor :as qp]
             [util :as u]]
            [metabase.api.common :as api]
            [metabase.api.common.internal :refer [route-fn-name]]
            [metabase.models
             [card :refer [Card]]
             [database :as database :refer [Database]]
             [query :as query]]
            [metabase.query-processor.util :as qputil]
            [metabase.util.schema :as su]
            [schema.core :as s])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
           org.apache.poi.ss.usermodel.Cell)
  
  
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

(defn meta-or-nil [obj]
  (when
    (and
      (instance? clojure.lang.IMeta obj)
      (instance? clojure.lang.IObj obj))
    (meta obj)))

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
        ((if (-> node meta-or-nil some?) #(with-meta % nil) identity) node)))
  macroexpanded-code))


(def ^:dynamic *the-macroexpand-level* nil)

(defn with-meta-or-identity [obj new-meta]
  (if (and
        (instance? clojure.lang.IMeta obj)
        (instance? clojure.lang.IObj obj))
      (with-meta obj new-meta)
    obj))

(defn mexpand1 [x]
      (let [r
      (if
        (seq? x)

        (let [
              _ (do
                  (println "got x")
                  (cprint x))
              r1
        (with-meta-or-identity
          (try
            (macroexpand x)
            (catch Exception e
              (println "Can't macroexpand")
              (cprint x)
              ))
          (meta x))]
          (println "r1")
          (cprint r1)
          (println "r2")
          (cprint (clojure.walk/macroexpand-all x))
          (println "..")
          r1)

        x)]
        ;(println (pr-str x) "return" (pr-str r))
        r))


(defn walk
  "Like `clojure.walk/walk`, but preserves metadata."
  [inner outer form]
(println "walk" (class form)
(cond
            (list? form) 1
            (instance? clojure.lang.IMapEntry form) 2
            (seq? form) 3
            (coll? form) 4
            :else 5)

)
(cprint form)
  (let [x (cond
            (list? form) (outer (apply list (map inner form)))
            (instance? clojure.lang.IMapEntry form) (outer (vec (map inner form)))
            (seq? form) (outer (doall (map inner form)))
            (coll? form) (outer (into (empty form) (map inner form)))
            :else (outer form))]
    (if (instance? clojure.lang.IObj x)
      (with-meta x (merge (meta form) (meta x)))
      x)))

(defn postwalk
  "Like `clojure.walk/postwalk`, but preserves metadata."
  [f form]
  (walk (partial postwalk f) f form))

(defn prewalk
  "Like `clojure.walk/prewalk`, but preserves metadata."
  [f form]
  (walk (partial prewalk f) identity (f form)))


(defn macroexpand-all
  [form]
  (try
          (prewalk
            mexpand1
            form)
    (catch Exception e
      (println "failed to macroexpand-all")
      (cprint form)
      (throw (java.lang.Exception. "foo"))
      )))


(defmacro dbg [body]
  ;(println "got something")
  ;(cprint body {:print-meta true})
  (let [code
        (macroexpand-all body)
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

    #_(println
      (dbg
        ^:line1 (identity ^{:line "bar"} (-> 1 ^{:bar "buz"} (plus42 ^:foo1 (inc 10))))))

(dbg ^{:row 54, :col 1, :end-row 64, :end-col 124, :filename "/data1/andrey/stuff/projects/cljtracer/dbgadder/../metabase/src/metabase/api/dataset.clj"} (api/defendpoint POST "/"
  "Execute a query and retrieve the results in the usual format."
  [:as {{:keys [database], :as query} :body}]
  {database s/Int}
  ;; don't permissions check the 'database' if it's the virtual database. That database doesn't actually exist :-)
  (when-not (= database database/virtual-id)
    (api/read-check Database database))
  ;; add sensible constraints for results limits on our query
  (let [source-card-id (query->source-card-id query)]
    (qp/process-query-and-save-execution! (assoc query :constraints default-query-constraints)
      {:executed-by api/*current-user-id*, :context :ad-hoc, :card-id source-card-id, :nested? (boolean source-card-id)}))))



#_(dbg
1
  )


           )
           )
  (newline)
  (newline)
  (newline)
  (newline)
  (newline)
    	(recur))
