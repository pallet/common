(ns pallet.common.string-test
  (:use
   pallet.common.string
   clojure.test))

(deftest substring-test
  (testing "one argument"
    (is (= "est" (substring "test" 1)))
    (is (= "" (substring "test" 10))))
  (testing "two arguments"
    (is (= "es" (substring "test" 1 3)))
    (is (= "" (substring "test" 10 12)))
    (is (= "est" (substring "test" 1 12)))))

(deftest quoted-test
  (is (= "\"test\"" (quoted "test")))
  (is (= "\"1\"" (quoted 1))))

(deftest underscore-test
  (is (= "underscore_tes_t" (underscore "underscore-tes-t"))))
