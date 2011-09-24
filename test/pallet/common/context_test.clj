(ns pallet.common.context-test
  (:require
   [pallet.common.context :as context]
   [pallet.common.logging.logutils :as logutils]
   [slingshot.core :as slingshot])
  (:use
   clojure.test))

(deftest in-context-test
  (testing "single level"
    (context/in-context
     "fred"
     (is (= ["fred"] (context/current-context))))
    (is (not (bound? #'context/*current-hierarchy*))))
  (testing "nested levels"
    (context/in-context
     "fred"
     (is (= ["fred"] (context/current-context)))
     (context/in-context
      "bloggs"
      (is (= ["fred" "bloggs"] (context/current-context))))
     (is (= ["fred"] (context/current-context))))
    (is (not (bound? #'context/*current-hierarchy*))))
  (testing "non-default key"
    (context/in-context
     "fred" {:key :fred}
     (is (= ["fred"]
              (context/current-context context/*current-hierarchy* :fred))))
    (is (not (bound? #'context/*current-hierarchy*))))
  (testing "non-default hierarchy"
    (let [hierarchy (context/make-hierarchy)]
      (context/in-context
       "fred" {:hierarchy hierarchy}
       (is (= ["fred"] (context/current-context))))
      (is (nil? (context/current-context hierarchy))))))

(deftest in-context-with-callback-test
  (let [seen-enter (atom nil)
        seen-exit (atom nil)]
    (context/in-context
     "fred" {:on-enter (fn on-enter [hierarchy key value]
                         (is (= key :pallet.common.context/default))
                         (is (= value "fred"))
                         (is (not @seen-enter))
                         (is (not @seen-exit))
                         (reset! seen-enter true))
             :on-exit (fn on-exit [hierarchy key value]
                        (is (= key :pallet.common.context/default))
                        (is (= value "fred"))
                        (is @seen-enter)
                        (is (not @seen-exit))
                        (reset! seen-exit true))}
     (is (= ["fred"] (context/current-context)))
     (is @seen-enter)
     (is (not @seen-exit)))
    (is (not (bound? #'context/*current-hierarchy*)))
    (is @seen-enter)
    (is @seen-exit)))

(deftest try-context-test
  (slingshot/try+
   (context/in-context
    "fred"
    (context/try-context
     (throw (Exception. "msg"))))
   (catch map? e
     (is (= ["fred"] (:context e)))
     (is (= "msg" (:message e))))))

(deftest with-context-test
  (slingshot/try+
   (context/with-context "fred"
     (throw (Exception. "msg")))
   (catch map? e
     (is (= ["fred"] (:context e)))
     (is (= "msg" (:message e))))))

(deftest with-logged-context-test
  (is (= "info -> fred\ninfo <- fred\n"
         (logutils/logging-to-string
           (context/with-logged-context
             (context/with-context "fred"
               nil))))))
