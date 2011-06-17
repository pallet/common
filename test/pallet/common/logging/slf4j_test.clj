(ns pallet.common.logging.slf4j-test
  (:use pallet.common.logging.slf4j)
  (:use clojure.test)
  (:require
   [clojure.java.io :as io]
   [clojure.string :as string]
   [clojure.tools.logging :as logging])
  (:import (org.slf4j MDC LoggerFactory Logger)))

(defn- clear-context []
  (MDC/clear))

(defn- test-clear-context []
  (is (empty? (MDC/getCopyOfContextMap))))

(defn- check-val
  ([key]
     (is (MDC/get key)))
  ([key val]
     (is (= val (MDC/get key)))))

(defn check-nil [key]
  (is (nil? (MDC/get key))))

(deftest test-nested-contexts
  (testing "Context is cleared after wrapper exits"
    (clear-context)
    (test-clear-context)
    (with-context ["a" "1"]
      (check-val "a" "1"))
    (test-clear-context)
    (MDC/put "a" "1")
    (with-context ["b" "2"]
      (check-val "a" "1")
      (check-val "b" "2"))
    (check-nil "b")
    (check-val "a" "1"))
  (testing "Contexts are nested properly"
    (clear-context)
    (with-context ["a" "1"]
      (with-context ["b" "2"]
        (check-val "a" "1")
        (check-val "b" "2"))
      (check-val "a" "1")
      (check-nil "b"))
    (check-nil "a"))
  (testing "Contexts are unstacked properly"
    (clear-context)
    (with-context ["a" "1"]
      (with-context ["a" "2"]
        (check-val "a" "2"))
      (check-val "a" "1"))
    (test-clear-context))
  (testing "Contexts are always cleared"
    (clear-context)
    (try
      (with-context ["a" "1"]
        (throw (RuntimeException.)))
      (catch Throwable e))
    (test-clear-context)))

(deftest test-non-string-args
  (testing "Keys can be keywords"
    (clear-context)
    (with-context [:a "a"]
      (check-val "a" "a")))
  (testing "Values can be numbers"
    (with-context ["a" 1]
      (check-val "a" "1")))
  (test-clear-context))

(deftest test-multiple-entries
  (clear-context)
  (with-context [:a 1 :b 2 :c 3 :d 4]
    (check-val "a" "1")
    (check-val "b" "2")
    (check-val "c" "3")
    (check-val "d" "4"))
  (test-clear-context))

(deftest test-no-entries
  (clear-context)
  (with-context []
    (test-clear-context)))

(defn count-lines [file]
  (try (count (string/split-lines (slurp file)))
       (catch Throwable e
         0)))

(defn last-line [file]
  (try (last (string/split-lines (slurp file)))
       (catch Throwable e
         nil)))

(def log-1 "log/1.log")
(def log-2 "log/2.log")
(def log-not-set "log/not-set.log")

(defn prime-logs [log]
  "This is needed for when running the tests from the command line. The log files are
resetted the first time you log a message, so if you count the lines before producing
any log entry the result will be the number of lines of before resetting the file, not 0"
  (let [msg "priming..."]
    (.debug log msg)
    (with-context [:my-key 1]
      (.debug log msg))
    (with-context [:my-key 2]
      (.debug log msg))))

(def prime-logs-fixture
  (fn [f]
    (let [log (LoggerFactory/getLogger "test")]
      (prime-logs log)
      (f))))

(use-fixtures :once prime-logs-fixture)

(deftest test-sifter
  (let [log (LoggerFactory/getLogger "test")
        lines-in-1 (count-lines log-1)
        lines-in-2 (count-lines log-2)
        lines-in-not-set (count-lines log-not-set)
        msg-1 "This is a log entry for key 1"
        msg-2 "This is a log entry for key 2"
        msg-not-set "This is a log entry without a key"]
    (.debug log msg-not-set)
    (with-context [:my-key 1]
      (.debug log msg-1))
    (with-context [:my-key 2]
      (.debug log msg-2))
    (testing "Only one line was added to log-1, the right one"
      (is (= (last-line log-1) msg-1))
      (is (= (inc lines-in-1) (count-lines log-1))))
    (testing "Only one line was added to log-2, the right one"
      (is (= (last-line log-2) msg-2))
      (is (= (inc lines-in-2) (count-lines log-2))))
    (testing "Only one line was added to log-not-set, the right one"
      (is (= (last-line log-not-set) msg-not-set))
      (is (= (inc lines-in-not-set) (count-lines log-not-set))))))


(deftest test-clojure-tools-logging
  (let [log-not-set-count (count-lines log-not-set)
        log-1-count (count-lines log-1)
        msg "this is my debug message from clojure.tools.logging"]
    (logging/warn msg)
    (testing "clojure.tools.logging adds one log line"
      (is (= (last-line log-not-set) msg))
      (is (= (inc log-not-set-count) (count-lines log-not-set))))
    (with-context [:my-key 1]
      (logging/debug msg)
      (testing "clojure.tools.logging works with contexts"
        (is (= (last-line log-1) msg))
        (is (= (inc log-1-count) (count-lines log-1)))))))
