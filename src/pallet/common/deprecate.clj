(ns pallet.common.deprecate
  "Deprecation forms"
  (:require
   [clojure.tools.logging :as logging]))

(defmacro find-caller-from-stack
  "Find the call site of a function. A macro so we don't create extra frames."
  ([] `(find-caller-from-stack 4))
  ([frame-depth]
     `(let [frame# (nth (.. (Thread/currentThread) getStackTrace) ~frame-depth)]
        [(.getFileName frame#) (.getLineNumber frame#)])))

(defn warn
  "Log a deprecation warning"
  ([message]
     (logging/warnf "DEPRECATED %s" message))
  ([file message]
     (logging/warnf "DEPRECATED [%s] %s" (or file "unknown") message))
  ([file line message]
     (logging/warnf
      "DEPRECATED [%s:%s] %s" (or file "unknown") (or line "unknown") message)))

(defmacro deprecated-macro
  "Generates a deprecated warning for a macro, allowing the source file and
   line to be captured"
  [form message]
  `(warn ~(:file (meta form) *file*) ~(:line (meta form)) ~message))

(defn deprecated
  "Generates a deprecated warning, locating file and line from the call stack."
  [message]
  (let [[file line] (find-caller-from-stack)]
    (warn file line message)))

(defn rename
  "Generates a deprecated message for renaming a function"
  [from to]
  (format "%s is deprecated, use %s" (pr-str from) (pr-str to)))

(defmacro forward-no-warn-var
  [f-name to-ns]
  `(def ~f-name ~(symbol (name to-ns) (name f-name))))

(defmacro forward-no-warn
  [f-name ns]
  (let [argv (gensym "argv")]
    `(defn ~f-name [~'& ~argv]
       (apply ~(symbol (name ns) (name f-name)) ~argv))))

(defmacro forward-fn-warn
  [f-name ns]
  (let [argv (gensym "argv")]
    `(defmacro ~f-name [~'& ~argv]
       `(do
          (deprecated-macro
           ~~'&form
           (deprecate-rename
            ~(list 'quote (symbol (name (ns-name *ns*)) (name '~f-name)))
            ~'~(list 'quote (symbol (name (ns-name ns)) (name f-name)))))
          ~~(list
             `list*
             (list 'quote (symbol (name ns) (name f-name)))
             argv)))))
(defmacro forward-fn-no-warn
  [f-name ns]
  `(forward-no-warn ~f-name ~ns))

(defmacro forward-fn
  [f-name ns]
  (if (System/getProperty "pallet.warn-on-resource-use")
    `(forward-fn-warn ~f-name ~ns)
    `(forward-fn-no-warn ~f-name ~ns)))

(defmacro forward-fns
  "Forward syms to ns"
  [ns & fns]
  `(do ~@(for [f fns] `(forward-fn ~f ~ns))))

(defmacro forward-vars
  "Forward syms to ns"
  [ns & syms]
  `(do ~@(for [sym syms] `(forward-no-warn-var ~sym ~ns))))
