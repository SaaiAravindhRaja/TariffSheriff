import { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { useCartStore, CartItem } from '@/store/cartStore'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { ShoppingCart, Trash2, Plus, Save, AlertCircle, Loader2 } from 'lucide-react'
import CartItemCard from '@/components/cart/CartItemCard'
import CountrySelect from '@/components/inputs/CountrySelect'
import HsCodeSelect, { type HsCodeOption } from '@/components/inputs/HsCodeSelect'
import { api, tariffApi, savedTariffsApi } from '@/services/api'
import { getDistance } from 'geolib'
import wc from 'world-countries'
import { useToast } from '@/components/ui/toast'
import { useAuth0 } from '@auth0/auth0-react'
import { useDbCountries } from '@/hooks/useDbCountries'

export function TariffCart() {
  const { isAuthenticated } = useAuth0()
  const { showToast } = useToast()
  const { items, addItem, updateItem, removeItem, clearCart, getTotalValue, getTotalTariff, getTotalWithTariff, getItemCount } = useCartStore()
  const { countries, loading: countriesLoading } = useDbCountries()
  
  // Form state: destination importer + one-at-a-time HS code entry
  const [newItem, setNewItem] = useState({
    hsCode: '',
    hsLabel: '',
    importerIso3: '',
    quantity: 1,
    unitValue: 0,
  })
  // Multi-select HS lines with per-line quantity and unit value
  const [selectedLines, setSelectedLines] = useState<Array<{ code: string; quantity: number; unitValue: number }>>([])
  
  const [calculating, setCalculating] = useState(false)
  const [saving, setSaving] = useState(false)
  const [saveName, setSaveName] = useState('')
  const [saveNotes, setSaveNotes] = useState('')
  const [pendingAdds, setPendingAdds] = useState<Array<{ id: string; hsCode: string; importerIso3: string }>>([])

  // Smart filtering state
  const [availableOrigins, setAvailableOrigins] = useState<string[]>([])
  const [availableHsCodes, setAvailableHsCodes] = useState<HsCodeOption[]>([])
  const [loadingOrigins, setLoadingOrigins] = useState(false)
  const [loadingHsCodes, setLoadingHsCodes] = useState(false)

  // Fetch available origin countries when importer is selected
  useEffect(() => {
    const fetchAvailableOrigins = async () => {
      if (!newItem.importerIso3) {
        setAvailableOrigins([])
        return
      }

      setLoadingOrigins(true)
      try {
        const response = await api.get<Array<{ importerIso3: string; originIso3: string; count: number }>>('/tariff-rate/routes')
        const routes = response.data
        
        const originsSet = new Set<string>()
        routes.forEach((route) => {
          if (route.importerIso3 === newItem.importerIso3 && route.originIso3) {
            originsSet.add(route.originIso3)
          }
        })
        
        const originsArray = Array.from(originsSet)
        setAvailableOrigins(originsArray)
        console.log(`✅ Cart: Found ${originsArray.length} export countries for importer ${newItem.importerIso3}`)
      } catch (err) {
        console.error('Failed to fetch available origins:', err)
        setAvailableOrigins([])
      } finally {
        setLoadingOrigins(false)
      }
    }

    fetchAvailableOrigins()
  }, [newItem.importerIso3])

  // Fetch available HS codes when importer is selected
  useEffect(() => {
    const fetchAvailableHsCodes = async () => {
      if (!newItem.importerIso3) {
        setAvailableHsCodes([])
        return
      }

      setLoadingHsCodes(true)
      try {
        const response = await api.get('/tariff-rate/', {
          params: {
            importerIso3: newItem.importerIso3,
            limit: 1000
          }
        })

        // Only include HS codes that have at least one trade route (i.e., originIso3 present)
        const grouped = new Map<string, { description: string; hasOrigin: boolean }>()
        ;(Array.isArray(response.data) ? response.data : []).forEach((rate: any) => {
          if (!rate?.hsCode) return
          const code = String(rate.hsCode).trim()
          const description = rate.description || 'No description available'
          const hasOrigin = !!rate.originIso3
          const existing = grouped.get(code)
          if (existing) {
            existing.hasOrigin = existing.hasOrigin || hasOrigin
          } else {
            grouped.set(code, { description, hasOrigin })
          }
        })
        const options: HsCodeOption[] = Array.from(grouped.entries())
          .filter(([, meta]) => meta.hasOrigin)
          .map(([code, meta]) => ({ code, description: meta.description }))
          .sort((a, b) => a.code.localeCompare(b.code))

        setAvailableHsCodes(options)
        console.log(`✅ Cart: Found ${options.length} HS codes for importer ${newItem.importerIso3}`)
      } catch (err) {
        console.error('Failed to fetch available HS codes:', err)
        setAvailableHsCodes([])
      } finally {
        setLoadingHsCodes(false)
      }
    }

    fetchAvailableHsCodes()
  }, [newItem.importerIso3])

  const handleAddToCart = async () => {
    if (!newItem.importerIso3 || selectedLines.length === 0) {
      showToast('Missing Information: Please fill in all required fields', 'error')
      return
    }

    setCalculating(true)
    const batchPending = selectedLines.map((l) => ({
      id: `pending-${l.code}-${Date.now()}-${Math.random().toString(36).slice(2, 4)}`,
      hsCode: l.code,
      importerIso3: newItem.importerIso3,
    }))
    setPendingAdds((prev) => [...prev, ...batchPending])
    const codeToPendingId = new Map(batchPending.map((p) => [p.hsCode, p.id]))
    // Build a quick map from current dropdown options for accurate descriptions
    const codeToDescription = new Map<string, string>(
      (availableHsCodes || []).map((o) => [o.code.replace(/\./g, ''), o.description || '']),
    )
    
    try {
      for (const line of selectedLines) {
        const pendingId = codeToPendingId.get(line.code)
        try {
          let hsCode = line.code.replace(/\./g, '').trim()
          if (hsCode.length > 0 && hsCode.length < 8 && /^\d+$/.test(hsCode)) {
            hsCode = hsCode.padEnd(8, '0')
          }

          const resolveCandidateOrigins = async () => {
            const listResp = await api.get('/tariff-rate/', {
              params: { importerIso3: newItem.importerIso3, hsCodes: [hsCode] }
            })
            const rows = (Array.isArray(listResp.data) ? listResp.data : listResp.data?.content || []) as any[]
            const origins = Array.from(new Set<string>(rows.map((row: any) => row.originIso3).filter((o: any) => !!o)))
            const description = (rows.find((r: any) => r.hsCode)?.description as string) || ''
            return { origins, description }
          }

          const { origins, description } = await resolveCandidateOrigins()

          // Always fetch MFN (origin-agnostic)
          let mfnRate: number | null = null
          try {
            const mfnResp = await tariffApi.getTariffRateLookup({ importerIso3: newItem.importerIso3, hsCode })
            const mfn = mfnResp.data?.rates?.find((r: any) => r.basis === 'MFN')
            mfnRate = mfn?.adValoremRate ?? null
          } catch (e) {
            console.error('Failed to fetch MFN rate for', hsCode, e)
          }

          let bestOrigin: string | null = null
          let bestPrefRate: number | null = null
          let bestRvc: number | null = null
          let bestAgreementName: string | null = null

          if (origins.length > 0) {
            const importerCenter = (() => {
              const c = (wc as any[]).find((x) => (x?.cca3 ?? '').toUpperCase() === newItem.importerIso3.toUpperCase())
              const latlng = c?.latlng as [number, number] | undefined // [lat, lon]
              if (!latlng) return null
              const [lat, lon] = latlng
              return { latitude: lat, longitude: lon }
            })()

            const lookups = await Promise.allSettled(
              origins.map((originIso3) =>
                tariffApi.getTariffRateLookup({ importerIso3: newItem.importerIso3, originIso3, hsCode })
                  .then(res => ({ originIso3, data: res.data }))
              )
            )

            type Candidate = {
              originIso3: string
              rvc: number | null
              rate: number | null
              agreementName: string | null
              distance: number | null
            }
            const candidates: Candidate[] = []
            for (const r of lookups) {
              if (r.status !== 'fulfilled') continue
              const { originIso3, data } = r.value as any
              const pref = data?.rates?.find((x: any) => x.basis === 'PREF' && x.adValoremRate != null)
              if (!pref) continue
              const rvcVal = pref.rvcThreshold != null ? Number(pref.rvcThreshold) : null
              const rateVal = pref.adValoremRate != null ? Number(pref.adValoremRate) : null
              let dist: number | null = null
              if (importerCenter) {
                const ocountry = (wc as any[]).find((x) => (x?.cca3 ?? '').toUpperCase() === originIso3.toUpperCase())
                const olatlng = ocountry?.latlng as [number, number] | undefined
                if (olatlng) {
                  const [olat, olon] = olatlng
                  dist = getDistance(importerCenter, { latitude: olat, longitude: olon })
                }
              }
              candidates.push({
                originIso3,
                rvc: rvcVal,
                rate: rateVal,
                agreementName: pref.agreementName ?? null,
                distance: dist,
              })
            }

            if (candidates.length > 0) {
              // Priority: lowest RVC (null => Infinity), then smallest distance, then lowest pref rate
              candidates.sort((a, b) => {
                const arvc = a.rvc == null ? Number.POSITIVE_INFINITY : a.rvc
                const brvc = b.rvc == null ? Number.POSITIVE_INFINITY : b.rvc
                if (arvc !== brvc) return arvc - brvc
                const ad = a.distance == null ? Number.POSITIVE_INFINITY : a.distance
                const bd = b.distance == null ? Number.POSITIVE_INFINITY : b.distance
                if (ad !== bd) return ad - bd
                const arate = a.rate == null ? Number.POSITIVE_INFINITY : a.rate
                const brate = b.rate == null ? Number.POSITIVE_INFINITY : b.rate
                return arate - brate
              })
              const top = candidates[0]
              bestOrigin = top.originIso3
              bestRvc = top.rvc
              bestPrefRate = top.rate
              bestAgreementName = top.agreementName
            }
          }

          if (mfnRate == null && bestPrefRate == null) {
            showToast(`No Tariff Rate Found: Could not find rate for HS code ${hsCode}`, 'error')
          } else {
            const normalizedCode = line.code.replace(/\./g, '')
            const finalDescription =
              codeToDescription.get(normalizedCode) ||
              description ||
              ''
            addItem({
              importerIso3: newItem.importerIso3,
              originIso3: bestOrigin || '',
              hsCode,
              hsLabel: finalDescription,
              mfnRate: mfnRate ?? undefined,
              preferentialRate: bestPrefRate ?? undefined,
              rvcThreshold: (bestRvc ?? undefined) as any,
              agreementName: bestAgreementName ?? undefined,
              quantity: line.quantity,
              unitValue: line.unitValue,
              selectedBasis: undefined as any,
              selectedRate: undefined as any,
              tariffAmount: 0,
              totalWithTariff: (line.quantity * line.unitValue),
            } as any)

            showToast(`Added HS ${hsCode}: best origin ${bestOrigin ?? 'N/A'}`, 'success')
          }
        } catch (err: any) {
          console.error('Failed to calculate tariff for line:', line.code, err)
          showToast(
            `Failed to add ${line.code}: ${err?.response?.data?.message || err?.message || 'Unknown error'}`,
            'error'
          )
        } finally {
          if (pendingId) {
            setPendingAdds((prev) => prev.filter((p) => p.id !== pendingId))
          }
        }
      }

      setSelectedLines([])
    } catch (error: any) {
      console.error('Failed to calculate tariff:', error)
      showToast(
        `Failed to Add Item: ${error?.response?.data?.message || 'Could not calculate tariff rate'}`,
        'error'
      )
    } finally {
      // any remaining pending (if any error path skipped finally) - clean up
      setPendingAdds((prev) => prev.filter((p) => !batchPending.find((b) => b.id === p.id)))
      setCalculating(false)
    }
  }

  const handleSaveCart = async () => {
    if (!isAuthenticated) {
      showToast('Login Required: Please login to save your cart', 'error')
      return
    }

    if (items.length === 0) {
      showToast('Empty Cart: Add items to cart before saving', 'error')
      return
    }

    setSaving(true)
    try {
      // Create calculation request for each item
      const calculationRequests = items.map((item) => ({
        name: saveName || `Multi-Product Import (${items.length} items)`,
        notes: saveNotes || `Includes: ${items.map(i => i.hsCode).join(', ')}`,
        importerIso3: item.importerIso3 || '',
        originIso3: item.originIso3 || '',
        hsCode: item.hsCode,
        quantity: item.quantity,
        unitValue: item.unitValue,
        totalValue: item.totalValue,
        basis: item.selectedBasis || 'MFN',
        tariffRate: item.selectedRate || 0,
        tariffAmount: item.tariffAmount || 0,
        totalWithTariff: item.totalWithTariff || item.totalValue,
        agreementId: item.agreementId,
        agreementName: item.agreementName,
        rvcPercentage: item.rvcPercentage,
      }))

      // Save each calculation (batch save not implemented in backend yet)
      for (const calc of calculationRequests) {
        await savedTariffsApi.save(calc)
      }

      showToast(`Cart Saved: Saved ${items.length} calculations`, 'success')
      
      clearCart()
      setSaveName('')
      setSaveNotes('')
    } catch (error) {
      console.error('Failed to save cart:', error)
      showToast('Save Failed: Failed to save cart', 'error')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="flex-1 space-y-6 p-6">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
      >
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold tracking-tight flex items-center gap-2">
              <ShoppingCart className="w-8 h-8" />
              Tariff Cart
            </h1>
            <p className="text-muted-foreground">
              Build your multi-product import scenario
            </p>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-lg font-semibold">{getItemCount()} items</span>
          </div>
        </div>
      </motion.div>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Add Item Form */}
        <motion.div
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.5, delay: 0.1 }}
          className="lg:col-span-1"
        >
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Plus className="w-5 h-5" />
                Add Item
              </CardTitle>
              <CardDescription>Add products to your import cart</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <label className="block text-sm font-medium mb-1">Importer Country</label>
                <CountrySelect
                  value={newItem.importerIso3}
                  onChange={(value: string | string[]) => {
                    const code = Array.isArray(value) ? value[0] : value
                    setNewItem({ ...newItem, importerIso3: code })
                  }}
                  placeholder="Select importer..."
                  countries={countries}
                  loading={countriesLoading}
                />
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">
                  HS Codes
                  {availableHsCodes.length > 0 && (
                    <span className="text-xs text-brand-500 ml-1">
                      • {availableHsCodes.length} available for this importer
                    </span>
                  )}
                </label>
                <HsCodeSelect
                  multi
                  values={selectedLines.map((l) => l.code)}
                  onChangeValues={(codes: string[]) => {
                    setSelectedLines((prev) => {
                      const prevMap = new Map(prev.map((l) => [l.code, l]))
                      const next: Array<{ code: string; quantity: number; unitValue: number }> = []
                      for (const code of codes) {
                        const existing = prevMap.get(code)
                        next.push({
                          code,
                          quantity: existing?.quantity ?? newItem.quantity,
                          unitValue: existing?.unitValue ?? newItem.unitValue,
                        })
                      }
                      return next
                    })
                  }}
                  placeholder="Search HS code..."
                  disabled={!newItem.importerIso3}
                  disabledMessage="Select importer first to load HS codes."
                  loading={loadingHsCodes}
                  options={availableHsCodes}
                />

                {/* Per-line quantity and unit value editors */}
                {selectedLines.length > 0 && (
                  <div className="mt-3 space-y-2">
                    {selectedLines.map((line, idx) => (
                      <div key={line.code} className="grid grid-cols-3 gap-2 items-end">
                        <div>
                          <label className="block text-xs text-gray-500">HS Code</label>
                          <div className="px-3 py-2 rounded-md border bg-gray-50 dark:bg-gray-800 text-sm font-mono">
                            {line.code}
                          </div>
                        </div>
                        <div>
                          <label className="block text-xs text-gray-500">Quantity</label>
                          <input
                            type="number"
                            min="1"
                            value={line.quantity}
                            onChange={(e) => {
                              const val = parseInt(e.target.value) || 1
                              setSelectedLines((prev) =>
                                prev.map((l) => (l.code === line.code ? { ...l, quantity: val } : l)),
                              )
                            }}
                            className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-brand-500 focus:border-brand-500 outline-none"
                          />
                        </div>
                        <div>
                          <label className="block text-xs text-gray-500">Unit Value (USD)</label>
                          <input
                            type="number"
                            min="0"
                            step="0.01"
                            value={line.unitValue}
                            onChange={(e) => {
                              const val = parseFloat(e.target.value) || 0
                              setSelectedLines((prev) =>
                                prev.map((l) => (l.code === line.code ? { ...l, unitValue: val } : l)),
                              )
                            }}
                            className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-brand-500 focus:border-brand-500 outline-none"
                          />
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <div className="pt-2 px-3 py-2 rounded-md bg-gray-100 dark:bg-gray-800 border border-gray-200 dark:border-gray-700">
                {selectedLines.length > 0 && (
                  <div className="mb-2 space-y-1 text-xs">
                    {selectedLines.map((line) => (
                      <div key={`subtotal-${line.code}`} className="flex justify-between">
                        <span className="font-mono">
                          {line.code} × {line.quantity} @ ${line.unitValue.toFixed(2)}
                        </span>
                        <span className="font-semibold">
                          ${(line.quantity * line.unitValue).toFixed(2)}
                        </span>
                      </div>
                    ))}
                  </div>
                )}
                <div className="flex justify-between items-center">
                  <span className="text-sm font-medium text-gray-600 dark:text-gray-400">Total Value:</span>
                  <span className="text-lg font-bold text-gray-900 dark:text-gray-100">
                    ${selectedLines.reduce((sum, l) => sum + (l.quantity * l.unitValue), 0).toFixed(2)}
                  </span>
                </div>
              </div>

              <Button onClick={handleAddToCart} className="w-full" disabled={calculating || !newItem.importerIso3 || selectedLines.length === 0}>
                {calculating ? (
                  <>
                    <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    Finding best origin...
                  </>
                ) : (
                  <>
                    <Plus className="w-4 h-4 mr-2" />
                    Add Selected HS Codes
                  </>
                )}
              </Button>
            </CardContent>
          </Card>
        </motion.div>

        {/* Cart Items */}
        <motion.div
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.5, delay: 0.2 }}
          className="lg:col-span-2"
        >
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle>Cart Items ({getItemCount()})</CardTitle>
                  <CardDescription>Tariffs calculated automatically when adding items</CardDescription>
                </div>
                <div className="flex gap-2">
                  <Button
                    variant="destructive"
                    size="sm"
                    onClick={clearCart}
                    disabled={items.length === 0}
                  >
                    Clear All
                  </Button>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              {items.length === 0 && pendingAdds.length === 0 ? (
                <div className="text-center py-12 text-gray-500">
                  <ShoppingCart className="w-16 h-16 mx-auto mb-4 opacity-20" />
                  <p>Your cart is empty</p>
                  <p className="text-sm">Add items using the form on the left</p>
                </div>
              ) : (
                <div className="space-y-3">
                  {items.map((item) => (
                    <CartItemCard
                      key={item.id}
                      hsCode={item.hsCode}
                      importerIso3={item.importerIso3}
                      originIso3={item.originIso3}
                      hsLabel={item.hsLabel}
                      agreementName={item.agreementName}
                      quantity={item.quantity}
                      unitValue={item.unitValue}
                      totalValue={item.totalValue}
                      mfnRate={item.mfnRate}
                      prefRate={item.preferentialRate}
                      rvcThreshold={item.rvcThreshold as any}
                      onRemove={() => removeItem(item.id)}
                    />
                  ))}
                  {pendingAdds.map((p) => (
                    <CartItemCard
                      key={p.id}
                      hsCode={p.hsCode}
                      importerIso3={p.importerIso3}
                      isLoading
                    />
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          {/* Summary & Save */}
          {items.length > 0 && (
            <Card className="mt-4">
              <CardHeader>
                <div className="flex items-center justify-between">
                  <CardTitle>Summary</CardTitle>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      try {
                        const style = `
                          <style>
                            :root { color-scheme: light dark; }
                            body { font-family: ui-sans-serif, system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial, 'Apple Color Emoji', 'Segoe UI Emoji'; margin: 24px; }
                            h1 { font-size: 20px; margin: 0 0 16px 0; }
                            h2 { font-size: 16px; margin: 16px 0 8px 0; }
                            table { width: 100%; border-collapse: collapse; margin-top: 8px; }
                            th, td { border: 1px solid #ddd; padding: 8px; font-size: 12px; vertical-align: top; }
                            th { background: #f5f5f5; text-align: left; }
                            .right { text-align: right; }
                            .muted { color: #666; font-size: 11px; }
                            .totals { margin-top: 16px; font-size: 14px; }
                          </style>
                        `
                        const currency = (n: number) => `$${(n || 0).toFixed(2)}`
                        const percent = (n?: number) => (n != null ? `${(n * 100).toFixed(2)}%` : '-') 
                        const rows = items.map((it: any) => {
                          const minRate = (it.preferentialRate ?? it.mfnRate) || 0
                          const maxRate = (it.mfnRate ?? it.preferentialRate) || 0
                          const minTotal = it.totalValue + (it.totalValue * minRate)
                          const maxTotal = it.totalValue + (it.totalValue * maxRate)
                          return `
                            <tr>
                              <td><div><strong>${it.hsCode}</strong></div><div class="muted">${it.hsLabel ?? ''}</div></td>
                              <td>${it.originIso3 || '-'}</td>
                              <td>${it.importerIso3 || '-'}</td>
                              <td class="right">${it.quantity}</td>
                              <td class="right">${currency(it.unitValue)}</td>
                              <td class="right">${currency(it.totalValue)}</td>
                              <td class="right">${percent(it.mfnRate)}</td>
                              <td class="right">${percent(it.preferentialRate)}</td>
                              <td class="right">${currency(minTotal)}</td>
                              <td class="right">${currency(maxTotal)}</td>
                            </tr>
                          `
                        }).join('')
                        const totals = items.reduce(
                          (acc: any, it: any) => {
                            const minRate = (it.preferentialRate ?? it.mfnRate) || 0
                            const maxRate = (it.mfnRate ?? it.preferentialRate) || 0
                            const minTotal = it.totalValue + (it.totalValue * minRate)
                            const maxTotal = it.totalValue + (it.totalValue * maxRate)
                            acc.min += minTotal
                            acc.max += maxTotal
                            acc.base += it.totalValue || 0
                            return acc
                          },
                          { min: 0, max: 0, base: 0 }
                        )
                        const html = `
                          <!doctype html>
                          <html>
                            <head>
                              <meta charset="utf-8" />
                              <title>Tariff Cart Export</title>
                              ${style}
                            </head>
                            <body>
                              <h1>Tariff Cart Export</h1>
                              <div class="muted">Generated on ${new Date().toLocaleString()}</div>
                              <h2>Items</h2>
                              <table>
                                <thead>
                                  <tr>
                                    <th>HS Code & Description</th>
                                    <th>Origin</th>
                                    <th>Importer</th>
                                    <th class="right">Qty</th>
                                    <th class="right">Unit</th>
                                    <th class="right">Value</th>
                                    <th class="right">MFN</th>
                                    <th class="right">Best PREF</th>
                                    <th class="right">Min Total</th>
                                    <th class="right">Max Total</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  ${rows}
                                </tbody>
                              </table>
                              <div class="totals">
                                <div><strong>Base Value:</strong> ${currency(totals.base)}</div>
                                <div><strong>Estimated minimum total (all PREF apply):</strong> ${currency(totals.min)}</div>
                                <div><strong>Estimated maximum total (MFN only):</strong> ${currency(totals.max)}</div>
                              </div>
                              <script>
                                window.onload = () => { window.print(); }
                              </script>
                            </body>
                          </html>
                        `
                        // Use an off-DOM iframe with srcdoc for reliable printing without pop-up blockers
                        const iframe = document.createElement('iframe')
                        iframe.style.position = 'fixed'
                        iframe.style.right = '0'
                        iframe.style.bottom = '0'
                        iframe.style.width = '0'
                        iframe.style.height = '0'
                        iframe.style.border = '0'
                        iframe.srcdoc = html
                        iframe.onload = () => {
                          try {
                            iframe.contentWindow?.focus()
                            iframe.contentWindow?.print()
                          } catch (e) {
                            console.error('Print failed:', e)
                          } finally {
                            setTimeout(() => iframe.remove(), 1500)
                          }
                        }
                        document.body.appendChild(iframe)
                      } catch (e) {
                        console.error('PDF export failed:', e)
                      }
                    }}
                  >
                    Export PDF
                  </Button>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  {(() => {
                    const totals = items.reduce(
                      (acc, it: any) => {
                        const minRate = (it.preferentialRate ?? it.mfnRate) || 0
                        const maxRate = (it.mfnRate ?? it.preferentialRate) || 0
                        const minTotal = it.totalValue + (it.totalValue * minRate)
                        const maxTotal = it.totalValue + (it.totalValue * maxRate)
                        acc.min += minTotal
                        acc.max += maxTotal
                        return acc
                      },
                      { min: 0, max: 0 }
                    )
                    return (
                      <>
                        <div className="flex justify-between text-lg">
                          <span>Estimated minimum total (all PREF apply):</span>
                          <span className="font-semibold">${totals.min.toFixed(2)}</span>
                        </div>
                        <div className="flex justify-between text-lg">
                          <span>Estimated maximum total (MFN only):</span>
                          <span className="font-semibold">${totals.max.toFixed(2)}</span>
                        </div>
                        <div className="flex justify-between text-xl font-bold border-t pt-2">
                          <span>Price range:</span>
                          <span>${totals.min.toFixed(2)} – ${totals.max.toFixed(2)}</span>
                        </div>
                      </>
                    )
                  })()}
                </div>

                {/* Saving disabled in this mode; calculations are rate+value derived */}
              </CardContent>
            </Card>
          )}
        </motion.div>
      </div>
    </div>
  )
}
