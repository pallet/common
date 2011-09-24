(ns pallet.common.context
  "A hierarchical context, with callbacks on entry and exit of a context."
  (:refer-clojure :exclude [make-hierarchy])
  (:require
   [clojure.string :as string]
   [clojure.tools.logging :as logging]
   [slingshot.core :as slingshot]))

(def ^{:dynamic true :doc "Thread specific current context"}
  *current-hierarchy*)

(defn make-hierarchy
  "Returns a new context hierarchy. Accepts optional callbacks for :on-enter
   and on-exit, which are called for every change in context."
  [& {:keys [on-enter on-exit] :as options}]
  options)

(defn options
  "Set the options for a hierarchy. Accepts :on-enter and :on-exit callback
   functions."
  ([hierarchy {:keys [on-enter on-exit] :as options}]
     (merge
      hierarchy
      (merge-with juxt (select-keys hierarchy [:on-enter :on-exit]) options)))
  ([{:keys [on-enter on-exit] :as options}]
     (options *current-hierarchy* options)))

(defn push-context
  [hierarchy key context]
  (if context
    (update-in hierarchy [key] (fn [v] (conj (or v []) context)))
    hierarchy))

(defn on-enter
  [hierarchy key context]
  (when-let [f (:on-enter hierarchy)]
    (f hierarchy key context)))

(defn on-exit
  [hierarchy key context]
  (when-let [f (:on-exit hierarchy)]
    (f hierarchy key context)))

(defn current-context
  "Return the current context."
  ([hierarchy key] (get hierarchy key))
  ([hierarchy] (current-context hierarchy ::default))
  ([] (current-context *current-hierarchy*)))

(defn- options-and-body
  "Extract an optional options map from the first component of body."
  [body]
  (let [options (if (map? (first body)) (first body) {})
        body (if (map? (first body)) (rest body) body)]
    [options body]))

(defmacro in-context
  "Create a scope by pushing a context onto the context hierarchy. On exit of
   the body, the context is popped."
  [context & body]
  (let [[options body] (options-and-body body)
        {:keys [hierarchy key on-enter on-exit]
         :or {hierarchy `(if (bound? #'*current-hierarchy*)
                           *current-hierarchy*
                           (make-hierarchy))
              key ::default}} options]
    `(let [context# ~context
           hierarchy# ~hierarchy
           key# ~key
           options# ~(select-keys options [:on-enter :on-exit])]
       (binding [*current-hierarchy* (options
                                      (push-context hierarchy# key# context#)
                                      options#)]
         (try
           (on-enter *current-hierarchy* key# context#)
           ~@body
           (finally
            (on-exit *current-hierarchy* key# context#)))))))

(defmacro try-context
  "Execute body, wrapping any exceptions in an exception which includes the
   current context."
  [& body]
  (let [[options body] (options-and-body body)
        {:keys [exception-type hierarchy key]
         :or {exception-type :runtime-exception
              hierarchy `*current-hierarchy*
              key ::default}} options]
    `(slingshot/try+
      ~@body
      (catch Exception e#
        (slingshot/throw+
         {:type ~exception-type
          :context (current-context ~hierarchy ~key)
          :message (.getMessage e#)})))))

(defmacro with-context
  "Wraps the body with a context, and re-throws wrapped exceptions"
  [context & body]
  (let [[options body] (options-and-body body)]
    `(let [options# ~options]
       (in-context ~context options#
        (try-context options#
         ~@body)))))

(defn throw+
  "Throws a map, containing the current context on the :context key"
  [& {:as exception-map}]
  (slingshot/throw+
   (assoc exception-map
     :context (dissoc *current-hierarchy* :on-enter :on-exit))))

(defn context-as-string
  [context]
  (string/join " " context))

(defmacro with-logged-context
  "Log context entries and exits"
  [& body]
  `(in-context
    nil
    {:on-enter (fn [hierarchy# key# context#]
                 (when context#
                   (logging/infof
                    "-> %s"
                    (context-as-string (current-context hierarchy# key#)))))
     :on-exit (fn [hierarchy# key# context#]
                (when context#
                  (logging/infof
                   "<- %s"
                   (context-as-string (current-context hierarchy# key#)))))}
    ~@body))
