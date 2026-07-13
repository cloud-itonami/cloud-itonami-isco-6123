(ns apiary.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [apiary.store :as store]
            [apiary.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Apiary"})
    (store/register-hive! st {:hive-id "H-1" :client-id "client-1"
                              :name "hive-north"
                              :max-sustainable-harvest-kg 20})
    st))

(defn- harvest [kg]
  {:op :approve-harvest :effect :propose :hive-id "H-1"
   :harvest-kg kg :confidence 0.9 :stake :low})

(def ^:private req {:client-id "client-1"})

(deftest ok-within-sustainable-yield
  (let [st (fresh-store)
        v (governor/check req {} (harvest 15) st)]
    (is (:ok? v))))

(deftest ok-at-exact-sustainable-yield
  (testing "harvest exactly at the ceiling is within margin"
    (let [st (fresh-store)
          v (governor/check req {} (harvest 20) st)]
      (is (:ok? v)))))

(deftest hard-on-harvest-exceeds-sustainable-yield
  (testing "exceeding the colony's sustainable yield is arithmetic, not judgement"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (harvest 40) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :harvest-exceeds-sustainable-yield (:rule %)) (:violations v))))))

(deftest hard-on-unknown-hive
  (let [st (fresh-store)
        v (governor/check req {} (assoc (harvest 15) :hive-id "H-ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-hive (:rule %)) (:violations v)))))

(deftest hard-on-foreign-hive
  (let [st (fresh-store)]
    (store/register-client! st {:client-id "client-2" :name "Other"})
    (let [v (governor/check {:client-id "client-2"} {} (harvest 15) st)]
      (is (:hard? v))
      (is (some #(= :hive-wrong-client (:rule %)) (:violations v))))))

(deftest hard-on-unregistered-client
  (let [st (fresh-store)
        v (governor/check {:client-id "nobody"} {} (harvest 15) st)]
    (is (:hard? v))
    (is (some #(= :no-client (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (harvest 15) :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest always-escalates-hive-treatment-even-at-high-confidence
  (testing "no hive-disease treatment administration without the governor gate"
    (let [st (fresh-store)
          v (governor/check req {} {:op :administer-hive-treatment :effect :propose
                                    :hive-id "H-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest always-escalates-defensive-colony-operation-even-at-high-confidence
  (testing "defensive-colony operations always require human sign-off"
    (let [st (fresh-store)
          v (governor/check req {} {:op :approve-defensive-colony-operation :effect :propose
                                    :hive-id "H-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (harvest 15) :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))
