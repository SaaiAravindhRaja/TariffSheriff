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
      <h1 className="text-2xl font-semibold">Saved Tariffs</h1>

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
