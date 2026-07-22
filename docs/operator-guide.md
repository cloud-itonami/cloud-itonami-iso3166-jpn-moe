# Operator Guide

Implementation: `src/greenprocurement/` (see README.md's Implementation
section for the module map). "the advisor" below is
`greenprocurement.greenprocurementllm`; "the Green Procurement
Compliance Governor" is `greenprocurement.governor`; グリーン購入法
(Green Purchasing Law) and 環境配慮契約法 (Environmentally-Conscious
Contracts Law) are separate `:track`s (`:green-purchasing` /
`:eco-conscious-contracts`) on the same client engagement, assessed and
filed independently.

## First Deployment

1. Confirm the client already uses (or has completed the equivalent of)
   `cloud-itonami-iso3166-jpn` for general Japan market-entry; this repo is
   an agency-specific supplement, not a substitute.
2. Register the client's intake: business type, the specific
   MOE-regulated activity involved (designated-procurement-item sales
   and/or a comprehensive-evaluation contract bid), prior filing/compliance
   history in Japan if any.
3. Run the advisor in read-only mode against Ministry of the Environment's
   (環境省) published Green Purchasing Law / Environmentally-Conscious
   Contracts Law guidance.
4. Compare the checklist against the client's current documentation.
5. Enable gated filing/compliance-draft assistance once the
   Green Procurement Compliance Governor contract is trusted; actual submission always
   requires human sign-off.

## Minimum Production Controls

- client-owned data store for compliance documents
- clear provenance (official MOE source citation) for every
  requirement surfaced
- approval workflow for any filing, registration, or compliance-program
  submission
- named referral relationship with Japan-licensed counsel or a registered
  agent for anything beyond checklist/draft assistance
- monthly audit export

## Certification

Certified operators must prove data provenance, audit traceability, that
automated actions cannot bypass the Green Procurement Compliance Governor, and a working
referral relationship with Japan-licensed counsel or a registered agent for
whatever licensed representation Japanese law requires for actual
MOE filings.
