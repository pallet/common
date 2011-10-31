(ns pallet.common.context
  "A hierarchical context, with callbacks on entry and exit of a context.
   The context is a map, with implementation scopes.  Options can be used to
   modify the behaviour of the context, as a form of middleware.

   :on-enter - the return value is merged into the context
   :on-exit - the return value is ignored"
  (:refer-clojure :exclude [make-context])
  (:require
   [clojure.string :as string]
   [clojure.tools.logging :as logging]
   [slingshot.core :as slingshot]))

(def ^{:dynamic true :doc "Thread specific current context"}
  *current-context*)

(def
  ^{:doc "The keys that control the behaviour of a context."}
  compose-keys
  [:on-enter :on-exit :on-exception])

(def
  ^{:doc "The keys that control the behaviour of a context."}
  override-keys
  [:format])

(def
  ^{:doc "The keys that control the behaviour of a context."}
  option-keys
  (concat override-keys compose-keys))

(def override-defaults
  {:format identity})

(defn set-context-scope
  [context scope]
  (let [scope-sym (gensym (name scope))]
    (->
     context
     (assoc ::current-scope scope)
     (assoc ::current-scope-sym scope-sym)
     (update-in [::scope-stack] (fn push-scope [s] (conj (or s []) scope-sym)))
     (assoc-in [::scope-options scope-sym]
               (assoc (select-keys context option-keys) :scope scope)))))

(defn make-context
  "Returns a new context. Accepts optional callbacks for :on-enter
   and on-exit, which are called for every change in context."
  [& {:keys [scope on-enter on-exit on-exception format]
      :or {scope ::default}
      :as options}]
  (letfn [(init-composed [context]
            (reduce
             #(update-in % [%2] (fn [f] (if f [f] [])))
             context compose-keys))]
    (set-context-scope
     (->
      (merge override-defaults options)
      (init-composed)
      (dissoc :scope))
     scope)))

(defn update-context-scope
  "Set the context scope for entries in a context."
  [context scope]
  (if (or (nil? scope) (= scope (::current-scope context)))
    context
    (set-context-scope context scope)))

(defn options
  "Set the options for a context. Accepts :on-enter and :on-exit callback
   functions."
  [context {:keys [scope on-enter on-exit on-exception format]
            :as options}]
  (let [scope (or
               scope
               (when (seq options)
                 (keyword (name (gensym "implied-scope")))))]
    (update-context-scope
     (merge
      (merge-with conj context (select-keys options compose-keys))
      (when scope override-defaults)    ; prevent inheritance
      (select-keys options override-keys))
     scope)))

(defn push-entry
  [context entry]
  (if entry
    (update-in
     context [(::current-scope-sym context)]
     (fn [v] (conj (or v []) entry)))
    context))

(defn on-enter
  [context entry]
  (if-let [f (:on-enter context)]
    (apply merge context (map (fn [f] (f context entry)) f))
    context))

(defn on-exit
  [context entry]
  (when-let [fns (:on-exit context)]
    (doseq [f fns] (f context entry))))

(defn on-exception
  [context exception-map]
  (if-let [f (:on-exception context)]
    (reduce (fn [m f] (f context m)) exception-map f)
    exception-map))

(defn current-context
  "Return the current context."
  [] (apply dissoc *current-context* ::current-scope option-keys ))

(defmacro in-context
  "Create a scope by pushing a context entry onto the context. On exit of
   the body, the context is popped.

   Recognised options are:
      scope on-enter on-exit on-exception format"
  [entry options & body]
  `(let [entry# ~entry
         options# ~options
         context# (if (bound? #'*current-context*)
                    *current-context*
                    (make-context))]
     (binding [*current-context* (->
                                  context#
                                  (options options#)
                                  (push-entry entry#)
                                  (on-enter entry#))]
       (try
         ~@body
         (finally
          (on-exit *current-context* entry#))))))

(declare formatted-context-entries)
(defmacro try-context
  "Execute body, wrapping any exceptions in an exception which includes the
   current context."
  [options & body]
  (let [{:keys [exception-type]
         :or {exception-type :runtime-exception}} options]
    `(slingshot/try+
      ~@body
      (catch Exception e#
        (slingshot/throw+
         (on-exception
          *current-context*
          {:type ~exception-type
           :context (formatted-context-entries *current-context*)})
         (.getMessage e#))))))

(defmacro with-context
  "Wraps the body with a context, and re-throws wrapped exceptions"
  [entry options & body]
  `(let [entry# ~entry
         options# ~(dissoc options :exception-type)]
     (in-context
      entry# options#
      (try-context
       ~(select-keys options [:exception-type])
       ~@body))))

(defn context-entries
  "Return the context entries for a context"
  [context]
  (mapcat context (::scope-stack context)))

(defn context-entries-as-string
  [entries]
  (string/join " " entries))

(defn formatted-scope-entries
  "Return the formatted context entries for the given scope"
  ([context scope]
     (map
      (-> context ::scope-options scope :format)
      (get context scope)))
  ([]
     (formatted-scope-entries
      *current-context* (last (::scope-stack *current-context*)))))

(defn formatted-context-entries
  "Return the formatted context entries for a context"
  ([context]
     (mapcat
      (partial formatted-scope-entries context)
      (::scope-stack context)))
  ([] (formatted-context-entries *current-context*)))

(defn formatted-context
  "Return the last formatted context entry for a context"
  ([context]
     (let [scope (last (::scope-stack context))]
       ((-> context ::scope-options scope :format)
        (last (get context scope))))
     )
  ([] (formatted-context *current-context*)))

(defmacro with-logged-context
  "Log context entries and exits"
  [& body]
  `(in-context
    nil
    {:on-enter (fn [context# entry#]
                 (when entry#
                   (logging/infof
                    "-> %s"
                    (string/join " " (formatted-context-entries context#)))))
     :on-exit (fn [context# entry#]
                (when entry#
                  (logging/infof
                   "<- %s"
                   (string/join " " (formatted-context-entries context#)))))}
    ~@body))


(defmacro context-history
  [{:keys [history-kw limit] :or {history-kw :history limit 100}}]
  `(fn context-history [context# entry#]
     (when entry#
       {::history-kw ~history-kw
        ~history-kw
        (let [history# (conj
                        (or (~history-kw context#)
                            (clojure.lang.PersistentQueue/EMPTY))
                        context#)]
          (if (> (count history#) ~limit) (pop history#) history#))})))

(defmacro with-context-history
  "Add context to a limited history"
  [{:keys [history-kw limit] :as options} & body]
  `(in-context
    nil
    {:on-enter (context-history ~options)}
    ~@body))

(defn formatted-history
  [context]
  (get context (::history-kw context))
  (map
   formatted-context
   (get context (::history-kw context))))

(defn scope-context-entries
  "Return a sequence of context entries for the specified scope"
  ([context scope]
     (mapcat
      context
      (filter
       (fn scope= [scope-sym]
         (= scope (-> context ::scope-options scope-sym :scope)))
       (::scope-stack context))))
  ([scope]
     (scope-context-entries *current-context* scope)))

(defn scope-formatted-context-entries
  "Return a sequence of formatted context entries for the specified scope"
  ([context scope]
     (mapcat
      (fn [scope-sym]
        (map
         (-> context ::scope-options scope-sym :format)
         (context scope-sym)))
      (filter
       (fn scope= [scope-sym]
         (= scope (-> context ::scope-options scope-sym :scope)))
       (::scope-stack context))))
  ([scope]
     (scope-formatted-context-entries *current-context* scope)))

(defn throw-map
  "Throws a map, containing the current context on the :context scope"
  [msg {:as exception-map}]
  (let [context (if (bound? #'*current-context*) *current-context* {})]
    (slingshot/throw+
     (on-exception
      context
      (assoc exception-map
        :context (formatted-context-entries context)
        :context-history (formatted-history context)))
     msg)))
