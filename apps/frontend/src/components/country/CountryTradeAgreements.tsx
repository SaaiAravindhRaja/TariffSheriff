import React, { useEffect, useMemo, useState } from 'react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { FileText, Shield, Users, CheckCircle } from 'lucide-react'
import { tariffApi } from '@/services/api'
import { Badge } from '@/components/ui/badge'

interface CountryTradeAgreementsProps {
  countryCode: string
}

type AgreementDto = {
  id: number
  name: string
  rvcThreshold?: number | null
}

type TariffRateDto = {
  id: number
  importerIso3?: string | null
  originIso3?: string | null
  basis?: string
}

export const CountryTradeAgreements: React.FC<CountryTradeAgreementsProps> = ({ countryCode }) => {
  const [agreements, setAgreements] = useState<AgreementDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [hasMfn, setHasMfn] = useState(false)

  useEffect(() => {
    let cancelled = false

    async function load() {
      setLoading(true)
      setError(null)
      try {
        const [agreementsRes, ratesRes] = await Promise.all([
          tariffApi.getAgreementsByCountry(countryCode),
          tariffApi.getTariffRates(),
        ])

        if (!cancelled) {
          const list: AgreementDto[] = Array.isArray(agreementsRes.data)
            ? agreementsRes.data
            : Array.isArray(agreementsRes.data?.content)
              ? agreementsRes.data.content
              : []
          setAgreements(list)
        }

        const rates: TariffRateDto[] = Array.isArray(ratesRes.data) ? ratesRes.data : []
        const mfnAvailable = rates.some(
          (rate) =>
            (rate.importerIso3 || '').toUpperCase() === countryCode.toUpperCase() &&
            (rate.basis || '').toUpperCase() === 'MFN' &&
            !(rate.originIso3 || '')
        )
        if (!cancelled) {
          setHasMfn(mfnAvailable)
        }
      } catch (e: any) {
        if (!cancelled) {
          setError(e?.message || 'Failed to load agreements')
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    load()
    return () => {
      cancelled = true
    }
  }, [countryCode])

  const averageRvc = useMemo(() => {
    const thresholds = agreements
      .map((agreement) => agreement.rvcThreshold)
      .filter((value): value is number => typeof value === 'number' && !Number.isNaN(value))
    if (!thresholds.length) return null
    return thresholds.reduce((sum, value) => sum + value, 0) / thresholds.length
  }, [agreements])

  return (
    <div className="space-y-6">
      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium flex items-center">
              <CheckCircle className="h-4 w-4 mr-2 text-green-600" />
              Agreements Found
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{agreements.length}</div>
            <p className="text-xs text-muted-foreground">
              Pulled from the backend database
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium flex items-center">
              <Users className="h-4 w-4 mr-2 text-blue-600" />
              Avg. RVC Threshold
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {averageRvc != null ? `${averageRvc.toFixed(1)}%` : '—'}
            </div>
            <p className="text-xs text-muted-foreground">
              Based on available agreement data
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium flex items-center">
              <Shield className="h-4 w-4 mr-2 text-purple-600" />
              MFN Available
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{hasMfn ? 'Yes' : 'No'}</div>
            <p className="text-xs text-muted-foreground">
              Determined from tariff-rate endpoint
            </p>
          </CardContent>
        </Card>
      </div>

      <div className="space-y-4">
        {loading ? (
          <div className="text-center py-8">
            <FileText className="w-10 h-10 text-muted-foreground mx-auto mb-2" />
            <div className="text-sm text-muted-foreground">Loading agreements…</div>
          </div>
        ) : error ? (
          <div className="text-center py-8 text-sm text-red-600">{error}</div>
        ) : agreements.length === 0 ? (
          <div className="text-center py-8">
            <FileText className="w-10 h-10 text-muted-foreground mx-auto mb-2" />
            <div className="text-sm text-muted-foreground">No agreements found for this country.</div>
          </div>
        ) : (
          agreements.map((agreement) => (
            <Card key={agreement.id} className="card-hover">
              <CardHeader>
                <div className="flex items-center justify-between">
                  <CardTitle className="text-lg">{agreement.name}</CardTitle>
                  {agreement.rvcThreshold != null && (
                    <Badge variant="outline">RVC: {agreement.rvcThreshold}%</Badge>
                  )}
                </div>
              </CardHeader>
              <CardContent>
                <p className="text-sm text-muted-foreground">
                  Additional agreement metadata (type, status, timeline) will appear here once the
                  new ISO3 schema is fully populated.
                </p>
              </CardContent>
            </Card>
          ))
        )}
      </div>
    </div>
  )
}
