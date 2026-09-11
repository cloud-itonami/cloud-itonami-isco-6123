(ns apiary.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [apiary.actor :as actor]
            [apiary.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Apiary"})
    (store/register-hive! st {:hive-id "H-1" :client-id "client-1"
                              :name "hive-north"
                              :max-sustainable-harvest-kg 20})
    st))

(deftest commits-an-in-ceiling-harvest
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-harvest :stake :low
                 :hive-id "H-1" :harvest-kg 15}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "client-1"))))))

(deftest holds-an-over-ceiling-harvest
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-harvest :stake :low
                 :hive-id "H-1" :harvest-kg 50}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :hold (:disposition (:state result))))
    (is (empty? (store/records-of st "client-1")))))

(deftest interrupts-then-administers-treatment-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :administer-hive-treatment :stake :low
                 :hive-id "H-1"}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "client-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "client-1")))))))
