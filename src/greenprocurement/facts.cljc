(ns greenprocurement.facts
  "Japan Ministry of the Environment (環境省, MOE) Green-Purchasing +
  Environmentally-Conscious-Contracts compliance catalog -- the ONLY
  source of regulatory-requirement facts this actor is allowed to cite
  (`greenprocurement.governor`'s spec-basis check enforces that every
  proposal touching `:compliance/assess`, `:filing/draft`, or
  `:filing/submit` cites this catalog and nothing invented).

  Every fact below was verified via web search against `env.go.jp` and
  `gpn.jp` government/industry domains during this repo's research pass
  (2026-07-22/23). Two tracks, each with its own legal basis -- do NOT
  merge them into one undifferentiated 'MOE requirement':

    :green-purchasing        -- グリーン購入法 (Act on Promotion of
                                 Procurement of Eco-Friendly Goods and
                                 Services by the State and Other
                                 Entities, commonly the \"Green
                                 Purchasing Law\"). Formal name: 国等に
                                 よる環境物品等の調達の推進等に関する
                                 法律.
    :eco-conscious-contracts -- 環境配慮契約法 (Act on Promotion of
                                 Contracts Considering the Reduction of
                                 Greenhouse Gas Emissions, etc.,
                                 commonly the \"Environmentally-
                                 Conscious Contracts Law\"). Formal
                                 name: 国等における温室効果ガス等の排出
                                 の削減に配慮した契約の推進に関する
                                 法律.

  Both tracks' Basic Policies (基本方針) were most recently revised by
  the SAME 2023-02-24 Cabinet decision (閣議決定).

  What this catalog deliberately does NOT claim (see README/dossier --
  never inventing a jurisdiction/agency's legal/regulatory requirements
  is this actor family's single most important safety property):
    - no enumeration of the 287 designated procurement items or the 22
      fields' specific names beyond the aggregate counts themselves
      (only the counts -- 22 fields / 287 items -- were verified, not
      an itemized list);
    - no example product categories or item codes (none independently
      verified; inventing illustrative examples in a public OSS repo
      would misrepresent an official government list);
    - no e-procurement portal URL beyond the `env.go.jp` pages cited
      below;
    - no penalty amounts for non-conformity;
    - no compliance deadline beyond the 2023-02-24 Cabinet decision
      date itself;
    - no specific environmental-engineering standard or numeric
      threshold for either the 判断の基準 (criteria for judgment) or
      the 総合評価 (comprehensive evaluation) scoring method -- this
      catalog stays at the compliance-PROCESS level (confirm / document
      / file), never the engineering-standard level.")

(def catalog
  {:green-purchasing
   {:name "グリーン購入法 -- 国等による環境物品等の調達の推進等に関する法律"
    :name-en "Act on Promotion of Procurement of Eco-Friendly Goods and Services by the State and Other Entities (\"Green Purchasing Law\")"
    :owner-authority "環境省 (Ministry of the Environment) -- 基本方針(Basic Policy)は閣議決定"
    :legal-basis "国等による環境物品等の調達の推進等に関する法律"
    :applies-to ["国の機関" "独立行政法人等" "地方公共団体" "事業者"]
    :basic-policy-revision "令和5年度(FY2023)基本方針改正"
    :basic-policy-revision-date "2023-02-24"
    :designated-procurement-fields-count 22
    :designated-procurement-items-count 287
    :official-portal "https://www.env.go.jp/policy/hozen/green/g-law/"
    :provenance "https://www.env.go.jp/policy/hozen/green/g-law/"
    :provenance-secondary
    ["https://www.gpn.jp/info/gpn/684eb268-fa6a-4d2d-a10a-629da29c9f0c"
     "https://www.env.go.jp/content/000064789.pdf"]
    :conformity-confirmation-is-prerequisite? true
    :process-description
    "特定調達品目を製造・輸入・販売する事業者は、当該品目区分の判断の基準(判断基準)への適合を適切に確認することを法が求める -- 判断の基準への適合確認を経ずに適合を主張することはできない。判断の基準の個別数値・品目一覧は本カタログでは扱わない(未検証)。"
    :required-evidence
    ["対象品目が特定調達品目(令和5年度基本方針: 22分野287品目, 2023-02-24閣議決定)に該当するかの確認記録"
     "特定調達品目の判断の基準(判断基準)への適合確認記録"
     "適合確認結果の保管・提出可能な記録形式での保持"]}

   :eco-conscious-contracts
   {:name "環境配慮契約法 -- 国等における温室効果ガス等の排出の削減に配慮した契約の推進に関する法律"
    :name-en "Act on Promotion of Contracts Considering the Reduction of Greenhouse Gas Emissions, etc. (\"Environmentally-Conscious Contracts Law\")"
    :owner-authority "環境省 (Ministry of the Environment) -- 基本方針はグリーン購入法基本方針と同日に閣議決定"
    :legal-basis "国等における温室効果ガス等の排出の削減に配慮した契約の推進に関する法律"
    :basic-policy-revision "グリーン購入法基本方針と同日改正 -- 再生可能エネルギー導入・脱炭素志向の内容を含む"
    :basic-policy-revision-date "2023-02-24"
    :official-portal "https://www.env.go.jp/policy/ga/index.html"
    :provenance "https://www.env.go.jp/policy/ga/index.html"
    :provenance-secondary
    ["https://www.env.go.jp/policy/hozen/green/g-law/setsumeikai.html"]
    :environmental-documentation-is-prerequisite? true
    :process-description
    "対象契約について、価格のみでなく環境性能を含めて総合的に評価(総合評価落札方式)し、価格と環境性能を総合的に評価した結果が最も優れた者と契約することを法が求める -- 価格のみによる落札者決定ではない。総合評価の個別配点・数値基準は本カタログでは扱わない(未検証)。"
    :required-evidence
    ["入札対象契約が環境配慮契約法の対象契約類型に該当するかの確認記録"
     "総合評価(価格+環境性能)に必要な環境性能証明書類の提出・保管記録"
     "総合評価結果(価格評価点+環境性能評価点)の記録"]}})

(def valid-tracks (set (keys catalog)))

(defn spec-basis [track] (get catalog track))

(defn coverage
  ([] (coverage (keys catalog)))
  ([tracks]
   (let [have (filter catalog tracks) missing (remove catalog tracks)]
     {:requested (count tracks) :covered (count have)
      :covered-tracks (vec (sort (map name have)))
      :missing-tracks (vec (sort (map name missing)))
      :note "R0 catalog seed -- Green Purchasing Law + Environmentally-Conscious Contracts Law only, JPN-MOE agency scope"})))

(defn required-evidence-satisfied? [track submitted]
  (when-let [{:keys [required-evidence]} (spec-basis track)]
    (= (count required-evidence) (count (filter (set submitted) required-evidence)))))

(defn evidence-checklist [track] (:required-evidence (spec-basis track) []))

(defn conformity-confirmation-prerequisite-track?
  "Is `track` one whose spec-basis names 判断の基準への適合確認
  (conformity-with-the-criteria-for-judgment confirmation) as a
  documented legal prerequisite before conformity may be claimed? (Only
  `:green-purchasing` today.)"
  [track]
  (boolean (:conformity-confirmation-is-prerequisite? (spec-basis track))))

(defn environmental-documentation-prerequisite-track?
  "Is `track` one whose spec-basis names environmental-performance
  documentation (for the 総合評価/comprehensive-evaluation bid-award
  method) as a documented legal prerequisite before a bid may be
  submitted? (Only `:eco-conscious-contracts` today.)"
  [track]
  (boolean (:environmental-documentation-is-prerequisite? (spec-basis track))))
