import { useEffect, useMemo, useRef, useState } from 'react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import CountrySelect from '@/components/inputs/CountrySelect'
import { useDbCountries } from '@/hooks/useDbCountries'
import { useSettings } from '@/contexts/SettingsContext'
import { tariffApi, savedTariffsApi, type TariffLookupResponse, type TariffRateOption } from '@/services/api'
import { TariffBreakdownChart } from '@/components/calculator/TariffBreakdownChart'
import { formatCurrency } from '@/lib/utils'
import SavedTariffs from '@/pages/SavedTariffs'

type CostField =
  | 'totalValue'
  | 'materialCost'
  | 'labourCost'
  | 'overheadCost'
  | 'profit'
  | 'otherCosts'
  | 'fob'
  | 'nonOriginValue'

type CostState = Record<CostField, number>

const initialCosts: CostState = {
  totalValue: 0,
  materialCost: 0,
  labourCost: 0,
  overheadCost: 0,
  profit: 0,
  otherCosts: 0,
  fob: 0,
  nonOriginValue: 0,
}

function parseNumber(value: string): number {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) return 0
  return parsed < 0 ? 0 : parsed
}


function resolveSelection(
  options: TariffRateOption[],
  selectedId: number | null,
  rvc: number,
) {
  if (!options.length) return null

  const withEligibility = options.map((option) => ({
    option,
    eligible:
      option.rvcThreshold === null ||
      option.rvcThreshold === undefined ||
      rvc >= option.rvcThreshold,
  }))

  const selectedEntry = withEligibility.find(({ option }) => option.id === selectedId)
  const eligibleSelected = selectedEntry && selectedEntry.eligible ? selectedEntry.option : null
  const fallbackEntry = eligibleSelected ? null : withEligibility.find(({ eligible }) => eligible)
  const fallbackOption = fallbackEntry?.option ?? withEligibility[0]?.option ?? null
  const chosen = eligibleSelected ?? fallbackOption
  const usedFallback = Boolean(chosen && chosen.id !== selectedId)

  return {
    selected: chosen,
    options: withEligibility,
    usedFallback,
  }
}

function computeResult(
  selection: TariffRateOption | null,
  costs: CostState,
  rvc: number,
) {
  if (!selection) {
    return null
  }

  const appliedRate = selection.adValoremRate ?? 0
  return {
    basis: selection.basis,
    appliedRate,
    totalDuty: costs.totalValue * appliedRate,
    rvc,
    rvcThreshold: selection.rvcThreshold ?? null,
  }
}

export function Calculator() {
  const { settings } = useSettings()
  const { countries, loading: countriesLoading, error: countriesError } = useDbCountries()

  const [form, setForm] = useState({
    importerIso3: '',
    originIso3: '',
    hsCode: '',
  })
  const [costs, setCosts] = useState<CostState>(initialCosts)
  const [lookup, setLookup] = useState<TariffLookupResponse | null>(null)
  // Manual agreement selection removed; selection is now auto-determined
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [simulatedValue, setSimulatedValue] = useState<number | null>(null)

  const [simulatedRate, setSimulatedRate] = useState<number | null>(null)
  const [isSimulating, setIsSimulating] = useState<boolean>(false)
  const [backendRvc, setBackendRvc] = useState<number>(0)
  const [calcResult, setCalcResult] = useState<{
    basis: string
    appliedRate: number
    totalDuty: number
    rvc: number
    rvcThreshold: number | null
  } | null>(null)
  const [saveName, setSaveName] = useState<string>("")
  const [saveNotes, setSaveNotes] = useState<string>("")
  const [saving, setSaving] = useState<boolean>(false)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [usedAgreementName, setUsedAgreementName] = useState<string | null>(null)
  const rvcDebounceRef = useRef<number | null>(null)
  const phase1AbortRef = useRef<AbortController | null>(null)
  const phase2AbortRef = useRef<AbortController | null>(null)
  const phase1ResultRef = useRef<any>(null)

  // Minimal ISO conversion helper (iso3 -> iso2); extend as needed
  const iso3ToIso2 = (code?: string | null): string | undefined => {
    if (!code) return undefined
    const map: Record<string, string> = {
      USA: 'US',
      CHL: 'CL',
      CHN: 'CN',
      KOR: 'KR',
      JPN: 'JP',
      CAN: 'CA',
      MEX: 'MX',
      AUS: 'AU',
      NZL: 'NZ',
      GBR: 'GB',
      DEU: 'DE',
      FRA: 'FR',
      ITA: 'IT',
      ESP: 'ES',
      PRT: 'PT',
      NLD: 'NL',
      SWE: 'SE',
      NOR: 'NO',
      DNK: 'DK',
      FIN: 'FI',
      EU: 'EU',
      EUR: 'EU',
    }
    const c = code.trim().toUpperCase()
    return map[c] || undefined
  }


  // Helper to find MFN ad valorem rate from lookup
  const mfnRate = useMemo(() => {
    if (!lookup?.rates?.length) return 0
    const candidates = lookup.rates.filter(
      (r) => (r as any).basis === 'MFN' || (r as any).agreementName == null
    )
    if (candidates.length > 0) {
      return candidates.reduce((min, cur) => {
        const val = Number(cur.adValoremRate ?? 0)
        return val < min ? val : min
      }, Number(candidates[0].adValoremRate ?? 0))
    }
    // Fallback: take the smallest ad valorem across all returned rates
    return lookup.rates.reduce((min, cur) => {
      const val = Number(cur.adValoremRate ?? 0)
      return val < min ? val : min
    }, Number(lookup.rates[0].adValoremRate ?? 0))
  }, [lookup])

  // Fetch backend-computed RVC whenever lookup or costs change
  useEffect(() => {
    if (!lookup) return
    if (rvcDebounceRef.current) {
      window.clearTimeout(rvcDebounceRef.current)
    }
    rvcDebounceRef.current = window.setTimeout(async () => {
      try {
        // Guard: don't call backend while FOB is invalid
        const originatingSum =
          costs.materialCost +
          costs.labourCost +
          costs.overheadCost +
          costs.profit +
          costs.otherCosts
        if (costs.fob <= 0 || originatingSum > costs.fob) {
          setBackendRvc(0)
          phase1ResultRef.current = null
          setCalcResult(null)
          setUsedAgreementName(null)
          return
        }
        // cancel previous in-flight phase1 request
        if (phase1AbortRef.current) {
          phase1AbortRef.current.abort()
        }
        const controller = new AbortController()
        phase1AbortRef.current = controller
        const response = await tariffApi.calculateTariff({
          mfnRate: mfnRate || 0,
          prefRate: 0,
          rvcThreshold: undefined,
          agreementId: undefined,
          quantity: 0,
          totalValue: costs.totalValue,
          materialCost: costs.materialCost,
          labourCost: costs.labourCost,
          overheadCost: costs.overheadCost,
          profit: costs.profit,
          otherCosts: costs.otherCosts,
          fob: costs.fob,
          nonOriginValue: costs.nonOriginValue,
        }, { signal: controller.signal })
        const rvc = Number(response?.data?.rvc ?? 0)
        setBackendRvc(Number.isFinite(rvc) ? rvc : 0)
        phase1ResultRef.current = response?.data ?? null
      } catch (err: any) {
        if (err?.name === 'CanceledError' || err?.code === 'ERR_CANCELED') {
          return
        }
        setBackendRvc(0)
        phase1ResultRef.current = null
      }
    }, 300)
    return () => {
      if (rvcDebounceRef.current) {
        window.clearTimeout(rvcDebounceRef.current)
      }
      if (phase1AbortRef.current) {
        phase1AbortRef.current.abort()
        phase1AbortRef.current = null
      }
    }
  }, [lookup, costs, mfnRate])

  // Determine best eligible option (minimum ad valorem among eligible); prefer MFN when none eligible
  const bestOption = useMemo(() => {
    if (!lookup?.rates?.length) return null as TariffRateOption | null
    const eligible = lookup.rates.filter((opt) => {
      const thr = (opt as any).rvcThreshold
      return thr == null || backendRvc >= (thr as number)
    })
    if (eligible.length === 0) {
      const mfn = lookup.rates.find(
        (r) => (r as any).basis === 'MFN' || (r as any).agreementName == null
      )
      return mfn ?? lookup.rates[0]
    }
    return eligible.reduce((min, cur) =>
      (cur.adValoremRate ?? 0) < (min.adValoremRate ?? 0) ? cur : min
      , eligible[0])
  }, [lookup, backendRvc])

  const selection = useMemo(() => {
    if (!lookup) return null
    const selectedId = bestOption?.id ?? null
    return resolveSelection(lookup.rates, selectedId, backendRvc)
  }, [lookup, backendRvc, bestOption])

  // Phase 2: Final calculation using best option
  useEffect(() => {
    const run = async () => {
      if (!lookup) return
      if (!bestOption) {
        setCalcResult(null)
        setUsedAgreementName(null)
        return
      }
      const originatingSum =
        costs.materialCost +
        costs.labourCost +
        costs.overheadCost +
        costs.profit +
        costs.otherCosts
      if (costs.fob <= 0 || originatingSum > costs.fob) {
        // Don't attempt final calc when FOB invalid
        setCalcResult(null)
        return
      }
      const isMfn = (bestOption as any).basis === 'MFN' || (bestOption as any).agreementName == null
      if (isMfn) {
        // Reuse phase 1 response
        const data = phase1ResultRef.current
        if (data) {
          setCalcResult({
            basis: data.basis,
            appliedRate: Number(data.appliedRate ?? 0),
            totalDuty: Number(data.totalDuty ?? 0),
            rvc: Number(data.rvc ?? backendRvc),
            rvcThreshold: data.rvcThreshold ?? null,
          })
          setUsedAgreementName('Most Favoured Nation')
        } else {
          // Show a provisional MFN result immediately while backend RVC is loading
          setCalcResult({
            basis: 'MFN',
            appliedRate: mfnRate || 0,
            totalDuty: costs.totalValue * (mfnRate || 0),
            rvc: backendRvc || 0,
            rvcThreshold: null,
          })
          setUsedAgreementName('Most Favoured Nation')
        }
        return
      }

      try {
        // cancel any in-flight phase2 request
        if (phase2AbortRef.current) {
          phase2AbortRef.current.abort()
        }
        const controller = new AbortController()
        phase2AbortRef.current = controller
        const resp = await tariffApi.calculateTariff({
          mfnRate: mfnRate || 0,
          prefRate: bestOption.adValoremRate ?? 0,
          rvcThreshold: (bestOption as any).rvcThreshold ?? undefined,
          agreementId: (bestOption as any).agreementId ?? undefined,
          quantity: 0,
          totalValue: costs.totalValue,
          materialCost: costs.materialCost,
          labourCost: costs.labourCost,
          overheadCost: costs.overheadCost,
          profit: costs.profit,
          otherCosts: costs.otherCosts,
          fob: costs.fob,
          nonOriginValue: costs.nonOriginValue,
        }, { signal: controller.signal })
        const data = resp?.data
        setCalcResult({
          basis: data.basis,
          appliedRate: Number(data.appliedRate ?? 0),
          totalDuty: Number(data.totalDuty ?? 0),
          rvc: Number(data.rvc ?? backendRvc),
          rvcThreshold: data.rvcThreshold ?? ((bestOption as any).rvcThreshold ?? null),
        })
        setUsedAgreementName((bestOption as any).agreementName ?? null)
      } catch (err: any) {
        if (err?.name === 'CanceledError' || err?.code === 'ERR_CANCELED') {
          return
        }
        setCalcResult(null)
        setUsedAgreementName((bestOption as any).agreementName ?? null)
      }
    }
    run()
    return () => {
      if (phase2AbortRef.current) {
        phase2AbortRef.current.abort()
        phase2AbortRef.current = null
      }
    }
  }, [lookup, bestOption, mfnRate, costs, backendRvc])

  // Manual selection syncing removed

  // Local computeResult no longer used for primary output

  // Optimistically keep Total Duty in sync with latest Customs Value using current appliedRate
  useEffect(() => {
    setCalcResult((prev) => {
      if (!prev) return prev
      const updatedTotalDuty = costs.totalValue * prev.appliedRate
      if (updatedTotalDuty === prev.totalDuty) return prev
      return { ...prev, totalDuty: updatedTotalDuty }
    })
  }, [costs.totalValue])

  const handleFormChange = (field: keyof typeof form, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value.toUpperCase() }))
  }

  const handleCostChange = (field: CostField, value: string) => {
    const numeric = parseNumber(value)
    setCosts((prev) => ({ ...prev, [field]: numeric }))
  }

  const handleLookup = async () => {
    setError(null)

    const importer = form.importerIso3.trim().toUpperCase()
    const hsCode = form.hsCode.replace(/\./g, '').trim()
    if (!importer || !hsCode) {
      setError('Importer and HS code are required.')
      return
    }

    setLoading(true)
    try {
      const response = await tariffApi.getTariffRateLookup({
        importerIso3: importer,
        originIso3: form.originIso3.trim() || undefined,
        hsCode,
      })
      const data = response.data
      setLookup(data)
      // Reset inputs and previous results for a fresh calculation flow
      setCosts(initialCosts)
      setCalcResult(null)
      setUsedAgreementName(null)
      setBackendRvc(0)
      setIsSimulating(false)
      setSimulatedRate(null)
      setSimulatedValue(null)
    } catch (err: any) {
      const message =
        err?.response?.data?.message ||
        err?.message ||
        'Unable to find tariff information.'
      setError(message)
      setLookup(null)
      // Clear any prior results when lookup fails
      setCalcResult(null)
      setUsedAgreementName(null)
      setBackendRvc(0)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="p-6 space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>1. Identify Your Trade</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4 md:grid-cols-3">
            <div className="space-y-2">
              <label className="text-sm font-medium">Importer (Destination)</label>
              <CountrySelect
                countries={countries}
                value={form.importerIso3}
                onChange={(code) =>
                  handleFormChange('importerIso3', Array.isArray(code) ? code[0] ?? '' : code ?? '')
                }
                loading={countriesLoading}
                error={countriesError}
                placeholder="Select importer"
                required
              />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Exporter (Origin)</label>
              <CountrySelect
                countries={countries}
                value={form.originIso3}
                onChange={(code) =>
                  handleFormChange('originIso3', Array.isArray(code) ? code[0] ?? '' : code ?? '')
                }
                loading={countriesLoading}
                error={countriesError}
                placeholder="Select origin (optional)"
              />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">HS Code</label>
              <Input
                value={form.hsCode}
                onChange={(event) => handleFormChange('hsCode', event.target.value)}
                placeholder="e.g. 870380"
              />
            </div>
          </div>
          <div className="flex items-center gap-3">
            <Button onClick={handleLookup} disabled={loading}>
              {loading ? 'Searching…' : 'Lookup Tariff Options'}
            </Button>
            {error && <p className="text-sm text-red-600">{error}</p>}
          </div>
        </CardContent>
      </Card>

      {lookup && (
        <Card>
          <CardHeader>
            <CardTitle>2. Fill in Costs & Choose an Agreement</CardTitle>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="grid gap-4 md:grid-cols-2">
              <div className="space-y-4">
                <div>
                  <h3 className="text-sm font-semibold">Cost Breakdown</h3>
                  <p className="text-xs text-muted-foreground">
                    Provide costs in {settings.currency}.
                  </p>
                </div>
                <div className="space-y-3">
                  {(
                    [
                      ['totalValue', 'Customs Value (Total)', true],
                      ['materialCost', 'Material Cost', true],
                      ['labourCost', 'Labour Cost', true],
                      ['overheadCost', 'Overhead Cost', true],
                      ['profit', 'Profit', true],
                      ['otherCosts', 'Other Costs', true],
                      ['fob', 'FOB Value', true],
                      // 'Non-originating Material (CIF)' removed as it is not used in calculation
                    ] as Array<[CostField, string, boolean]>
                  ).map(([field, label, required]) => (
                    <div key={field} className="space-y-1">
                      <label className="text-sm font-medium">
                        {label}
                        {required ? ' *' : ''}
                      </label>
                      <Input
                        type="number"
                        inputMode="decimal"
                        value={costs[field]}
                        onChange={(event) => handleCostChange(field, event.target.value)}
                        onFocus={(event) => {
                          // Select existing content (e.g., the default 0) so typing replaces it immediately
                          event.currentTarget.select()
                        }}
                        min={0}
                      />
                    </div>
                  ))}
                </div>
              </div>

              <div className="space-y-4">
                <div>
                  <h3 className="text-sm font-semibold">Agreement Options</h3>
                  <p className="text-xs text-muted-foreground">
                    Agreements that do not meet the RVC threshold are disabled automatically.
                  </p>
                </div>
                <div className="space-y-2">
                  {selection?.options?.length ? (
                    selection.options.map(({ option, eligible }) => {
                      const isSelected = selection?.selected?.id === option.id
                      return (
                        <div
                          key={option.id}
                          className={`w-full rounded-md border px-4 py-3 text-left ${isSelected
                              ? 'border-brand-500 bg-brand-50'
                              : 'border-border bg-background'
                            } ${eligible ? '' : 'opacity-50'}`}
                        >
                          <div className="flex items-center justify-between">
                            <div>
                              <p className="text-sm font-medium">
                                {option.agreementName ?? 'Most Favoured Nation'}
                              </p>
                              <p className="text-xs text-muted-foreground uppercase">
                                {option.basis}
                              </p>
                            </div>
                            <div className="text-right">
                              <p className="text-sm font-semibold">
                                {(option.adValoremRate ?? 0) * 100}%
                              </p>
                              {option.rvcThreshold != null && (
                                <p className="text-xs text-muted-foreground">
                                  RVC ≥ {option.rvcThreshold}%
                                </p>
                              )}
                            </div>
                          </div>
                        </div>
                      )
                    })
                  ) : (
                    <p className="text-sm text-muted-foreground">
                      No tariff agreements were returned for this search. Confirm the HS code and try again.
                    </p>
                  )}
                </div>
                {/* Tariff Simulation Controls */}
                <div className="rounded-md border p-4 bg-blue-50/50 space-y-3">
                  <div className="flex items-center justify-between">
                    <h4 className="text-sm font-semibold text-blue-900">Simulate Tariff Rate & Value</h4>
                    <Button
                      size="sm"
                      variant={isSimulating ? "default" : "outline"}
                      onClick={() => {
                        setIsSimulating(!isSimulating)
                        if (isSimulating) {
                          setSimulatedRate(null)
                          setSimulatedValue(null)
                        }
                      }}
                    >
                      {isSimulating ? 'Reset' : 'Simulate'}
                    </Button>
                  </div>

                  {isSimulating && (
                    <div className="space-y-4">
                      {/* Tariff Rate Controls */}
                      <div className="space-y-2">
                        <label className="text-xs font-medium text-blue-900">Tariff Rate</label>
                        <div className="flex items-center gap-3">
                          <Input
                            type="number"
                            inputMode="decimal"
                            min="0"
                            max="100"
                            step="0.1"
                            value={simulatedRate ?? ''}
                            onChange={(e) => {
                              const val = parseFloat(e.target.value)
                              setSimulatedRate(isNaN(val) ? 0 : Math.max(0, Math.min(100, val)))
                            }}
                            placeholder="Enter rate %"
                            className="w-24"
                          />
                          <span className="text-sm font-medium">%</span>
                        </div>
                        <input
                          type="range"
                          min="0"
                          max="100"
                          step="0.1"
                          value={simulatedRate ?? 0}
                          onChange={(e) => setSimulatedRate(parseFloat(e.target.value))}
                          className="w-full h-2 bg-blue-200 rounded-lg appearance-none cursor-pointer accent-blue-600"
                        />
                        <p className="text-xs text-blue-700">
                          Testing a {(simulatedRate ?? 0).toFixed(1)}% tariff rate
                        </p>
                      </div>

                      {/* Customs Value Controls */}
                      <div className="space-y-2">
                        <label className="text-xs font-medium text-blue-900">Customs Value ({settings.currency})</label>
                        <div className="flex items-center gap-3">
                          <Input
                            type="number"
                            inputMode="decimal"
                            min="0"
                            step="100"
                            value={simulatedValue ?? costs.totalValue}
                            onChange={(e) => {
                              const val = parseFloat(e.target.value)
                              setSimulatedValue(isNaN(val) ? costs.totalValue : Math.max(0, val))
                            }}
                            placeholder="Enter customs value"
                            className="flex-1"
                          />
                          <Button
                            size="sm"
                            variant="ghost"
                            onClick={() => setSimulatedValue(costs.totalValue)}
                            className="text-xs"
                          >
                            Reset
                          </Button>
                        </div>
                        <input
                          type="range"
                          min="0"
                          max={costs.totalValue * 2}
                          step={costs.totalValue / 100}
                          value={simulatedValue ?? costs.totalValue}
                          onChange={(e) => setSimulatedValue(parseFloat(e.target.value))}
                          className="w-full h-2 bg-blue-200 rounded-lg appearance-none cursor-pointer accent-blue-600"
                        />
                        <div className="flex justify-between text-xs text-blue-700">
                          <span>Testing {formatCurrency(simulatedValue ?? costs.totalValue, settings.currency)}</span>
                          <span className="text-muted-foreground">
                            {((((simulatedValue ?? costs.totalValue) - costs.totalValue) / costs.totalValue) * 100).toFixed(0)}%
                            {(simulatedValue ?? costs.totalValue) >= costs.totalValue ? ' increase' : ' decrease'}
                          </span>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
                <div className="rounded-md border px-4 py-3 bg-muted/30">
                  <p className="text-sm font-medium">RVC</p>
                  <p className="text-2xl font-bold">{backendRvc.toFixed(2)}%</p>
                  <p className="text-xs text-muted-foreground">
                    {(selection?.selected?.agreementName ?? 'Current selection')}{' '}
                    requires {selection?.selected?.rvcThreshold ?? 'N/A'}%.
                  </p>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {lookup && (costs.fob <= 0 || (
        costs.materialCost +
        costs.labourCost +
        costs.overheadCost +
        costs.profit +
        costs.otherCosts) > costs.fob) && (
          <Card>
            <CardHeader>
              <CardTitle>3. Results</CardTitle>
            </CardHeader>
            <CardContent>
              {costs.fob <= 0 ? (
                <p className="text-sm text-red-600">Enter a valid FOB value greater than 0 to compute RVC and duty.</p>
              ) : (
                <p className="text-sm text-red-600">Originating costs (Material + Labour + Overhead + Profit + Other) must not exceed FOB. Adjust your inputs to proceed.</p>
              )}
            </CardContent>
          </Card>
        )}

      {calcResult && (
        <Card>
          <CardHeader>
            <CardTitle>3. Results</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 md:grid-cols-2">
            <div className="space-y-2">
              <p className="text-sm text-muted-foreground">Tariff Basis</p>
              <p className="text-lg font-semibold">
                {isSimulating ? 'Simulated' : calcResult.basis}
                {!isSimulating && usedAgreementName ? ` • ${usedAgreementName}` : ''}
              </p>
            </div>
            <div className="space-y-2">
              <p className="text-sm text-muted-foreground">Applied Rate</p>
              <p className="text-lg font-semibold">
                {((isSimulating ? (simulatedRate ?? 0) / 100 : calcResult.appliedRate) * 100).toFixed(2)}%
              </p>
            </div>
            <div className="space-y-2">
              <p className="text-sm text-muted-foreground">Total Cost</p>
              <p className="text-lg font-semibold">
                {formatCurrency(
                  (isSimulating
                    ? (simulatedValue ?? costs.totalValue) * ((simulatedRate ?? 0) / 100)
                    : calcResult.totalDuty) + (isSimulating ? (simulatedValue ?? costs.totalValue) : costs.totalValue),
                  settings.currency
                )}
              </p>
            </div>
            <div className="space-y-2">
              <p className="text-sm text-muted-foreground">
                {isSimulating ? 'Simulated Duty' : 'RVC vs Threshold'}
              </p>
              <p className="text-lg font-semibold">
                {isSimulating
                  ? formatCurrency((simulatedValue ?? costs.totalValue) * ((simulatedRate ?? 0) / 100), settings.currency)
                  : `${calcResult.rvc.toFixed(2)}% / ${calcResult.rvcThreshold ?? 'N/A'}%`
                }
              </p>
            </div>
          </CardContent>
        </Card>
      )}

      {calcResult && (
        <div className="grid gap-6">
          {isSimulating ? (
            // Show both charts side by side when simulating
            <div className="grid gap-4 md:grid-cols-2">
              <div className="space-y-2">
                <h3 className="text-sm font-semibold text-muted-foreground">Original Calculation</h3>
                <TariffBreakdownChart
                  data={{
                    baseValue: costs.totalValue,
                    tariffAmount: calcResult.totalDuty,
                    additionalFees: 0,
                    totalCost: costs.totalValue + calcResult.totalDuty,
                    breakdown: [
                      {
                        type: 'Duty',
                        rate: calcResult.appliedRate,
                        amount: calcResult.totalDuty,
                        description: 'Tariff duty based on applied rate',
                      },
                    ],
                  }}
                />
              </div>
              <div className="space-y-2">
                <h3 className="text-sm font-semibold text-blue-700">Simulated Scenario</h3>
                <TariffBreakdownChart
    data={{
      baseValue: simulatedValue ?? costs.totalValue,
      tariffAmount: (simulatedValue ?? costs.totalValue) * ((simulatedRate ?? 0) / 100),
      additionalFees: 0,
      totalCost: (simulatedValue ?? costs.totalValue) + ((simulatedValue ?? costs.totalValue) * ((simulatedRate ?? 0) / 100)),
      breakdown: [
        {
          type: 'Duty',
          rate: (simulatedRate ?? 0) / 100,
          amount: (simulatedValue ?? costs.totalValue) * ((simulatedRate ?? 0) / 100),
          description: 'Simulated tariff duty',
        },
      ],
    }}
  />
              </div>
            </div>
          ) : (
            // Show only original chart when not simulating
            <TariffBreakdownChart
              data={{
                baseValue: costs.totalValue,
                tariffAmount: calcResult.totalDuty,
                additionalFees: 0,
                totalCost: costs.totalValue + calcResult.totalDuty,
                breakdown: [
                  {
                    type: 'Duty',
                    rate: calcResult.appliedRate,
                    amount: calcResult.totalDuty,
                    description: 'Tariff duty based on applied rate',
                  },
                ],
              }}
            />
          )}
          {/* Save controls */}
          <div className="space-y-3 md:col-span-2 mt-2">
            <div className="grid gap-3 md:grid-cols-2">
              <div className="space-y-1">
                <label className="text-sm font-medium">Label</label>
                <Input
                  value={saveName}
                  onChange={(e) => setSaveName(e.target.value)}
                  placeholder="e.g., Chile → US EV shipment"
                />
              </div>
              <div className="space-y-1">
                <label className="text-sm font-medium">Notes</label>
                <Input
                  value={saveNotes}
                  onChange={(e) => setSaveNotes(e.target.value)}
                  placeholder="Optional notes"
                />
              </div>
            </div>
            <div className="flex items-center gap-3">
              <Button
                disabled={saving}
                onClick={async () => {
                  if (!calcResult || !lookup) return
                  setSaving(true)
                  setSaveError(null)
                  try {
                    const hsCode = form.hsCode.replace(/\./g, '').trim()
                    const best = bestOption
                    await savedTariffsApi.save({
                      input: {
                        mfnRate: mfnRate || 0,
                        prefRate: best?.adValoremRate ?? 0,
                        rvcThreshold: calcResult.rvcThreshold ?? undefined,
                        agreementId: (best as any)?.agreementId ?? undefined,
                        quantity: 0,
                        totalValue: costs.totalValue,
                        materialCost: costs.materialCost,
                        labourCost: costs.labourCost,
                        overheadCost: costs.overheadCost,
                        profit: costs.profit,
                        otherCosts: costs.otherCosts,
                        fob: costs.fob,
                        nonOriginValue: costs.nonOriginValue,
                      },
                      result: {
                        totalCost: costs.totalValue + calcResult.totalDuty,
                        tariffBasis: calcResult.basis,
                        appliedRate: calcResult.appliedRate,
                        calculatedRvc: calcResult.rvc,
                        rvcThreshold: calcResult.rvcThreshold ?? undefined,
                      },
                      name: saveName,
                      notes: saveNotes,
                      hsCode,
                      importerIso2: iso3ToIso2(form.importerIso3),
                      originIso2: iso3ToIso2(form.originIso3),
                    })
                    setSaveName("")
                    setSaveNotes("")
                  } catch (e: any) {
                    setSaveError(e?.response?.data?.message || e?.message || 'Failed to save')
                  } finally {
                    setSaving(false)
                  }
                }}
              >
                {saving ? 'Saving…' : 'Save Calculation'}
              </Button>
              {saveError && <span className="text-sm text-red-600">{saveError}</span>}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default Calculator
