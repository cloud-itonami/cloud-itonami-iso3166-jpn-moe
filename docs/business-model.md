# Business Model: Independent MOE Green-Purchasing & Environmentally-Conscious-Contracts Compliance Service — Japan (MOE)

Implementation: `src/greenprocurement/` — see README.md's
Implementation section. The Trust Controls below are enforced in code
by `greenprocurement.governor` (spec-basis/no-fabrication HARD check,
conformity-confirmation-missing HARD check, environmental-
documentation-missing HARD check, engagement-fee-mismatch check,
confidence-floor/actuation gate, double-draft/double-submit guards)
and `greenprocurement.phase` (`:filing/submit` absent from every
phase's `:auto` set).

## Classification

- Repository: `cloud-itonami-iso3166-jpn-moe`
- ISO 3166 (agency-level): `JPN-MOE`, parent `JPN`
- Ooyake cross-reference: `gov.jpn.moe` (Ministry of the Environment / 環境省)
- Activity: グリーン購入法 (Green Purchasing Law) designated-procurement-item
  conformity confirmation for an operator selling into a category covered
  by MOE's Basic Policy (22 fields / 287 items per the 2023-02-24 Cabinet
  decision), and 環境配慮契約法 (Environmentally-Conscious Contracts Law)
  総合評価 (comprehensive-evaluation) contract-bid documentation for an
  operator bidding on a public contract where price and environmental
  performance are evaluated together
- Social impact: [:green-procurement-clarity :sustainable-contracting-access :public-spend-transparency]

## Customer

- a manufacturer, importer, or seller of goods/services that may fall
  under a グリーン購入法 designated-procurement-item category, confirming
  conformity to the 判断の基準 (criteria for judgment) before claiming it
- an operator bidding on a public contract subject to 環境配慮契約法's
  総合評価落札方式 (comprehensive-evaluation bid-award method), preparing
  the environmental-performance documentation the evaluation requires
- a foreign eco-friendly-goods vendor confirming MOE Green Purchasing Law
  prerequisites before bidding into a Japanese public-sector contract

## Offer

- グリーン購入法 designated-procurement-item conformity-confirmation
  walkthrough (判断の基準 checklist)
- 環境配慮契約法 総合評価 (comprehensive-evaluation) bid-documentation
  checklist
- ongoing regulatory-change monitoring for MOE Basic Policy revisions
- compliance-audit export package for the operator's own records

## Revenue

- per-engagement compliance-review fee
- recurring regulatory-change monitoring subscription
- compliance-audit export package

## Trust Controls

- any actual filing, registration, or compliance-program submission
  requires Green Procurement Compliance Governor clearance and always escalates to human
  sign-off (`:filing/submit` is never automated at any phase)
- a false or fabricated regulatory-requirement claim is a HARD hold that
  cannot be overridden by human approval alone — it must be corrected
  against a cited MOE source first
- this service does **not** provide legal or tax advice; characterization
  and filing on the client's behalf beyond checklist/draft assistance
  routes to Japan-licensed counsel or a registered agent
- every requirement cites the official MOE source or
  regulation, never invented

## Boundary with adjacent actors (read before forking)

- **`cloud-itonami-iso3166-jpn`**: the COUNTRY-level coordinator (general
  Japan public-sector market entry). This repo is a narrower, deeper
  AGENCY-level leaf — most operators need the country-level blueprint plus
  only the agency-level blueprints that actually apply to their contract.
- **`com-etzhayyim-ooyake`** (etzhayyim/root): read-only civic-wayfinding
  mirror of government structure, non-commercial, barred from acting as or
  for the government (G3 impersonation ban). This blueprint is commercial
  and never claims to be Ministry of the Environment or an official channel.
- **`matsurigoto`** (etzhayyim/root): sovereign e-government statecraft —
  literally the government. This blueprint is an independent operator that
  engages with MOE under its public rules — never the
  agency itself.
- **`com-etzhayyim-toritsugi`** (etzhayyim/root): guides a consenting
  INDIVIDUAL citizen through their OWN procedure, non-profit,
  donation-only. This blueprint's client is a business operator, not an
  individual citizen, and it is commercial.
- **`cloud-itonami-M6910`**: helps a client BECOME a legal entity
  (incorporation, ISIC 6910) — a prior, different regulatory phase (company
  law). This blueprint assumes incorporation is already done and handles
  MOE-specific compliance (a different regulatory domain).

## Domain note (reconciled during implementation)

This blueprint's original text (pre-implementation) described 環境影響評価法
(environmental impact assessment) and 廃棄物処理法 (waste-management permit)
compliance, complementing `cloud-itonami-cofog-05.1`'s waste-collection
theme. The verified research dossier available for the implementation pass
that produced `src/greenprocurement/` covered only グリーン購入法 (Green
Purchasing Law) and 環境配慮契約法 (Environmentally-Conscious Contracts Law)
against `env.go.jp`/`gpn.jp` sources — this actor family's governing
principle (see Trust Controls above) is to never model a regulatory
requirement without a verified source, so the implemented scope followed
the dossier rather than inventing impact-assessment/waste-permit specifics
with no verified sourcing. A future pass with a verified dossier for
environmental impact assessment and/or waste-management permits could add
those as additional tracks in `greenprocurement.facts`, or as a sibling
blueprint.
