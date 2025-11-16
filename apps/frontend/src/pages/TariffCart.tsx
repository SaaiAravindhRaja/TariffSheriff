import { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { useCartStore, CartItem } from '@/store/cartStore'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { ShoppingCart, Trash2, Plus, Save, AlertCircle, Loader2 } from 'lucide-react'
import CountrySelect from '@/components/inputs/CountrySelect'
import HsCodeSelect from '@/components/inputs/HsCodeSelect'
import { api, tariffApi, savedTariffsApi } from '@/services/api'
import { useToast } from '@/hooks/use-toast'
import { useAuth0 } from '@auth0/auth0-react'
import { useDbCountries } from '@/hooks/useDbCountries'

export function TariffCart() {
  const { isAuthenticated } = useAuth0()
  const { toast } = useToast()
  const { items, addItem, updateItem, removeItem, clearCart, getTotalValue, getTotalTariff, getTotalWithTariff, getItemCount } = useCartStore()
  const { countries, loading: countriesLoading } = useDbCountries()
  
  // Form state for adding new item
  const [newItem, setNewItem] = useState({
    hsCode: '',
    hsLabel: '',
    originIso3: '',
    importerIso3: '',
    quantity: 1,
    unitValue: 0,
  })
  
  const [calculating, setCalculating] = useState(false)
  const [saving, setSaving] = useState(false)
  const [saveName, setSaveName] = useState('')
  const [saveNotes, setSaveNotes] = useState('')

  // Smart filtering state
  const [availableOrigins, setAvailableOrigins] = useState<string[]>([])
  const [availableHsCodes, setAvailableHsCodes] = useState<string[]>([])
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

  // Fetch available HS codes when both importer and origin are selected
  useEffect(() => {
    const fetchAvailableHsCodes = async () => {
      if (!newItem.importerIso3 || !newItem.originIso3) {
        setAvailableHsCodes([])
        return
      }

      setLoadingHsCodes(true)
      try {
        const response = await api.get('/tariff-rate/', {
          params: {
            importerIso3: newItem.importerIso3,
            originIso3: newItem.originIso3,
            limit: 1000
          }
        })
        
        const hsCodesSet = new Set<string>()
        response.data.forEach((rate: any) => {
          if (rate.hsCode) {
            hsCodesSet.add(rate.hsCode)
          }
        })
        
        const hsCodesArray = Array.from(hsCodesSet)
        setAvailableHsCodes(hsCodesArray)
        console.log(`✅ Cart: Found ${hsCodesArray.length} HS codes for route ${newItem.importerIso3} → ${newItem.originIso3}`)
      } catch (err) {
        console.error('Failed to fetch available HS codes:', err)
        setAvailableHsCodes([])
      } finally {
        setLoadingHsCodes(false)
      }
    }

    fetchAvailableHsCodes()
  }, [newItem.importerIso3, newItem.originIso3])

  const handleAddToCart = async () => {
    if (!newItem.hsCode || !newItem.importerIso3 || !newItem.originIso3 || newItem.unitValue <= 0) {
      toast({
        title: 'Missing Information',
        description: 'Please fill in all required fields',
        variant: 'destructive',
      })
      return
    }

    setCalculating(true)
    
    try {
      // Pad HS code to 8 digits with trailing zeros if needed
      let hsCode = newItem.hsCode.replace(/\./g, '').trim()
      if (hsCode.length > 0 && hsCode.length < 8 && /^\d+$/.test(hsCode)) {
        hsCode = hsCode.padEnd(8, '0')
        console.log(`⚠️ TariffCart: Padded HS code from ${newItem.hsCode} to ${hsCode}`)
      }
      
      // Calculate tariff immediately when adding
      const response = await tariffApi.getTariffRateLookup({
        importerIso3: newItem.importerIso3,
        originIso3: newItem.originIso3,
        hsCode: hsCode,
      })
      
      const data = response.data
      const mfnRate = data.rates?.find((r) => r.basis === 'MFN')
      const prefRate = data.rates?.find((r) => r.basis === 'PREF')
      
      // Use preferential rate if available, otherwise MFN
      const selectedRate = prefRate || mfnRate
      
      if (!selectedRate || selectedRate.adValoremRate === null) {
        toast({
          title: 'No Tariff Rate Found',
          description: `Could not find tariff rate for HS code ${hsCode}`,
          variant: 'destructive',
        })
        setCalculating(false)
        return
      }
      
      const totalValue = newItem.quantity * newItem.unitValue
      const tariffAmount = (totalValue * selectedRate.adValoremRate) / 100
      
      // Add item to cart with calculated tariff (use padded HS code)
      addItem({
        ...newItem,
        hsCode: hsCode,  // Use the padded HS code
        mfnRate: mfnRate?.adValoremRate,
        preferentialRate: prefRate?.adValoremRate,
        selectedBasis: selectedRate.basis as 'MFN' | 'PREF',
        selectedRate: selectedRate.adValoremRate,
        tariffAmount,
        totalWithTariff: totalValue + tariffAmount,
        agreementId: selectedRate.agreementId,
        agreementName: selectedRate.agreementName,
        rvcThreshold: selectedRate.rvcThreshold,
      } as any)
      
      // Reset form
      setNewItem({
        hsCode: '',
        hsLabel: '',
        originIso3: '',
        importerIso3: '',
        quantity: 1,
        unitValue: 0,
      })
      
      toast({
        title: 'Added to Cart',
        description: `Item added with ${selectedRate.adValoremRate}% tariff rate`,
      })
    } catch (error: any) {
      console.error('Failed to calculate tariff:', error)
      toast({
        title: 'Failed to Add Item',
        description: error.response?.data?.message || 'Could not calculate tariff rate',
        variant: 'destructive',
      })
    } finally {
      setCalculating(false)
    }
  }

  const handleSaveCart = async () => {
    if (!isAuthenticated) {
      toast({
        title: 'Login Required',
        description: 'Please login to save your cart',
        variant: 'destructive',
      })
      return
    }

    if (items.length === 0) {
      toast({
        title: 'Empty Cart',
        description: 'Add items to cart before saving',
        variant: 'destructive',
      })
      return
    }

    setSaving(true)
    try {
      // Create calculation request for each item
      const calculationRequests = items.map((item) => ({
        name: saveName || `Multi-Product Import (${items.length} items)`,
        notes: saveNotes || `Includes: ${items.map(i => i.hsCode).join(', ')}`,
        importerIso2: item.importerIso3?.substring(0, 2), // Convert ISO3 to ISO2
        originIso2: item.originIso3?.substring(0, 2),
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

      toast({
        title: 'Cart Saved',
        description: `Saved ${items.length} calculations`,
      })
      
      clearCart()
      setSaveName('')
      setSaveNotes('')
    } catch (error) {
      console.error('Failed to save cart:', error)
      toast({
        title: 'Save Failed',
        description: 'Failed to save cart',
        variant: 'destructive',
      })
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
                  Origin Country
                  {availableOrigins.length > 0 && (
                    <span className="text-xs text-brand-500 ml-1">
                      • {availableOrigins.length} trade route{availableOrigins.length !== 1 ? 's' : ''} available
                    </span>
                  )}
                </label>
                <CountrySelect
                  value={newItem.originIso3}
                  onChange={(value: string | string[]) => {
                    const code = Array.isArray(value) ? value[0] : value
                    setNewItem({ ...newItem, originIso3: code })
                  }}
                  placeholder="Select origin..."
                  countries={countries}
                  loading={countriesLoading || loadingOrigins}
                />
                {availableOrigins.length > 0 && !availableOrigins.includes(newItem.originIso3) && newItem.originIso3 && (
                  <p className="text-xs text-amber-600 mt-1">
                    ⚠️ No direct trade data available for this route
                  </p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">
                  HS Code & Product
                  {availableHsCodes.length > 0 && (
                    <span className="text-xs text-brand-500 ml-1">
                      • {availableHsCodes.length} code{availableHsCodes.length !== 1 ? 's' : ''} available for this route
                    </span>
                  )}
                </label>
                <HsCodeSelect
                  value={newItem.hsCode}
                  onChange={(code: string, label?: string) => {
                    setNewItem({ ...newItem, hsCode: code, hsLabel: label || '' })
                  }}
                  placeholder="Search HS code..."
                  filterCodes={availableHsCodes.length > 0 ? availableHsCodes : undefined}
                />
                {availableHsCodes.length > 0 && !availableHsCodes.includes(newItem.hsCode.replace(/\./g, '')) && newItem.hsCode && (
                  <p className="text-xs text-amber-600 mt-1">
                    ⚠️ This HS code may not have data for the selected route
                  </p>
                )}
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-sm font-medium mb-1">Quantity</label>
                  <input
                    type="number"
                    min="1"
                    value={newItem.quantity}
                    onChange={(e) => setNewItem({ ...newItem, quantity: parseInt(e.target.value) || 1 })}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-brand-500 focus:border-brand-500 outline-none"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">Unit Value (USD)</label>
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={newItem.unitValue}
                    onChange={(e) => setNewItem({ ...newItem, unitValue: parseFloat(e.target.value) || 0 })}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 focus:ring-2 focus:ring-brand-500 focus:border-brand-500 outline-none"
                  />
                </div>
              </div>

              <div className="pt-2 px-3 py-2 rounded-md bg-gray-100 dark:bg-gray-800 border border-gray-200 dark:border-gray-700">
                <div className="flex justify-between items-center">
                  <span className="text-sm font-medium text-gray-600 dark:text-gray-400">Total Value:</span>
                  <span className="text-lg font-bold text-gray-900 dark:text-gray-100">
                    ${(newItem.quantity * newItem.unitValue).toFixed(2)}
                  </span>
                </div>
              </div>

              <Button onClick={handleAddToCart} className="w-full" disabled={calculating}>
                {calculating ? (
                  <>
                    <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    Calculating...
                  </>
                ) : (
                  <>
                    <Plus className="w-4 h-4 mr-2" />
                    Add to Cart
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
              {items.length === 0 ? (
                <div className="text-center py-12 text-gray-500">
                  <ShoppingCart className="w-16 h-16 mx-auto mb-4 opacity-20" />
                  <p>Your cart is empty</p>
                  <p className="text-sm">Add items using the form on the left</p>
                </div>
              ) : (
                <div className="space-y-3">
                  {items.map((item) => (
                    <div
                      key={item.id}
                      className="border rounded-lg p-4 bg-gray-50 dark:bg-gray-800/50"
                    >
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <div className="flex items-center gap-2 mb-2">
                            <span className="font-mono text-sm font-semibold text-blue-600 dark:text-blue-400">
                              {item.hsCode}
                            </span>
                            <span className="text-xs text-gray-500">
                              {item.originIso3} → {item.importerIso3}
                            </span>
                          </div>
                          <p className="text-sm text-gray-700 dark:text-gray-300 mb-2">
                            {item.hsLabel || 'Product description not available'}
                          </p>
                          <div className="grid grid-cols-2 gap-2 text-sm">
                            <div>
                              <span className="text-gray-500">Quantity:</span> {item.quantity}
                            </div>
                            <div>
                              <span className="text-gray-500">Unit Value:</span> ${item.unitValue.toFixed(2)}
                            </div>
                            <div>
                              <span className="text-gray-500">Total Value:</span> ${item.totalValue.toFixed(2)}
                            </div>
                            {item.selectedRate !== undefined && (
                              <>
                                <div>
                                  <span className="text-gray-500">Rate:</span> {item.selectedRate}% ({item.selectedBasis})
                                </div>
                                <div>
                                  <span className="text-gray-500">Tariff:</span> ${item.tariffAmount?.toFixed(2)}
                                </div>
                                <div className="font-semibold">
                                  <span className="text-gray-500">Total:</span> ${item.totalWithTariff?.toFixed(2)}
                                </div>
                              </>
                            )}
                            {item.agreementName && (
                              <div className="col-span-2 text-xs text-green-600 dark:text-green-400">
                                Agreement: {item.agreementName}
                              </div>
                            )}
                          </div>
                        </div>
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => removeItem(item.id)}
                          className="text-red-500 hover:text-red-700 hover:bg-red-50 dark:hover:bg-red-900/20"
                        >
                          <Trash2 className="w-4 h-4" />
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          {/* Summary & Save */}
          {items.length > 0 && (
            <Card className="mt-4">
              <CardHeader>
                <CardTitle>Summary</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <div className="flex justify-between text-lg">
                    <span>Total Value:</span>
                    <span className="font-semibold">${getTotalValue().toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between text-lg text-orange-600 dark:text-orange-400">
                    <span>Total Tariffs:</span>
                    <span className="font-semibold">${getTotalTariff().toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between text-xl font-bold border-t pt-2">
                    <span>Total with Tariffs:</span>
                    <span>${getTotalWithTariff().toFixed(2)}</span>
                  </div>
                </div>

                {isAuthenticated && (
                  <div className="border-t pt-4 space-y-3">
                    <div>
                      <label className="block text-sm font-medium mb-1">Save As (Optional)</label>
                      <input
                        type="text"
                        value={saveName}
                        onChange={(e) => setSaveName(e.target.value)}
                        placeholder="e.g., EV Parts Import Q1 2025"
                        className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 placeholder:text-gray-400 dark:placeholder:text-gray-500 focus:ring-2 focus:ring-brand-500 focus:border-brand-500 outline-none"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium mb-1">Notes (Optional)</label>
                      <textarea
                        value={saveNotes}
                        onChange={(e) => setSaveNotes(e.target.value)}
                        placeholder="Add notes about this import scenario..."
                        rows={2}
                        className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 placeholder:text-gray-400 dark:placeholder:text-gray-500 focus:ring-2 focus:ring-brand-500 focus:border-brand-500 outline-none resize-none"
                      />
                    </div>
                    <Button onClick={handleSaveCart} disabled={saving} className="w-full">
                      <Save className="w-4 h-4 mr-2" />
                      {saving ? 'Saving...' : 'Save Cart'}
                    </Button>
                  </div>
                )}

                {!isAuthenticated && (
                  <div className="flex items-start gap-2 p-3 bg-blue-50 dark:bg-blue-900/20 rounded-lg text-sm">
                    <AlertCircle className="w-4 h-4 text-blue-600 mt-0.5" />
                    <p className="text-blue-700 dark:text-blue-300">
                      Login to save your cart for later
                    </p>
                  </div>
                )}
              </CardContent>
            </Card>
          )}
        </motion.div>
      </div>
    </div>
  )
}
