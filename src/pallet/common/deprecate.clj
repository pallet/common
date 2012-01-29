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

(defmacro forward-no-warn
  ([f-name ns]
     (let [argv (gensym "argv")]
       `(defn ~f-name [~'& ~argv]
          (apply ~(symbol (name ns) (name f-name)) ~argv))))
  ([f-name ns version]
     (let [argv (gensym "argv")]
       `(defn ~f-name {:deprecated ~version} [~'& ~argv]
          (apply ~(symbol (name ns) (name f-name)) ~argv)))))

(defn warn-and-forward
  [f-name ns form argv]
  `[(deprecated-macro
     ~form
     (rename
      ~(list 'quote (symbol (name (ns-name *ns*)) (name f-name)))
      ~(list 'quote (symbol (name ns) (name f-name))))
     )
    ~(list
      `list*
      (list 'quote (symbol (name ns) (name f-name)))
      argv
      )])

(defmacro forward-fn-warn
  ([f-name ns]
     (let [argv (gensym "argv")]
       `(defmacro ~f-name [~'& ~argv]
          ~@(warn-and-forward f-name ns '&form argv))))
  ([f-name ns ver]
     (let [argv (gensym "argv")]
       `(defmacro ~f-name {:deprecated ~ver} [~'& ~argv]
          ~@(warn-and-forward f-name ns '&form argv)))))

;; (defmacro forward-fn-warn
;;   [f-name ns]
;;   (let [argv (gensym "argv")]
;;     `(defmacro ~f-name [~'& ~argv]
;;        `(do
;;           (deprecated-macro
;;            ~~'&form
;;            (deprecate-rename
;;             ~(list 'quote (symbol (name (ns-name *ns*)) (name '~f-name)))
;;             ~'~(list 'quote (symbol (name (ns-name ns)) (name f-name)))))
;;           ~~(list
;;              `list*
;;              (list 'quote (symbol (name ns) (name f-name)))
;;              argv)))))

(defmacro forward-fn-no-warn
  ([f-name ns]
     `(forward-no-warn ~f-name ~ns))
  ([f-name ns version]
     `(forward-no-warn ~f-name ~ns ~version)))

(defmacro forward-fn
  ([f-name ns ver]
     (if (System/getProperty "pallet.warn-on-resource-use")
       `(forward-fn-warn ~f-name ~ns ~ver)
       `(forward-fn-no-warn ~f-name ~ns ~ver)))
  ([f-name ns]
     (if (System/getProperty "pallet.warn-on-resource-use")
       `(forward-fn-warn ~f-name ~ns)
       `(forward-fn-no-warn ~f-name ~ns))))

(defmacro forward-fns
  "Forward syms to ns"
  {:arglists '[[ns & fn-syms][version-string ns & fn-syms]]}
  [ns-or-ver & fns]
  (if (string? ns-or-ver)
    `(do ~@(for [f (rest fns)] `(forward-fn ~f ~(first fns) ~ns-or-ver)))
    `(do ~@(for [f fns] `(forward-fn ~f ~ns-or-ver)))))

(defmacro forward-vars
  "Forward syms to ns"
  {:arglists '[[ns & syms][version-string ns & syms]]}
  [ns-or-ver & syms]
  (if (string? ns-or-ver)
    `(do ~@(for [sym (rest syms)]
             `(forward-no-warn ~sym ~(first syms) ~ns-or-ver)))
    `(do ~@(for [sym syms] `(forward-no-warn ~sym ~ns-or-ver)))))
