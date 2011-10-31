(ns pallet.common.deprecate-test
  (:require
   [pallet.common.deprecate :as deprecate]
   pallet.common.deprecate.test-vars)
  (:use
   [clojure.test]))

(deprecate/forward-vars
 pallet.common.deprecate.test-vars five)

(deprecate/forward-fns
 pallet.common.deprecate.test-vars six)

(deftest forward-vars-test
  (is (= pallet.common.deprecate.test-vars/five five))
  (is (= 5 five)))

(deftest forward-fns-test
  (is (not= pallet.common.deprecate.test-vars/six six))
  (is (= 6 (six))))
