# Business Model: Independent MOE Environmental-Assessment & Waste-Permit Compliance Service — Japan (MOE)

## Classification

- Repository: `cloud-itonami-iso3166-jpn-moe`
- ISO 3166 (agency-level): `JPN-MOE`, parent `JPN`
- Ooyake cross-reference: `gov.jpn.moe` (Ministry of the Environment / 環境省)
- Activity: environmental impact assessment (環境影響評価法/環境アセスメント) requirements for a public infrastructure project, and waste-management permit (廃棄物処理法) compliance for an operator handling collection, transport, or disposal under a public contract (complementing cloud-itonami-cofog-05.1's waste-collection theme with the national regulatory-agency side)
- Social impact: [:environmental-review-clarity :waste-permit-access :public-spend-transparency]

## Customer

- an operator whose public infrastructure project triggers an environmental impact assessment (環境アセスメント) requirement
- an operator handling waste collection, transport, or disposal under a public contract requiring a Waste Management Act (廃棄物処理法) permit
- a foreign environmental-services vendor confirming MOE permit prerequisites before bidding

## Offer

- environmental impact assessment (環境アセスメント) requirement-screening walkthrough
- Waste Management Act (廃棄物処理法) permit classification and application checklist
- ongoing regulatory-change monitoring for MOE standard updates
- compliance-audit export package for the operator's own records

## Revenue

- per-engagement compliance-review fee
- recurring regulatory-change monitoring subscription
- compliance-audit export package

## Trust Controls

- any actual filing, registration, or compliance-program submission
  requires Environmental Compliance Governor clearance and always escalates to human
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
- **`cloud-itonami-cofog-05.1`** (Independent Municipal Waste Collection
  Robotics, MOE blueprint only): a jurisdiction-agnostic LOCAL operator
  template for the waste-collection function itself. This blueprint is the
  NATIONAL regulatory-agency side (MOE permits) an operator needs
  regardless of which municipality it serves.
