# Database Overview

Purpose: model tariff data so the backend can answer “what agreement/rate applies between importer I and origin O for product P?” (single static snapshot).

Core tables
- `country`: customs territories (importers and origins, keyed by ISO3).
- `agreement`: trade agreement metadata (name + optional RVC threshold).
- `agreement_party`: membership table linking agreements to ISO3 codes.
- `hs_product`: tariff line catalog per importer destination, HS version, and HS code.
- `tariff_rate`: rates for a product, holding MFN baseline and preferential overrides plus optional non-ad-valorem text.

Modeling notes
- Directed edges: treat `tariff_rate` as an importer → origin edge for a given `hs_product`.
- MFN baseline: `basis='MFN'` with `origin_iso3 NULL` applies to any origin (fallback when no preference applies).
- Preferential override: `basis='PREF'` requires `agreement_id` and `origin_iso3` and can override MFN if RVC allows.
- Validity windows removed for now (static 2022 dataset); future migrations can add temporal columns back.
- Rate data: `ad_valorem_rate` plus a boolean/text pair (`is_non_ad_valorem`, `non_ad_valorem_text`) when specific notes exist.

Where this lives
- Migrations: `apps/backend/src/main/resources/db/migration/` (V1 schema, V2 EV sample seed).
- Test: `apps/backend/src/test/java/com/tariffsheriff/backend/TariffSchemaIntegrationTest.java` (verifies schema + seed).
