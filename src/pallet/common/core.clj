(ns pallet.common.core
  "General forms"
  (:require
   [clojure.string :as string]
   [clojure.contrib.def :as def]))

(defn- kw-doc
  "Create a doc string for the specified keyword arguments. kw-args is a
   sequence of vectors, each specifying a symbol, docstring and possibly a
   default"
  [kw-args]
  (str
   \newline
   \newline
   (string/join
    \newline
    (map
     (fn [[symbol description default :as args]]
       (format
        "- :%s  %s.%s"
        symbol description
        (if (> (count args) 2) (str " Default: " (pr-str default)) "")))
     kw-args))))

(defmacro defnkw
  "Define a function taking keyword arguments.  The keyword arguments are
   specified after a '& symbol, as a sequence of vectors, each specifying a
   symbol, docstring and possibly a default.

   A description of the keyword arguments is appended to any docstring supplied
   to the functions.

       (defnkw myfn [a b & [[s1 \"s1 arg\" 1]
                            [s2 \"s2 arg\"]]]
         (+ a b s1 (or s2 0)))"
  [name & args]
  (let [[name [args & body]] (def/name-with-attributes name args)]
    (when (not= '& (last (butlast args)))
      (throw (Exception. "defnkw without args of the form [... & []]")))
    (let [kw-args (last args)
          pos-args (butlast (butlast args))
          name (vary-meta
                name
                #(update-in % [:doc] (fn [doc] (str doc (kw-doc kw-args)))))]
      `(defn ~name [~@pos-args & ~{:keys (vec (map first kw-args))
                                   :or (into {} (map
                                                 (fn [[symbol# _# default#]]
                                                   [symbol# default#])
                                                 (filter #(get % 2) kw-args)))}]
         ~@body))))
