(ns greenprocurement.governor
  "Green Procurement Compliance Governor -- the independent compliance
  layer that earns the GreenProcurement-LLM the right to commit. The
  LLM has no notion of what グリーン購入法 (Green Purchasing Law) or
  環境配慮契約法 (Environmentally-Conscious Contracts Law) actually
  require, whether a designated-procurement-item's conformity to the
  判断の基準 (criteria for judgment) is actually confirmed, whether the
  environmental-performance documentation a 総合評価 (comprehensive-
  evaluation) contract bid needs is actually on file, whether a claimed
  engagement fee actually equals base + months x rate (+ optional
  export package), or when a draft stops being a draft and becomes a
  real-world MOE filing, so this MUST be a separate system able to
  *reject* a proposal and fall back to HOLD.

  `:itonami.blueprint/governor` is `:green-procurement-compliance-governor`
  (blueprint.edn).

  This blueprint's own text (docs/business-model.md Trust Controls:
  'any actual filing, registration, or compliance-program submission
  requires Green Procurement Compliance Governor clearance and always
  escalates to human sign-off'; 'a false or fabricated regulatory-
  requirement claim is a HARD hold that cannot be overridden by human
  approval alone') names exactly the checks below.

  Seven checks, in priority order, ALL HARD violations except the
  confidence/actuation gate: a human approver CANNOT override the hard
  ones. The confidence/actuation gate is SOFT: it asks a human to look
  (low confidence / actuation), and the human may approve -- but see
  `greenprocurement.phase`: for `:stake :actuation/draft-filing`/
  `:actuation/submit-filing` NO phase ever allows auto-commit either.
  Two independent layers agree that actuation is always a human call.

    1. Spec-basis                     -- did the compliance-track
                                          proposal cite an OFFICIAL
                                          source
                                          (`greenprocurement.facts`), or
                                          invent one?
    2. Evidence incomplete            -- for `:filing/draft`/
                                          `:filing/submit`, has the
                                          track actually been assessed
                                          with a full evidence checklist
                                          on file?
    3. Conformity confirmation missing -- for `:filing/submit` on the
                                          `:green-purchasing` track,
                                          when the engagement declares
                                          `:requires-conformity-
                                          confirmation? true`,
                                          INDEPENDENTLY verify
                                          `:conformity-confirmed?` is
                                          true. グリーン購入法上、特定調
                                          達品目の製造者・輸入者・販売者
                                          は、判断の基準への適合を適切
                                          に確認することを法が求める
                                          (env.go.jp) -- a genuine legal
                                          prerequisite before conformity
                                          may be claimed in a filing.
    4. Environmental documentation
       missing                        -- for `:filing/submit` on the
                                          `:eco-conscious-contracts`
                                          track, when the engagement
                                          declares `:requires-
                                          environmental-documentation?
                                          true`, INDEPENDENTLY verify
                                          `:environmental-documentation-
                                          verified?` is true. 環境配慮契
                                          約法は、総合評価落札方式の対象
                                          契約について、価格と環境性能
                                          を総合的に評価することを求め
                                          る (env.go.jp) -- submitting a
                                          comprehensive-evaluation bid
                                          without the environmental-
                                          performance documentation on
                                          file is structurally
                                          incomplete, not merely risky.
    5. Engagement fee mismatch        -- for `:filing/submit`,
                                          INDEPENDENTLY recompute
                                          whether the engagement's own
                                          `:claimed-fee` equals `base-
                                          fee + monthly-rate x
                                          monitoring-months` (+ optional
                                          export-fee when `:audit-
                                          export?` is true) -- honest
                                          reapplication of the ground-
                                          truth-recompute discipline
                                          sibling actors use, matched
                                          against this repo's own three
                                          revenue lines (per-engagement
                                          compliance-review fee +
                                          recurring monitoring
                                          subscription + compliance-
                                          audit export package).
    6. Confidence floor / actuation
       gate                             -- LLM confidence below
                                          threshold, OR the op is
                                          `:filing/draft`/
                                          `:filing/submit` (REAL acts)
                                          -> escalate.

  Two more guards, double-draft/double-submit prevention, are enforced
  off dedicated per-track `:gp-drafted?`/`:gp-submitted?`/
  `:ecc-drafted?`/`:ecc-submitted?` facts (never a `:status` value)."
  (:require [greenprocurement.facts :as facts]
            [greenprocurement.registry :as registry]
            [greenprocurement.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Drafting a real MOE compliance filing package and submitting a real
  MOE filing are the two real-world actuation events this actor
  performs."
  #{:actuation/draft-filing :actuation/submit-filing})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:compliance/assess` (or `:filing/draft`/`:filing/submit`)
  proposal with no spec-basis citation is a HARD violation -- never
  invent MOE's Green-Purchasing or Environmentally-Conscious-Contracts
  requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:compliance/assess :filing/draft :filing/submit} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案はコンプライアンス要件として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:filing/draft`/`:filing/submit`, the track's required
  evidence checklist must actually be satisfied."
  [{:keys [op subject track]} st]
  (when (contains? #{:filing/draft :filing/submit} op)
    (let [assessment (store/assessment-of st subject track)]
      (when-not (and assessment
                     (facts/required-evidence-satisfied?
                      track (:checklist assessment)))
        [{:rule :evidence-incomplete
          :detail (str subject "/" (name track) " の必要書類が充足していない状態での提案")}]))))

(defn- conformity-confirmation-missing-violations
  "For `:filing/submit` on the `:green-purchasing` track, when the
  engagement declares `:requires-conformity-confirmation? true`,
  INDEPENDENTLY verify `:conformity-confirmed?` is true -- 判断の基準へ
  の適合確認は、グリーン購入法が求める genuine legal prerequisite.
  CONDITIONAL on the engagement's own ground truth."
  [{:keys [op subject track]} st]
  (when (and (= op :filing/submit) (= track :green-purchasing))
    (let [e (store/engagement st subject)]
      (when (and (true? (:requires-conformity-confirmation? e))
                 (not (true? (:conformity-confirmed? e))))
        [{:rule :conformity-confirmation-missing
          :detail (str subject " は特定調達品目の判断の基準への適合確認が未完了 -- 提出提案は進められない")}]))))

(defn- environmental-documentation-missing-violations
  "For `:filing/submit` on the `:eco-conscious-contracts` track, when
  the engagement declares `:requires-environmental-documentation?
  true`, INDEPENDENTLY verify `:environmental-documentation-verified?`
  is true. 環境配慮契約法の総合評価落札方式は、環境性能証明書類を伴う
  総合評価を求める -- submitting without it is structurally incomplete."
  [{:keys [op subject track]} st]
  (when (and (= op :filing/submit) (= track :eco-conscious-contracts))
    (let [e (store/engagement st subject)]
      (when (and (true? (:requires-environmental-documentation? e))
                 (not (true? (:environmental-documentation-verified? e))))
        [{:rule :environmental-documentation-missing
          :detail (str subject " は総合評価に必要な環境性能証明書類が未確認 -- 提出提案は進められない")}]))))

(defn- engagement-fee-mismatch-violations
  "For `:filing/submit`, INDEPENDENTLY recompute whether the
  engagement's own claimed fee equals base + months x rate (+ optional
  export-fee)."
  [{:keys [op subject]} st]
  (when (= op :filing/submit)
    (let [e (store/engagement st subject)]
      (when-not (registry/engagement-fee-matches-claim? e)
        [{:rule :engagement-fee-mismatch
          :detail (str subject " の申告手数料(" (:claimed-fee e)
                      ")が独立再計算値(" (registry/compute-engagement-fee e) ")と一致しない")}]))))

(defn- already-drafted-violations
  "For `:filing/draft`, refuses to draft the SAME engagement/track
  twice."
  [{:keys [op subject track]} st]
  (when (= op :filing/draft)
    (when (store/engagement-track-drafted? st subject track)
      [{:rule :already-drafted
        :detail (str subject "/" (name track) " は既にドラフト済み")}])))

(defn- already-submitted-violations
  "For `:filing/submit`, refuses to submit the SAME engagement/track
  twice."
  [{:keys [op subject track]} st]
  (when (= op :filing/submit)
    (when (store/engagement-track-submitted? st subject track)
      [{:rule :already-submitted
        :detail (str subject "/" (name track) " は既に提出済み")}])))

(defn check
  "Censors a GreenProcurement-LLM proposal against the governor rules.
  Returns {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (conformity-confirmation-missing-violations request st)
                           (environmental-documentation-missing-violations request st)
                           (engagement-fee-mismatch-violations request st)
                           (already-drafted-violations request st)
                           (already-submitted-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :track      (:track request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
