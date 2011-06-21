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
         (with-out-str
           (logutils/suppress-logging
            (logging/warn "Hello"))))))

(deftest with-context-test
  (is (= "warn Hello\n"
         (logutils/with-context [:kw "val"]
           (with-out-str
             (logutils/logging-to-stdout
              (logging/warn "Hello")))))))

(deftest with-threshold-test
  (is
   (logutils/with-threshold [:warn]
     (with-out-str
       (logutils/logging-to-stdout
        (logging/warn "Hello"))))))


(deftest logging-threshold-fixture-test
  ;; TODO - improve these
  (is (= ""
         (with-out-str
           ((logutils/logging-threshold-fixture)
            #(logging/error "Hello")))))
  (is (= ""
         (with-out-str
           ((logutils/logging-threshold-fixture)
            #(logging/warn "Hello")))))
  (is (= ""
         (with-out-str
           ((logutils/logging-threshold-fixture :warn)
            #(logging/warn "Hello"))))))
