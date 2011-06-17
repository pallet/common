(ns pallet.common.logging.slf4j
  "Functions for manipulating slf4j"
  (:import
   (org.slf4j Logger LoggerFactory MDC)))


;;;; Mapped Diagnostic Context (MDC)
(defn put-context [key val]
  (MDC/put (name key) (str val)))

(defmacro with-context
  "Specify the logging context for a given body. `bindings` is a vector of
   keyword value pairs to be set on the Mapped Diagnostic Context."
  [bindings & body]
  `(let [current-context-map# (MDC/getCopyOfContextMap)]
     (try
       (doseq [[k# v#] (partition 2 ~bindings)]
         (put-context k# v#))
       ~@body
       (finally
        (if current-context-map#
          (MDC/setContextMap current-context-map#)
          (MDC/clear))))))
