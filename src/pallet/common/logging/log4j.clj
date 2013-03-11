(ns pallet.common.logging.log4j
  "Functions for manipulating log4j"
  (:import
   [org.apache.log4j AppenderSkeleton Priority]))

(defn configure-from-path
  "Configure log4j using specified configuration file path."
  [path]
  (org.apache.log4j.xml.DOMConfigurator/configure
   (java.net.URL. path)))

(def log-priorities
  {:warn  Priority/WARN
   :debug Priority/DEBUG
   :fatal Priority/FATAL
   :info  Priority/INFO
   :error Priority/ERROR})

(defn appender
  "Get the appender with the specified name"
  [appender-name]
  (.. (org.apache.log4j.Logger/getRootLogger) (getAppender appender-name)))

(defmacro with-appender-threshold
  "A scope for logging with an appender at the specified threshold"
  [[level & [appender-name]] & body]
  `(let [appender-name# (or ~appender-name "console")]
     (if-let [^AppenderSkeleton appender# (appender appender-name#)]
       (let [threshold# (.getThreshold appender#)]
         (try
           (.setThreshold
            appender# ^Priority (~level log-priorities Priority/WARN))
           ~@body
           (finally
            (.setThreshold appender# threshold#))))
       (throw
        (RuntimeException.
         (format "Could not find appender \"%s\"" appender-name#))))))

(defn logging-threshold-fixture
  "A fixture to set the logging level of a specified appender"
  ([] (logging-threshold-fixture :error "console"))
  ([level] (logging-threshold-fixture level "console"))
  ([level appender-name]
     (fn [f]
       (with-appender-threshold [level appender-name]
         (f)))))
