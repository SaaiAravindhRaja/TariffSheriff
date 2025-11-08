# Usage: Agreement-Aware Lookup
## Spring JPA CRUD against Neon (quick start)

Prereqs
- Set env vars to your Neon dev/prod (JDBC URL with `sslmode=require`, user, password).
- Start the app or use tests that connect to Neon.

Common repository calls
```java
// Resolve HS product id
var productOpt = hsProductRepository.findByDestinationIso3IgnoreCaseAndHsCode("USA", "870380");
var product = productOpt.orElseThrow();

// MFN rate for a date
var mfn = tariffRateRepository.findApplicableMfn("USA", product.getId(), shipmentDate).stream().findFirst();

// Preferential rate for a date (if origin + agreement apply)
var pref = tariffRateRepository.findApplicablePreferential("USA", "MEX", product.getId(), shipmentDate).stream().findFirst();

```

Service-layer (recommended)
```java
var product = hsProductService.get(productId);
var mfnList = tariffRateService.findApplicableMfn("USA", product.getId(), shipmentDate);
var prefList = tariffRateService.findApplicablePreferential("USA", "MEX", product.getId(), shipmentDate);
```

Pagination & sorting
```java
var page = countryService.searchByName("uni", PageRequest.of(0, 20, Sort.by("name").ascending()));
```

Transactions
- Reads are `@Transactional(readOnly = true)` by default in services.
- Writes are wrapped in `@Transactional` so errors roll back the whole operation.


Goal: decide which rate/agreement applies for importer I, origin O, HS (version/code) on date D, returning a single decision the backend can use.

Inputs
- `importer_iso3` (I), `origin_iso3` (O)
- `hs_version`, `hs_code`
- `date` (D)

1) Resolve product (hs_product id)
```sql
SELECT id
FROM hs_product
WHERE destination_iso3 = :I
  AND hs_version = :hs_version
  AND hs_code    = :hs_code;
```

2) List in-force agreements between I and O on D (optional pre-check)
```sql
SELECT a.id, a.name
FROM agreement a
JOIN agreement_party p_i ON p_i.agreement_id = a.id AND p_i.country_iso3 = :I
JOIN agreement_party p_o ON p_o.agreement_id = a.id AND p_o.country_iso3 = :O
WHERE a.status = 'in_force'
  AND (a.entered_into_force IS NULL OR a.entered_into_force <= :D);
```

3) Preferential candidate (PREF override)
```sql
SELECT tr.*, a.name AS agreement_name
FROM tariff_rate tr
JOIN agreement a ON a.id = tr.agreement_id
WHERE tr.importer_iso3 = :I
  AND tr.origin_iso3   = :O
  AND tr.hs_product_id = :P
  AND tr.basis         = 'PREF'
ORDER BY tr.id DESC
LIMIT 1;
```

4) MFN baseline (fallback)
```sql
SELECT tr.*
FROM tariff_rate tr
WHERE tr.importer_iso3 = :I
  AND tr.origin_iso3   IS NULL
  AND tr.hs_product_id = :P
  AND tr.basis         = 'MFN'
ORDER BY tr.id DESC
LIMIT 1;
```

5) Build the decision
- If a preferential row exists in step 3, use it; otherwise use MFN from step 4.
- Decision fields: `basis` (PREF|MFN), `ad_valorem_rate`, `is_non_ad_valorem`, `non_ad_valorem_text`,
  `agreement_id/name` (nullable for MFN), `source_ref`.

Edge cases
- No `hs_product` → return a clear error to the caller (unknown HS line for importer).
- No `PREF` candidate → MFN applies.
- Data consistency is enforced by constraints (e.g., MFN origin must be NULL; PREF requires `agreement_id` and `origin_iso3`).

Performance
- The following indexes support lookups: `idx_tariff_lookup_iso3` on `(importer_iso3, hs_product_id)` and
  `idx_pref_lookup_iso3` on `(importer_iso3, origin_iso3, hs_product_id)`.
- Parameterize queries and cache `hs_product` resolution when appropriate.
