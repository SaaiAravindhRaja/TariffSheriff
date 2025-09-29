# Usage: Agreement-Aware Lookup
## Spring JPA CRUD against Neon (quick start)

Prereqs
- Set env vars to your Neon dev/prod (JDBC URL with `sslmode=require`, user, password).
- Start the app or use tests that connect to Neon.

Common repository calls
```java
// Resolve HS product id
var productOpt = hsProductRepository.findByDestination_IdAndHsVersionAndHsCode(importerId, "2022", "870380");
var product = productOpt.orElseThrow();

// MFN rate for a date
var mfn = tariffRateRepository.findApplicableMfn(importerId, product.getId(), shipmentDate).stream().findFirst();

// Preferential rate for a date (if origin + agreement apply)
var pref = tariffRateRepository.findApplicablePreferential(importerId, originId, product.getId(), shipmentDate).stream().findFirst();

// VAT by importer
var vat = vatRepository.findByImporter_Id(importerId);
```

Service-layer (recommended)
```java
var product = hsProductService.get(productId);
var mfnList = tariffRateService.findApplicableMfn(importerId, product.getId(), shipmentDate);
var prefList = tariffRateService.findApplicablePreferential(importerId, originId, product.getId(), shipmentDate);
var vat = vatService.getForImporter(importerId);
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
- `importer_id` (I), `origin_id` (O)
- `hs_version`, `hs_code`
- `date` (D)

1) Resolve product (hs_product id)
```sql
SELECT id
FROM hs_product
WHERE destination_id = :I
  AND hs_version = :hs_version
  AND hs_code    = :hs_code;
```

2) List in-force agreements between I and O on D (optional pre-check)
```sql
SELECT a.id, a.name
FROM agreement a
JOIN agreement_party p_i ON p_i.agreement_id = a.id AND p_i.country_id = :I
JOIN agreement_party p_o ON p_o.agreement_id = a.id AND p_o.country_id = :O
WHERE a.status = 'in_force'
  AND (a.entered_into_force IS NULL OR a.entered_into_force <= :D);
```

3) Preferential candidate (PREF override)
```sql
SELECT tr.*, a.name AS agreement_name
FROM tariff_rate tr
JOIN agreement a ON a.id = tr.agreement_id
WHERE tr.importer_id   = :I
  AND tr.origin_id     = :O
  AND tr.hs_product_id = :P
  AND tr.basis         = 'PREF'
  AND tr.valid_from   <= :D
  AND (tr.valid_to IS NULL OR tr.valid_to >= :D)
  AND a.status         = 'in_force'
  AND (a.entered_into_force IS NULL OR a.entered_into_force <= :D)
ORDER BY tr.valid_from DESC
LIMIT 1;
```

4) MFN baseline (fallback)
```sql
SELECT tr.*
FROM tariff_rate tr
WHERE tr.importer_id   = :I
  AND tr.origin_id     IS NULL
  AND tr.hs_product_id = :P
  AND tr.basis         = 'MFN'
  AND tr.valid_from   <= :D
  AND (tr.valid_to IS NULL OR tr.valid_to >= :D)
ORDER BY tr.valid_from DESC
LIMIT 1;
```

5) Build the decision
- If a preferential row exists in step 3, use it; otherwise use MFN from step 4.
- Decision fields: `basis` (PREF|MFN), `rate_type`, `ad_valorem_rate`/`specific_amount`(+`specific_unit`) as applicable,
  `agreement_id/name` (nullable for MFN), `valid_from/to`, `source_ref`.

6) Optional gating and extras
```sql
-- Rules of Origin (flag certificate/threshold)
SELECT *
FROM roo_rule
WHERE agreement_id = :decision.agreement_id
  AND hs_product_id = :P;

-- VAT for importer (combine downstream if needed)
SELECT standard_rate FROM vat WHERE importer_id = :I;
```

Edge cases
- No `hs_product` → return a clear error to the caller (unknown HS line for importer).
- No `PREF` candidate → MFN applies.
- Data consistency is enforced by constraints (e.g., MFN origin must be NULL; PREF requires `agreement_id` and `origin_id`).

Performance
- The following indexes support lookups: `idx_tariff_lookup` on `(importer_id, hs_product_id, valid_from DESC)` and
  `idx_pref_lookup` on `(importer_id, origin_id, hs_product_id, valid_from DESC)`.
- Parameterize queries and cache `hs_product` resolution when appropriate.

