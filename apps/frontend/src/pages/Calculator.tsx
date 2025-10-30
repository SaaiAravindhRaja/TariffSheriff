import { useEffect, useMemo, useState } from 'react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import CountrySelect from '@/components/inputs/CountrySelect'
import { useDbCountries } from '@/hooks/useDbCountries'
import { useSettings } from '@/contexts/SettingsContext'
import { tariffApi, type TariffLookupResponse, type TariffRateOption } from '@/services/api'
import { formatCurrency } from '@/lib/utils'

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
  return Number.isFinite(parsed) ? parsed : 0
}

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
    importerIso2: '',
    originIso2: '',
    hsCode: '',
  })
  const [costs, setCosts] = useState<CostState>(initialCosts)
  const [lookup, setLookup] = useState<TariffLookupResponse | null>(null)
  const [selectedRateId, setSelectedRateId] = useState<number | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const rvcPercentage = useMemo(() => calculateRvc(costs), [costs])

  const selection = useMemo(() => {
    if (!lookup) return null
    return resolveSelection(lookup.rates, selectedRateId, rvcPercentage)
  }, [lookup, selectedRateId, rvcPercentage])

  useEffect(() => {
    if (
      selection?.selected &&
      selection.usedFallback &&
      selection.selected.id !== selectedRateId
    ) {
      setSelectedRateId(selection.selected.id)
    }
  }, [selection, selectedRateId])

  const result = useMemo(() => {
    return computeResult(selection?.selected ?? null, costs, rvcPercentage)
  }, [selection, costs, rvcPercentage])

  const handleFormChange = (field: keyof typeof form, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value.toUpperCase() }))
  }

  const handleCostChange = (field: CostField, value: string) => {
    const numeric = parseNumber(value)
    setCosts((prev) => ({ ...prev, [field]: numeric }))
  }

  const handleLookup = async () => {
    setError(null)

    const importer = form.importerIso2.trim().toUpperCase()
    const hsCode = form.hsCode.replace(/\./g, '').trim()
    if (!importer || !hsCode) {
      setError('Importer and HS code are required.')
      return
    }

    setLoading(true)
    try {
      const response = await tariffApi.getTariffRateLookup({
        importerIso2: importer,
        originIso2: form.originIso2.trim() || undefined,
        hsCode,
      })
      const data = response.data
      setLookup(data)
      setSelectedRateId(null)
    } catch (err: any) {
      const message =
        err?.response?.data?.message ||
        err?.message ||
        'Unable to find tariff information.'
      setError(message)
      setLookup(null)
      setSelectedRateId(null)
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
                value={form.importerIso2}
                onChange={(code) =>
                  handleFormChange('importerIso2', Array.isArray(code) ? code[0] ?? '' : code ?? '')
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
                value={form.originIso2}
                onChange={(code) =>
                  handleFormChange('originIso2', Array.isArray(code) ? code[0] ?? '' : code ?? '')
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
                    Provide costs in {settings.currency}. These values drive the RVC calculation.
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
                      ['nonOriginValue', 'Non-originating Material (CIF)', false],
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
                        </button>
                      )
                    })
                  ) : (
                    <p className="text-sm text-muted-foreground">
                      No tariff agreements were returned for this search. Confirm the HS code and try again.
                    </p>
                  )}
                </div>
                <div className="rounded-md border px-4 py-3 bg-muted/30">
                  <p className="text-sm font-medium">Calculated RVC</p>
                  <p className="text-2xl font-bold">{rvcPercentage.toFixed(2)}%</p>
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

      {result && (
        <Card>
          <CardHeader>
            <CardTitle>3. Results</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 md:grid-cols-2">
            <div className="space-y-2">
              <p className="text-sm text-muted-foreground">Tariff Basis</p>
              <p className="text-lg font-semibold">{result.basis}</p>
            </div>
            <div className="space-y-2">
              <p className="text-sm text-muted-foreground">Applied Rate</p>
              <p className="text-lg font-semibold">{(result.appliedRate * 100).toFixed(2)}%</p>
            </div>
            <div className="space-y-2">
              <p className="text-sm text-muted-foreground">Total Duty</p>
              <p className="text-lg font-semibold">
                {formatCurrency(result.totalDuty, settings.currency)}
              </p>
            </div>
            <div className="space-y-2">
              <p className="text-sm text-muted-foreground">RVC vs Threshold</p>
              <p className="text-lg font-semibold">
                {result.rvc.toFixed(2)}% / {result.rvcThreshold ?? 'N/A'}%
              </p>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  )
}

export default Calculator
