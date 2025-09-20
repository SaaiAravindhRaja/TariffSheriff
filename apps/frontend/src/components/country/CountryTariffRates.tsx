import React from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { Package, TrendingUp, AlertCircle } from 'lucide-react';

interface CountryTariffRatesProps {
  countryCode: string;
}

// Mock tariff data
const generateTariffData = (_countryCode: string) => [
  { category: 'Electronics', rate: 12.5, change: '+2.1%', status: 'increased' },
  { category: 'Automotive', rate: 8.3, change: '-1.2%', status: 'decreased' },
  { category: 'Textiles', rate: 15.7, change: '+0.5%', status: 'increased' },
  { category: 'Machinery', rate: 6.2, change: '-0.8%', status: 'decreased' },
  { category: 'Chemicals', rate: 9.8, change: '+1.3%', status: 'increased' },
  { category: 'Food & Beverages', rate: 18.4, change: '+3.2%', status: 'increased' }
];

const chartData = [
  { category: 'Electronics', current: 12.5, previous: 10.4 },
  { category: 'Automotive', current: 8.3, previous: 9.5 },
  { category: 'Textiles', current: 15.7, previous: 15.2 },
  { category: 'Machinery', current: 6.2, previous: 7.0 },
  { category: 'Chemicals', current: 9.8, previous: 8.5 },
  { category: 'Food', current: 18.4, previous: 15.2 }
];

export const CountryTariffRates: React.FC<CountryTariffRatesProps> = ({ countryCode }) => {
  const tariffData = generateTariffData(countryCode);
  const avgRate = tariffData.reduce((sum, item) => sum + item.rate, 0) / tariffData.length;

  return (
    <div className="grid gap-6 lg:grid-cols-2">
      {/* Tariff Rates Table */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center">
            <Package className="w-5 h-5 mr-2" />
            Current Tariff Rates
          </CardTitle>
          <CardDescription>
            Tariff rates by product category
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {tariffData.map((item, index) => (
              <div key={index} className="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-800/50 rounded-lg">
                <div>
                  <div className="font-medium">{item.category}</div>
                  <div className="text-sm text-muted-foreground">
                    {item.rate}% tariff rate
                  </div>
                </div>
                <div className="text-right">
                  <Badge 
                    variant={item.status === 'increased' ? 'destructive' : 'success'}
                    className="text-xs"
                  >
                    {item.change}
                  </Badge>
                </div>
              </div>
            ))}
          </div>
          
          <div className="mt-6 p-4 bg-blue-50 dark:bg-blue-900/20 rounded-lg">
            <div className="flex items-center justify-between">
              <div>
                <div className="font-medium text-blue-900 dark:text-blue-100">
                  Average Tariff Rate
                </div>
                <div className="text-sm text-blue-700 dark:text-blue-300">
                  Across all categories
                </div>
              </div>
              <div className="text-2xl font-bold text-blue-600 dark:text-blue-400">
                {avgRate.toFixed(1)}%
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Tariff Comparison Chart */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center">
            <TrendingUp className="w-5 h-5 mr-2" />
            Rate Comparison
          </CardTitle>
          <CardDescription>
            Current vs previous period tariff rates
          </CardDescription>
        </CardHeader>
        <CardContent>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={chartData} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" className="opacity-30" />
              <XAxis 
                dataKey="category" 
                className="text-xs"
                tick={{ fontSize: 10 }}
                angle={-45}
                textAnchor="end"
                height={80}
              />
              <YAxis 
                className="text-xs"
                tick={{ fontSize: 12 }}
                tickFormatter={(value) => `${value}%`}
              />
              <Tooltip 
                formatter={(value: number, name: string) => [
                  `${value}%`, 
                  name === 'current' ? 'Current Rate' : 'Previous Rate'
                ]}
                contentStyle={{
                  backgroundColor: 'hsl(var(--card))',
                  border: '1px solid hsl(var(--border))',
                  borderRadius: '8px'
                }}
              />
              <Bar dataKey="previous" fill="#94a3b8" name="previous" />
              <Bar dataKey="current" fill="#3b82f6" name="current" />
            </BarChart>
          </ResponsiveContainer>
          
          <div className="mt-4 p-3 bg-amber-50 dark:bg-amber-900/20 rounded-lg">
            <div className="flex items-start space-x-2">
              <AlertCircle className="w-4 h-4 text-amber-600 dark:text-amber-400 mt-0.5" />
              <div className="text-sm">
                <div className="font-medium text-amber-900 dark:text-amber-100">
                  Rate Changes Notice
                </div>
                <div className="text-amber-700 dark:text-amber-300">
                  Tariff rates are subject to change based on trade agreements and policy updates.
                </div>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};