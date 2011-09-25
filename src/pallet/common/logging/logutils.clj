(ns pallet.common.logging.logutils
  "Utilities for logging"
  (:require
   [clojure.tools.logging :as logging]
   [clojure.stacktrace :as stacktrace]))

;;; Try loading logger specific namespaces
(doseq [ns ['pallet.common.logging.slf4j 'pallet.common.logging.log4j
            'pallet.common.logging.logback]]
  (try
    (require ns)
    (catch Exception _)))


(defmacro tools-logging-compat []
  (if (try
        (Class/forName "clojure.tools.logging.Log")
        (catch ClassNotFoundException _))
    `(do
       ;; pre tools.logging 0.2.0
       (deftype ~'NullLogger
           []
         clojure.tools.logging.Log
         (impl-enabled? [log# level#] false)
         (impl-write! [log# level# throwable# message#]))
       (def null-log (delay (NullLogger.)))

       (deftype ~'NullLoggerFactory
           []
         clojure.tools.logging.LogFactory
         (impl-name [factory#] "null logger")
         (impl-get-log [factory# log-ns#] @null-log))
       (def null-logger-factory (delay (NullLoggerFactory.)))

;;; A stdout logger
;;; Logs everyting to stdout.  Can be useful to test logging.
       (deftype ~'StdoutLogger
           []
         clojure.tools.logging.Log
         (impl-enabled? [log# level#] true)
         (impl-write! [log# level# throwable# message#]
           (println (name level#) message#)
           (when throwable#
             (stacktrace/print-stack-trace
              (stacktrace/root-cause throwable#)))))
       (def stdout-log (delay (StdoutLogger.)))

       (deftype ~'StdoutLoggerFactory
           []
         clojure.tools.logging.LogFactory
         (impl-name [factory#] "stdout logger")
         (impl-get-log [factory# log-ns#] @stdout-log))
       (def stdout-logger-factory (delay (StdoutLoggerFactory.)))

       (defmacro with-logger-factory
         [factory# & body#]
         `(binding [logging/*log-factory* ~factory#] ~@body#)))
    `(do
       ;; tools.logging 0.2.0 and up
;;; A null logger
;;; Suppresses all logging.  Can be useful to quiet test cases.
       (deftype ~'NullLogger
           []
         clojure.tools.logging.impl.Logger
         (enabled? [log# level#] false)
         (write! [log# level# throwable# message#]))

       (def null-log (delay (NullLogger.)))

       (deftype ~'NullLoggerFactory
           []
         clojure.tools.logging.impl.LoggerFactory
         (name [factory#] "null logger")
         (get-logger [factory# log-ns#] @null-log))
       (def null-logger-factory (delay (NullLoggerFactory.)))

;;; A stdout logger
;;; Logs everyting to stdout.  Can be useful to test logging.
       (deftype ~'StdoutLogger
           []
         clojure.tools.logging.impl.Logger
         (enabled? [log# level#] true)
         (write! [log# level# throwable# message#]
           (println (name level#) message#)
           (when throwable#
             (stacktrace/print-stack-trace
              (stacktrace/root-cause throwable#)))))
       (def stdout-log (delay (StdoutLogger.)))

       (deftype ~'StdoutLoggerFactory
           []
         clojure.tools.logging.impl.LoggerFactory
         (name [factory#] "stdout logger")
         (get-logger [factory# log-ns#] @stdout-log))
       (def stdout-logger-factory (delay (StdoutLoggerFactory.)))

       (defmacro with-logger-factory
         [factory# & body#]
         `(binding [logging/*logger-factory* ~factory#] ~@body#)))))

(tools-logging-compat)


;;; Macros to use specific logging implementations in a given scope
(defmacro logging-to-stdout
  "Send log messages to stdout for inspection"
  [& forms]
  `(with-logger-factory @stdout-logger-factory
     ~@forms))

(defmacro logging-to-string
  "Send log messages to a string for inspection"
  [& forms]
  `(with-out-str
     (with-logger-factory @stdout-logger-factory
       ~@forms)))

(defmacro suppress-logging
  "Prevent log messages to reduce test log noise"
  [& forms]
  `(with-logger-factory @null-logger-factory
     ~@forms))

(defmacro with-context
  "Specify the logging context for a given `body`. `bindings` is a vector of
   keyword value pairs to be set on the Mapped Diagnostic Context. If the
   current logger doesn't support contexts, then the body is just wrapped in a
   `do`. slf4j is the only supported logger at present."
  [bindings & body]
  (try
    (require 'pallet.common.logging.slf4j)
    `(pallet.common.logging.slf4j/with-context [~@bindings]
       ~@body)
    (catch Exception _
      (logging/warn
       "Logging contexts are not supported by your logger configuration")
      `(do ~@body))))

(defmacro with-threshold
  [[level & [appender-name logger-name]] & body]
  (try
       (require 'pallet.common.logging.logback)
       `(pallet.common.logging.logback/with-logger-level
          [~level ~appender-name ~logger-name] ~@body)
       (catch Exception _
         (try
           (require 'pallet.common.logging.log4j)
           `(pallet.common.logging.log4j/with-appender-threshold
              [~level ~appender-name] ~@body)
           (catch Exception _
             (fn [f] (f)))))))

(defmacro logging-threshold-fixture
  "A fixture to set the logging level of a specified logger"
  ([level appender-name logger-name]
     (try
       (require 'pallet.common.logging.logback)
       `(pallet.common.logging.logback/logging-threshold-fixture
         ~level ~appender-name ~logger-name)
       (catch Exception _
         (try
           (require 'pallet.common.logging.log4j)
           `(pallet.common.logging.log4j/logging-threshold-fixture
             ~level ~appender-name ~logger-name)
           (catch Exception _
             (fn [f] (f)))))))
  ([level appender-name] `(logging-threshold-fixture ~level appender-name nil))
  ([level] `(logging-threshold-fixture ~level nil nil))
  ([] `(logging-threshold-fixture :error nil nil)))

(defmacro with-log-to-string
  "Target the logger for an output string."
  [[& args] & body]
  (try
    (require 'pallet.common.logging.logback)
    `(pallet.common.logging.logback/with-log-to-string [~@args] ~@body)
    (catch Exception _
      `(with-out-str ~@body))))
