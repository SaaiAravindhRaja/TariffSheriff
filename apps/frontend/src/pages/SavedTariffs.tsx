import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { savedTariffsApi, SavedTariffSummary, PageResponse } from '@/services/api'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Loader2, Trash2 } from 'lucide-react'
import { useState } from 'react'

export function SavedTariffs() {
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['saved-tariffs', page, size],
    queryFn: async () => {
      const res = await savedTariffsApi.list({ page, size })
      return res.data as PageResponse<SavedTariffSummary>
    },
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

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {content.map(item => (
          <Card key={item.id} className="p-4 flex flex-col gap-3">
            <div className="flex justify-between items-start">
              <div>
                <div className="text-lg font-medium">{item.name || 'Untitled'}</div>
                <div className="text-xs text-muted-foreground">{new Date(item.createdAt).toLocaleString()}</div>
              </div>
              <Button variant="ghost" size="icon" onClick={() => del.mutate(item.id)} title="Delete">
                <Trash2 className="h-4 w-4" />
              </Button>
            </div>
            <div className="text-sm grid grid-cols-2 gap-y-1 gap-x-4">
              <div>Rate Used: <span className="font-medium">{item.rateUsed ?? '-'}</span></div>
              <div>Applied Rate: <span className="font-medium">{item.appliedRate != null ? (item.appliedRate * 100).toFixed(2) + '%' : '-'}</span></div>
              <div>Calculated RVC: <span className="font-medium">{item.rvcComputed != null ? item.rvcComputed.toFixed(2) + '%' : '-'}</span></div>
              <div>RVC Threshold: <span className="font-medium">{item.rvcThreshold != null ? item.rvcThreshold + '%' : 'N/A'}</span></div>
              <div>HS Code: <span className="font-medium">{item.hsCode ?? '-'}</span></div>
              <div>Route: <span className="font-medium">{item.importerIso2 ?? '-'} {item.originIso2 ? `← ${item.originIso2}` : ''}</span></div>
              <div className="col-span-2">Total Cost: <span className="font-medium">{item.totalTariff != null ? item.totalTariff.toFixed(2) : '-'}</span></div>
            </div>
          </Card>
        ))}
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
