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
  AlertTriangle,
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
  History,
  HelpCircle,
  Calculator2
} from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Switch } from '@/components/ui/switch';
import CountrySelect from '@/components/inputs/CountrySelect';
import { formatCurrency, formatPercentage, formatDate } from '@/lib/utils';
import { useSettings } from '@/contexts/SettingsContext';
import safeLocalStorage from '@/lib/safeLocalStorage';
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
  // Cost breakdown fields
  materialCost: number;
  labourCost: number;
  overheadCost: number;
  profit: number;
  otherCosts: number;
  fobValue: number;
}

interface TradeAgreement {
  type: 'MFN' | 'RVC' | 'ROOS';
  name: string;
  rate: number;
  description: string;
  requirements?: string[];
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
    specialConditions: [],
    // Cost breakdown fields
    materialCost: 0,
    labourCost: 0,
    overheadCost: 0,
    profit: 0,
    otherCosts: 0,
    fobValue: 0
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
  const [tradeAgreements, setTradeAgreements] = useState<TradeAgreement[]>([]);
  const [selectedAgreement, setSelectedAgreement] = useState<TradeAgreement | null>(null);
  const [basicInfoComplete, setBasicInfoComplete] = useState(false);

  // Auto-save to localStorage
  useEffect(() => {
    const saved = safeLocalStorage.get('tariff-calculator-draft');
    if (saved) {
      try {
        setProductInfo(prev => ({ ...prev, ...saved }));
      } catch (e) {
        console.error('Failed to load saved draft:', e);
      }
    }
  }, []);

  useEffect(() => {
    const timer = setTimeout(() => {
      safeLocalStorage.set('tariff-calculator-draft', productInfo);
    }, 1000);
    return () => clearTimeout(timer);
  }, [productInfo]);

  // Validation logic
  const validateBasicInfo = useCallback((): Record<string, string> => {
    const errors: Record<string, string> = {};

    if (!productInfo.hsCode.trim()) {
      errors.hsCode = 'HS Code is required';
    } else if (!/^\d{4,10}(\.\d{2})*$/.test(productInfo.hsCode)) {
      errors.hsCode = 'Invalid HS Code format (e.g., 8703.80.10)';
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

  const validateCalculateForm = useCallback((): Record<string, string> => {
    const errors: Record<string, string> = {};

    if (!basicInfoComplete) {
      errors.basicInfo = 'Please complete Basic Info first';
    }

    if (productInfo.quantity <= 0) {
      errors.quantity = 'Quantity must be greater than 0';
    }

    if (productInfo.materialCost < 0) {
      errors.materialCost = 'Material cost cannot be negative';
    }

    if (productInfo.labourCost < 0) {
      errors.labourCost = 'Labour cost cannot be negative';
    }

    if (productInfo.overheadCost < 0) {
      errors.overheadCost = 'Overhead cost cannot be negative';
    }

    if (productInfo.profit < 0) {
      errors.profit = 'Profit cannot be negative';
    }

    if (productInfo.otherCosts < 0) {
      errors.otherCosts = 'Other costs cannot be negative';
    }

    return errors;
  }, [productInfo, basicInfoComplete]);

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

  // Fetch trade agreements based on basic info
  const fetchTradeAgreements = useCallback(async () => {
    const errors = validateBasicInfo();
    setValidationErrors(errors);

    if (Object.keys(errors).length > 0) {
      return;
    }

    setIsCalculating(true);

    try {
      // Simulate API call to get trade agreements
      await new Promise(resolve => setTimeout(resolve, 1500));

      const mockAgreements: TradeAgreement[] = [
        {
          type: 'MFN',
          name: 'Most Favoured Nation',
          rate: 12.5,
          description: 'Standard WTO tariff rate',
          requirements: ['Commercial Invoice', 'Packing List']
        },
        {
          type: 'RVC',
          name: 'Regional Value Content (USMCA)',
          rate: 8.0,
          description: 'Preferential rate under USMCA agreement',
          requirements: ['Certificate of Origin', 'RVC Calculation', 'Supporting Documents']
        },
        {
          type: 'ROOS',
          name: 'Rules of Origin Specific (USMCA)',
          rate: 6.5,
          description: 'Specific rules of origin qualification',
          requirements: ['Certificate of Origin', 'Production Records', 'Material Certificates']
        }
      ];

      setTradeAgreements(mockAgreements);
      setBasicInfoComplete(true);
      setSelectedAgreement(mockAgreements[0]); // Default to MFN

    } catch (error) {
      console.error('Failed to fetch trade agreements:', error);
      setValidationErrors({ general: 'Failed to fetch trade agreements. Please try again.' });
    } finally {
      setIsCalculating(false);
    }
  }, [productInfo.hsCode, productInfo.originCountry, productInfo.destinationCountry, validateBasicInfo]);

  // Enhanced calculation logic
  const handleCalculate = async () => {
    const errors = validateCalculateForm();
    setValidationErrors(errors);

    if (Object.keys(errors).length > 0) {
      return;
    }

    if (!selectedAgreement) {
      setValidationErrors({ agreement: 'Please select a trade agreement first' });
      return;
    }

    setIsCalculating(true);

    try {
      // Simulate comprehensive API call
      await new Promise(resolve => setTimeout(resolve, 2000));

      // Calculate FOB value from cost components
      const totalCosts = productInfo.materialCost + productInfo.labourCost + 
                        productInfo.overheadCost + productInfo.profit + productInfo.otherCosts;
      const fobValue = totalCosts * productInfo.quantity;
      
      // Update FOB value in product info
      updateProductInfo('fobValue', fobValue);

      const baseValue = fobValue; // Base value for calculations
      const dutiableValue = fobValue; // FOB value is the dutiable value
      const actualRate = selectedAgreement.rate / 100; // Convert percentage to decimal
      const baseTariffRate = 0.125; // 12.5% base MFN rate
      const preferentialRate = 0.065; // 6.5% preferential rate

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
      const saved = safeLocalStorage.get('saved-calculations') || [];
      const savedArray = Array.isArray(saved) ? saved : [];
      savedArray.unshift(calculation);
      safeLocalStorage.set('saved-calculations', savedArray.slice(0, 50)); // Keep last 50
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
                  <TabsTrigger value="calculate" disabled={!basicInfoComplete}>Calculate</TabsTrigger>
                  <TabsTrigger value="logistics">Logistics</TabsTrigger>
                  <TabsTrigger value="compliance">Compliance</TabsTrigger>
                </TabsList>

                {/* Basic Information Tab */}
                <TabsContent value="basic" className="space-y-4">
                  <div className="grid grid-cols-1 gap-4">
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

                    <div className="grid grid-cols-2 gap-4">
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
                        {validationErrors.originCountry && (
                          <p className="text-sm text-red-600">{validationErrors.originCountry}</p>
                        )}
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
                        {validationErrors.destinationCountry && (
                          <p className="text-sm text-red-600">{validationErrors.destinationCountry}</p>
                        )}
                      </div>
                    </div>

                    <div className="flex justify-end pt-4">
                      <Button 
                        onClick={fetchTradeAgreements}
                        disabled={isCalculating}
                        className="min-w-[200px]"
                      >
                        {isCalculating ? (
                          <>
                            <RefreshCw className="w-4 h-4 mr-2 animate-spin" />
                            Fetching Agreements...
                          </>
                        ) : (
                          <>
                            <Search className="w-4 h-4 mr-2" />
                            Get Trade Agreements
                          </>
                        )}
                      </Button>
                    </div>

                    {/* Trade Agreements Results */}
                    {tradeAgreements.length > 0 && (
                      <div className="space-y-4 pt-4 border-t">
                        <h3 className="text-lg font-semibold flex items-center gap-2">
                          <Globe className="w-5 h-5" />
                          Available Trade Agreements
                        </h3>
                        <div className="grid gap-3">
                          {tradeAgreements.map((agreement, index) => (
                            <div
                              key={index}
                              className={`p-4 border rounded-lg cursor-pointer transition-colors ${
                                selectedAgreement?.type === agreement.type
                                  ? 'border-brand-500 bg-brand-50 dark:bg-brand-900/20'
                                  : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600'
                              }`}
                              onClick={() => setSelectedAgreement(agreement)}
                            >
                              <div className="flex items-center justify-between">
                                <div>
                                  <div className="flex items-center gap-2">
                                    <Badge variant={agreement.type === 'MFN' ? 'secondary' : 'default'}>
                                      {agreement.type}
                                    </Badge>
                                    <span className="font-medium">{agreement.name}</span>
                                  </div>
                                  <p className="text-sm text-muted-foreground mt-1">{agreement.description}</p>
                                  {agreement.requirements && (
                                    <div className="mt-2">
                                      <p className="text-xs text-muted-foreground">Requirements:</p>
                                      <ul className="text-xs text-muted-foreground list-disc list-inside">
                                        {agreement.requirements.map((req, idx) => (
                                          <li key={idx}>{req}</li>
                                        ))}
                                      </ul>
                                    </div>
                                  )}
                                </div>
                                <div className="text-right">
                                  <div className="text-lg font-bold text-brand-600">
                                    {agreement.rate}%
                                  </div>
                                  <div className="text-xs text-muted-foreground">Tariff Rate</div>
                                </div>
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                </TabsContent>

                {/* Calculate Tab */}
                <TabsContent value="calculate" className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <label className="text-sm font-medium">Quantity *</label>
                      <Input
                        type="number"
                        placeholder="1"
                        value={productInfo.quantity}
                        onChange={(e) => updateProductInfo('quantity', parseInt(e.target.value) || 0)}
                        className={validationErrors.quantity ? 'border-red-500' : ''}
                      />
                      {validationErrors.quantity && (
                        <p className="text-sm text-red-600">{validationErrors.quantity}</p>
                      )}
                    </div>
                    <div className="space-y-2">
                      <label className="text-sm font-medium">Currency</label>
                      <select
                        value={productInfo.currency}
                        onChange={(e) => updateProductInfo('currency', e.target.value)}
                        className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                      >
                        <option value="USD">USD</option>
                        <option value="EUR">EUR</option>
                        <option value="GBP">GBP</option>
                        <option value="JPY">JPY</option>
                        <option value="CAD">CAD</option>
                      </select>
                    </div>
                  </div>

                  <div className="space-y-4">
                    <h4 className="font-medium">Cost Breakdown</h4>
                    <div className="grid grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <label className="text-sm font-medium">Material Cost</label>
                        <Input
                          type="number"
                          step="0.01"
                          placeholder="0.00"
                          value={productInfo.materialCost}
                          onChange={(e) => updateProductInfo('materialCost', parseFloat(e.target.value) || 0)}
                          className={validationErrors.materialCost ? 'border-red-500' : ''}
                        />
                        {validationErrors.materialCost && (
                          <p className="text-sm text-red-600">{validationErrors.materialCost}</p>
                        )}
                      </div>
                      <div className="space-y-2">
                        <label className="text-sm font-medium">Labour Cost</label>
                        <Input
                          type="number"
                          step="0.01"
                          placeholder="0.00"
                          value={productInfo.labourCost}
                          onChange={(e) => updateProductInfo('labourCost', parseFloat(e.target.value) || 0)}
                          className={validationErrors.labourCost ? 'border-red-500' : ''}
                        />
                        {validationErrors.labourCost && (
                          <p className="text-sm text-red-600">{validationErrors.labourCost}</p>
                        )}
                      </div>
                      <div className="space-y-2">
                        <label className="text-sm font-medium">Overhead Cost</label>
                        <Input
                          type="number"
                          step="0.01"
                          placeholder="0.00"
                          value={productInfo.overheadCost}
                          onChange={(e) => updateProductInfo('overheadCost', parseFloat(e.target.value) || 0)}
                          className={validationErrors.overheadCost ? 'border-red-500' : ''}
                        />
                        {validationErrors.overheadCost && (
                          <p className="text-sm text-red-600">{validationErrors.overheadCost}</p>
                        )}
                      </div>
                      <div className="space-y-2">
                        <label className="text-sm font-medium">Profit</label>
                        <Input
                          type="number"
                          step="0.01"
                          placeholder="0.00"
                          value={productInfo.profit}
                          onChange={(e) => updateProductInfo('profit', parseFloat(e.target.value) || 0)}
                          className={validationErrors.profit ? 'border-red-500' : ''}
                        />
                        {validationErrors.profit && (
                          <p className="text-sm text-red-600">{validationErrors.profit}</p>
                        )}
                      </div>
                    </div>
                  </div>

                  <div className="flex justify-end pt-4">
                    <Button 
                      onClick={handleCalculate}
                      disabled={isCalculating}
                      className="min-w-[200px]"
                      variant="gradient"
                    >
                      {isCalculating ? (
                        <>
                          <RefreshCw className="w-4 h-4 mr-2 animate-spin" />
                          Calculating...
                        </>
                      ) : (
                        <>
                          <CalculatorIcon className="w-4 h-4 mr-2" />
                          Calculate Tariffs
                        </>
                      )}
                    </Button>
                  </div>
                </TabsContent>

                {/* Logistics Tab */}
                <TabsContent value="logistics" className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <label className="text-sm font-medium">Weight</label>
                      <div className="flex gap-2">
                        <Input
                          type="number"
                          step="0.01"
                          placeholder="0"
                          value={productInfo.weight}
                          onChange={(e) => updateProductInfo('weight', parseFloat(e.target.value) || 0)}
                          className="flex-1"
                        />
                        <select
                          value={productInfo.weightUnit}
                          onChange={(e) => updateProductInfo('weightUnit', e.target.value)}
                          className="rounded-md border border-input bg-background px-3 py-2 text-sm"
                        >
                          <option value="kg">kg</option>
                          <option value="lbs">lbs</option>
                        </select>
                      </div>
                    </div>
                    <div className="space-y-2">
                      <label className="text-sm font-medium">Shipment Date</label>
                      <Input
                        type="date"
                        value={productInfo.shipmentDate}
                        onChange={(e) => updateProductInfo('shipmentDate', e.target.value)}
                      />
                    </div>
                  </div>
                </TabsContent>

                {/* Compliance Tab */}
                <TabsContent value="compliance" className="space-y-4">
                  <div className="space-y-2">
                    <label className="text-sm font-medium">Incoterms</label>
                    <select
                      value={productInfo.incoterms}
                      onChange={(e) => updateProductInfo('incoterms', e.target.value)}
                      className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                    >
                      <option value="CIF">CIF - Cost, Insurance, and Freight</option>
                      <option value="FOB">FOB - Free on Board</option>
                      <option value="EXW">EXW - Ex Works</option>
                      <option value="DDP">DDP - Delivered Duty Paid</option>
                    </select>
                  </div>
                </TabsContent>
              </Tabs>
            </CardContent>
          </Card>
        </motion.div>

        {/* Results Panel */}
        <motion.div
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.5, delay: 0.4 }}
          className="space-y-6"
        >
          {calculation ? (
            <>
              {/* Summary Card */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <DollarSign className="w-5 h-5" />
                    Calculation Results
                  </CardTitle>
                  <CardDescription>
                    Total import costs and breakdown
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    <div className="grid grid-cols-2 gap-4 text-sm">
                      <div className="flex justify-between">
                        <span className="text-muted-foreground">Base Value:</span>
                        <span className="font-medium">{formatCurrency(calculation.results.baseValue)}</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-muted-foreground">Tariff Amount:</span>
                        <span className="font-medium">{formatCurrency(calculation.results.tariffAmount)}</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-muted-foreground">VAT:</span>
                        <span className="font-medium">{formatCurrency(calculation.results.taxes.vat)}</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-muted-foreground">Processing Fees:</span>
                        <span className="font-medium">{formatCurrency(calculation.results.fees.processing)}</span>
                      </div>
                    </div>
                    <div className="border-t pt-4">
                      <div className="flex justify-between text-lg font-bold">
                        <span>Total Cost:</span>
                        <span className="text-brand-600">{formatCurrency(calculation.results.totalCost)}</span>
                      </div>
                      <div className="flex justify-between text-sm text-muted-foreground">
                        <span>Effective Rate:</span>
                        <span>{formatPercentage(calculation.results.effectiveRate)}</span>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>

              {/* Charts */}
              {showComparison && calculation.results.alternativeRoutes.length > 0 && (
                <ComparisonChart
                  baseResult={{
                    baseValue: calculation.results.baseValue,
                    tariffRate: calculation.results.tariffRate,
                    totalCost: calculation.results.totalCost
                  }}
                  alternatives={calculation.results.alternativeRoutes.map(route => ({
                    country: route.countryName,
                    tariffRate: route.tariffRate,
                    savings: route.savings
                  }))}
                />
              )}

              <TariffBreakdownChart
                data={{
                  baseValue: calculation.results.baseValue,
                  tariffAmount: calculation.results.tariffAmount,
                  additionalFees: calculation.results.fees.processing + calculation.results.fees.inspection,
                  totalCost: calculation.results.totalCost,
                  breakdown: calculation.results.breakdown
                }}
              />

              <HistoricalRatesChart
                hsCode={calculation.productInfo.hsCode}
                originCountry={calculation.productInfo.originCountry}
                destinationCountry={calculation.productInfo.destinationCountry}
              />

              <CostAnalysisChart
                data={{
                  baseValue: calculation.results.baseValue,
                  tariffAmount: calculation.results.tariffAmount,
                  additionalFees: calculation.results.fees.processing + calculation.results.fees.inspection,
                  totalCost: calculation.results.totalCost
                }}
              />
            </>
          ) : (
            <Card>
              <CardContent className="flex flex-col items-center justify-center py-12">
                <CalculatorIcon className="w-12 h-12 text-muted-foreground mb-4" />
                <h3 className="text-lg font-medium mb-2">Ready to Calculate</h3>
                <p className="text-muted-foreground text-center">
                  Complete the form and click calculate to see your tariff breakdown
                </p>
              </CardContent>
            </Card>
          )}
        </motion.div>
      </div>
    </div>
  );
}