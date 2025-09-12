import React from 'react'
import { motion } from 'framer-motion'
import { Calculator, ArrowRight, Zap } from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { formatCurrency } from '@/lib/utils'

export function QuickCalculator() {
  const [formData, setFormData] = React.useState({
    productValue: '',
    quantity: '',
    origin: '',
    destination: ''
  })
  const [result, setResult] = React.useState<{
    tariffAmount: number
    totalCost: number
    tariffRate: number
  } | null>(null)
  const [isCalculating, setIsCalculating] = React.useState(false)

  const handleCalculate = async () => {
    if (!formData.productValue || !formData.quantity) return
    
    setIsCalculating(true)
    
    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    const baseValue = parseFloat(formData.productValue) * parseInt(formData.quantity)
    const tariffRate = 0.125 // 12.5% example rate
    const tariffAmount = baseValue * tariffRate
    const totalCost = baseValue + tariffAmount
    
    setResult({
      tariffAmount,
      totalCost,
      tariffRate
    })
    
    setIsCalculating(false)
  }

  const handleInputChange = (field: string, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }))
    setResult(null) // Clear result when inputs change
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center space-x-2">
          <div className="p-2 bg-brand-100 dark:bg-brand-900/20 rounded-lg">
            <Calculator className="w-5 h-5 text-brand-600" />
          </div>
          <div>
            <CardTitle>Quick Calculator</CardTitle>
            <CardDescription>
              Get instant tariff estimates for your shipments
            </CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-2">
            <label className="text-sm font-medium">Product Value ($)</label>
            <Input
              type="number"
              placeholder="1000"
              value={formData.productValue}
              onChange={(e) => handleInputChange('productValue', e.target.value)}
              className="input-focus"
            />
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium">Quantity</label>
            <Input
              type="number"
              placeholder="10"
              value={formData.quantity}
              onChange={(e) => handleInputChange('quantity', e.target.value)}
              className="input-focus"
            />
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-2">
            <label className="text-sm font-medium">Origin Country</label>
            <Input
              placeholder="e.g., China"
              value={formData.origin}
              onChange={(e) => handleInputChange('origin', e.target.value)}
              className="input-focus"
            />
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium">Destination</label>
            <Input
              placeholder="e.g., United States"
              value={formData.destination}
              onChange={(e) => handleInputChange('destination', e.target.value)}
              className="input-focus"
            />
          </div>
        </div>

        <Button 
          onClick={handleCalculate}
          disabled={!formData.productValue || !formData.quantity || isCalculating}
          className="w-full"
          variant="gradient"
        >
          {isCalculating ? (
            <div className="flex items-center space-x-2">
              <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
              <span>Calculating...</span>
            </div>
          ) : (
            <div className="flex items-center space-x-2">
              <Zap className="w-4 h-4" />
              <span>Calculate Tariff</span>
              <ArrowRight className="w-4 h-4" />
            </div>
          )}
        </Button>

        {result && (
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="mt-4 p-4 bg-gradient-to-r from-brand-50 to-brand-100 dark:from-brand-900/20 dark:to-brand-800/20 rounded-lg border border-brand-200 dark:border-brand-800"
          >
            <div className="flex items-center justify-between mb-3">
              <h4 className="font-medium text-brand-700 dark:text-brand-300">
                Calculation Result
              </h4>
              <Badge variant="success" className="text-xs">
                {(result.tariffRate * 100).toFixed(1)}% Rate
              </Badge>
            </div>
            
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-muted-foreground">Base Value:</span>
                <span className="font-medium">
                  {formatCurrency(result.totalCost - result.tariffAmount)}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Tariff Amount:</span>
                <span className="font-medium text-warning-600">
                  {formatCurrency(result.tariffAmount)}
                </span>
              </div>
              <div className="border-t pt-2 flex justify-between">
                <span className="font-medium">Total Cost:</span>
                <span className="font-bold text-lg text-brand-600">
                  {formatCurrency(result.totalCost)}
                </span>
              </div>
            </div>
          </motion.div>
        )}
      </CardContent>
    </Card>
  )
}