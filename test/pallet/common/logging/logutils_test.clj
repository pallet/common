(ns pallet.common.logging.logutils-test
  (:use clojure.test)
  (:require
   [pallet.common.logging.logutils :as logutils]
   [clojure.tools.logging :as logging]))

(deftest logging-to-stdout-test
  (is (= "warn Hello\n"
         (with-out-str
           (logutils/logging-to-stdout
            (logging/warn "Hello"))))))

(deftest suppress-logging-test
  ;; this test could be improved
  (is (= ""
         (logutils/with-log-to-string ["CONSOLE" "console"]
           (logutils/suppress-logging
            (logging/warn "Hello"))))))

(deftest with-context-test
  (is (= "warn Hello\n"
         (logutils/with-context [:kw "val"]
           (with-out-str
             (logutils/logging-to-stdout
              (logging/warn "Hello")))))))

(deftest with-threshold-test
  (is (= "Hello\n"
         (logutils/with-log-to-string ["CONSOLE" "console"]
           (logging/log "console" :warn nil "Hello"))))
  (is (= ""
         (logutils/with-log-to-string ["CONSOLE" "console"]
           (logutils/with-threshold [:error "CONSOLE" "console"]
             (logging/log "console" :warn nil "Hello"))))))


(deftest logging-threshold-fixture-test
  (is (= "Hello\n"
         (logutils/with-log-to-string ["CONSOLE" "console"]
           ((logutils/logging-threshold-fixture :warn "CONSOLE" "console")
            #(logging/log "console" :warn nil "Hello")))))
  (is (= ""
         (logutils/with-log-to-string ["CONSOLE" "console"]
           ((logutils/logging-threshold-fixture :error "CONSOLE" "console")
            #(logging/log "console" :warn nil "Hello"))))))
