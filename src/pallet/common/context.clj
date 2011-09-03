(ns pallet.common.context
  "A hierarchical context, with callbacks on entry and exit of a context."
  (:refer-clojure :exclude [make-hierarchy]))

(defn make-hierarchy
  "Returns a new context hierarchy. Accepts optional callbacks for :on-enter
   and on-exit, which are called for every change in context."
  [& {:keys [on-enter on-exit]
      :or {on-enter (constantly nil) on-exit (constantly nil)}}]
  (atom {::on-enter on-enter ::on-exit on-exit}))

(def ^{:private true :doc "The default hierarchy"}
  default-hierarchy (make-hierarchy))

(defn options!
  "Set the options for a hierarchy. Accepts :on-enter and :on-exit callback
   functions."
  ([hierarchy {:keys [on-enter on-exit] :as options}]
     (swap! hierarchy merge {::on-enter (or on-enter (::on-enter hierarchy))
                             ::on-exit (or on-exit (::on-exit hierarchy))}))
  ([{:keys [on-enter on-exit] :as options}]
     (options! default-hierarchy options)))

(defmacro in-context
  "Create a scope by pushing a context onto the context hierarchy. On exit of
   the body, the context is popped."
  [context [& {:keys [hierarchy key]
               :or {hierarchy `(deref (var default-hierarchy))
                    key ::default}}] & body]
  `(let [context# ~context
         hierarchy# ~hierarchy
         key# ~key]
     (swap! hierarchy# update-in [key#] (fn [v#] (conj (or v# []) context#)))
     (try
       ((::on-enter @hierarchy#) hierarchy# key# context#)
       ~@body
       (finally
        (swap! hierarchy# update-in [key#] pop)
        ((::on-exit @hierarchy#) hierarchy# key# context#)))))
