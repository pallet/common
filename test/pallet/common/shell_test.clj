(ns pallet.common.shell-test
  (:use
   pallet.common.shell
   clojure.test)
  (:require
   [clojure.tools.logging :as logging]))

(deftest system-test
  (cond
   (.canRead (java.io.File. "/usr/bin/true")) (is (= {:exit 0 :out "" :err ""}
                                                     (system "/usr/bin/true")))
   (.canRead (java.io.File. "/bin/true")) (is (= {:exit 0 :out "" :err ""}
                                                 (system "/bin/true")))
   :else (logging/warn "Skipping system-test")))

(deftest bash-test
  (is (= {:exit 0 :out "fred\n" :err ""}
         (bash "echo fred"))))
