(ns apiary.store
  "SSoT for the ISCO-08 6123 independent apiary operations actor
  (itonami actor pattern, ADR-2607011000 / CLAUDE.md Actors section;
  README's 'Robotics premise' — a hive-monitoring robot performs
  temperature/humidity sensing and external inspection under this
  advisor/governor pair, which never dispatches hardware itself).
  Modeled on cloud-itonami-isco-4311's bookkeeping.store.

  Domain:

    client — a registered organization (:client-id, :name)
    hive   — a registered hive {:hive-id :client-id :name
             :max-sustainable-harvest-kg number}.
             `:max-sustainable-harvest-kg` is the registered ceiling a
             proposed harvest must not exceed — harvesting more honey
             than the colony can sustain endangers the hive, and that
             is arithmetic, not judgement.
    record — a committed operating record (approved harvest) —
             written ONLY via commit-record!.
    ledger — append-only audit trail, commit or hold."
  )

(defprotocol Store
  (client [s client-id])
  (hive [s hive-id])
  (records-of [s client-id])
  (ledger [s])
  (register-client! [s client])
  (register-hive! [s h])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (client [_ client-id] (get-in @a [:clients client-id]))
  (hive [_ hive-id] (get-in @a [:hives hive-id]))
  (records-of [_ client-id] (filter #(= client-id (:client-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-client! [s client]
    (swap! a assoc-in [:clients (:client-id client)] client) s)
  (register-hive! [s h]
    (swap! a assoc-in [:hives (:hive-id h)] h) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:clients {} :hives {} :records [] :ledger []}
                                   seed)))))
