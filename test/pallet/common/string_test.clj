(ns pallet.common.string-test
  (:use
   pallet.common.string
   clojure.test))

(deftest substring-test
  (is (= "est" (substring 1 "test")))
  (is (= "" (substring 10 "test"))))

(deftest add-quotes-test
  (is (= "\"test\"" (add-quotes "test"))))

(deftest underscore-test
  (is (= "underscore_tes_t" (underscore "underscore-tes-t"))))
