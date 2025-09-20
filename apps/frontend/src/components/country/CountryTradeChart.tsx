import React from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, AreaChart, Area } from 'recharts';
import { TrendingUp, TrendingDown, DollarSign } from 'lucide-react';

interface CountryTradeChartProps {
  countryCode: string;
}

// Mock data - replace with real API calls
const generateTradeData = (_countryCode: string) => [
  { month: 'Jan', imports: 2400, exports: 2800, balance: 400 },
  { month: 'Feb', imports: 2600, exports: 3200, balance: 600 },
  { month: 'Mar', imports: 2800, exports: 3100, balance: 300 },
  { month: 'Apr', imports: 3200, exports: 3600, balance: 400 },
  { month: 'May', imports: 3400, exports: 3800, balance: 400 },
  { month: 'Jun', imports: 3600, exports: 4200, balance: 600 },
  { month: 'Jul', imports: 3800, exports: 4400, balance: 600 },
  { month: 'Aug', imports: 4000, exports: 4600, balance: 600 },
  { month: 'Sep', imports: 4200, exports: 4800, balance: 600 },
  { month: 'Oct', imports: 4400, exports: 5000, balance: 600 },
  { month: 'Nov', imports: 4600, exports: 5200, balance: 600 },
  { month: 'Dec', imports: 4800, exports: 5400, balance: 600 }
];

export const CountryTradeChart: React.FC<CountryTradeChartProps> = ({ countryCode }) => {
  const tradeData = generateTradeData(countryCode);
  
  const currentMonth = tradeData[tradeData.length - 1];
  const previousMonth = tradeData[tradeData.length - 2];
  
  const importsChange = currentMonth && previousMonth 
    ? ((currentMonth.imports - previousMonth.imports) / previousMonth.imports) * 100 
    : 0;
  const exportsChange = currentMonth && previousMonth 
    ? ((currentMonth.exports - previousMonth.exports) / previousMonth.exports) * 100 
    : 0;

  return (
    <div className="grid gap-6 lg:grid-cols-3">
      {/* Trade Summary Cards */}
      <div className="space-y-4">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium flex items-center">
              <TrendingUp className="w-4 h-4 mr-2 text-green-600" />
              Exports
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">${currentMonth?.exports || 0}M</div>
            <p className={`text-xs ${exportsChange >= 0 ? 'text-green-600' : 'text-red-600'}`}>
              {exportsChange >= 0 ? '+' : ''}{exportsChange.toFixed(1)}% from last month
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium flex items-center">
              <TrendingDown className="w-4 h-4 mr-2 text-blue-600" />
              Imports
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">${currentMonth?.imports || 0}M</div>
            <p className={`text-xs ${importsChange >= 0 ? 'text-green-600' : 'text-red-600'}`}>
              {importsChange >= 0 ? '+' : ''}{importsChange.toFixed(1)}% from last month
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium flex items-center">
              <DollarSign className="w-4 h-4 mr-2 text-purple-600" />
              Trade Balance
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">${currentMonth?.balance || 0}M</div>
            <p className="text-xs text-muted-foreground">
              {(currentMonth?.balance || 0) >= 0 ? 'Surplus' : 'Deficit'}
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Trade Volume Chart */}
      <div className="lg:col-span-2">
        <Card>
          <CardHeader>
            <CardTitle>Trade Volume Trends</CardTitle>
            <CardDescription>
              Monthly imports and exports over the past year
            </CardDescription>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={300}>
              <AreaChart data={tradeData}>
                <CartesianGrid strokeDasharray="3 3" className="opacity-30" />
                <XAxis 
                  dataKey="month" 
                  className="text-xs"
                  tick={{ fontSize: 12 }}
                />
                <YAxis 
                  className="text-xs"
                  tick={{ fontSize: 12 }}
                  tickFormatter={(value) => `$${value}M`}
                />
                <Tooltip 
                  formatter={(value: number, name: string) => [
                    `$${value}M`, 
                    name.charAt(0).toUpperCase() + name.slice(1)
                  ]}
                  labelFormatter={(label) => `Month: ${label}`}
                  contentStyle={{
                    backgroundColor: 'hsl(var(--card))',
                    border: '1px solid hsl(var(--border))',
                    borderRadius: '8px'
                  }}
                />
                <Area
                  type="monotone"
                  dataKey="exports"
                  stackId="1"
                  stroke="#10b981"
                  fill="#10b981"
                  fillOpacity={0.6}
                />
                <Area
                  type="monotone"
                  dataKey="imports"
                  stackId="2"
                  stroke="#3b82f6"
                  fill="#3b82f6"
                  fillOpacity={0.6}
                />
              </AreaChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};