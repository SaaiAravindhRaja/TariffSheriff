import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { savedTariffsApi, SavedTariffSummary, PageResponse, tariffApi } from '@/services/api'
import { Button } from '@/components/ui/button'
import { ChevronDown, ChevronUp, Loader2, Trash2 } from 'lucide-react'
import { useMemo, useState } from 'react'

export function SavedTariffs() {
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [expandedId, setExpandedId] = useState<number | null>(null)
  const [exportingDetails, setExportingDetails] = useState(false)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['saved-tariffs', page, size],
    queryFn: async () => {
      const res = await savedTariffsApi.list({ page, size })
      return res.data as PageResponse<SavedTariffSummary>
    },
  })

  const expandedDetail = useQuery({
    queryKey: ['saved-tariff-detail', expandedId],
    queryFn: async () => {
      if (expandedId == null) return null
      const res = await savedTariffsApi.get(expandedId)
      return res.data as any
    },
    enabled: expandedId != null,
  })

  const hsLabelQuery = useQuery({
    queryKey: ['hs-label', expandedId, expandedDetail.data?.hsCode],
    queryFn: async () => {
      const code: string | undefined = expandedDetail.data?.hsCode
      if (!code) return null
      const res = await tariffApi.searchHsProducts({ q: code, limit: 10 })
      const list = (res.data || []) as Array<{ hsCode: string; hsLabel: string }>
      const exact = list.find(x => x.hsCode?.replace(/\./g, '') === code?.replace(/\./g, ''))
      return (exact?.hsLabel || list[0]?.hsLabel || null) as string | null
    },
    enabled: expandedId != null && !!expandedDetail.data?.hsCode,
  })

  const exportTableCsv = () => {
    try {
      if (!data?.content?.length) return
      const rows = data.content
      const headers = [
        'Name',
        'Importer ISO',
        'Origin ISO',
        'Total Value (USD)',
        'Total After Tariff (USD)',
        'Agreement',
      ]
      const esc = (val: any) => {
        if (val == null) return ''
        const s = String(val)
        if (/[",\n]/.test(s)) return '"' + s.replace(/"/g, '""') + '"'
        return s
      }
      const toFixedOrEmpty = (num: number | null | undefined) =>
        typeof num === 'number' && Number.isFinite(num) ? num.toFixed(2) : ''
      const lines = [headers.join(',')]
      for (const r of rows) {
        lines.push([
          esc(r.name || 'Untitled'),
          esc(r.importerIso2 ?? ''),
          esc(r.originIso2 ?? ''),
          esc(toFixedOrEmpty(r.totalValue as any)),
          esc(toFixedOrEmpty(r.totalTariff as any)),
          esc(r.agreementName ?? ''),
        ].join(','))
      }
      const csv = lines.join('\r\n')
      const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `saved-tariffs-page-${page + 1}.csv`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
    } catch (e) {
      console.error('Failed to export CSV', e)
    }
  }

  const exportDetailsCsv = async () => {
    try {
      if (!data?.content?.length) return
      setExportingDetails(true)
      const rows = data.content
      const headers = [
        'Name',
        'Notes',
        'Importer ISO',
        'Origin ISO',
        'HS Code',
        'HS Product Name',
        'Agreement',
        'Rate Type',
        'Applied Rate (%)',
        'Total Value (USD)',
        'Total Tariff (USD)',
        'RVC Threshold (%)',
        'Computed RVC (%)',
        'Material Cost (USD)',
        'Labour Cost (USD)',
        'Overhead Cost (USD)',
        'Profit (USD)',
        'Other Costs (USD)',
        'FOB (USD)'
      ]
      const esc = (val: any) => {
        if (val == null) return ''
        const s = String(val)
        if (/[",\n]/.test(s)) return '"' + s.replace(/"/g, '""') + '"'
        return s
      }
      const num2 = (n: any) => {
        const v = Number(n)
        return Number.isFinite(v) ? v.toFixed(2) : ''
      }
      const pct2 = (n: any) => {
        const v = Number(n)
        return Number.isFinite(v) ? v.toFixed(2) : ''
      }
      const lines: string[] = [headers.join(',')]
      const details = await Promise.all(rows.map(async (r) => {
        const detailResp = await savedTariffsApi.get(r.id)
        const detail = detailResp.data as any
        let hsLabel: string | null = null
        try {
          if (detail?.hsCode) {
            const res = await tariffApi.searchHsProducts({ q: detail.hsCode, limit: 10 })
            const list = (res.data || []) as Array<{ hsCode: string; hsLabel: string }>
            const code = String(detail.hsCode)
            const exact = list.find(x => x.hsCode?.replace(/\./g, '') === code.replace(/\./g, ''))
            hsLabel = exact?.hsLabel || list[0]?.hsLabel || null
          }
        } catch {
          // Ignore HS label lookup failures during export
          void 0
        }
        return { summary: r, detail, hsLabel }
      }))
      for (const { summary, detail, hsLabel } of details) {
        const d = detail || {}
        const input = d.input || {}
        const result = d.result || {}
        const rateType = result.rateUsed ?? result.basis ?? ''
        const appliedRatePct = Number(result.appliedRate)
        const appliedRateStr = Number.isFinite(appliedRatePct) ? (appliedRatePct * 100).toFixed(2) : ''
        lines.push([
          esc(d.name || summary.name || 'Untitled'),
          esc(d.notes || ''),
          esc(d.importerIso2 ?? summary.importerIso2 ?? ''),
          esc(d.originIso2 ?? summary.originIso2 ?? ''),
          esc(d.hsCode ?? ''),
          esc(hsLabel ?? ''),
          esc(summary.agreementName ?? ''),
          esc(rateType),
          esc(appliedRateStr),
          esc(num2(input.totalValue)),
          esc(num2(result.totalTariff)),
          esc(pct2(input.rvcThreshold)),
          esc(pct2(result.rvcComputed)),
          esc(num2(input.materialCost)),
          esc(num2(input.labourCost)),
          esc(num2(input.overheadCost)),
          esc(num2(input.profit)),
          esc(num2(input.otherCosts)),
          esc(num2(input.fob)),
        ].join(','))
      }
      const csv = lines.join('\r\n')
      const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `saved-tariffs-details-page-${page + 1}.csv`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
    } catch (e) {
      console.error('Failed to export detailed CSV', e)
    } finally {
      setExportingDetails(false)
    }
  }

  const del = useMutation({
    mutationFn: async (id: number) => {
      await savedTariffsApi.delete(id)
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['saved-tariffs'] })
    }
  })

  if (isLoading) {
    return (
      <div className="p-6"><Loader2 className="h-5 w-5 animate-spin" /></div>
    )
  }
  if (isError || !data) {
    return <div className="p-6 text-red-600">Failed to load saved tariffs.</div>
  }

  const { content, totalPages } = data

  return (
    <div className="p-6 space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Saved Tariffs</h1>
        <div className="flex items-center gap-2">
          <Button variant="outline" onClick={exportTableCsv}>Export Table CSV</Button>
          <Button variant="default" disabled={exportingDetails} onClick={exportDetailsCsv}>
            {exportingDetails ? (
              <span className="inline-flex items-center gap-2"><Loader2 className="h-4 w-4 animate-spin" /> Exporting…</span>
            ) : 'Export Details CSV'}
          </Button>
        </div>
      </div>

      <div className="overflow-x-auto rounded-md border">
        <table className="min-w-full text-sm">
          <thead className="bg-muted/50">
            <tr>
              <th className="px-4 py-2 text-left font-medium">Name</th>
              <th className="px-4 py-2 text-left font-medium">Importer ISO</th>
              <th className="px-4 py-2 text-left font-medium">Origin ISO</th>
              <th className="px-4 py-2 text-right font-medium">Total Value (USD)</th>
              <th className="px-4 py-2 text-right font-medium">Total After Tariff (USD)</th>
              <th className="px-4 py-2 text-left font-medium">Agreement</th>
              <th className="px-4 py-2 text-right font-medium">Actions</th>
            </tr>
          </thead>
          <tbody>
            {content.map(item => (
              <>
                <tr key={item.id} className="border-t">
                  <td className="px-4 py-2">
                    <div className="flex flex-col">
                      <span className="font-medium">{item.name || 'Untitled'}</span>
                      <span className="text-xs text-muted-foreground">{new Date(item.createdAt).toLocaleString()}</span>
                    </div>
                  </td>
                  <td className="px-4 py-2">{item.importerIso2 ?? '-'}</td>
                  <td className="px-4 py-2">{item.originIso2 ?? '-'}</td>
                  <td className="px-4 py-2 text-right">{item.totalValue != null ? item.totalValue.toFixed(2) : '-'}</td>
                  <td className="px-4 py-2 text-right">{item.totalTariff != null ? item.totalTariff.toFixed(2) : '-'}</td>
                  <td className="px-4 py-2">{item.agreementName ?? '-'}</td>
                  <td className="px-4 py-2 text-right space-x-1">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setExpandedId(prev => prev === item.id ? null : item.id)}
                      title={expandedId === item.id ? 'Hide details' : 'Show details'}
                    >
                      {expandedId === item.id ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
                    </Button>
                    <Button variant="ghost" size="icon" onClick={() => del.mutate(item.id)} title="Delete">
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </td>
                </tr>
                {expandedId === item.id && (
                  <tr className="border-t">
                    <td className="px-4 py-3" colSpan={7}>
                      {expandedDetail.isLoading ? (
                        <div className="flex items-center gap-2 text-muted-foreground">
                          <Loader2 className="h-4 w-4 animate-spin" /> Loading details…
                        </div>
                      ) : expandedDetail.isError || !expandedDetail.data ? (
                        <div className="text-sm text-red-600">Failed to load details.</div>
                      ) : (
                        <div className="text-sm">
                          <div className="grid gap-8 md:grid-cols-2">
                            {/* Left: Meta/info (swapped) */}
                            <div className="space-y-3">
                              <div className="rounded-md border bg-muted/20 p-4">
                                <div className="text-sm font-semibold mb-3">Info</div>
                                <div className="space-y-3">
                                  <div>
                                    <div className="text-xs text-muted-foreground">Name</div>
                                    <div className="font-medium">{expandedDetail.data.name || 'Untitled'}</div>
                                  </div>
                                  <div>
                                    <div className="text-xs text-muted-foreground">Notes</div>
                                    <div className="font-medium whitespace-pre-wrap break-words">{expandedDetail.data.notes || '-'}</div>
                                  </div>
                                  <div className="grid gap-3 md:grid-cols-2">
                                    <div>
                                      <div className="text-xs text-muted-foreground">Importer</div>
                                      <div className="font-medium">{expandedDetail.data.importerIso2 ?? '-'}</div>
                                    </div>
                                    <div>
                                      <div className="text-xs text-muted-foreground">Origin</div>
                                      <div className="font-medium">{expandedDetail.data.originIso2 ?? '-'}</div>
                                    </div>
                                  </div>
                                  <div className="grid gap-3 md:grid-cols-2">
                                    <div>
                                      <div className="text-xs text-muted-foreground">HS Code</div>
                                      <div className="font-medium">{expandedDetail.data.hsCode ?? '-'}</div>
                                    </div>
                                    <div>
                                      <div className="text-xs text-muted-foreground">HS Product Name</div>
                                      <div className="font-medium">{hsLabelQuery.data ?? '-'}</div>
                                    </div>
                                  </div>
                                  <div className="grid gap-3 md:grid-cols-2">
                                    <div>
                                      <div className="text-xs text-muted-foreground">Agreement</div>
                                      <div className="font-medium">{item.agreementName ?? '-'}</div>
                                    </div>
                                    <div>
                                      <div className="text-xs text-muted-foreground">Rate Type</div>
                                      <div className="font-medium">{expandedDetail.data.result?.rateUsed ?? expandedDetail.data.result?.basis ?? '-'}</div>
                                    </div>
                                  </div>
                                </div>
                              </div>
                            </div>

                            {/* Right: Calculation-related (swapped) */}
                            <div className="space-y-4 md:border-l md:pl-4">
                              <div className="rounded-md border bg-muted/20 p-4">
                                <div className="text-sm font-semibold mb-3">Calculation</div>
                                <div className="grid gap-3 md:grid-cols-2">
                                  <div>
                                    <div className="text-xs text-muted-foreground">Total Value</div>
                                    <div className="font-medium">{expandedDetail.data.input?.totalValue != null ? Number(expandedDetail.data.input.totalValue).toFixed(2) : '-'}</div>
                                  </div>
                                  <div>
                                    <div className="text-xs text-muted-foreground">Total After Tariff</div>
                                    <div className="font-medium">{expandedDetail.data.result?.totalTariff != null ? Number(expandedDetail.data.result.totalTariff).toFixed(2) : '-'}</div>
                                  </div>
                                  <div>
                                    <div className="text-xs text-muted-foreground">RVC Threshold</div>
                                    <div className="font-medium">{expandedDetail.data.input?.rvcThreshold != null ? `${Number(expandedDetail.data.input.rvcThreshold).toFixed(2)}%` : '-'}</div>
                                  </div>
                                  <div>
                                    <div className="text-xs text-muted-foreground">Computed RVC</div>
                                    <div className="font-medium">{expandedDetail.data.result?.rvcComputed != null ? `${Number(expandedDetail.data.result.rvcComputed).toFixed(2)}%` : '-'}</div>
                                  </div>
                                  <div>
                                    <div className="text-xs text-muted-foreground">Applied Rate</div>
                                    <div className="font-medium">{expandedDetail.data.result?.appliedRate != null ? `${(Number(expandedDetail.data.result.appliedRate) * 100).toFixed(2)}%` : '-'}</div>
                                  </div>
                                </div>
                              </div>

                              <div className="rounded-md border bg-muted/20 p-4">
                                <div className="text-sm font-semibold mb-3">Cost Breakdown</div>
                                <div className="grid gap-3 md:grid-cols-2">
                                  <div>
                                    <div className="text-xs text-muted-foreground">Material Cost</div>
                                    <div className="font-medium">{expandedDetail.data.input?.materialCost != null ? Number(expandedDetail.data.input.materialCost).toFixed(2) : '-'}</div>
                                  </div>
                                  <div>
                                    <div className="text-xs text-muted-foreground">Labour Cost</div>
                                    <div className="font-medium">{expandedDetail.data.input?.labourCost != null ? Number(expandedDetail.data.input.labourCost).toFixed(2) : '-'}</div>
                                  </div>
                                  <div>
                                    <div className="text-xs text-muted-foreground">Overhead Cost</div>
                                    <div className="font-medium">{expandedDetail.data.input?.overheadCost != null ? Number(expandedDetail.data.input.overheadCost).toFixed(2) : '-'}</div>
                                  </div>
                                  <div>
                                    <div className="text-xs text-muted-foreground">Profit</div>
                                    <div className="font-medium">{expandedDetail.data.input?.profit != null ? Number(expandedDetail.data.input.profit).toFixed(2) : '-'}</div>
                                  </div>
                                  <div>
                                    <div className="text-xs text-muted-foreground">Other Costs</div>
                                    <div className="font-medium">{expandedDetail.data.input?.otherCosts != null ? Number(expandedDetail.data.input.otherCosts).toFixed(2) : '-'}</div>
                                  </div>
                                  <div>
                                    <div className="text-xs text-muted-foreground">FOB</div>
                                    <div className="font-medium">{expandedDetail.data.input?.fob != null ? Number(expandedDetail.data.input.fob).toFixed(2) : '-'}</div>
                                  </div>
                                </div>
                              </div>
                            </div>
                          </div>
                        </div>
                      )}
                    </td>
                  </tr>
                )}
              </>
            ))}
          </tbody>
        </table>
      </div>

      <div className="flex gap-2 items-center">
        <Button variant="outline" disabled={page<=0} onClick={() => setPage(p => Math.max(0, p-1))}>Prev</Button>
        <div className="text-sm">Page {page+1} / {Math.max(totalPages,1)}</div>
        <Button variant="outline" disabled={page+1>=totalPages} onClick={() => setPage(p => p+1)}>Next</Button>
      </div>
    </div>
  )
}

export default SavedTariffs
