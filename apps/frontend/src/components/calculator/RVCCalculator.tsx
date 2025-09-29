import React from 'react';
import { Calculator, CheckCircle, AlertTriangle, Info } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { formatCurrency, formatPercentage } from '@/lib/utils';

interface RVCCalculatorProps {
  materialCost: number;
  labourCost: number;
  overheadCost: number;
  profit: number;
  otherCosts: number;
  fobValue: number;
  nonOriginatingMaterialsValue: number;
  method: 'direct' | 'indirect';
  threshold: number;
  currency: string;
}

export function RVCCalculator({
  materialCost,
  labourCost,
  overheadCost,
  profit,
  otherCosts,
  fobValue,
  nonOriginatingMaterialsValue,
  method,
  threshold,
  currency
}: RVCCalculatorProps) {
  // Calculate RVC using Direct Formula
  const calculateDirectRVC = () => {
    if (fobValue === 0) return 0;
    return ((materialCost + labourCost + overheadCost + profit + otherCosts) / fobValue) * 100;
  };

  // Calculate RVC using Indirect/Build-Down Formula
  const calculateIndirectRVC = () => {
    if (fobValue === 0) return 0;
    return ((fobValue - nonOriginatingMaterialsValue) / fobValue) * 100;
  };

  const directRVC = calculateDirectRVC();
  const indirectRVC = calculateIndirectRVC();
  const currentRVC = method === 'direct' ? directRVC : indirectRVC;
  const qualifies = currentRVC >= threshold;

  return (
    <Card className="border-2 border-dashed border-brand-200 dark:border-brand-800">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Calculator className="w-5 h-5" />
          AANZFTA Regional Value Content (RVC)
        </CardTitle>
        <CardDescription>
          Article 4 - Goods Not Wholly Produced or Obtained
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* RVC Method Selection Info */}
        <div className="grid grid-cols-2 gap-4">
          <div className={`p-4 rounded-lg border-2 ${method === 'direct' ? 'border-brand-500 bg-brand-50 dark:bg-brand-900/20' : 'border-gray-200 dark:border-gray-700'}`}>
            <h4 className="font-medium text-sm mb-2">Direct Formula (Build-Up)</h4>
            <div className="text-xs text-muted-foreground mb-2">
              (Material + Labour + Overhead + Profit + Other) / FOB × 100%
            </div>
            <div className="text-lg font-bold text-brand-600">
              {formatPercentage(directRVC / 100)}
            </div>
          </div>
          <div className={`p-4 rounded-lg border-2 ${method === 'indirect' ? 'border-brand-500 bg-brand-50 dark:bg-brand-900/20' : 'border-gray-200 dark:border-gray-700'}`}>
            <h4 className="font-medium text-sm mb-2">Indirect Formula (Build-Down)</h4>
            <div className="text-xs text-muted-foreground mb-2">
              (FOB - Non-Originating Materials) / FOB × 100%
            </div>
            <div className="text-lg font-bold text-brand-600">
              {formatPercentage(indirectRVC / 100)}
            </div>
          </div>
        </div>

        {/* Current Calculation */}
        <div className="p-4 bg-gradient-to-r from-gray-50 to-gray-100 dark:from-gray-800 dark:to-gray-900 rounded-lg">
          <div className="flex items-center justify-between mb-4">
            <h4 className="font-semibold">Current RVC Calculation ({method === 'direct' ? 'Direct' : 'Indirect'} Method)</h4>
            <Badge variant={qualifies ? 'success' : 'destructive'} className="flex items-center gap-1">
              {qualifies ? <CheckCircle className="w-3 h-3" /> : <AlertTriangle className="w-3 h-3" />}
              {qualifies ? 'Qualifies' : 'Does Not Qualify'}
            </Badge>
          </div>

          {method === 'direct' ? (
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span>Material Cost:</span>
                <span className="font-medium">{formatCurrency(materialCost, currency)}</span>
              </div>
              <div className="flex justify-between">
                <span>Labour Cost:</span>
                <span className="font-medium">{formatCurrency(labourCost, currency)}</span>
              </div>
              <div className="flex justify-between">
                <span>Overhead Cost:</span>
                <span className="font-medium">{formatCurrency(overheadCost, currency)}</span>
              </div>
              <div className="flex justify-between">
                <span>Profit:</span>
                <span className="font-medium">{formatCurrency(profit, currency)}</span>
              </div>
              <div className="flex justify-between">
                <span>Other Costs:</span>
                <span className="font-medium">{formatCurrency(otherCosts, currency)}</span>
              </div>
              <div className="border-t pt-2 flex justify-between">
                <span>Total Originating Value:</span>
                <span className="font-bold">{formatCurrency(materialCost + labourCost + overheadCost + profit + otherCosts, currency)}</span>
              </div>
              <div className="flex justify-between">
                <span>FOB Value:</span>
                <span className="font-bold">{formatCurrency(fobValue, currency)}</span>
              </div>
            </div>
          ) : (
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span>FOB Value:</span>
                <span className="font-medium">{formatCurrency(fobValue, currency)}</span>
              </div>
              <div className="flex justify-between">
                <span>Non-Originating Materials (CIF):</span>
                <span className="font-medium">{formatCurrency(nonOriginatingMaterialsValue, currency)}</span>
              </div>
              <div className="border-t pt-2 flex justify-between">
                <span>Originating Value:</span>
                <span className="font-bold">{formatCurrency(fobValue - nonOriginatingMaterialsValue, currency)}</span>
              </div>
            </div>
          )}

          <div className="mt-4 p-3 bg-white dark:bg-gray-800 rounded border">
            <div className="flex items-center justify-between">
              <span className="font-semibold">RVC Percentage:</span>
              <span className="text-2xl font-bold text-brand-600">{formatPercentage(currentRVC / 100)}</span>
            </div>
            <div className="flex items-center justify-between text-sm text-muted-foreground">
              <span>Required Threshold:</span>
              <span>{formatPercentage(threshold / 100)}</span>
            </div>
            <div className="mt-2">
              <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2">
                <div 
                  className={`h-2 rounded-full transition-all duration-500 ${qualifies ? 'bg-green-500' : 'bg-red-500'}`}
                  style={{ width: `${Math.min(currentRVC, 100)}%` }}
                />
              </div>
            </div>
          </div>
        </div>

        {/* Qualification Status */}
        <div className={`p-4 rounded-lg border ${qualifies ? 'bg-green-50 border-green-200 dark:bg-green-900/20 dark:border-green-800' : 'bg-red-50 border-red-200 dark:bg-red-900/20 dark:border-red-800'}`}>
          <div className="flex items-start gap-2">
            {qualifies ? (
              <CheckCircle className="w-5 h-5 text-green-600 dark:text-green-400 mt-0.5" />
            ) : (
              <AlertTriangle className="w-5 h-5 text-red-600 dark:text-red-400 mt-0.5" />
            )}
            <div>
              <h4 className={`font-medium ${qualifies ? 'text-green-800 dark:text-green-200' : 'text-red-800 dark:text-red-200'}`}>
                {qualifies ? 'Preferential Rate Qualification' : 'Preferential Rate Not Met'}
              </h4>
              <p className={`text-sm mt-1 ${qualifies ? 'text-green-700 dark:text-green-300' : 'text-red-700 dark:text-red-300'}`}>
                {qualifies 
                  ? `This product qualifies for preferential tariff treatment under AANZFTA with an RVC of ${formatPercentage(currentRVC / 100)}.`
                  : `This product does not meet the ${formatPercentage(threshold / 100)} RVC threshold. Current RVC is ${formatPercentage(currentRVC / 100)}.`
                }
              </p>
              {!qualifies && (
                <p className="text-xs mt-2 text-red-600 dark:text-red-400">
                  Consider increasing originating content or reducing non-originating materials to qualify for preferential rates.
                </p>
              )}
            </div>
          </div>
        </div>

        {/* Legal Reference */}
        <div className="p-3 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg">
          <div className="flex items-start gap-2">
            <Info className="w-4 h-4 text-blue-600 dark:text-blue-400 mt-0.5" />
            <div className="text-xs text-blue-700 dark:text-blue-300">
              <p className="font-medium mb-1">Legal Reference: AANZFTA Article 4</p>
              <p>The value of goods shall be determined in accordance with Article VII of GATT 1994 and the Agreement on Customs Valuation.</p>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}