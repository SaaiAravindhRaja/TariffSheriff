import React from 'react';
import { BookOpen, Calculator, Info, AlertCircle } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';

export function RVCInfoPanel() {
  return (
    <Card className="border-blue-200 dark:border-blue-800">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-blue-800 dark:text-blue-200">
          <BookOpen className="w-5 h-5" />
          AANZFTA Regional Value Content Guide
        </CardTitle>
        <CardDescription>
          Understanding Article 4 - Goods Not Wholly Produced or Obtained
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* Formula Explanations */}
        <div className="space-y-4">
          <h4 className="font-semibold flex items-center gap-2">
            <Calculator className="w-4 h-4" />
            RVC Calculation Methods
          </h4>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="p-4 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg">
              <h5 className="font-medium text-green-800 dark:text-green-200 mb-2">
                Direct Formula (Build-Up)
              </h5>
              <div className="text-sm text-green-700 dark:text-green-300 mb-3">
                <code className="bg-green-100 dark:bg-green-800 px-2 py-1 rounded text-xs">
                  (Material + Labour + Overhead + Profit + Other) / FOB × 100%
                </code>
              </div>
              <p className="text-xs text-green-600 dark:text-green-400">
                Calculates the percentage of originating content by adding up all qualifying costs and dividing by FOB value.
              </p>
            </div>
            
            <div className="p-4 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg">
              <h5 className="font-medium text-blue-800 dark:text-blue-200 mb-2">
                Indirect Formula (Build-Down)
              </h5>
              <div className="text-sm text-blue-700 dark:text-blue-300 mb-3">
                <code className="bg-blue-100 dark:bg-blue-800 px-2 py-1 rounded text-xs">
                  (FOB - Non-Originating Materials) / FOB × 100%
                </code>
              </div>
              <p className="text-xs text-blue-600 dark:text-blue-400">
                Calculates originating content by subtracting non-originating materials from FOB value.
              </p>
            </div>
          </div>
        </div>

        {/* Cost Component Definitions */}
        <div className="space-y-3">
          <h4 className="font-semibold">Cost Component Definitions</h4>
          <div className="space-y-2 text-sm">
            <div className="flex items-start gap-2">
              <Badge variant="outline" className="text-xs mt-0.5">Material</Badge>
              <p className="text-muted-foreground">
                Value of originating materials, parts or produce that are acquired or self-produced by the producer
              </p>
            </div>
            <div className="flex items-start gap-2">
              <Badge variant="outline" className="text-xs mt-0.5">Labour</Badge>
              <p className="text-muted-foreground">
                Wages, remuneration and other employee benefits paid to workers
              </p>
            </div>
            <div className="flex items-start gap-2">
              <Badge variant="outline" className="text-xs mt-0.5">Overhead</Badge>
              <p className="text-muted-foreground">
                Total overhead expense including factory overhead and administrative costs
              </p>
            </div>
            <div className="flex items-start gap-2">
              <Badge variant="outline" className="text-xs mt-0.5">Other</Badge>
              <p className="text-muted-foreground">
                Costs for placing goods in transport: domestic transport, storage, port handling, brokerage fees
              </p>
            </div>
            <div className="flex items-start gap-2">
              <Badge variant="outline" className="text-xs mt-0.5">FOB</Badge>
              <p className="text-muted-foreground">
                Free-on-Board value as defined in Article 1 (Definitions)
              </p>
            </div>
          </div>
        </div>

        {/* Legal Reference */}
        <div className="p-3 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg">
          <div className="flex items-start gap-2">
            <Info className="w-4 h-4 text-gray-600 dark:text-gray-400 mt-0.5" />
            <div className="text-xs text-gray-700 dark:text-gray-300">
              <p className="font-medium mb-1">Legal Framework</p>
              <p className="mb-2">
                The value of goods under this Chapter shall be determined in accordance with:
              </p>
              <ul className="list-disc list-inside space-y-1 ml-2">
                <li>Article VII of GATT 1994</li>
                <li>Agreement on Customs Valuation</li>
                <li>AANZFTA Article 4 (Goods Not Wholly Produced or Obtained)</li>
              </ul>
            </div>
          </div>
        </div>

        {/* Important Notes */}
        <div className="p-3 bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-lg">
          <div className="flex items-start gap-2">
            <AlertCircle className="w-4 h-4 text-yellow-600 dark:text-yellow-400 mt-0.5" />
            <div className="text-xs text-yellow-700 dark:text-yellow-300">
              <p className="font-medium mb-1">Important Notes</p>
              <ul className="list-disc list-inside space-y-1 ml-2">
                <li>Non-originating materials include materials of undetermined origin</li>
                <li>Materials that are self-produced are not considered non-originating</li>
                <li>CIF value should be used for non-originating materials at time of importation</li>
                <li>Both formulas should yield similar results when properly calculated</li>
              </ul>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}