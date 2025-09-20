import { useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
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
  TrendingUp,
  Info,
  Zap,
  Shield,
  Globe,
  BookOpen,
  BarChart3,
  RefreshCw,
  Copy,
  ExternalLink,
  Bookmark,
  History
} from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Switch } from '@/components/ui/switch';
import CountrySelect from '@/components/inputs/CountrySelect';
import { CountryFlag } from '@/components/ui/CountryFlag';
import { formatCurrency, formatPercentage, formatDate } from '@/lib/utils';
import { useSettings } from '@/contexts/SettingsContext';
import { TariffBreakdownChart } from '@/components/calculator/TariffBreakdownChart';
import { ComparisonChart } from '@/components/calculator/ComparisonChart';
import { HistoricalRatesChart } from '@/components/calculator/HistoricalRatesChart';
import { CostAnalysisChart } from '@/components/calculator/CostAnalysisChart';

// Enhanced interfaces for professional calculator
interface ProductInfo {
  description: string;
  hsCode: string;
  hsCodeDescription: string;
  category: string;
  quantity: number;
  unitValue: number;
  currency: string;
  weight: number;
  weightUnit: 'kg' | 'lbs';
  dimensions: {
    length: number;
    width: number;
    height: number;
    unit: 'cm' | 'in';
  };
  originCountry: string;
  destinationCountry: string;
  shipmentDate: string;
  incoterms: string;
  certificates: string[];
  specialConditions: string[];
}

interface TariffCalculation {
  id: string;
  timestamp: string;
  productInfo: ProductInfo;
  results: {
    baseValue: number;
    dutiableValue: number;
    tariffRate: number;
    tariffAmount: number;
    additionalDuties: number;
    taxes: {
      vat: number;
      excise: number;
      other: number;
    };
    fees: {
      processing: number;
      inspection: number;
      storage: number;
      other: number;
    };
    totalCost: number;
    effectiveRate: number;
    breakdown: Array<{
      type: string;
      category: 'duty' | 'tax' | 'fee';
      rate: number;
      amount: number;
      description: string;
      legal_basis: string;
    }>;
    appliedRules: Array<{
      ruleId: string;
      description: string;
      source: string;
      validFrom: string;
      validTo: string;
      confidence: number;
      tradeAgreement?: string;
    }>;
    warnings: Array<{
      type: 'info' | 'warning' | 'error';
      message: string;
      recommendation?: string;
    }>;
    alternativeRoutes: Array<{
      country: string;
      countryName: string;
      tariffRate: number;
      totalCost: number;
      savings: number;
      savingsPercentage: number;
      tradeAgreement?: string;
      transitTime?: number;
    }>;
    compliance: {
      requiredDocuments: string[];
      certificates: string[];
      restrictions: string[];
      prohibitions: string[];
    };
  };
}

interface HSCodeSuggestion {
  code: string;
  description: string;
  category: string;
  confidence: number;
}

export function Calculator() {
  const { settings } = useSettings();
  
  // Enhanced form state
  const [productInfo, setProductInfo] = useState<ProductInfo>({
    description: '',
    hsCode: '',
    hsCodeDescription: '',
    category: '',
    quantity: 1,
    unitValue: 0,
    currency: settings.currency,
    weight: 0,
    weightUnit: 'kg',
    dimensions: {
      length: 0,
      width: 0,
      height: 0,
      unit: 'cm'
    },
    originCountry: '',
    destinationCountry: '',
    shipmentDate: new Date().toISOString().split('T')[0],
    incoterms: 'CIF',
    certificates: [],
    specialConditions: []
  });

  // Calculation state
  const [calculation, setCalculation] = useState<TariffCalculation | null>(null);
  const [isCalculating, setIsCalculating] = useState(false);
  const [calculationHistory, setCalculationHistory] = useState<TariffCalculation[]>([]);
  
  // UI state
  const [activeTab, setActiveTab] = useState('basic');
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [showComparison, setShowComparison] = useState(false);
  const [hsCodeSuggestions, setHsCodeSuggestions] = useState<HSCodeSuggestion[]>([]);
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});
  
  // Auto-save to localStorage
  useEffect(() => {
    const saved = localStorage.getItem('tariff-calculator-draft');
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        setProductInfo(prev => ({ ...prev, ...parsed }));
      } catch (e) {
        console.error('Failed to load saved draft:', e);
      }
    }
  }, []);

  useEffect(() => {
    const timer = setTimeout(() => {
      localStorage.setItem('tariff-calculator-draft', JSON.stringify(productInfo));
    }, 1000);
    return () => clearTimeout(timer);
  }, [productInfo]);

  // Validation logic
  const validateForm = useCallback((): Record<string, string> => {
    const errors: Record<string, string> = {};
    
    if (!productInfo.description.trim()) {
      errors.description = 'Product description is required';
    }
    
    if (!productInfo.hsCode.trim()) {
      errors.hsCode = 'HS Code is required';
    } else if (!/^\d{4,10}(\.\d{2})*$/.test(productInfo.hsCode)) {
      errors.hsCode = 'Invalid HS Code format (e.g., 8703.80.10)';
    }
    
    if (productInfo.quantity <= 0) {
      errors.quantity = 'Quantity must be greater than 0';
    }
    
    if (productInfo.unitValue <= 0) {
      errors.unitValue = 'Unit value must be greater than 0';
    }
    
    if (!productInfo.originCountry) {
      errors.originCountry = 'Origin country is required';
    }
    
    if (!productInfo.destinationCountry) {
      errors.destinationCountry = 'Destination country is required';
    }
    
    if (productInfo.originCountry === productInfo.destinationCountry) {
      errors.destinationCountry = 'Origin and destination must be different';
    }
    
    return errors;
  }, [productInfo]);

  // HS Code lookup simulation
  const searchHSCode = useCallback(async (query: string) => {
    if (query.length < 3) {
      setHsCodeSuggestions([]);
      return;
    }
    
    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 300));
    
    const mockSuggestions: HSCodeSuggestion[] = [
      {
        code: '8703.80.10',
        description: 'Electric vehicles with lithium-ion batteries',
        category: 'Motor Vehicles',
        confidence: 0.95
      },
      {
        code: '8703.80.90',
        description: 'Other electric vehicles',
        category: 'Motor Vehicles',
        confidence: 0.87
      },
      {
        code: '8507.60.00',
        description: 'Lithium-ion batteries',
        category: 'Electrical Equipment',
        confidence: 0.72
      }
    ];
    
    setHsCodeSuggestions(mockSuggestions);
  }, []);

  // Enhanced calculation logic
  const handleCalculate = async () => {
    const errors = validateForm();
    setValidationErrors(errors);
    
    if (Object.keys(errors).length > 0) {
      return;
    }
    
    setIsCalculating(true);
    
    try {
      // Simulate comprehensive API call
      await new Promise(resolve => setTimeout(resolve, 3000));
      
      const baseValue = productInfo.unitValue * productInfo.quantity;
      const dutiableValue = baseValue; // Could be adjusted based on incoterms
      
      // Complex tariff calculation
      const baseTariffRate = 0.125; // 12.5%
      const preferentialRate = 0.08; // 8% with trade agreement
      const actualRate = productInfo.originCountry === 'MX' ? preferentialRate : baseTariffRate;
      
      const tariffAmount = dutiableValue * actualRate;
      const vatRate = 0.20; // 20% VAT
      const vatAmount = (dutiableValue + tariffAmount) * vatRate;
      
      const processingFee = Math.min(dutiableValue * 0.005, 500); // 0.5% capped at $500
      const inspectionFee = 75; // Fixed fee
      
      const totalCost = dutiableValue + tariffAmount + vatAmount + processingFee + inspectionFee;
      const effectiveRate = ((totalCost - dutiableValue) / dutiableValue) * 100;
      
      const newCalculation: TariffCalculation = {
        id: `calc_${Date.now()}`,
        timestamp: new Date().toISOString(),
        productInfo: { ...productInfo },
        results: {
          baseValue,
          dutiableValue,
          tariffRate: actualRate,
          tariffAmount,
          additionalDuties: 0,
          taxes: {
            vat: vatAmount,
            excise: 0,
            other: 0
          },
          fees: {
            processing: processingFee,
            inspection: inspectionFee,
            storage: 0,
            other: 0
          },
          totalCost,
          effectiveRate: effectiveRate / 100,
          breakdown: [
            {
              type: 'Import Duty',
              category: 'duty',
              rate: actualRate,
              amount: tariffAmount,
              description: `${actualRate === preferentialRate ? 'Preferential' : 'MFN'} tariff rate`,
              legal_basis: actualRate === preferentialRate ? 'USMCA Agreement' : 'WTO MFN Schedule'
            },
            {
              type: 'Value Added Tax',
              category: 'tax',
              rate: vatRate,
              amount: vatAmount,
              description: 'Standard VAT on imported goods',
              legal_basis: 'VAT Act Section 12'
            },
            {
              type: 'Processing Fee',
              category: 'fee',
              rate: 0.005,
              amount: processingFee,
              description: 'Customs processing and documentation',
              legal_basis: 'Customs Tariff Schedule'
            },
            {
              type: 'Inspection Fee',
              category: 'fee',
              rate: 0,
              amount: inspectionFee,
              description: 'Mandatory goods inspection',
              legal_basis: 'Import Safety Regulations'
            }
          ],
          appliedRules: [
            {
              ruleId: `${productInfo.destinationCountry}-${productInfo.hsCode}-2024`,
              description: `Tariff classification for ${productInfo.hsCodeDescription || 'specified goods'}`,
              source: 'Harmonized Tariff Schedule',
              validFrom: '2024-01-01',
              validTo: '2024-12-31',
              confidence: 0.95,
              tradeAgreement: actualRate === preferentialRate ? 'USMCA' : undefined
            }
          ],
          warnings: [
            ...(productInfo.weight > 1000 ? [{
              type: 'warning' as const,
              message: 'Heavy goods may require special handling',
              recommendation: 'Consider freight consolidation to reduce costs'
            }] : []),
            ...(baseValue > 100000 ? [{
              type: 'info' as const,
              message: 'High-value shipment detected',
              recommendation: 'Additional documentation may be required'
            }] : [])
          ],
          alternativeRoutes: [
            {
              country: 'MX',
              countryName: 'Mexico',
              tariffRate: 0.08,
              totalCost: dutiableValue + (dutiableValue * 0.08) + vatAmount + processingFee + inspectionFee,
              savings: actualRate === baseTariffRate ? (dutiableValue * (baseTariffRate - 0.08)) : 0,
              savingsPercentage: actualRate === baseTariffRate ? ((baseTariffRate - 0.08) / baseTariffRate) * 100 : 0,
              tradeAgreement: 'USMCA',
              transitTime: 5
            },
            {
              country: 'CA',
              countryName: 'Canada',
              tariffRate: 0.095,
              totalCost: dutiableValue + (dutiableValue * 0.095) + vatAmount + processingFee + inspectionFee,
              savings: actualRate === baseTariffRate ? (dutiableValue * (baseTariffRate - 0.095)) : 0,
              savingsPercentage: actualRate === baseTariffRate ? ((baseTariffRate - 0.095) / baseTariffRate) * 100 : 0,
              tradeAgreement: 'USMCA',
              transitTime: 3
            }
          ],
          compliance: {
            requiredDocuments: [
              'Commercial Invoice',
              'Packing List',
              'Bill of Lading',
              'Certificate of Origin'
            ],
            certificates: [
              'CE Marking (if applicable)',
              'Safety Certificate',
              'Environmental Compliance'
            ],
            restrictions: [
              'Import license may be required for quantities > 10 units',
              'Battery regulations apply for electric vehicles'
            ],
            prohibitions: []
          }
        }
      };
      
      setCalculation(newCalculation);
      setCalculationHistory(prev => [newCalculation, ...prev.slice(0, 9)]); // Keep last 10
      
    } catch (error) {
      console.error('Calculation failed:', error);
      setValidationErrors({ general: 'Calculation failed. Please try again.' });
    } finally {
      setIsCalculating(false);
    }
  };

  // Update product info
  const updateProductInfo = (field: keyof ProductInfo, value: any) => {
    setProductInfo(prev => ({ ...prev, [field]: value }));
    
    // Clear validation error for this field
    if (validationErrors[field]) {
      setValidationErrors(prev => {
        const newErrors = { ...prev };
        delete newErrors[field];
        return newErrors;
      });
    }
    
    // Clear calculation when inputs change
    if (calculation) {
      setCalculation(null);
    }
  };

  // Save calculation
  const saveCalculation = () => {
    if (calculation) {
      const saved = JSON.parse(localStorage.getItem('saved-calculations') || '[]');
      saved.unshift(calculation);
      localStorage.setItem('saved-calculations', JSON.stringify(saved.slice(0, 50))); // Keep last 50
    }
  };

  // Export calculation
  const exportCalculation = () => {
    if (calculation) {
      const dataStr = JSON.stringify(calculation, null, 2);
      const dataBlob = new Blob([dataStr], { type: 'application/json' });
      const url = URL.createObjectURL(dataBlob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `tariff-calculation-${calculation.id}.json`;
      link.click();
      URL.revokeObjectURL(url);
    }
  };

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
                  <CountrySelect
                    placeholder="Type to search (e.g., Saudi, Singapore)"
                    value={formData.originCountry}
                    onChange={(code) => {
                      const single = Array.isArray(code) ? code[0] ?? '' : code ?? ''
                      handleInputChange('originCountry', String(single))
                    }}
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium flex items-center gap-1">
                    <MapPin className="w-4 h-4" />
                    Destination Country
                  </label>
                  <CountrySelect
                    placeholder="Type to search (e.g., United, Spain)"
                    value={formData.destinationCountry}
                    onChange={(code) => {
                      const single = Array.isArray(code) ? code[0] ?? '' : code ?? ''
                      handleInputChange('destinationCountry', String(single))
                    }}
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