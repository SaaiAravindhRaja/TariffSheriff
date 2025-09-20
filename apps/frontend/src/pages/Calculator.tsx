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

// Core interfaces
interface ProductInfo {
  description: string;
  hsCode: string;
  hsCodeDescription?: string;
  quantity: number;
  unitValue: number;
  weight: number;
  originCountry: string;
  destinationCountry: string;
  shipmentDate: string;
  currency: string;
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
      category: string;
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
      type: 'warning' | 'info';
      message: string;
      recommendation: string;
    }>;
    alternativeRoutes: Array<{
      country: string;
      countryName: string;
      tariffRate: number;
      totalCost: number;
      savings: number;
      savingsPercentage: number;
      tradeAgreement?: string;
      transitTime: number;
    }>;
    compliance: {
      requiredDocuments: string[];
      certificates: string[];
      restrictions: string[];
      prohibitions: string[];
    };
  };
}

// EV-focused interfaces for tariff calculator
interface EVProduct {
  id: string;
  description: string;
  hsCode: string;
  evType: 'BEV' | 'PHEV' | 'HEV' | 'FCEV'; // Battery, Plug-in Hybrid, Hybrid, Fuel Cell
  batteryCapacity: number; // kWh
  range: number; // km
  manufacturer: string;
  model: string;
  year: number;
  quantity: number;
  unitValue: number;
  currency: string;
  originCountry: string;
  destinationCountry: string;
  shipmentDate: string;
  certificates: string[];
}

interface EVTariffCalculation {
  id: string;
  timestamp: string;
  product: EVProduct;
  results: {
    baseValue: number;
    tariffRate: number;
    tariffAmount: number;
    evIncentive: number; // EV-specific incentives/penalties
    carbonTax: number;
    vat: number;
    processingFee: number;
    totalCost: number;
    effectiveRate: number;
    breakdown: Array<{
      type: string;
      rate: number;
      amount: number;
      description: string;
    }>;
    appliedRules: Array<{
      ruleId: string;
      description: string;
      source: string;
      tradeAgreement?: string;
    }>;
    alternativeRoutes: Array<{
      country: string;
      countryName: string;
      tariffRate: number;
      totalCost: number;
      savings: number;
      tradeAgreement?: string;
    }>;
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

  // Core state variables
  const [productInfo, setProductInfo] = useState({
    description: '',
    hsCode: '',
    hsCodeDescription: '',
    quantity: 1,
    unitValue: 0,
    weight: 0,
    originCountry: '',
    destinationCountry: '',
    shipmentDate: new Date().toISOString().split('T')[0],
    currency: settings.currency
  });

  const [calculation, setCalculation] = useState<any>(null);
  const [calculationHistory, setCalculationHistory] = useState<any[]>([]);
  const [hsCodeSuggestions, setHsCodeSuggestions] = useState<HSCodeSuggestion[]>([]);
  const [showAdvanced, setShowAdvanced] = useState(false);

  // EV products state
  const [evProducts, setEvProducts] = useState<EVProduct[]>([
    {
      id: '1',
      description: '',
      hsCode: '8703.80.10',
      evType: 'BEV',
      batteryCapacity: 0,
      range: 0,
      manufacturer: '',
      model: '',
      year: new Date().getFullYear(),
      quantity: 1,
      unitValue: 0,
      currency: settings.currency,
      originCountry: '',
      destinationCountry: '',
      shipmentDate: new Date().toISOString().split('T')[0],
      certificates: []
    }
  ]);

  // Calculation results for each product
  const [calculations, setCalculations] = useState<Record<string, EVTariffCalculation>>({});
  const [isCalculating, setIsCalculating] = useState(false);
  const [showComparison, setShowComparison] = useState(false);
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});

  // Auto-save EV products
  useEffect(() => {
    const timer = setTimeout(() => {
      localStorage.setItem('ev-calculator-draft', JSON.stringify(evProducts));
    }, 1000);
    return () => clearTimeout(timer);
  }, [evProducts]);

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
      const dataUri = 'data:application/json;charset=utf-8,' + encodeURIComponent(dataStr);

      const exportFileDefaultName = `tariff-calculation-${calculation.id}.json`;

      const linkElement = document.createElement('a');
      linkElement.setAttribute('href', dataUri);
      linkElement.setAttribute('download', exportFileDefaultName);
      linkElement.click();
    }
  };

  // EV Product Management
  const addProduct = () => {
    const newProduct: EVProduct = {
      id: `ev_${Date.now()}`,
      description: '',
      hsCode: '8703.80.10',
      evType: 'BEV',
      batteryCapacity: 0,
      range: 0,
      manufacturer: '',
      model: '',
      year: new Date().getFullYear(),
      quantity: 1,
      unitValue: 0,
      currency: settings.currency,
      originCountry: '',
      destinationCountry: '',
      shipmentDate: new Date().toISOString().split('T')[0],
      certificates: []
    };
    setEvProducts(prev => [...prev, newProduct]);
  };

  const removeProduct = (id: string) => {
    setEvProducts(prev => prev.filter(p => p.id !== id));
    setCalculations(prev => {
      const newCalcs = { ...prev };
      delete newCalcs[id];
      return newCalcs;
    });
  };

  const updateProduct = (id: string, field: keyof EVProduct, value: any) => {
    setEvProducts(prev => prev.map(p =>
      p.id === id ? { ...p, [field]: value } : p
    ));

    // Clear calculation for this product when data changes
    if (calculations[id]) {
      setCalculations(prev => {
        const newCalcs = { ...prev };
        delete newCalcs[id];
        return newCalcs;
      });
    }
  };

  // Calculate single EV
  const calculateSingle = async (productId: string) => {
    const product = evProducts.find(p => p.id === productId);
    if (!product) return;

    setIsCalculating(true);

    try {
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 1000));

      const baseValue = product.unitValue * product.quantity;
      const tariffRate = getEVTariffRate(product);
      const tariffAmount = baseValue * tariffRate;
      const evIncentive = getEVIncentive(product);
      const carbonTax = getCarbonTax(product);
      const vat = (baseValue + tariffAmount) * 0.20; // 20% VAT
      const processingFee = Math.min(baseValue * 0.005, 500);

      const totalCost = baseValue + tariffAmount - evIncentive + carbonTax + vat + processingFee;

      const calculation: EVTariffCalculation = {
        id: `calc_${productId}_${Date.now()}`,
        timestamp: new Date().toISOString(),
        product,
        results: {
          baseValue,
          tariffRate,
          tariffAmount,
          evIncentive,
          carbonTax,
          vat,
          processingFee,
          totalCost,
          effectiveRate: ((totalCost - baseValue) / baseValue),
          breakdown: [
            {
              type: 'Import Duty',
              rate: tariffRate,
              amount: tariffAmount,
              description: `${product.evType} tariff rate`
            },
            {
              type: 'EV Incentive',
              rate: evIncentive / baseValue,
              amount: -evIncentive,
              description: 'Green vehicle incentive'
            },
            {
              type: 'Carbon Tax',
              rate: carbonTax / baseValue,
              amount: carbonTax,
              description: 'Environmental impact tax'
            },
            {
              type: 'VAT',
              rate: 0.20,
              amount: vat,
              description: 'Value Added Tax'
            }
          ],
          appliedRules: [
            {
              ruleId: `EV-${product.hsCode}-2024`,
              description: `Electric vehicle classification for ${product.evType}`,
              source: 'EV Tariff Schedule',
              tradeAgreement: product.originCountry === 'MX' ? 'USMCA' : undefined
            }
          ],
          alternativeRoutes: []
        }
      };

      setCalculations(prev => ({ ...prev, [productId]: calculation }));

    } catch (error) {
      console.error('Calculation failed:', error);
    } finally {
      setIsCalculating(false);
    }
  };

  // Calculate all EVs
  const calculateAll = async () => {
    setIsCalculating(true);

    for (const product of evProducts) {
      if (product.description && product.unitValue && product.originCountry && product.destinationCountry) {
        await calculateSingle(product.id);
      }
    }

    setIsCalculating(false);
  };

  // EV-specific tariff logic
  const getEVTariffRate = (product: EVProduct): number => {
    // Simplified EV tariff logic
    const baseRates = {
      'BEV': 0.10,  // 10% for Battery Electric
      'PHEV': 0.12, // 12% for Plug-in Hybrid
      'HEV': 0.15,  // 15% for Hybrid
      'FCEV': 0.08  // 8% for Fuel Cell
    };

    let rate = baseRates[product.evType];

    // Trade agreement adjustments
    if (product.originCountry === 'MX' || product.originCountry === 'CA') {
      rate *= 0.7; // 30% reduction for USMCA
    }

    // Battery capacity incentives
    if (product.batteryCapacity > 75) {
      rate *= 0.9; // 10% reduction for large batteries
    }

    return rate;
  };

  const getEVIncentive = (product: EVProduct): number => {
    // EV incentives based on type and battery capacity
    const baseIncentives = {
      'BEV': 2500,
      'PHEV': 1500,
      'HEV': 500,
      'FCEV': 3000
    };

    let incentive = baseIncentives[product.evType];

    // Battery capacity bonus
    if (product.batteryCapacity > 50) {
      incentive += 1000;
    }

    return incentive * product.quantity;
  };

  const getCarbonTax = (product: EVProduct): number => {
    // Carbon tax (lower for EVs)
    const carbonRates = {
      'BEV': 0,     // No carbon tax for pure electric
      'PHEV': 200,  // Low carbon tax for plug-in hybrid
      'HEV': 500,   // Medium carbon tax for hybrid
      'FCEV': 0     // No carbon tax for fuel cell
    };

    return carbonRates[product.evType] * product.quantity;
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
            <Zap className="w-8 h-8 text-brand-600" />
            EV Tariff Calculator
          </h1>
          <p className="text-muted-foreground">
            Professional import duty calculations for Electric Vehicles with EV-specific incentives and classifications
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
              <CardTitle className="flex items-center gap-2">
                <Zap className="w-5 h-5" />
                EV Tariff Calculator
              </CardTitle>
              <CardDescription>
                Calculate import tariffs for electric vehicles with industry-specific classifications
              </CardDescription>
            </CardHeader>
            <CardContent>
              {/* EV Tariff Calculation Table */}
              <div className="space-y-6">
                <div className="overflow-x-auto">
                  <table className="w-full border-collapse border border-gray-300 dark:border-gray-600">
                    <thead>
                      <tr className="bg-gray-50 dark:bg-gray-800">
                        <th className="border border-gray-300 dark:border-gray-600 px-4 py-3 text-left text-sm font-medium">EV Model</th>
                        <th className="border border-gray-300 dark:border-gray-600 px-4 py-3 text-left text-sm font-medium">Type</th>
                        <th className="border border-gray-300 dark:border-gray-600 px-4 py-3 text-left text-sm font-medium">HS Code</th>
                        <th className="border border-gray-300 dark:border-gray-600 px-4 py-3 text-left text-sm font-medium">Battery (kWh)</th>
                        <th className="border border-gray-300 dark:border-gray-600 px-4 py-3 text-left text-sm font-medium">Range (km)</th>
                        <th className="border border-gray-300 dark:border-gray-600 px-4 py-3 text-left text-sm font-medium">Qty</th>
                        <th className="border border-gray-300 dark:border-gray-600 px-4 py-3 text-left text-sm font-medium">Unit Value</th>
                        <th className="border border-gray-300 dark:border-gray-600 px-4 py-3 text-left text-sm font-medium">Origin</th>
                        <th className="border border-gray-300 dark:border-gray-600 px-4 py-3 text-left text-sm font-medium">Destination</th>
                        <th className="border border-gray-300 dark:border-gray-600 px-4 py-3 text-left text-sm font-medium">Tariff Rate</th>
                        <th className="border border-gray-300 dark:border-gray-600 px-4 py-3 text-left text-sm font-medium">Total Cost</th>
                        <th className="border border-gray-300 dark:border-gray-600 px-4 py-3 text-left text-sm font-medium">Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {evProducts.map((product, index) => (
                        <tr key={product.id} className="hover:bg-gray-50 dark:hover:bg-gray-800/50">
                          <td className="border border-gray-300 dark:border-gray-600 px-2 py-2">
                            <Input
                              placeholder="Tesla Model Y"
                              value={product.description}
                              onChange={(e) => updateProduct(product.id, 'description', e.target.value)}
                              className="border-0 bg-transparent"
                            />
                          </td>
                          <td className="border border-gray-300 dark:border-gray-600 px-2 py-2">
                            <select
                              className="w-full px-2 py-1 border-0 bg-transparent text-sm"
                              value={product.evType}
                              onChange={(e) => updateProduct(product.id, 'evType', e.target.value)}
                            >
                              <option value="BEV">BEV</option>
                              <option value="PHEV">PHEV</option>
                              <option value="HEV">HEV</option>
                              <option value="FCEV">FCEV</option>
                            </select>
                          </td>
                          <td className="border border-gray-300 dark:border-gray-600 px-2 py-2">
                            <select
                              className="w-full px-2 py-1 border-0 bg-transparent text-sm"
                              value={product.hsCode}
                              onChange={(e) => updateProduct(product.id, 'hsCode', e.target.value)}
                            >
                              <option value="8703.80.10">8703.80.10 - BEV</option>
                              <option value="8703.80.20">8703.80.20 - PHEV</option>
                              <option value="8703.80.30">8703.80.30 - HEV</option>
                              <option value="8703.80.40">8703.80.40 - FCEV</option>
                            </select>
                          </td>
                          <td className="border border-gray-300 dark:border-gray-600 px-2 py-2">
                            <Input
                              type="number"
                              placeholder="75"
                              value={product.batteryCapacity || ''}
                              onChange={(e) => updateProduct(product.id, 'batteryCapacity', parseFloat(e.target.value) || 0)}
                              className="border-0 bg-transparent w-20"
                            />
                          </td>
                          <td className="border border-gray-300 dark:border-gray-600 px-2 py-2">
                            <Input
                              type="number"
                              placeholder="500"
                              value={product.range || ''}
                              onChange={(e) => updateProduct(product.id, 'range', parseFloat(e.target.value) || 0)}
                              className="border-0 bg-transparent w-20"
                            />
                          </td>
                          <td className="border border-gray-300 dark:border-gray-600 px-2 py-2">
                            <Input
                              type="number"
                              min="1"
                              value={product.quantity || ''}
                              onChange={(e) => updateProduct(product.id, 'quantity', parseInt(e.target.value) || 1)}
                              className="border-0 bg-transparent w-16"
                            />
                          </td>
                          <td className="border border-gray-300 dark:border-gray-600 px-2 py-2">
                            <Input
                              type="number"
                              placeholder="45000"
                              value={product.unitValue || ''}
                              onChange={(e) => updateProduct(product.id, 'unitValue', parseFloat(e.target.value) || 0)}
                              className="border-0 bg-transparent w-24"
                            />
                          </td>
                          <td className="border border-gray-300 dark:border-gray-600 px-2 py-2">
                            <CountrySelect
                              placeholder="Origin"
                              value={product.originCountry}
                              onChange={(code) => {
                                const single = Array.isArray(code) ? code[0] ?? '' : code ?? '';
                                updateProduct(product.id, 'originCountry', String(single));
                              }}
                              className="border-0 bg-transparent"
                            />
                          </td>
                          <td className="border border-gray-300 dark:border-gray-600 px-2 py-2">
                            <CountrySelect
                              placeholder="Destination"
                              value={product.destinationCountry}
                              onChange={(code) => {
                                const single = Array.isArray(code) ? code[0] ?? '' : code ?? '';
                                updateProduct(product.id, 'destinationCountry', String(single));
                              }}
                              className="border-0 bg-transparent"
                            />
                          </td>
                          <td className="border border-gray-300 dark:border-gray-600 px-2 py-2 text-center">
                            {calculations[product.id] ? (
                              <Badge variant="secondary">
                                {formatPercentage(calculations[product.id].results.tariffRate)}
                              </Badge>
                            ) : (
                              <span className="text-muted-foreground text-sm">-</span>
                            )}
                          </td>
                          <td className="border border-gray-300 dark:border-gray-600 px-2 py-2 text-center">
                            {calculations[product.id] ? (
                              <span className="font-medium">
                                {formatCurrency(calculations[product.id].results.totalCost, product.currency)}
                              </span>
                            ) : (
                              <span className="text-muted-foreground text-sm">-</span>
                            )}
                          </td>
                          <td className="border border-gray-300 dark:border-gray-600 px-2 py-2">
                            <div className="flex gap-1">
                              <Button
                                size="sm"
                                variant="outline"
                                onClick={() => calculateSingle(product.id)}
                                disabled={!product.description || !product.unitValue || !product.originCountry || !product.destinationCountry}
                              >
                                <CalculatorIcon className="w-3 h-3" />
                              </Button>
                              {evProducts.length > 1 && (
                                <Button
                                  size="sm"
                                  variant="outline"
                                  onClick={() => removeProduct(product.id)}
                                >
                                  ×
                                </Button>
                              )}
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                {/* Table Actions */}
                <div className="flex justify-between items-center">
                  <Button
                    variant="outline"
                    onClick={addProduct}
                  >
                    + Add EV Model
                  </Button>

                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      onClick={calculateAll}
                      disabled={evProducts.length === 0 || isCalculating}
                    >
                      Calculate All
                    </Button>
                    <Button
                      variant="gradient"
                      onClick={calculateAll}
                      disabled={evProducts.length === 0 || isCalculating}
                    >
                      {isCalculating ? (
                        <div className="flex items-center space-x-2">
                          <RefreshCw className="w-4 h-4 animate-spin" />
                          <span>Calculating...</span>
                        </div>
                      ) : (
                        <div className="flex items-center space-x-2">
                          <Zap className="w-4 h-4" />
                          <span>Calculate EV Tariffs</span>
                        </div>
                      )}
                    </Button>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </motion.div>

        {/* Simple Results Summary */}
        {Object.keys(calculations).length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.4 }}
          >
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <DollarSign className="w-5 h-5" />
                  EV Fleet Summary
                </CardTitle>
              </CardHeader>
              <CardContent>
                {(() => {
                  const totalCost = Object.values(calculations).reduce((sum, calc) => sum + calc.results.totalCost, 0);
                  const totalBaseValue = Object.values(calculations).reduce((sum, calc) => sum + calc.results.baseValue, 0);
                  const totalIncentives = Object.values(calculations).reduce((sum, calc) => sum + calc.results.evIncentive, 0);

                  return (
                    <div className="grid grid-cols-3 gap-4 text-center">
                      <div className="p-4 bg-blue-50 dark:bg-blue-900/20 rounded-lg">
                        <div className="text-2xl font-bold text-blue-600 dark:text-blue-400">
                          {formatCurrency(totalBaseValue, settings.currency)}
                        </div>
                        <div className="text-sm text-muted-foreground">Fleet Value</div>
                      </div>
                      <div className="p-4 bg-green-50 dark:bg-green-900/20 rounded-lg">
                        <div className="text-2xl font-bold text-green-600 dark:text-green-400">
                          {formatCurrency(totalIncentives, settings.currency)}
                        </div>
                        <div className="text-sm text-muted-foreground">EV Incentives</div>
                      </div>
                      <div className="p-4 bg-brand-50 dark:bg-brand-900/20 rounded-lg">
                        <div className="text-2xl font-bold text-brand-600 dark:text-brand-400">
                          {formatCurrency(totalCost, settings.currency)}
                        </div>
                        <div className="text-sm text-muted-foreground">Total Cost</div>
                      </div>
                    </div>
                  );
                })()}
              </CardContent>
            </Card>
          </motion.div>
        )}
      </div>
    </div>
  );
}
