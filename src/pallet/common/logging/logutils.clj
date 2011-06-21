(ns pallet.common.logging.logutils
  "Utilities for logging"
  (:require
   [clojure.tools.logging :as logging]
   [clojure.stacktrace :as stacktrace]))

;;; A null logger
;;; Suppresses all logging.  Can be useful to quiet test cases.
(deftype NullLog
    []
  clojure.tools.logging.Log
  (impl-enabled? [log level] false)
  (impl-write! [log level throwable message]))

(def null-log (delay (NullLog.)))

(deftype NullLogger
    []
  clojure.tools.logging.LogFactory
  (impl-name [factory] "null logger")
  (impl-get-log [factory log-ns] @null-log))

(def null-logger (delay (NullLogger.)))

;;; A stdout logger
;;; Logs everyting to stdout.  Can be useful to test logging.
(deftype StdoutLog
    []
  clojure.tools.logging.Log
  (impl-enabled? [log level] true)
  (impl-write! [log level throwable message]
    (println (name level) message)
    (when throwable
      (stacktrace/print-stack-trace
       (stacktrace/root-cause throwable)))))

(def stdout-log (delay (StdoutLog.)))

(deftype StdoutLogger
    []
  clojure.tools.logging.LogFactory
  (impl-name [factory] "stdout logger")
  (impl-get-log [factory log-ns] @stdout-log))

(def stdout-logger (delay (StdoutLogger.)))


;;; Macros to use specific logging implementations in a given scope
(defmacro logging-to-stdout
  "Send log messages to stdout for inspection"
  [& forms]
  `(binding [clojure.tools.logging/*log-factory* @stdout-logger]
     ~@forms))

(defmacro suppress-logging
  "Prevent log messages to reduce test log noise"
  [& forms]
  `(binding [clojure.tools.logging/*log-factory* @null-logger]
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
