(ns pallet.common.context-test
  (:require
   [pallet.common.context :as context])
  (:use
   clojure.test))

(deftest in-context-test
  (testing "single level"
    (context/in-context
     "fred" []
     (is (= ["fred"]
              (:pallet.common.context/default
                @@#'pallet.common.context/default-hierarchy))))
    (is (= []
             (:pallet.common.context/default
               @@#'pallet.common.context/default-hierarchy))))
  (testing "nested levels"
    (context/in-context
     "fred" []
     (is (= ["fred"]
              (:pallet.common.context/default
                @@#'pallet.common.context/default-hierarchy)))
     (context/in-context
      "bloggs" []
      (is (= ["fred" "bloggs"]
               (:pallet.common.context/default
                 @@#'pallet.common.context/default-hierarchy))))
     (is (= ["fred"]
              (:pallet.common.context/default
                @@#'pallet.common.context/default-hierarchy))))
    (is (= []
             (:pallet.common.context/default
               @@#'pallet.common.context/default-hierarchy))))
  (testing "non-default key"
    (context/in-context
     "fred" [:key :fred]
     (is (= ["fred"]
              (:fred @@#'pallet.common.context/default-hierarchy))))
    (is (= []
             (:fred @@#'pallet.common.context/default-hierarchy))))
  (testing "non-default hierarchy"
    (let [hierarchy (context/make-hierarchy)]
      (context/in-context
       "fred" [:hierarchy hierarchy]
       (is (= ["fred"] (:pallet.common.context/default @hierarchy))))
      (is (= [] (:pallet.common.context/default @hierarchy))))))

(deftest in-context-with-callback-test
  (let [seen-enter (atom nil)
        seen-exit (atom nil)
        hierarchy (context/make-hierarchy)]
    (context/options!
     hierarchy
     {:on-enter (fn on-enter [hierarchy1 key value]
                  (is (= hierarchy hierarchy1))
                  (is (= key :pallet.common.context/default))
                  (is (= value "fred"))
                  (is (not @seen-enter))
                  (is (not @seen-exit))
                  (reset! seen-enter true))
      :on-exit (fn on-exit [hierarchy1 key value]
                  (is (= hierarchy hierarchy1))
                  (is (= key :pallet.common.context/default))
                  (is (= value "fred"))
                  (is @seen-enter)
                  (is (not @seen-exit))
                  (reset! seen-exit true))})
    (context/in-context
     "fred" [:hierarchy hierarchy]
     (is (= ["fred"] (:pallet.common.context/default @hierarchy)))
     (is @seen-enter)
     (is (not @seen-exit)))
    (is (= [] (:pallet.common.context/default @hierarchy)))
    (is @seen-enter)
    (is @seen-exit)))
