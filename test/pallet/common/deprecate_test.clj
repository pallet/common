(ns pallet.common.deprecate-test
  (:use
   clojure.test)
  (:require
   pallet.common.deprecate
   [pallet.common.deprecate :as deprecate]))

;; test forwards two (randomly chosen) functions from this test namespace
(System/clearProperty "pallet.warn-on-resource-use")
(deftest forward-fns-no-warn-test
  (testing "without warning"
    (deprecate/forward-fns "0.5.0" pallet.common.deprecate warn)
    (deprecate/forward-fns pallet.common.deprecate deprecated)
    (is (= "0.5.0" (:deprecated (meta #'warn))))
    (is (= (deprecate/warn "s") (warn "s")))
    (is (nil? (:deprecated (meta #'deprecated))))
    (is (= (pallet.common.deprecate/deprecated "s") (deprecated "s")))
    )
)

(System/setProperty "pallet.warn-on-resource-use" "true")
(deftest forward-fns-warn-test
   (testing "with warning"
     (deprecate/forward-fns "0.5.0" pallet.common.deprecate warn)
     (deprecate/forward-fns pallet.common.deprecate deprecated)
     (is (= "0.5.0" (:deprecated (meta #'warn))))
     (is (= (deprecate/warn "s") (warn "s")))
     (is (nil? (:deprecated (meta #'deprecated))))
     (is (= (deprecate/deprecated "s") (deprecated "s")))))
