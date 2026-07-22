(ns greenprocurement.store
  "SSoT for the JPN-MOE (Ministry of the Environment) compliance actor,
  behind a `Store` protocol so the backend is a swap, not a rewrite --
  the same seam every prior cloud-itonami actor in this fleet uses.

    - `MemStore`     -- atom of EDN. The deterministic default for
                        dev/tests/demo (no deps).
    - `DatomicStore` -- backed by `langchain.db`, a Datomic-API-compatible
                        EAV store, using `langchain-store.core` for the
                        shared EDN-blob codec + event-log helpers
                        instead of a hand-rolled `enc`/`dec*`
                        (ADR-2607141600).

  Both implement the same protocol and pass the same contract
  (test/greenprocurement/store_contract_test.clj).

  The primary entity here is an `engagement` -- one operator's
  compliance engagement carrying BOTH parallel tracks:

    :green-purchasing        -- グリーン購入法 (Green Purchasing Law)
                                 designated-procurement-item conformity
    :eco-conscious-contracts -- 環境配慮契約法 (Environmentally-Conscious
                                 Contracts Law) comprehensive-evaluation
                                 (総合評価) bid readiness

  plus two engagement-level, per-track gating facts grounded in
  `greenprocurement.facts`: `:conformity-confirmed?` (green-purchasing
  track's 判断の基準適合確認) and `:environmental-documentation-
  verified?` (eco-conscious-contracts track's 環境性能証明書類).

  filing-draft and filing-submit actuation events apply per-TRACK to
  the SAME engagement record (draft first, submit later, independently
  for each track). Dedicated double-actuation-guard booleans per track
  (`:gp-drafted?`/`:gp-submitted?`/`:ecc-drafted?`/`:ecc-submitted?`,
  never a single `:status` value).

  The ledger stays append-only on every backend."
  (:require [greenprocurement.registry :as registry]
            [langchain.db :as d]
            [langchain-store.core :as ls]))

(defprotocol Store
  (engagement [s id])
  (all-engagements [s])
  (assessment-of [s engagement-id track] "committed track assessment, or nil")
  (ledger [s])
  (draft-history [s] "the append-only filing-draft history")
  (submit-history [s] "the append-only filing-submit history")
  (next-draft-sequence [s track])
  (next-submit-sequence [s track])
  (engagement-track-drafted? [s engagement-id track])
  (engagement-track-submitted? [s engagement-id track])
  (commit-record! [s record] "apply a committed op's record to the SSoT")
  (append-ledger! [s fact]   "append one immutable decision fact")
  (with-engagements [s engagements] "replace/seed the engagement directory"))

;; ----------------------- track-scoped field mapping -----------------------
;; No dynamic keyword construction -- each track's drafted?/submitted?/
;; draft-number/submit-number fields are explicit, named keys (mirrors
;; the rest of this fleet's explicit-boolean-field style). `:gp`/`:ecc`
;; field prefixes are shorthand for `:green-purchasing` /
;; `:eco-conscious-contracts`.

(def ^:private track-fields
  {:green-purchasing        {:drafted? :gp-drafted?   :draft-number :gp-draft-number
                              :submitted? :gp-submitted? :submit-number :gp-submit-number}
   :eco-conscious-contracts {:drafted? :ecc-drafted?   :draft-number :ecc-draft-number
                              :submitted? :ecc-submitted? :submit-number :ecc-submit-number}})

;; ----------------------------- demo data -----------------------------

(defn demo-data
  "A small, self-contained engagement set covering both actuation
  lifecycles (draft, submit) across both tracks, plus the governor's
  own dossier-grounded checks: a clean case (eng-1, includes the
  compliance-audit export package revenue line), an unregistered-track
  fabrication-defense case (eng-2), a fee-mismatch case (eng-3), a
  missing-conformity-confirmation case (eng-4, `:green-purchasing`
  track), and a missing-environmental-documentation case (eng-5,
  `:eco-conscious-contracts` track)."
  []
  {:engagements
   {"eng-1" {:id "eng-1" :operator "Kita Eco Goods KK" :portal "MOE green-purchasing / eco-conscious-contracts"
             :base-fee 500000 :monthly-rate 30000 :monitoring-months 12
             :audit-export? true :export-fee 100000 :claimed-fee 960000.0
             :requires-conformity-confirmation? true :conformity-confirmed? true
             :requires-environmental-documentation? true :environmental-documentation-verified? true
             :gp-drafted? false :gp-submitted? false
             :ecc-drafted? false :ecc-submitted? false
             :status :intake}
    "eng-2" {:id "eng-2" :operator "Atlantis Sustainable Supply LLC" :portal "MOE green-purchasing / eco-conscious-contracts"
             :base-fee 500000 :monthly-rate 30000 :monitoring-months 12
             :audit-export? true :export-fee 100000 :claimed-fee 960000.0
             :requires-conformity-confirmation? true :conformity-confirmed? true
             :requires-environmental-documentation? true :environmental-documentation-verified? true
             :gp-drafted? false :gp-submitted? false
             :ecc-drafted? false :ecc-submitted? false
             :status :intake}
    "eng-3" {:id "eng-3" :operator "Minami Green Systems KK" :portal "MOE green-purchasing / eco-conscious-contracts"
             :base-fee 500000 :monthly-rate 30000 :monitoring-months 12
             :audit-export? false :export-fee nil :claimed-fee 1200000.0
             :requires-conformity-confirmation? true :conformity-confirmed? true
             :requires-environmental-documentation? true :environmental-documentation-verified? true
             :gp-drafted? false :gp-submitted? false
             :ecc-drafted? false :ecc-submitted? false
             :status :intake}
    "eng-4" {:id "eng-4" :operator "Higashi Eco Products KK" :portal "MOE green-purchasing / eco-conscious-contracts"
             :base-fee 500000 :monthly-rate 30000 :monitoring-months 12
             :audit-export? false :export-fee nil :claimed-fee 860000.0
             :requires-conformity-confirmation? true :conformity-confirmed? false
             :requires-environmental-documentation? true :environmental-documentation-verified? true
             :gp-drafted? false :gp-submitted? false
             :ecc-drafted? false :ecc-submitted? false
             :status :intake}
    "eng-5" {:id "eng-5" :operator "Nishi Environmental Contracting KK" :portal "MOE green-purchasing / eco-conscious-contracts"
             :base-fee 500000 :monthly-rate 30000 :monitoring-months 12
             :audit-export? false :export-fee nil :claimed-fee 860000.0
             :requires-conformity-confirmation? true :conformity-confirmed? true
             :requires-environmental-documentation? true :environmental-documentation-verified? false
             :gp-drafted? false :gp-submitted? false
             :ecc-drafted? false :ecc-submitted? false
             :status :intake}}})

;; ----------------------------- shared commit logic -----------------------------

(defn- draft-filing!
  [s engagement-id track]
  (let [seq-n (next-draft-sequence s track)
        result (registry/register-draft engagement-id track seq-n)
        {:keys [drafted? draft-number]} (get track-fields track)]
    {:result result
     :engagement-patch {drafted? true
                        draft-number (get result "draft_number")}}))

(defn- submit-filing!
  [s engagement-id track]
  (let [seq-n (next-submit-sequence s track)
        result (registry/register-submit engagement-id track seq-n)
        {:keys [submitted? submit-number]} (get track-fields track)]
    {:result result
     :engagement-patch {submitted? true
                        submit-number (get result "submit_number")}}))

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (engagement [_ id] (get-in @a [:engagements id]))
  (all-engagements [_] (sort-by :id (vals (:engagements @a))))
  (assessment-of [_ engagement-id track] (get-in @a [:assessments engagement-id track]))
  (ledger [_] (:ledger @a))
  (draft-history [_] (:draft-records @a))
  (submit-history [_] (:submit-records @a))
  (next-draft-sequence [_ track] (get-in @a [:draft-sequences track] 0))
  (next-submit-sequence [_ track] (get-in @a [:submit-sequences track] 0))
  (engagement-track-drafted? [_ engagement-id track]
    (boolean (get-in @a [:engagements engagement-id (:drafted? (get track-fields track))])))
  (engagement-track-submitted? [_ engagement-id track]
    (boolean (get-in @a [:engagements engagement-id (:submitted? (get track-fields track))])))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :engagement/upsert
      (swap! a update-in [:engagements (:id value)] merge value)

      :assessment/set
      (let [[engagement-id track] path]
        (swap! a assoc-in [:assessments engagement-id track] payload))

      :engagement/mark-drafted
      (let [[engagement-id track] path
            {:keys [result engagement-patch]} (draft-filing! s engagement-id track)]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:draft-sequences track] (fnil inc 0))
                       (update-in [:engagements engagement-id] merge engagement-patch)
                       (update :draft-records registry/append result))))
        result)

      :engagement/mark-submitted
      (let [[engagement-id track] path
            {:keys [result engagement-patch]} (submit-filing! s engagement-id track)]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:submit-sequences track] (fnil inc 0))
                       (update-in [:engagements engagement-id] merge engagement-patch)
                       (update :submit-records registry/append result))))
        result)
      nil)
    s)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-engagements [s engagements] (when (seq engagements) (swap! a assoc :engagements engagements)) s))

(defn seed-db
  "A MemStore seeded with the demo engagement set."
  []
  (->MemStore (atom (assoc (demo-data)
                           :assessments {}
                           :ledger [] :draft-sequences {} :draft-records []
                           :submit-sequences {} :submit-records []))))

;; ----------------------------- DatomicStore (langchain.db) -----------------------------

(def ^:private schema
  {:engagement/id                   {:db/unique :db.unique/identity}
   :assessment/key                  {:db/unique :db.unique/identity}
   :ledger/seq                      {:db/unique :db.unique/identity}
   :draft-record/seq                {:db/unique :db.unique/identity}
   :submit-record/seq               {:db/unique :db.unique/identity}
   :draft-sequence/track            {:db/unique :db.unique/identity}
   :submit-sequence/track           {:db/unique :db.unique/identity}})

(defn- engagement->tx [{:keys [id operator portal base-fee monthly-rate monitoring-months
                               audit-export? export-fee claimed-fee
                               requires-conformity-confirmation? conformity-confirmed?
                               requires-environmental-documentation? environmental-documentation-verified?
                               gp-drafted? gp-draft-number gp-submitted? gp-submit-number
                               ecc-drafted? ecc-draft-number ecc-submitted? ecc-submit-number
                               status]}]
  (cond-> {:engagement/id id}
    operator                              (assoc :engagement/operator operator)
    portal                                (assoc :engagement/portal portal)
    base-fee                              (assoc :engagement/base-fee base-fee)
    monthly-rate                          (assoc :engagement/monthly-rate monthly-rate)
    monitoring-months                     (assoc :engagement/monitoring-months monitoring-months)
    (some? audit-export?)                 (assoc :engagement/audit-export? audit-export?)
    export-fee                            (assoc :engagement/export-fee export-fee)
    claimed-fee                           (assoc :engagement/claimed-fee claimed-fee)
    (some? requires-conformity-confirmation?) (assoc :engagement/requires-conformity-confirmation? requires-conformity-confirmation?)
    (some? conformity-confirmed?)         (assoc :engagement/conformity-confirmed? conformity-confirmed?)
    (some? requires-environmental-documentation?) (assoc :engagement/requires-environmental-documentation? requires-environmental-documentation?)
    (some? environmental-documentation-verified?) (assoc :engagement/environmental-documentation-verified? environmental-documentation-verified?)
    (some? gp-drafted?)                   (assoc :engagement/gp-drafted? gp-drafted?)
    gp-draft-number                       (assoc :engagement/gp-draft-number gp-draft-number)
    (some? gp-submitted?)                 (assoc :engagement/gp-submitted? gp-submitted?)
    gp-submit-number                      (assoc :engagement/gp-submit-number gp-submit-number)
    (some? ecc-drafted?)                  (assoc :engagement/ecc-drafted? ecc-drafted?)
    ecc-draft-number                      (assoc :engagement/ecc-draft-number ecc-draft-number)
    (some? ecc-submitted?)                (assoc :engagement/ecc-submitted? ecc-submitted?)
    ecc-submit-number                     (assoc :engagement/ecc-submit-number ecc-submit-number)
    status                                (assoc :engagement/status status)))

(def ^:private engagement-pull
  [:engagement/id :engagement/operator :engagement/portal :engagement/base-fee :engagement/monthly-rate
   :engagement/monitoring-months :engagement/audit-export? :engagement/export-fee :engagement/claimed-fee
   :engagement/requires-conformity-confirmation? :engagement/conformity-confirmed?
   :engagement/requires-environmental-documentation? :engagement/environmental-documentation-verified?
   :engagement/gp-drafted? :engagement/gp-draft-number
   :engagement/gp-submitted? :engagement/gp-submit-number
   :engagement/ecc-drafted? :engagement/ecc-draft-number
   :engagement/ecc-submitted? :engagement/ecc-submit-number
   :engagement/status])

(defn- pull->engagement [m]
  (when (:engagement/id m)
    {:id (:engagement/id m) :operator (:engagement/operator m) :portal (:engagement/portal m)
     :base-fee (:engagement/base-fee m) :monthly-rate (:engagement/monthly-rate m)
     :monitoring-months (:engagement/monitoring-months m)
     :audit-export? (boolean (:engagement/audit-export? m)) :export-fee (:engagement/export-fee m)
     :claimed-fee (:engagement/claimed-fee m)
     :requires-conformity-confirmation? (boolean (:engagement/requires-conformity-confirmation? m))
     :conformity-confirmed? (boolean (:engagement/conformity-confirmed? m))
     :requires-environmental-documentation? (boolean (:engagement/requires-environmental-documentation? m))
     :environmental-documentation-verified? (boolean (:engagement/environmental-documentation-verified? m))
     :gp-drafted? (boolean (:engagement/gp-drafted? m))
     :gp-draft-number (:engagement/gp-draft-number m)
     :gp-submitted? (boolean (:engagement/gp-submitted? m))
     :gp-submit-number (:engagement/gp-submit-number m)
     :ecc-drafted? (boolean (:engagement/ecc-drafted? m))
     :ecc-draft-number (:engagement/ecc-draft-number m)
     :ecc-submitted? (boolean (:engagement/ecc-submitted? m))
     :ecc-submit-number (:engagement/ecc-submit-number m)
     :status (:engagement/status m)}))

(defn- assessment-key [engagement-id track] (str engagement-id "::" (name track)))

(defrecord DatomicStore [conn]
  Store
  (engagement [_ id]
    (pull->engagement (d/pull (d/db conn) engagement-pull [:engagement/id id])))
  (all-engagements [_]
    (->> (d/q '[:find [?id ...] :where [?e :engagement/id ?id]] (d/db conn))
         (map #(pull->engagement (d/pull (d/db conn) engagement-pull [:engagement/id %])))
         (sort-by :id)))
  (assessment-of [_ engagement-id track]
    (ls/dec* (d/q '[:find ?p . :in $ ?k
                   :where [?a :assessment/key ?k] [?a :assessment/payload ?p]]
                 (d/db conn) (assessment-key engagement-id track))))
  (ledger [_] (ls/read-stream conn :ledger/seq :ledger/fact))
  (draft-history [_] (ls/read-stream conn :draft-record/seq :draft-record/record))
  (submit-history [_] (ls/read-stream conn :submit-record/seq :submit-record/record))
  (next-draft-sequence [_ track]
    (or (d/q '[:find ?n . :in $ ?t
              :where [?e :draft-sequence/track ?t] [?e :draft-sequence/next ?n]]
            (d/db conn) track)
        0))
  (next-submit-sequence [_ track]
    (or (d/q '[:find ?n . :in $ ?t
              :where [?e :submit-sequence/track ?t] [?e :submit-sequence/next ?n]]
            (d/db conn) track)
        0))
  (engagement-track-drafted? [s engagement-id track]
    (boolean (get (engagement s engagement-id) (:drafted? (get track-fields track)))))
  (engagement-track-submitted? [s engagement-id track]
    (boolean (get (engagement s engagement-id) (:submitted? (get track-fields track)))))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :engagement/upsert
      (d/transact! conn [(engagement->tx value)])

      :assessment/set
      (let [[engagement-id track] path]
        (d/transact! conn [{:assessment/key (assessment-key engagement-id track)
                            :assessment/payload (ls/enc payload)}]))

      :engagement/mark-drafted
      (let [[engagement-id track] path
            {:keys [result engagement-patch]} (draft-filing! s engagement-id track)
            next-n (inc (next-draft-sequence s track))]
        (d/transact! conn
                     [(engagement->tx (assoc engagement-patch :id engagement-id))
                      {:draft-sequence/track track :draft-sequence/next next-n}
                      {:draft-record/seq (count (draft-history s)) :draft-record/record (ls/enc (get result "record"))}])
        result)

      :engagement/mark-submitted
      (let [[engagement-id track] path
            {:keys [result engagement-patch]} (submit-filing! s engagement-id track)
            next-n (inc (next-submit-sequence s track))]
        (d/transact! conn
                     [(engagement->tx (assoc engagement-patch :id engagement-id))
                      {:submit-sequence/track track :submit-sequence/next next-n}
                      {:submit-record/seq (count (submit-history s)) :submit-record/record (ls/enc (get result "record"))}])
        result)
      nil)
    s)
  (append-ledger! [s fact]
    (ls/append-blob! conn :ledger/seq :ledger/fact (count (ledger s)) fact)
    fact)
  (with-engagements [s engagements]
    (when (seq engagements) (d/transact! conn (mapv engagement->tx (vals engagements)))) s))

(defn datomic-store
  ([] (datomic-store {}))
  ([{:keys [engagements]}]
   (let [s (->DatomicStore (d/create-conn schema))]
     (with-engagements s engagements))))

(defn datomic-seed-db
  []
  (datomic-store (demo-data)))
