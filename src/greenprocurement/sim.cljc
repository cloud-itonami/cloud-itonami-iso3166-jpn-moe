(ns greenprocurement.sim
  "Demo driver -- `clojure -M:dev:run`. Walks a clean engagement through
  intake -> green-purchasing track assessment -> filing draft
  (escalate/approve/commit) -> filing submit (escalate/approve/commit),
  same for the eco-conscious-contracts track, then shows HARD-hold
  scenarios grounded in the dossier: fabrication defense, fee mismatch,
  missing conformity confirmation (判断の基準), and missing
  environmental documentation (総合評価)."
  (:require [langgraph.graph :as g]
            [greenprocurement.store :as store]
            [greenprocurement.operation :as op]))

(def operator {:actor-id "op-1" :actor-role :green-procurement-operator :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn -main [& _]
  (let [db (store/seed-db)
        actor (op/build db)]
    (println "== engagement/intake eng-1 (clean) ==")
    (println (exec-op actor "t1" {:op :engagement/intake :subject "eng-1"
                                  :patch {:id "eng-1" :operator "Kita Eco Goods KK"}} operator))

    (println "== compliance/assess eng-1/green-purchasing (escalates -- human approves) ==")
    (println (exec-op actor "t2" {:op :compliance/assess :subject "eng-1" :track :green-purchasing} operator))
    (println (approve! actor "t2"))

    (println "== filing/draft eng-1/green-purchasing (always escalates -- actuation/draft-filing) ==")
    (let [r (exec-op actor "t3" {:op :filing/draft :subject "eng-1" :track :green-purchasing} operator)]
      (println r)
      (println "-- human operator approves --")
      (println (approve! actor "t3")))

    (println "== filing/submit eng-1/green-purchasing (always escalates -- actuation/submit-filing) ==")
    (let [r (exec-op actor "t4" {:op :filing/submit :subject "eng-1" :track :green-purchasing} operator)]
      (println r)
      (println "-- human operator approves --")
      (println (approve! actor "t4")))

    (println "== compliance/assess eng-1/eco-conscious-contracts + draft + submit (clean, escalates each time) ==")
    (println (exec-op actor "t4b" {:op :compliance/assess :subject "eng-1" :track :eco-conscious-contracts} operator))
    (println (approve! actor "t4b"))
    (println (exec-op actor "t4c" {:op :filing/draft :subject "eng-1" :track :eco-conscious-contracts} operator))
    (println (approve! actor "t4c"))
    (println (exec-op actor "t4d" {:op :filing/submit :subject "eng-1" :track :eco-conscious-contracts} operator))
    (println (approve! actor "t4d"))

    (println "== compliance/assess eng-2/green-purchasing (no spec-basis -> HARD hold) ==")
    (println (exec-op actor "t5" {:op :compliance/assess :subject "eng-2" :track :green-purchasing :no-spec? true} operator))

    (println "== compliance/assess eng-3/green-purchasing (sets up fee-mismatch) ==")
    (println (exec-op actor "t6" {:op :compliance/assess :subject "eng-3" :track :green-purchasing} operator))
    (println (approve! actor "t6"))
    (println (exec-op actor "t6b" {:op :filing/draft :subject "eng-3" :track :green-purchasing} operator))
    (println (approve! actor "t6b"))
    (println "== filing/submit eng-3/green-purchasing (fee mismatch -> HARD hold) ==")
    (println (exec-op actor "t7" {:op :filing/submit :subject "eng-3" :track :green-purchasing} operator))

    (println "== compliance/assess eng-4/green-purchasing (sets up conformity-confirmation-missing) ==")
    (println (exec-op actor "t8" {:op :compliance/assess :subject "eng-4" :track :green-purchasing} operator))
    (println (approve! actor "t8"))
    (println (exec-op actor "t8b" {:op :filing/draft :subject "eng-4" :track :green-purchasing} operator))
    (println (approve! actor "t8b"))
    (println "== filing/submit eng-4/green-purchasing (conformity-confirmation-missing -> HARD hold) ==")
    (println (exec-op actor "t9" {:op :filing/submit :subject "eng-4" :track :green-purchasing} operator))

    (println "== compliance/assess eng-5/eco-conscious-contracts (sets up environmental-documentation-missing) ==")
    (println (exec-op actor "t10" {:op :compliance/assess :subject "eng-5" :track :eco-conscious-contracts} operator))
    (println (approve! actor "t10"))
    (println (exec-op actor "t10b" {:op :filing/draft :subject "eng-5" :track :eco-conscious-contracts} operator))
    (println (approve! actor "t10b"))
    (println "== filing/submit eng-5/eco-conscious-contracts (environmental-documentation-missing -> HARD hold) ==")
    (println (exec-op actor "t11" {:op :filing/submit :subject "eng-5" :track :eco-conscious-contracts} operator))

    (println "== filing/draft eng-1/green-purchasing AGAIN (double-draft -> HARD hold) ==")
    (println (exec-op actor "t12" {:op :filing/draft :subject "eng-1" :track :green-purchasing} operator))

    (println "== filing/submit eng-1/green-purchasing AGAIN (double-submit -> HARD hold) ==")
    (println (exec-op actor "t13" {:op :filing/submit :subject "eng-1" :track :green-purchasing} operator))

    (println "== audit ledger ==")
    (doseq [f (store/ledger db)] (println f))

    (println "== draft records ==")
    (doseq [r (store/draft-history db)] (println r))

    (println "== submit records ==")
    (doseq [r (store/submit-history db)] (println r))))
