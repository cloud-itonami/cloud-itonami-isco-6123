(ns apiary.governor
  "ApiaryOperationsGovernor — the independent safety/traceability
  layer named in this repository's README/business-model.md, gating
  the robot-dispensed physical work (temperature/humidity sensing,
  external-hive inspection) an advisor may propose. The governor
  never dispatches hardware itself. Modeled on
  cloud-itonami-isco-4311's bookkeeping.governor. Harvest twist: a
  proposed harvest is arithmetic comparison against the registered
  sustainable-yield ceiling — harvesting more honey than the colony
  can sustain endangers the hive, and that is arithmetic, not
  judgement.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. client provenance — the organization must be registered.
    2. no-actuation      — proposal :effect must be :propose (the
                           governor never dispatches hardware; it only
                           gates what the robot may execute).
    3. hive basis           — a harvest approval must cite a
                           REGISTERED hive belonging to this client.
    4. sustainable-yield ceiling — the proposed harvest-kg must not
                           exceed the hive's registered
                           :max-sustainable-harvest-kg (arithmetic,
                           not judgement).
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off per
  business-model.md's Trust Controls — these are :high/
  :safety-critical regardless of confidence):
    5. :op :administer-hive-treatment (no hive-disease treatment
                           administration without the governor gate).
    6. :op :approve-defensive-colony-operation (defensive-colony
                           operations always require human sign-off).
    7. low confidence (< `confidence-floor`)."
  (:require [apiary.store :as store]))

(def confidence-floor 0.6)

(def ^:private always-escalate-ops #{:administer-hive-treatment
                                     :approve-defensive-colony-operation})

(defn- hard-violations [{:keys [request proposal]} client-record h]
  (let [{:keys [op harvest-kg]} proposal
        harvest? (= :approve-harvest op)]
    (cond-> []
      (nil? client-record)
      (conj {:rule :no-client :detail "未登録 client"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（governor はハードウェアを直接起動しない）"})

      (and harvest? (nil? h))
      (conj {:rule :unknown-hive :detail "未登録 hive への採蜜承認は不可"})

      (and harvest? h (not= (:client-id h) (:client-id request)))
      (conj {:rule :hive-wrong-client :detail "hive が別 client のもの"})

      (and harvest? h (number? harvest-kg) (> harvest-kg (:max-sustainable-harvest-kg h)))
      (conj {:rule :harvest-exceeds-sustainable-yield
             :detail (str "採蜜量 " harvest-kg "kg > 登録済み持続可能上限 "
                          (:max-sustainable-harvest-kg h)
                          "kg（コロニーの持続可能量超過は算術であって判断ではない）")}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `apiary.store/Store`. Pure — never mutates the
  store, never dispatches the robot."
  [request context proposal store]
  (let [client-record (store/client store (:client-id request))
        h (some->> (:hive-id proposal) (store/hive store))
        hard (hard-violations {:request request :proposal proposal}
                              client-record h)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        always-risky? (contains? always-escalate-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
