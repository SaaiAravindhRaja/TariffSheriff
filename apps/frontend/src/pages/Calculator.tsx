import React from 'react'
import { motion } from 'framer-motion'
import { 
  Calculator as CalculatorIcon, 
  Search, 
  Filter, 
  Save, 
  Download,
  Calendar,
  MapPin,
  Package,
  DollarSign,
  Percent,
  FileText,
  AlertCircle,
  CheckCircle,
  Clock,
  TrendingUp
} from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { formatCurrency, formatPercentage, getCountryFlag } from '@/lib/utils'
import { TariffBreakdownChart } from '@/components/calculator/TariffBreakdownChart'
import { ComparisonChart } from '@/components/calculator/ComparisonChart'
import { HistoricalRatesChart } from '@/components/calculator/HistoricalRatesChart'
import { CostAnalysisChart } from '@/components/calculator/CostAnalysisChart'

export function Calculator() {
  const [formData, setFormData] = React.useState({
    productDescription: '',
    hsCode: '',
    quantity: '',
    unitValue: '',
    currency: 'USD',
    originCountry: '',
    destinationCountry: '',
    shipmentDate: new Date().toISOString().split('T')[0],
    certificates: [] as string[]
  })

  const [result, setResult] = React.useState<{
    baseValue: number
    tariffRate: number
    tariffAmount: number
    additionalFees: number
    totalCost: number
    breakdown: Array<{
      type: string
      rate: number
      amount: number
      description: string
    }>
    appliedRules: Array<{
      ruleId: string
      description: string
      source: string
      validFrom: string
      validTo: string
    }>
    alternativeRoutes?: Array<{
      country: string
      tariffRate: number
      savings: number
    }>
  } | null>(null)

  const [isCalculating, setIsCalculating] = React.useState(false)
  const [showComparison, setShowComparison] = React.useState(false)

  const handleCalculate = async () => {
    if (!formData.productDescription || !formData.quantity || !formData.unitValue) return
    
    setIsCalculating(true)
    
    // Simulate API call with realistic data
    await new Promise(resolve => setTimeout(resolve, 2000))
    
    const baseValue = parseFloat(formData.unitValue) * parseInt(formData.quantity)
    const tariffRate = 0.125 // 12.5% example rate
    const tariffAmount = baseValue * tariffRate
    const additionalFees = baseValue * 0.025 // 2.5% additional fees
    const totalCost = baseValue + tariffAmount + additionalFees
    
    setResult({
      baseValue,
      tariffRate,
      tariffAmount,
      additionalFees,
      totalCost,
      breakdown: [
        {
          type: 'Base Tariff',
          rate: 0.125,
          amount: tariffAmount,
          description: 'Standard MFN tariff rate for electric vehicles'
        },
        {
          type: 'Processing Fee',
          rate: 0.015,
          amount: baseValue * 0.015,
          description: 'Customs processing and documentation fee'
        },
        {
          type: 'Security Fee',
          rate: 0.01,
          amount: baseValue * 0.01,
          description: 'Border security and inspection fee'
        }
      ],
      appliedRules: [
        {
          ruleId: 'US-8703.80-2024',
          description: 'Electric vehicles with lithium-ion batteries',
          source: 'US Harmonized Tariff Schedule',
          validFrom: '2024-01-01',
          validTo: '2024-12-31'
        },
        {
          ruleId: 'USMCA-AUTO-2024',
          description: 'USMCA automotive provisions',
          source: 'USMCA Trade Agreement',
          validFrom: '2020-07-01',
          validTo: '2026-07-01'
        }
      ],
      alternativeRoutes: [
        { country: 'Mexico', tariffRate: 0.08, savings: baseValue * 0.045 },
        { country: 'Canada', tariffRate: 0.095, savings: baseValue * 0.03 }
      ]
    })
    
    setIsCalculating(false)
  }

  const handleInputChange = (field: string, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }))
    if (result) setResult(null) // Clear result when inputs change
  }

  return (
    <div className="flex-1 space-y-6 p-6">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="flex items-center justify-between"
      >
        <div>
          <h1 className="text-3xl font-bold tracking-tight flex items-center gap-3">
            <CalculatorIcon className="w-8 h-8 text-brand-600" />
            Tariff Calculator
          </h1>
          <p className="text-muted-foreground">
            Calculate precise import tariffs and fees for your shipments
          </p>
        </div>
        <div className="flex items-center space-x-2">
          <Button variant="outline">
            <Filter className="w-4 h-4 mr-2" />
            Advanced Filters
          </Button>
          {result && (
            <>
              <Button variant="outline">
                <Download className="w-4 h-4 mr-2" />
                Export PDF
              </Button>
              <Button variant="outline">
                <Save className="w-4 h-4 mr-2" />
                Save Calculation
              </Button>
            </>
          )}
          <Button 
            variant="gradient"
            onClick={() => setShowComparison(!showComparison)}
            disabled={!result}
          >
            <TrendingUp className="w-4 h-4 mr-2" />
            Compare Routes
          </Button>
        </div>
      </motion.div>

      {/* Main Content */}
      <div className="grid gap-6 lg:grid-cols-3">
        {/* Input Form */}
        <motion.div
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.5, delay: 0.2 }}
          className="lg:col-span-2"
        >
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Package className="w-5 h-5" />
                Product Information
              </CardTitle>
              <CardDescription>
                Enter details about your product and shipment
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              {/* Product Details */}
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <label className="text-sm font-medium">Product Description</label>
                  <Input 
                    placeholder="e.g., Tesla Model Y Electric Vehicle"
                    value={formData.productDescription}
                    onChange={(e) => handleInputChange('productDescription', e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">HS Code</label>
                  <div className="relative">
                    <Input 
                      placeholder="e.g., 8703.80.10"
                      value={formData.hsCode}
                      onChange={(e) => handleInputChange('hsCode', e.target.value)}
                    />
                    <Search className="absolute right-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                  </div>
                </div>
              </div>
              
              {/* Quantity and Value */}
              <div className="grid grid-cols-3 gap-4">
                <div className="space-y-2">
                  <label className="text-sm font-medium">Quantity</label>
                  <Input 
                    type="number" 
                    placeholder="1"
                    value={formData.quantity}
                    onChange={(e) => handleInputChange('quantity', e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Unit Value</label>
                  <Input 
                    type="number" 
                    placeholder="45000"
                    value={formData.unitValue}
                    onChange={(e) => handleInputChange('unitValue', e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">Currency</label>
                  <Input 
                    placeholder="USD"
                    value={formData.currency}
                    onChange={(e) => handleInputChange('currency', e.target.value)}
                  />
                </div>
              </div>

              {/* Countries and Date */}
              <div className="grid grid-cols-3 gap-4">
                <div className="space-y-2">
                  <label className="text-sm font-medium flex items-center gap-1">
                    <MapPin className="w-4 h-4" />
                    Origin Country
                  </label>
                  <Input 
                    placeholder="e.g., China"
                    value={formData.originCountry}
                    onChange={(e) => handleInputChange('originCountry', e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium flex items-center gap-1">
                    <MapPin className="w-4 h-4" />
                    Destination Country
                  </label>
                  <Input 
                    placeholder="e.g., United States"
                    value={formData.destinationCountry}
                    onChange={(e) => handleInputChange('destinationCountry', e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium flex items-center gap-1">
                    <Calendar className="w-4 h-4" />
                    Shipment Date
                  </label>
                  <Input 
                    type="date"
                    value={formData.shipmentDate}
                    onChange={(e) => handleInputChange('shipmentDate', e.target.value)}
                  />
                </div>
              </div>

              <Button 
                onClick={handleCalculate}
                disabled={!formData.productDescription || !formData.quantity || !formData.unitValue || isCalculating}
                className="w-full" 
                variant="gradient" 
                size="lg"
              >
                {isCalculating ? (
                  <div className="flex items-center space-x-2">
                    <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                    <span>Calculating Tariffs...</span>
                  </div>
                ) : (
                  <div className="flex items-center space-x-2">
                    <CalculatorIcon className="w-5 h-5" />
                    <span>Calculate Tariff</span>
                  </div>
                )}
              </Button>
            </CardContent>
          </Card>
        </motion.div>

        {/* Results Panel */}
        <motion.div
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.5, delay: 0.3 }}
        >
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <DollarSign className="w-5 h-5" />
                Calculation Result
              </CardTitle>
              <CardDescription>
                Detailed breakdown of tariffs and fees
              </CardDescription>
            </CardHeader>
            <CardContent>
              {!result ? (
                <div className="text-center py-8 text-muted-foreground">
                  <CalculatorIcon className="w-12 h-12 mx-auto mb-4 opacity-50" />
                  <p>Enter product details to see calculation results</p>
                </div>
              ) : (
                <motion.div
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="space-y-4"
                >
                  {/* Summary */}
                  <div className="p-4 bg-gradient-to-r from-brand-50 to-brand-100 dark:from-brand-900/20 dark:to-brand-800/20 rounded-lg border border-brand-200 dark:border-brand-800">
                    <div className="flex items-center justify-between mb-3">
                      <h4 className="font-medium text-brand-700 dark:text-brand-300">
                        Total Import Cost
                      </h4>
                      <Badge variant="success" className="text-xs">
                        {formatPercentage(result.tariffRate)} Rate
                      </Badge>
                    </div>
                    
                    <div className="text-2xl font-bold text-brand-600 mb-2">
                      {formatCurrency(result.totalCost)}
                    </div>
                    
                    <div className="space-y-1 text-sm">
                      <div className="flex justify-between">
                        <span className="text-muted-foreground">Base Value:</span>
                        <span className="font-medium">{formatCurrency(result.baseValue)}</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-muted-foreground">Tariff:</span>
                        <span className="font-medium text-warning-600">
                          {formatCurrency(result.tariffAmount)}
                        </span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-muted-foreground">Additional Fees:</span>
                        <span className="font-medium text-warning-600">
                          {formatCurrency(result.additionalFees)}
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Breakdown */}
                  <div className="space-y-2">
                    <h5 className="font-medium text-sm flex items-center gap-1">
                      <Percent className="w-4 h-4" />
                      Fee Breakdown
                    </h5>
                    {result.breakdown.map((item, index) => (
                      <div key={index} className="flex items-center justify-between p-2 rounded border">
                        <div>
                          <div className="text-sm font-medium">{item.type}</div>
                          <div className="text-xs text-muted-foreground">{formatPercentage(item.rate)}</div>
                        </div>
                        <div className="text-sm font-medium">
                          {formatCurrency(item.amount)}
                        </div>
                      </div>
                    ))}
                  </div>

                  {/* Applied Rules */}
                  <div className="space-y-2">
                    <h5 className="font-medium text-sm flex items-center gap-1">
                      <FileText className="w-4 h-4" />
                      Applied Rules
                    </h5>
                    {result.appliedRules.map((rule, index) => (
                      <div key={index} className="p-2 rounded border">
                        <div className="flex items-center gap-2 mb-1">
                          <CheckCircle className="w-3 h-3 text-success-500" />
                          <div className="text-xs font-medium">{rule.ruleId}</div>
                        </div>
                        <div className="text-xs text-muted-foreground">
                          {rule.description}
                        </div>
                      </div>
                    ))}
                  </div>
                </motion.div>
              )}
            </CardContent>
          </Card>
        </motion.div>
      </div>

      {/* Charts and Analytics */}
      {result && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.5 }}
          className="grid gap-6 lg:grid-cols-2"
        >
          <TariffBreakdownChart data={result} />
          <HistoricalRatesChart 
            hsCode={formData.hsCode} 
            originCountry={formData.originCountry}
            destinationCountry={formData.destinationCountry}
          />
        </motion.div>
      )}

      {/* Comparison and Cost Analysis */}
      {result && showComparison && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.6 }}
          className="grid gap-6 lg:grid-cols-2"
        >
          <ComparisonChart 
            baseResult={result}
            alternatives={result.alternativeRoutes || []}
          />
          <CostAnalysisChart data={result} />
        </motion.div>
      )}
    </div>
  )
}