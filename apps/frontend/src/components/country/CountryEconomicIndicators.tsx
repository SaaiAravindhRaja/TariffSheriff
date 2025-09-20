import React from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, RadarChart, PolarGrid, PolarAngleAxis, PolarRadiusAxis, Radar } from 'recharts';
import { TrendingUp, DollarSign, Users, Building, Globe, BarChart3 } from 'lucide-react';
import { formatCurrency } from '@/lib/utils';

interface CountryEconomicIndicatorsProps {
  countryCode: string;
}

// Mock economic data
const generateEconomicData = (_countryCode: string) => ({
  gdpTrend: [
    { year: '2019', gdp: 1.15, growth: 2.1 },
    { year: '2020', gdp: 1.08, growth: -6.1 },
    { year: '2021', gdp: 1.12, growth: 3.7 },
    { year: '2022', gdp: 1.18, growth: 5.4 },
    { year: '2023', gdp: 1.24, growth: 5.1 },
    { year: '2024', gdp: 1.31, growth: 5.6 }
  ],
  competitiveness: [
    { indicator: 'Infrastructure', score: 85 },
    { indicator: 'Innovation', score: 78 },
    { indicator: 'Market Size', score: 92 },
    { indicator: 'Trade Openness', score: 88 },
    { indicator: 'Financial System', score: 82 },
    { indicator: 'Labor Market', score: 75 }
  ],
  keyMetrics: {
    gdpPerCapita: 52000,
    unemploymentRate: 4.2,
    inflationRate: 2.8,
    currentAccountBalance: 45.2,
    foreignReserves: 180.5,
    publicDebt: 68.3,
    tradeBalance: 12.4,
    fdiInflows: 28.9
  }
});

export const CountryEconomicIndicators: React.FC<CountryEconomicIndicatorsProps> = ({ countryCode }) => {
  const economicData = generateEconomicData(countryCode);

  return (
    <div className="space-y-6">
      {/* Key Economic Metrics */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {[
          {
            title: 'GDP per Capita',
            value: formatCurrency(economicData.keyMetrics.gdpPerCapita, 'USD'),
            change: '+5.2%',
            icon: DollarSign,
            color: 'blue'
          },
          {
            title: 'Unemployment Rate',
            value: `${economicData.keyMetrics.unemploymentRate}%`,
            change: '-0.3%',
            icon: Users,
            color: 'green'
          },
          {
            title: 'Inflation Rate',
            value: `${economicData.keyMetrics.inflationRate}%`,
            change: '+0.1%',
            icon: TrendingUp,
            color: 'orange'
          },
          {
            title: 'Trade Balance',
            value: `$${economicData.keyMetrics.tradeBalance}B`,
            change: '+8.7%',
            icon: Globe,
            color: 'purple'
          }
        ].map((metric, index) => {
          const Icon = metric.icon;
          
          return (
            <Card key={index} className="card-hover">
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium flex items-center">
                  <Icon className={`w-4 h-4 mr-2 text-${metric.color}-600`} />
                  {metric.title}
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{metric.value}</div>
                <p className={`text-xs ${
                  metric.change.startsWith('+') ? 'text-green-600' : 'text-red-600'
                }`}>
                  {metric.change} from last year
                </p>
              </CardContent>
            </Card>
          );
        })}
      </div>

      {/* GDP Growth Trend */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center">
            <BarChart3 className="w-5 h-5 mr-2" />
            GDP Growth Trend
          </CardTitle>
          <CardDescription>
            Gross Domestic Product growth over the past 6 years
          </CardDescription>
        </CardHeader>
        <CardContent>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={economicData.gdpTrend}>
              <CartesianGrid strokeDasharray="3 3" className="opacity-30" />
              <XAxis 
                dataKey="year" 
                className="text-xs"
                tick={{ fontSize: 12 }}
              />
              <YAxis 
                yAxisId="gdp"
                orientation="left"
                className="text-xs"
                tick={{ fontSize: 12 }}
                tickFormatter={(value) => `$${value}T`}
              />
              <YAxis 
                yAxisId="growth"
                orientation="right"
                className="text-xs"
                tick={{ fontSize: 12 }}
                tickFormatter={(value) => `${value}%`}
              />
              <Tooltip 
                formatter={(value: number, name: string) => [
                  name === 'gdp' ? `$${value}T` : `${value}%`,
                  name === 'gdp' ? 'GDP' : 'Growth Rate'
                ]}
                contentStyle={{
                  backgroundColor: 'hsl(var(--card))',
                  border: '1px solid hsl(var(--border))',
                  borderRadius: '8px'
                }}
              />
              <Line
                yAxisId="gdp"
                type="monotone"
                dataKey="gdp"
                stroke="#3b82f6"
                strokeWidth={3}
                dot={{ fill: '#3b82f6', strokeWidth: 2, r: 4 }}
              />
              <Line
                yAxisId="growth"
                type="monotone"
                dataKey="growth"
                stroke="#10b981"
                strokeWidth={2}
                strokeDasharray="5 5"
                dot={{ fill: '#10b981', strokeWidth: 2, r: 3 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </CardContent>
      </Card>

      {/* Economic Indicators Grid */}
      <div className="grid gap-6 lg:grid-cols-2">
        {/* Competitiveness Radar */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center">
              <Building className="w-5 h-5 mr-2" />
              Competitiveness Index
            </CardTitle>
            <CardDescription>
              Key competitiveness indicators (0-100 scale)
            </CardDescription>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={300}>
              <RadarChart data={economicData.competitiveness}>
                <PolarGrid />
                <PolarAngleAxis 
                  dataKey="indicator" 
                  tick={{ fontSize: 10 }}
                />
                <PolarRadiusAxis 
                  angle={90} 
                  domain={[0, 100]}
                  tick={{ fontSize: 10 }}
                />
                <Radar
                  name="Score"
                  dataKey="score"
                  stroke="#3b82f6"
                  fill="#3b82f6"
                  fillOpacity={0.3}
                  strokeWidth={2}
                />
                <Tooltip 
                  formatter={(value: number) => [`${value}/100`, 'Score']}
                  contentStyle={{
                    backgroundColor: 'hsl(var(--card))',
                    border: '1px solid hsl(var(--border))',
                    borderRadius: '8px'
                  }}
                />
              </RadarChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        {/* Additional Economic Metrics */}
        <Card>
          <CardHeader>
            <CardTitle>Additional Economic Metrics</CardTitle>
            <CardDescription>
              Other important economic indicators
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {[
                {
                  label: 'Current Account Balance',
                  value: `$${economicData.keyMetrics.currentAccountBalance}B`,
                  description: 'Balance of trade and transfers'
                },
                {
                  label: 'Foreign Reserves',
                  value: `$${economicData.keyMetrics.foreignReserves}B`,
                  description: 'International reserves'
                },
                {
                  label: 'Public Debt',
                  value: `${economicData.keyMetrics.publicDebt}%`,
                  description: 'As percentage of GDP'
                },
                {
                  label: 'FDI Inflows',
                  value: `$${economicData.keyMetrics.fdiInflows}B`,
                  description: 'Foreign direct investment'
                }
              ].map((metric, index) => (
                <div key={index} className="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-800/50 rounded-lg">
                  <div>
                    <div className="font-medium">{metric.label}</div>
                    <div className="text-sm text-muted-foreground">
                      {metric.description}
                    </div>
                  </div>
                  <div className="text-lg font-bold text-blue-600 dark:text-blue-400">
                    {metric.value}
                  </div>
                </div>
              ))}
            </div>

            <div className="mt-6 p-4 bg-blue-50 dark:bg-blue-900/20 rounded-lg">
              <div className="flex items-start space-x-2">
                <TrendingUp className="w-4 h-4 text-blue-600 dark:text-blue-400 mt-0.5" />
                <div className="text-sm">
                  <div className="font-medium text-blue-900 dark:text-blue-100">
                    Economic Outlook
                  </div>
                  <div className="text-blue-700 dark:text-blue-300">
                    Strong economic fundamentals with positive growth trajectory and stable trade relationships.
                  </div>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};