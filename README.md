# cloud-itonami-iso3166-jpn-moe

Open ISO 3166 Agency Blueprint for **JPN-MOE**: Ministry of the Environment
(環境省, MOE) — a Japan-agency-level LEAF under
the `cloud-itonami-iso3166-jpn` country-level coordinator.

This repository designs a forkable OSS business for an independent
compliance consultant: an already-incorporated operator (typically one
already using `cloud-itonami-iso3166-jpn` for general Japan market entry)
gets a Compliance Advisor + independent **Green Procurement Compliance Governor** to
navigate グリーン購入法 (Green Purchasing Law — designated-procurement-item
conformity to the 判断の基準/criteria for judgment) and 環境配慮契約法
(Environmentally-Conscious Contracts Law — 総合評価/comprehensive-evaluation
contract bids that weigh environmental performance alongside price).

## No robotics premise — digital/data service exemption

Agency-specific compliance navigation is a pure data/software service with
no physical-domain work — the same exemption class as `cloud-itonami-6310`
and `cloud-itonami-gtin-*`. `blueprint.edn` sets
`:itonami.blueprint/robotics false` and `:required-technologies` lists only
real capabilities (`:identity`, `:forms`, `:dmn`, `:bpmn`, `:audit-ledger`),
no `:robotics`.

## Core Contract

```text
operator intake + prior filing/compliance history
        |
        v
Compliance Advisor -> Green Procurement Compliance Governor -> compliance draft, or human sign-off
        |
        v
gated filing / registration / compliance-program submission + audit ledger
```

No automated proposal can submit a filing or registration the governor
refuses, suppress a compliance record, or claim a legal conclusion the
governor has not cleared. `:filing/submit` is never in any phase's `:auto`
set — it always requires human sign-off (mirrors `cloud-itonami-M6910`'s
`filing-submit-never-auto-at-any-phase` invariant).

## Implementation

`src/greenprocurement/` — a langgraph-clj StateGraph actor, same
containment shape as `cloud-itonami-iso3166-ago`'s `marketentry.*` /
`cloud-itonami-iso3166-jpn-digital`'s `digitalprocurement.*` /
`cloud-itonami-iso3166-jpn-mod`'s `defensecompliance.*` (advisor sealed
to proposals-only, independent governor, append-only ledger, `Store`
protocol swap, phase gate):

- `facts.cljc` — the Green Purchasing Law + Environmentally-Conscious
  Contracts Law catalog, the ONLY source of regulatory-requirement
  facts the actor may cite. Two tracks, `:green-purchasing` and
  `:eco-conscious-contracts`, each with its own legal basis. Both
  Basic Policies were most recently revised by the same 2023-02-24
  Cabinet decision; the Green Purchasing Law entry carries only the
  verified aggregate counts (22 fields / 287 designated procurement
  items), never an itemized list or invented example categories.
- `governor.cljc` — the Green Procurement Compliance Governor: a
  spec-basis/no-fabrication HARD check, an evidence-incomplete check, a
  **conformity-confirmation-missing** HARD check (`:green-purchasing`
  track, `:filing/submit` — 判断の基準/criteria-for-judgment conformity
  confirmation is a documented legal prerequisite before conformity may
  be claimed), an **environmental-documentation-missing** HARD check
  (`:eco-conscious-contracts` track, `:filing/submit` — 総合評価/
  comprehensive-evaluation bids require environmental-performance
  documentation on file, not price alone), an independently-recomputed
  engagement-fee-mismatch check (three revenue lines: base fee +
  monitoring subscription + optional audit-export package), a
  confidence-floor/actuation gate, and double-draft/double-submit
  guards, per track.
- `store.cljc` — `MemStore`/`DatomicStore` (via
  `kotoba-lang/langchain-store`, not a hand-rolled `enc`/`dec*`) for
  the `engagement` entity, which tracks the `:green-purchasing` and
  `:eco-conscious-contracts` tracks' filing state independently, plus
  the engagement-level `:conformity-confirmed?` and `:environmental-
  documentation-verified?` gates.
- `registry.cljc` — pure-function filing-draft/filing-submit record
  construction, one sequence per track.
- `greenprocurementllm.cljc` — the Compliance Advisor (mock LLM,
  proposals only).
- `operation.cljc` — the StateGraph: intake → advise → govern → decide
  → [request-approval →] commit/hold, `interrupt-before` on human
  approval.
- `phase.cljc` — phase 0→3 rollout; `:filing/draft`/`:filing/submit`
  are permanently absent from every phase's `:auto` set.

Ops: `:engagement/intake`, `:compliance/assess` (per-track evidence
checklist), `:filing/draft`, `:filing/submit` — the latter three take a
`:track` (`:green-purchasing` or `:eco-conscious-contracts`) in the
request, since one engagement runs both tracks independently.

## What this is NOT

- **Not Ministry of the Environment (環境省) itself, and not the
  government of Japan.** See [`docs/business-model.md`](docs/business-model.md)
  for the boundary with `com-etzhayyim-ooyake`, `matsurigoto`,
  `com-etzhayyim-toritsugi`, `legal-entity.etzhayyim.com`,
  `cloud-itonami-M6910`, and the country-level `cloud-itonami-iso3166-jpn`.
- **Not legal or tax advice.** Every regulatory claim must cite the
  official MOE source and route final filings to
  Japan-licensed counsel or a registered agent where the law requires
  licensed representation.

## Capability layer

Resolves via [`kotoba-lang/iso3166`](https://github.com/kotoba-lang/iso3166)
(code `JPN-MOE`, `:parent "JPN"`, cross-referenced to ooyake's
`gov.jpn.moe`). Required capabilities:

- :identity
- :forms
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
