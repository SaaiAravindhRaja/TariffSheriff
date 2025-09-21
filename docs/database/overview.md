# Database Overview

Purpose: model tariff data so the backend can answer “what agreement/rate applies between importer I and origin O for product P on date D?”.

Core tables
- `country`: customs territories (importers and origins).
- `agreement`: trade agreement metadata (type, status, entered_into_force).
- `agreement_party`: membership table linking agreements to countries.
- `hs_product`: tariff line catalog per importer destination, HS version, and HS code.
- `tariff_rate`: concrete, date-bounded rates for a product; holds MFN baseline and preferential overrides.
- `vat`: importer VAT for landed-cost calculations.
- `roo_rule`: Rules of Origin hints (method/threshold, certificate required) for agreements per product.

Modeling notes
- Directed edges: treat `tariff_rate` as an importer → origin edge for a given `hs_product` and validity window.
- MFN baseline: `basis='MFN'` with `origin_id NULL` applies to any origin (fallback when no preference applies).
- Preferential override: `basis='PREF'` requires `agreement_id` and `origin_id` and can override MFN if applicable.
- Validity windows: `valid_from` and optional `valid_to`; pick the most recent row effective on date D.
- Rate types: `ad_valorem`, `specific`, `compound` (support mixed charging models).

Where this lives
- Migrations: `apps/backend/src/main/resources/db/migration/` (V1 schema, V2 mock EV seed, V3 consistency checks).
- Test: `apps/backend/src/test/java/com/tariffsheriff/backend/TariffSchemaIntegrationTest.java` (verifies schema + seed).

