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
    <div className="flex-1 space-y-6 p-6 max-w-7xl mx-auto">
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
            Professional Tariff Calculator
          </h1>
          <p className="text-muted-foreground">
            Industry-standard import duty and tax calculations with compliance insights
          </p>
        </div>
        <div className="flex items-center space-x-2">
          <Button 
            variant="outline" 
            onClick={() => setShowAdvanced(!showAdvanced)}
          >
            <Filter className="w-4 h-4 mr-2" />
            {showAdvanced ? 'Basic' : 'Advanced'} Mode
          </Button>
          {calculation && (
            <>
              <Button variant="outline" onClick={exportCalculation}>
                <Download className="w-4 h-4 mr-2" />
                Export
              </Button>
              <Button variant="outline" onClick={saveCalculation}>
                <Save className="w-4 h-4 mr-2" />
                Save
              </Button>
            </>
          )}
          <Button 
            variant="gradient"
            onClick={() => setShowComparison(!showComparison)}
            disabled={!calculation}
          >
            <TrendingUp className="w-4 h-4 mr-2" />
            Route Analysis
          </Button>
        </div>
      </motion.div>

      {/* Validation Errors */}
      {Object.keys(validationErrors).length > 0 && (
        <motion.div
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-4"
        >
          <div className="flex items-start">
            <AlertCircle className="w-5 h-5 text-red-600 dark:text-red-400 mr-2 mt-0.5" />
            <div>
              <h4 className="font-medium text-red-800 dark:text-red-200">Please correct the following errors:</h4>
              <ul className="mt-2 text-sm text-red-700 dark:text-red-300 space-y-1">
                {Object.entries(validationErrors).map(([field, error]) => (
                  <li key={field}>• {error}</li>
                ))}
              </ul>
            </div>
          </div>
        </motion.div>
      )}

      {/* Main Content */}
      <div className="grid gap-6 lg:grid-cols-3">
        {/* Enhanced Input Form */}
        <motion.div
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.5, delay: 0.2 }}
          className="lg:col-span-2"
        >
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Package className="w-5 h-5" />
                  Product & Shipment Details
                </div>
                <div className="flex items-center gap-2">
                  <Switch
                    checked={showAdvanced}
                    onCheckedChange={setShowAdvanced}
                  />
                  <span className="text-sm text-muted-foreground">Advanced</span>
                </div>
              </CardTitle>
              <CardDescription>
                Enter comprehensive product and shipment information for accurate calculations
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
                <TabsList className="grid w-full grid-cols-4">
                  <TabsTrigger value="basic">Basic Info</TabsTrigger>
                  <TabsTrigger value="classification">Classification</TabsTrigger>
                  <TabsTrigger value="logistics">Logistics</TabsTrigger>
                  <TabsTrigger value="compliance">Compliance</TabsTrigger>
                </TabsList>

                {/* Basic Information Tab */}
                <TabsContent value="basic" className="space-y-4">
                  <div className="grid grid-cols-1 gap-4">
                    <div className="space-y-2">
                      <label className="text-sm font-medium">Product Description *</label>
                      <Input 
                        placeholder="e.g., Tesla Model Y Long Range Electric Vehicle"
                        value={productInfo.description}
                        onChange={(e) => updateProductInfo('description', e.target.value)}
                        className={validationErrors.description ? 'border-red-500' : ''}
                      />
                      {validationErrors.description && (
                        <p className="text-sm text-red-600">{validationErrors.description}</p>
                      )}
                    </div>
                    
                    <div className="grid grid-cols-3 gap-4">
                      <div className="space-y-2">
                        <label className="text-sm font-medium">Quantity *</label>
                        <Input 
                          type="number" 
                          min="1"
                          placeholder="1"
                          value={productInfo.quantity || ''}
                          onChange={(e) => updateProductInfo('quantity', parseInt(e.target.value) || 0)}
                          className={validationErrors.quantity ? 'border-red-500' : ''}
                        />
                      </div>
                      <div className="space-y-2">
                        <label className="text-sm font-medium">Unit Value *</label>
                        <Input 
                          type="number" 
                          min="0"
                          step="0.01"
                          placeholder="45000.00"
                          value={productInfo.unitValue || ''}
                          onChange={(e) => updateProductInfo('unitValue', parseFloat(e.target.value) || 0)}
                          className={validationErrors.unitValue ? 'border-red-500' : ''}
                        />
                      </div>
                      <div className="space-y-2">
                        <label className="text-sm font-medium">Currency</label>
                        <select
                          className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-800"
                          value={productInfo.currency}
                          onChange={(e) => updateProductInfo('currency', e.target.value)}
                        >
                          <option value="USD">USD - US Dollar</option>
                          <option value="EUR">EUR - Euro</option>
                          <option value="GBP">GBP - British Pound</option>
                          <option value="JPY">JPY - Japanese Yen</option>
                          <option value="CAD">CAD - Canadian Dollar</option>
                          <option value="AUD">AUD - Australian Dollar</option>
                        </select>
                      </div>
                    </div>

                    <div className="grid grid-cols-3 gap-4">
                      <div className="space-y-2">
                        <label className="text-sm font-medium flex items-center gap-1">
                          <MapPin className="w-4 h-4" />
                          Origin Country *
                        </label>
                        <CountrySelect
                          placeholder="Select origin country"
                          value={productInfo.originCountry}
                          onChange={(code) => {
                            const single = Array.isArray(code) ? code[0] ?? '' : code ?? '';
                            updateProductInfo('originCountry', String(single));
                          }}
                          className={validationErrors.originCountry ? 'border-red-500' : ''}
                        />
                      </div>
                      <div className="space-y-2">
                        <label className="text-sm font-medium flex items-center gap-1">
                          <MapPin className="w-4 h-4" />
                          Destination Country *
                        </label>
                        <CountrySelect
                          placeholder="Select destination country"
                          value={productInfo.destinationCountry}
                          onChange={(code) => {
                            const single = Array.isArray(code) ? code[0] ?? '' : code ?? '';
                            updateProductInfo('destinationCountry', String(single));
                          }}
                          className={validationErrors.destinationCountry ? 'border-red-500' : ''}
                        />
                      </div>
                      <div className="space-y-2">
                        <label className="text-sm font-medium flex items-center gap-1">
                          <Calendar className="w-4 h-4" />
                          Shipment Date
                        </label>
                        <Input 
                          type="date"
                          value={productInfo.shipmentDate}
                          onChange={(e) => updateProductInfo('shipmentDate', e.target.value)}
                        />
                      </div>
                    </div>
                  </div>
                </TabsContent>

                {/* Classification Tab */}
                <TabsContent value="classification" className="space-y-4">
                  <div className="space-y-4">
                    <div className="space-y-2">
                      <label className="text-sm font-medium">HS Code *</label>
                      <div className="relative">
                        <Input 
                          placeholder="e.g., 8703.80.10"
                          value={productInfo.hsCode}
                          onChange={(e) => {
                            updateProductInfo('hsCode', e.target.value);
                            searchHSCode(e.target.value);
                          }}
                          className={validationErrors.hsCode ? 'border-red-500' : ''}
                        />
                        <Search className="absolute right-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                      </div>
                      {validationErrors.hsCode && (
                        <p className="text-sm text-red-600">{validationErrors.hsCode}</p>
                      )}
                    </div>

                    {/* HS Code Suggestions */}
                    {hsCodeSuggestions.length > 0 && (
                      <div className="space-y-2">
                        <label className="text-sm font-medium">Suggested Classifications</label>
                        <div className="space-y-2">
                          {hsCodeSuggestions.map((suggestion, index) => (
                            <button
                              key={index}
                              type="button"
                              onClick={() => {
                                updateProductInfo('hsCode', suggestion.code);
                                updateProductInfo('hsCodeDescription', suggestion.description);
                                updateProductInfo('category', suggestion.category);
                                setHsCodeSuggestions([]);
                              }}
                              className="w-full text-left p-3 border rounded-lg hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors"
                            >
                              <div className="flex items-center justify-between">
                                <div>
                                  <div className="font-medium">{suggestion.code}</div>
                                  <div className="text-sm text-muted-foreground">{suggestion.description}</div>
                                  <div className="text-xs text-muted-foreground">{suggestion.category}</div>
                                </div>
                                <Badge variant="secondary">
                                  {Math.round(suggestion.confidence * 100)}% match
                                </Badge>
                              </div>
                            </button>
                          ))}
                        </div>
                      </div>
                    )}

                    <div className="space-y-2">
                      <label className="text-sm font-medium">HS Code Description</label>
                      <Input 
                        placeholder="Auto-filled from HS code lookup"
                        value={productInfo.hsCodeDescription}
                        onChange={(e) => updateProductInfo('hsCodeDescription', e.target.value)}
                        disabled
                      />
                    </div>

                    <div className="space-y-2">
                      <label className="text-sm font-medium">Product Category</label>
                      <select
                        className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-800"
                        value={productInfo.category}
                        onChange={(e) => updateProductInfo('category', e.target.value)}
                      >
                        <option value="">Select category</option>
                        <option value="Electronics">Electronics</option>
                        <option value="Automotive">Automotive</option>
                        <option value="Textiles">Textiles</option>
                        <option value="Machinery">Machinery</option>
                        <option value="Chemicals">Chemicals</option>
                        <option value="Food & Beverages">Food & Beverages</option>
                      </select>
                    </div>
                  </div>
                </TabsContent>

                {/* Logistics Tab */}
                <TabsContent value="logistics" className="space-y-4">
                  {showAdvanced && (
                    <div className="space-y-4">
                      <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                          <label className="text-sm font-medium">Weight</label>
                          <div className="flex gap-2">
                            <Input 
                              type="number"
                              min="0"
                              step="0.1"
                              placeholder="1500"
                              value={productInfo.weight || ''}
                              onChange={(e) => updateProductInfo('weight', parseFloat(e.target.value) || 0)}
                            />
                            <select
                              className="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-800"
                              value={productInfo.weightUnit}
                              onChange={(e) => updateProductInfo('weightUnit', e.target.value)}
                            >
                              <option value="kg">kg</option>
                              <option value="lbs">lbs</option>
                            </select>
                          </div>
                        </div>
                        <div className="space-y-2">
                          <label className="text-sm font-medium">Incoterms</label>
                          <select
                            className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-800"
                            value={productInfo.incoterms}
                            onChange={(e) => updateProductInfo('incoterms', e.target.value)}
                          >
                            <option value="CIF">CIF - Cost, Insurance & Freight</option>
                            <option value="FOB">FOB - Free on Board</option>
                            <option value="EXW">EXW - Ex Works</option>
                            <option value="DDP">DDP - Delivered Duty Paid</option>
                            <option value="DDU">DDU - Delivered Duty Unpaid</option>
                          </select>
                        </div>
                      </div>

                      <div className="space-y-2">
                        <label className="text-sm font-medium">Dimensions (L × W × H)</label>
                        <div className="flex gap-2">
                          <Input 
                            type="number"
                            placeholder="Length"
                            value={productInfo.dimensions.length || ''}
                            onChange={(e) => updateProductInfo('dimensions', {
                              ...productInfo.dimensions,
                              length: parseFloat(e.target.value) || 0
                            })}
                          />
                          <Input 
                            type="number"
                            placeholder="Width"
                            value={productInfo.dimensions.width || ''}
                            onChange={(e) => updateProductInfo('dimensions', {
                              ...productInfo.dimensions,
                              width: parseFloat(e.target.value) || 0
                            })}
                          />
                          <Input 
                            type="number"
                            placeholder="Height"
                            value={productInfo.dimensions.height || ''}
                            onChange={(e) => updateProductInfo('dimensions', {
                              ...productInfo.dimensions,
                              height: parseFloat(e.target.value) || 0
                            })}
                          />
                          <select
                            className="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-800"
                            value={productInfo.dimensions.unit}
                            onChange={(e) => updateProductInfo('dimensions', {
                              ...productInfo.dimensions,
                              unit: e.target.value as 'cm' | 'in'
                            })}
                          >
                            <option value="cm">cm</option>
                            <option value="in">in</option>
                          </select>
                        </div>
                      </div>
                    </div>
                  )}
                </TabsContent>

                {/* Compliance Tab */}
                <TabsContent value="compliance" className="space-y-4">
                  {showAdvanced && (
                    <div className="space-y-4">
                      <div className="space-y-2">
                        <label className="text-sm font-medium">Certificates & Licenses</label>
                        <div className="grid grid-cols-2 gap-2">
                          {[
                            'Certificate of Origin',
                            'CE Marking',
                            'FDA Approval',
                            'FCC Certification',
                            'ISO Certificate',
                            'Safety Certificate'
                          ].map((cert) => (
                            <label key={cert} className="flex items-center space-x-2">
                              <input
                                type="checkbox"
                                checked={productInfo.certificates.includes(cert)}
                                onChange={(e) => {
                                  if (e.target.checked) {
                                    updateProductInfo('certificates', [...productInfo.certificates, cert]);
                                  } else {
                                    updateProductInfo('certificates', productInfo.certificates.filter(c => c !== cert));
                                  }
                                }}
                                className="rounded"
                              />
                              <span className="text-sm">{cert}</span>
                            </label>
                          ))}
                        </div>
                      </div>

                      <div className="space-y-2">
                        <label className="text-sm font-medium">Special Conditions</label>
                        <div className="grid grid-cols-1 gap-2">
                          {[
                            'Temporary Import',
                            'Re-export',
                            'Duty Drawback',
                            'Bonded Warehouse',
                            'Free Trade Zone'
                          ].map((condition) => (
                            <label key={condition} className="flex items-center space-x-2">
                              <input
                                type="checkbox"
                                checked={productInfo.specialConditions.includes(condition)}
                                onChange={(e) => {
                                  if (e.target.checked) {
                                    updateProductInfo('specialConditions', [...productInfo.specialConditions, condition]);
                                  } else {
                                    updateProductInfo('specialConditions', productInfo.specialConditions.filter(c => c !== condition));
                                  }
                                }}
                                className="rounded"
                              />
                              <span className="text-sm">{condition}</span>
                            </label>
                          ))}
                        </div>
                      </div>
                    </div>
                  )}
                </TabsContent>
              </Tabs>

              {/* Calculate Button */}
              <div className="pt-6 border-t">
                <Button 
                  onClick={handleCalculate}
                  disabled={Object.keys(validateForm()).length > 0 || isCalculating}
                  className="w-full" 
                  variant="gradient" 
                  size="lg"
                >
                  {isCalculating ? (
                    <div className="flex items-center space-x-2">
                      <RefreshCw className="w-5 h-5 animate-spin" />
                      <span>Calculating Comprehensive Tariff Analysis...</span>
                    </div>
                  ) : (
                    <div className="flex items-center space-x-2">
                      <Zap className="w-5 h-5" />
                      <span>Calculate Professional Tariff Analysis</span>
                    </div>
                  )}
                </Button>
              </div>
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