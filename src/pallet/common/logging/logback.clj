(ns pallet.common.logging.logback
  "Functions for manipulating logback"
  (:require
   [clojure.tools.logging :as logging]))

(def log-priorities
  {:warn ch.qos.logback.classic.Level/WARN
   :debug ch.qos.logback.classic.Level/DEBUG
   :fatal ch.qos.logback.classic.Level/ERROR
   :info ch.qos.logback.classic.Level/INFO
   :error ch.qos.logback.classic.Level/ERROR
   :off ch.qos.logback.classic.Level/OFF
   :all ch.qos.logback.classic.Level/ALL})

(def root-logger-name org.slf4j.Logger/ROOT_LOGGER_NAME)

(defn logger
  "Get the logger with the specified name"
  ([] (logger root-logger-name))
  ([^String logger-name]
     (org.slf4j.LoggerFactory/getLogger logger-name)))

(defmacro with-logger-level
  "A scope for logging with a logger at the specified lvel"
  [[level & [logger-name]] & body]
  `(let [logger-name# ~logger-name]
     (if-let [logger# (logger (or logger-name# root-logger-name))]
       (let [level# (.getLevel logger#)]
         (try
           (.setLevel
            logger# (~level log-priorities ch.qos.logback.classic.Level/WARN))
           ~@body
           (finally
            (.setLevel  logger# level#))))
       (throw
        (RuntimeException.
         (format "Could not find logger \"%s\"" logger-name#))))))

(defn logging-threshold-fixture
  "A fixture to set the logging level of a specified logger"
  ([] (logging-threshold-fixture :error root-logger-name))
  ([level] (logging-threshold-fixture level root-logger-name))
  ([level logger-name]
     (fn [f]
       (with-logger-level [level logger-name]
         (f)))))

(defn slf4j-logback-status
  "Print logback status when configured under slf4j"
  []
  (ch.qos.logback.core.util.StatusPrinter/print
   (org.slf4j.LoggerFactory/getILoggerFactory)))
