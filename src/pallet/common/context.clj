(ns pallet.common.context
  "A hierarchical context, with callbacks on entry and exit of a context.
   The context is a map, with implementation keys.  Options can be used to
   modify the behaviour of the context, as a form of middleware."
  (:refer-clojure :exclude [make-context])
  (:require
   [clojure.string :as string]
   [clojure.tools.logging :as logging]
   [slingshot.core :as slingshot]))

(def ^{:dynamic true :doc "Thread specific current context"}
  *current-context*)

(def
  ^{:doc "The keys that control the behaviour of a context."}
  juxt-keys
  [:on-enter :on-exit :on-exception])

(def
  ^{:doc "The keys that control the behaviour of a context."}
  override-keys
  [:format])

(def
  ^{:doc "The keys that control the behaviour of a context."}
  option-keys
  (concat override-keys juxt-keys))

(defn make-context
  "Returns a new context context. Accepts optional callbacks for :on-enter
   and on-exit, which are called for every change in context."
  [& {:keys [on-enter on-exit on-exception format key]
      :or {key ::default format identity}
      :as options}]
  (let [key-sym (gensym (name key))]
    (merge
     (dissoc options :key :format)
     {::current-key key
      ::current-key-sym key-sym
      ::key-stack [key-sym]
      ::key-options {key-sym {:format format}}
      :format format})))

(defn options
  "Set the options for a context. Accepts :on-enter and :on-exit callback
   functions."
  [context {:keys [on-enter on-exit on-exception format]
            :or {format identity}
            :as options}]
  (->
   context
   (merge (merge-with juxt (select-keys context juxt-keys) options))
   (assoc :format format)))

(defn update-context-key
  "Set the context key for entries in a context."
  [context key]
  (if (or (nil? key) (= key (::current-key context)))
    context
    (let [key-sym (gensym (name key))]
      (->
       context
       (assoc ::current-key key)
       (assoc ::current-key-sym key-sym)
       (update-in [::key-stack] conj key-sym)
       (assoc-in [::key-options key-sym] (select-keys context option-keys))))))

(defn push-entry
  [context entry]
  (if entry
    (update-in
     context [(::current-key-sym context)]
     (fn [v] (conj (or v []) entry)))
    context))

(defn on-enter
  [context entry]
  (if-let [f (:on-enter context)]
    (f context entry)
    context))

(defn on-exit
  [context entry]
  (if-let [f (:on-exit context)]
    (f context entry)
    context))

(defn on-exception
  [context exception-map]
  (if-let [f (:on-exception context)]
    (f context exception-map)
    exception-map))

(defn current-context
  "Return the current context."
  [] (apply dissoc *current-context* ::current-key option-keys ))

(defn- options-and-body
  "Extract an optional options map from the first component of body."
  [body]
  (let [options (if (map? (first body)) (first body) {})
        body (if (map? (first body)) (rest body) body)]
    [options body]))

(defmacro in-context
  "Create a scope by pushing a context onto the context context. On exit of
   the body, the context is popped."
  [entry & body]
  (let [[options body] (options-and-body body)
        {:keys [key on-enter on-exit]} options]
    `(let [entry# ~entry
           context# (if (bound? #'*current-context*)
                      *current-context*
                      (make-context))
           key# ~key
           options# ~(select-keys options option-keys)]
       (binding [*current-context* (->
                                    context#
                                    (options options#)
                                    (update-context-key key#)
                                    (push-entry entry#))]
         (try
           (on-enter *current-context* entry#)
           ~@body
           (finally
            (on-exit *current-context* entry#)))))))

(defmacro try-context
  "Execute body, wrapping any exceptions in an exception which includes the
   current context."
  [& body]
  (let [[options body] (options-and-body body)
        {:keys [exception-type context key]
         :or {exception-type :runtime-exception}} options]
    `(slingshot/try+
      ~@body
      (catch Exception e#
        (slingshot/throw+
         (on-exception
          *current-context*
          {:type ~exception-type
           :context (apply dissoc *current-context* option-keys)
           :message (.getMessage e#)}))))))

(defmacro with-context
  "Wraps the body with a context, and re-throws wrapped exceptions"
  [entry & body]
  (let [[options body] (options-and-body body)]
    `(let [options# ~options]
       (in-context ~entry options#
        (try-context options#
         ~@body)))))

(defn throw+
  "Throws a map, containing the current context on the :context key"
  [& {:as exception-map}]
  (slingshot/throw+
   (on-exception
    *current-context*
    (assoc exception-map
      :context (apply dissoc *current-context* option-keys)))))

(defn context-entries
  "Return the context entries for a context"
  [context]
  (mapcat context (::key-stack context)))

(defn context-entries-as-string
  [entries]
  (string/join " " entries))

(defn formatted-context-entries
  "Return the formatted context entries for a context"
  ([context]
     (mapcat
      (fn format-context-for-key [key]
        (map
         (-> context ::key-options key :format)
         (get context key)))
      (::key-stack context)))
  ([] (formatted-context-entries *current-context*)))


(defmacro with-logged-context
  "Log context entries and exits"
  [& body]
  `(in-context
    nil
    {:on-enter (fn [context# entry#]
                 (when entry#
                   (logging/infof
                    "-> %s"
                    (string/join " " (formatted-context-entries)))))
     :on-exit (fn [context# entry#]
                (when entry#
                  (logging/infof
                   "<- %s"
                   (string/join " " (formatted-context-entries)))))}
    ~@body))

(defmacro with-context-history
  "Add context to a history"
  [& body]
  `(in-context
    nil
    {:on-enter (fn [context# key# entry#]
                 (when entry#
                   (logging/infof
                    "-> %s"
                    (context-as-string (current-context context# key#)))))
     :on-exit (fn [context# key# entry#]
                (when entry#
                  (logging/infof
                   "<- %s"
                   (context-as-string (current-context context# key#)))))}
    ~@body))
