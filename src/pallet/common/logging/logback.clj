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

(defn set-filters [appender filters]
  (.clearAllFilters appender)
  (doseq [filter filters]
    (.addFilter appender filter)))

(defmacro with-logger-level
  "A scope for logging with a logger at the specified level"
  [[level & [appender-name logger-name]] & body]
  `(let [logger-name# ~logger-name
         appender-name# (or ~appender-name "CONSOLE")]
     (if-let [logger# (logger (or logger-name# root-logger-name))]
       (if-let [appender# (.getAppender logger# appender-name#)]
         (let [filters# (.getCopyOfAttachedFiltersList appender#)
               filter# (ch.qos.logback.classic.filter.LevelFilter.)]
           (try
             (.addFilter appender# filter#)
             (doto filter#
               (.setOnMatch ch.qos.logback.core.spi.FilterReply/ACCEPT)
               (.setOnMismatch ch.qos.logback.core.spi.FilterReply/DENY)
               (.setLevel
                (~level log-priorities ch.qos.logback.classic.Level/WARN))
               (.start))
             ~@body
             (.stop filter#)
             (finally
              (set-filters appender# filters#))))
         (throw
          (RuntimeException.
           (format "Could not find appender \"%s\"" appender-name#))))
       (throw
          (RuntimeException.
           (format "Could not find logger \"%s\"" logger-name#))))))

(defn logging-threshold-fixture
  "A fixture to set the logging level of a specified logger"
  ([]
     (logging-threshold-fixture :error "CONSOLE" root-logger-name))
  ([level]
     (logging-threshold-fixture level "CONSOLE" root-logger-name))
  ([level appender-name]
     (logging-threshold-fixture level appender-name root-logger-name))
  ([level appender-name logger-name]
     (fn [f]
       (with-logger-level [level appender-name logger-name]
         (f)))))

(defn slf4j-logback-status
  "Print logback status when configured under slf4j"
  []
  (ch.qos.logback.core.util.StatusPrinter/print
   (org.slf4j.LoggerFactory/getILoggerFactory)))

(defmacro with-log-to-string
  "A scope for logging with a logger at the specified level"
  [[& [appender-name logger-name]] & body]
  `(let [logger-name# ~logger-name
         appender-name# (or ~appender-name "CONSOLE")
         out# System/out]
     (if-let [logger# (logger (or logger-name# root-logger-name))]
       (if-let [appender# (.getAppender logger# appender-name#)]
         (try
           (let [s# (java.io.ByteArrayOutputStream.)]
             (System/setOut (java.io.PrintStream. s#))
             (.setTarget appender# "System.out")
             ~@body
             (str s#))
           (finally
            (System/setOut out#)
            (.setTarget appender# "System.out")))
         (throw
          (RuntimeException.
           (format "Could not find appender \"%s\"" appender-name#))))
       (throw
          (RuntimeException.
           (format "Could not find logger \"%s\"" logger-name#))))))
