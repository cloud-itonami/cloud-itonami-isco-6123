(ns apiary.advisor
  "ApiaryAdvisor — the advisor named in this repository's README,
  proposing an apiary operation (approve a harvest, administer a hive
  treatment, approve a defensive-colony operation) from an apiary
  management plan and hive health protocol. Swappable mock/llm; the
  advisor ONLY proposes — `apiary.governor` checks the sustainable-yield
  ceiling independently and always escalates treatment/defensive-colony
  decisions. Modeled on cloud-itonami-isco-4311's advisor.

  A proposal: {:op :approve-harvest|:administer-hive-treatment|:approve-defensive-colony-operation
               :effect :propose :hive-id str :harvest-kg number
               :stake kw :confidence n :rationale str}")

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake hive-id harvest-kg] :as request}]
  {:op op
   :effect :propose
   :hive-id hive-id
   :harvest-kg harvest-kg
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed " (name op) " for client " (:client-id request))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are an apiary-operations advisor. Given a request, propose an
   :op, the :hive-id and :harvest-kg, an honest :confidence and a
   :stake. Never call an over-sustainable-yield harvest conforming —
   the governor checks it against the registered hive ceiling.
   Hive-treatment and defensive-colony decisions always require human
   sign-off regardless of confidence.")

(defn- parse-proposal [content]
  (try
    (let [p (read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
