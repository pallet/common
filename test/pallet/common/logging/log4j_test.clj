(ns pallet.common.logging.log4j-test
  (:use
   pallet.common.logging.log4j
   clojure.test)
  (:require
   [clojure.contrib.logging :as logging]))

(deftest with-appender-threshold-test
  (with-appender-threshold [:error]
    (logging/info "hello"))
  (is (thrown-with-msg? RuntimeException #"Could not find appender"
        (with-appender-threshold [:error "doesnt-exist"]
          (logging/info "hello")))))
