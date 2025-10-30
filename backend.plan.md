<!-- ddce408d-5265-4e0c-ac56-f0c693061c95 567a5151-99a5-4cad-9663-d019f4c856ef -->
# Backend-Driven Tariff Calculator Migration (Auto-Select Best Agreement)

## Backend findings (calculation sources)

- Endpoints used by the calculator:
```39:49:apps/backend/src/main/java/com/tariffsheriff/backend/tariff/controller/TariffRateController.java
    @GetMapping("/lookup")
    public TariffRateLookupDto getTariffRateAndAgreement(@RequestParam String importerIso2,
                                                         @RequestParam(required = false) String originIso2,
                                                         @RequestParam String hsCode) {
        return tariffRateService.getTariffRateWithAgreement(importerIso2, originIso2, hsCode);
    }

    @PostMapping("/calculate")
    public TariffCalculationResponse calculateTariffRate(@jakarta.validation.Valid @RequestBody TariffRateRequestDto tariffCalculationData) {
        return tariffRateService.calculateTariffRate(tariffCalculationData);
    }
```

- Calculation logic (agreement selection is NOT automatic server-side; server applies pref rate only if provided and eligible):
```123:148:apps/backend/src/main/java/com/tariffsheriff/backend/tariff/service/TariffRateServiceImpl.java
    public com.tariffsheriff.backend.tariff.dto.TariffCalculationResponse calculateTariffRate(TariffRateRequestDto rq) {
        BigDecimal mfnRate = rq.getMfnRate();
        BigDecimal prefRate = rq.getPrefRate();
        BigDecimal threshold = rq.getRvcThreshold();

        BigDecimal rvc = rq.getMaterialCost()
                .add(rq.getLabourCost())
                .add(rq.getOverheadCost())
                .add(rq.getProfit())
                .add(rq.getOtherCosts())
                .divide(rq.getFob(), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        boolean canApplyPref = prefRate != null && threshold != null && rvc.compareTo(threshold) >= 0;
        BigDecimal appliedRate = (canApplyPref ? prefRate : mfnRate);
        String basis = (canApplyPref ? "PREF" : "MFN");

        BigDecimal totalDuty = rq.getTotalValue().multiply(appliedRate);

        return new com.tariffsheriff.backend.tariff.dto.TariffCalculationResponse(
                basis,
                appliedRate,
                totalDuty,
                rvc,
                threshold
        );
    }
```

- Request DTO (note `agreementId` is accepted but not used by current calculation logic):
```10:29:apps/backend/src/main/java/com/tariffsheriff/backend/tariff/dto/TariffRateRequestDto.java
public class TariffRateRequestDto {
    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal mfnRate;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal prefRate;

    @JsonAlias("rvc")
    private BigDecimal rvcThreshold;

    private Long agreementId;
    private Integer quantity;
    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal totalValue;
    // ... costs ...
}
```


## Frontend changes plan (user no longer selects agreement)

- Remove the "Quick Calculator" feature entirely and its references:
  - Delete `apps/frontend/src/components/dashboard/QuickCalculator.tsx` and any imports/usages in dashboard/nav.

- Calculator page (`apps/frontend/src/pages/Calculator.tsx`)
  - Keep `GET /tariff-rate/lookup` to obtain MFN and preferential options.
  - Remove the agreement selection UI and state (`selectedRateId`, selection buttons). No manual choice.
  - Auto-select best agreement client-side, then call backend to calculate:

1) Compute provisional RVC from costs (existing logic can be reused):

```39:48:apps/frontend/src/pages/Calculator.tsx
function calculateRvc(costs: CostState): number {
  if (costs.fob <= 0) return 0
  const originating =
    costs.materialCost +
    costs.labourCost +
    costs.overheadCost +
    costs.profit +
    costs.otherCosts
  return (originating / costs.fob) * 100
}
```

2) Identify MFN option from lookup (e.g., `basis === 'MFN'` or where `agreementName` is null) and capture its `adValoremRate` as `mfnRate`.

3) Filter preferential options to those eligible: `option.rvcThreshold == null || rvc >= option.rvcThreshold`.

4) Choose the best preferential option as the one with the lowest `adValoremRate`. If none eligible, no preferential option is chosen.

5) Build request to `POST /tariff-rate/calculate`:

       - `mfnRate`: from MFN option.
       - If a best preferential exists: `prefRate = best.adValoremRate`, `rvcThreshold = best.rvcThreshold`, `agreementId = best.agreementId`.
       - Else: omit `prefRate`/`rvcThreshold` (or set to null) so backend applies MFN.
       - Include all cost inputs (`totalValue`, `materialCost`, `labourCost`, `overheadCost`, `profit`, `otherCosts`, `fob`, `nonOriginValue`, `quantity`).

6) Render the backend response `TariffCalculationResponse` and display which basis/agreement was used:

       - If applied basis is MFN, show "Used: MFN".
       - If preferential applied, show "Used: {agreementName}" (from the chosen option in step 4).
  - Remove `computeResult`-based local totals for primary output; always show backend `appliedRate`, `totalDuty`, and backend-computed `rvc`/`rvcThreshold` for eligibility messaging.
  - Keep a small inline panel showing provisional client-side RVC for transparency; label it clearly as "preview" and bind final eligibility to backend values.

- Charts and derived visualizations
  - No changes to client-side math in:
    - `TariffBreakdownChart.tsx`, `CostAnalysisChart.tsx`, `ComparisonChart.tsx`, `HistoricalRatesChart.tsx`.
  - Where available, feed charts with backend outputs (e.g., `appliedRate`, `totalDuty`) instead of local estimates.

- Utilities
  - Keep `apps/frontend/src/lib/utils.ts` helpers (e.g., `calculateTariffTotal`) for UI-only derivations; do not use for primary tariff computation once backend values are wired.

## UX copy and behavior updates

- Replace the agreement selection list with a read-only list of options:
  - Eligible options (by RVC) are listed with their rates; the auto-selected best is highlighted.
  - Ineligible options are shown disabled with a brief reason (e.g., "RVC below threshold").
- Show a badge near results: "Used: MFN" or "Used: {Agreement Name}".

## Acceptance checkpoints

- With identical inputs, Calculator shows backend `appliedRate` and `totalDuty` and the UI clearly indicates the used agreement or MFN.
- No manual selection controls remain on the Calculator.
- Quick Calculator component is removed and no longer referenced.
- Charts render using client-side math but consume backend totals/rates when present.

## Risks/notes

- Current backend ignores `agreementId` in calculation. Passing it is harmless but not required; eligibility and applied rate derive from `prefRate` and `rvcThreshold`.
- If lookup does not explicitly include an MFN entry, derive MFN from the option with `basis === 'MFN'` or fall back to the smallest `adValoremRate` where `agreementName` is null.

### To-dos

- [#] Remove QuickCalculator component and all references
  - Reference: `apps/frontend/src/components/dashboard/QuickCalculator.tsx`
  ```32:36:apps/frontend/src/components/dashboard/QuickCalculator.tsx
  const baseValue = parseFloat(formData.productValue) * parseInt(formData.quantity)
  const tariffRate = 0.125 // 12.5% example rate
  const tariffAmount = baseValue * tariffRate
  const totalCost = baseValue + tariffAmount
  ```

- [#] Refactor Calculator.tsx to remove manual agreement selection UI/state
  - Reference: `apps/frontend/src/pages/Calculator.tsx` selection UI and state
  ```109:118:apps/frontend/src/pages/Calculator.tsx
  const [selectedRateId, setSelectedRateId] = useState<number | null>(null)
  const selection = useMemo(() => {
    if (!lookup) return null
    return resolveSelection(lookup.rates, selectedRateId, rvcPercentage)
  }, [lookup, selectedRateId, rvcPercentage])
  ```
  ```280:321:apps/frontend/src/pages/Calculator.tsx
  {selection?.options?.length ? (
    selection.options.map(({ option, eligible }) => {
      const isSelected = selection?.selected?.id === option.id
      return (
        <button
          key={option.id}
          type="button"
          onClick={() => {
            if (eligible) {
              setSelectedRateId(option.id)
            }
          }}
          className={`w-full rounded-md border px-4 py-3 text-left transition ${
            isSelected
              ? 'border-brand-500 bg-brand-50'
              : 'border-border bg-background'
          } ${eligible ? '' : 'opacity-50 cursor-not-allowed'}`}
          disabled={!eligible}
        >
          ...
        </button>
      )
    })
  ) : (
  ```

- [ ] Compute provisional RVC client-side; display as preview only
  - Reference: `apps/frontend/src/pages/Calculator.tsx`
  ```39:48:apps/frontend/src/pages/Calculator.tsx
  function calculateRvc(costs: CostState): number {
    if (costs.fob <= 0) return 0
    const originating =
      costs.materialCost +
      costs.labourCost +
      costs.overheadCost +
      costs.profit +
      costs.otherCosts
    return (originating / costs.fob) * 100
  }
  ```

- [ ] Auto-select best eligible agreement (minimum ad valorem) from lookup
  - Reference: `GET /tariff-rate/lookup` endpoint
  ```39:46:apps/backend/src/main/java/com/tariffsheriff/backend/tariff/controller/TariffRateController.java
      @GetMapping("/lookup")
      public TariffRateLookupDto getTariffRateAndAgreement(...)
  ```

- [ ] Derive MFN rate from lookup; fall back when no eligible preferential option
  - Reference: `TariffRateOption` fields returned by lookup (basis, adValoremRate, rvcThreshold)

- [ ] Call POST /tariff-rate/calculate with chosen rates and all cost inputs
  - Reference: API endpoint and calculation logic
  ```46:49:apps/backend/src/main/java/com/tariffsheriff/backend/tariff/controller/TariffRateController.java
      @PostMapping("/calculate")
      public TariffCalculationResponse calculateTariffRate(...)
  ```
  ```123:148:apps/backend/src/main/java/com/tariffsheriff/backend/tariff/service/TariffRateServiceImpl.java
  public TariffCalculationResponse calculateTariffRate(TariffRateRequestDto rq) { ... }
  ```
  ```10:29:apps/backend/src/main/java/com/tariffsheriff/backend/tariff/dto/TariffRateRequestDto.java
  public class TariffRateRequestDto { /* mfnRate, prefRate, rvcThreshold, agreementId, costs */ }
  ```

- [ ] Bind backend appliedRate/totalDuty/RVC/rvcThreshold to results UI
  - Reference: `TariffCalculationResponse`
  ```5:11:apps/backend/src/main/java/com/tariffsheriff/backend/tariff/dto/TariffCalculationResponse.java
  public record TariffCalculationResponse(
      String basis,
      BigDecimal appliedRate,
      BigDecimal totalDuty,
      BigDecimal rvc,
      BigDecimal rvcThreshold
  ) {}
  ```

- [ ] Show “Used: MFN” or “Used: {Agreement Name}” in results
  - Reference: agreement names from lookup options; basis from calculation response

- [ ] Feed charts with backend totals/rates when available; keep local math
  - Reference: `apps/frontend/src/components/calculator/TariffBreakdownChart.tsx`, `CostAnalysisChart.tsx`, `ComparisonChart.tsx`, `HistoricalRatesChart.tsx`

- [ ] Audit utils to avoid calculateTariffTotal for primary result
  - Reference: `apps/frontend/src/lib/utils.ts`
  ```72:78:apps/frontend/src/lib/utils.ts
  export function calculateTariffTotal(
    baseValue: number,
    tariffRate: number,
    additionalFees: number = 0
  ): number {
    return baseValue * (1 + tariffRate) + additionalFees
  }
  ```

- [ ] Add error/edge handling (missing MFN, zero FOB, invalid inputs)
  - Reference: current validation snippets in `Calculator.tsx` (e.g., importer/HS required), RVC zero-FOB guard

- [ ] Add tests: eligible pref path, ineligible pref fallback to MFN, no-pref cases
  - Reference: exercise `lookup` flow + `calculate` payload building and response binding