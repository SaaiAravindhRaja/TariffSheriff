import React from 'react'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, ReferenceLine } from 'recharts'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Brain, AlertCircle } from 'lucide-react'

const historicalData = [
  { month: 'Jan 23', actual: 12.5, predicted: null, confidence: null, type: 'historical' },
  { month: 'Feb 23', actual: 13.2, predicted: null, confidence: null, type: 'historical' },
  { month: 'Mar 23', actual: 11.8, predicted: null, confidence: null, type: 'historical' },
  { month: 'Apr 23', actual: 14.1, predicted: null, confidence: null, type: 'historical' },
  { month: 'May 23', actual: 12.9, predicted: null, confidence: null, type: 'historical' },
  { month: 'Jun 23', actual: 13.5, predicted: null, confidence: null, type: 'historical' },
  { month: 'Jul 23', actual: 12.2, predicted: null, confidence: null, type: 'historical' },
  { month: 'Aug 23', actual: 13.8, predicted: null, confidence: null, type: 'historical' },
  { month: 'Sep 23', actual: 12.7, predicted: null, confidence: null, type: 'historical' },
  { month: 'Oct 23', actual: 14.3, predicted: null, confidence: null, type: 'historical' },
  { month: 'Nov 23', actual: 13.1, predicted: null, confidence: null, type: 'historical' },
  { month: 'Dec 23', actual: 12.4, predicted: 12.6, confidence: 95, type: 'current' }
]

const forecastData = [
  { month: 'Jan 24', actual: null, predicted: 13.1, confidence: 92, type: 'forecast' },
  { month: 'Feb 24', actual: null, predicted: 13.8, confidence: 89, type: 'forecast' },
  { month: 'Mar 24', actual: null, predicted: 12.9, confidence: 87, type: 'forecast' },
  { month: 'Apr 24', actual: null, predicted: 14.5, confidence: 84, type: 'forecast' },
  { month: 'May 24', actual: null, predicted: 13.7, confidence: 82, type: 'forecast' },
  { month: 'Jun 24', actual: null, predicted: 14.2, confidence: 79, type: 'forecast' }
]

const allData = [...historicalData, ...forecastData]

const scenarios = [
  {
    name: 'Optimistic',
    description: 'Trade tensions ease, policies stabilize',
    impact: -2.5,
    probability: 25,
    color: '#22c55e'
  },
  {
    name: 'Base Case',
    description: 'Current trends continue',
    impact: 0,
    probability: 50,
    color: '#0ea5e9'
  },
  {
    name: 'Pessimistic',
    description: 'Increased trade barriers, policy uncertainty',
    impact: 3.2,
    probability: 25,
    color: '#ef4444'
  }
]

export function PredictiveAnalyticsChart() {
  const [selectedScenario, setSelectedScenario] = React.useState('Base Case')
  
  const avgConfidence = forecastData.reduce((sum, item) => sum + (item.confidence || 0), 0) / forecastData.length
  const forecastTrend = forecastData[forecastData.length - 1]?.predicted - historicalData[historicalData.length - 1]?.actual

  const CustomTooltip = ({ active, payload, label }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload
      return (
        <div className="bg-background border border-border rounded-lg shadow-lg p-3">
          <p className="font-medium text-foreground mb-2">{label}</p>
          <div className="space-y-1 text-sm">
            {data.actual !== null && (
              <div className="flex justify-between">
                <span className="text-muted-foreground">Actual Rate:</span>
                <span className="font-medium">{data.actual.toFixed(2)}%</span>
              </div>
            )}
            {data.predicted !== null && (
              <div className="flex justify-between">
                <span className="text-muted-foreground">Predicted Rate:</span>
                <span className="font-medium text-brand-600">{data.predicted.toFixed(2)}%</span>
              </div>
            )}
            {data.confidence !== null && (
              <div className="flex justify-between">
                <span className="text-muted-foreground">Confidence:</span>
                <span className="font-medium">{data.confidence}%</span>
              </div>
            )}
            <div className="text-xs text-muted-foreground capitalize">
              {data.type} data
            </div>
          </div>
        </div>
      )
    }
    return null
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle className="flex items-center gap-2">
              <Brain className="w-5 h-5 text-brand-600" />
              Predictive Analytics
            </CardTitle>
            <CardDescription>
              AI-powered tariff rate forecasts and scenario analysis
            </CardDescription>
          </div>
          <div className="flex gap-2">
            <Badge variant="info" className="text-xs">
              {avgConfidence.toFixed(0)}% Confidence
            </Badge>
            <Badge 
              variant={forecastTrend > 0 ? 'destructive' : 'success'}
              className="text-xs"
            >
              {forecastTrend > 0 ? '+' : ''}{forecastTrend?.toFixed(1)}% Trend
            </Badge>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <div className="h-80">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={allData} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
              <defs>
                <linearGradient id="confidenceGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#0ea5e9" stopOpacity={0.2}/>
                  <stop offset="95%" stopColor="#0ea5e9" stopOpacity={0}/>
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" className="opacity-30" />
              <XAxis 
                dataKey="month" 
                axisLine={false}
                tickLine={false}
                className="text-xs text-muted-foreground"
                angle={-45}
                textAnchor="end"
                height={60}
              />
              <YAxis 
                axisLine={false}
                tickLine={false}
                className="text-xs text-muted-foreground"
                tickFormatter={(value) => `${value}%`}
              />
              <Tooltip content={<CustomTooltip />} />
              
              {/* Reference line to separate historical from forecast */}
              <ReferenceLine x="Dec 23" stroke="#94a3b8" strokeDasharray="2 2" />
              
              {/* Historical actual data */}
              <Line
                type="monotone"
                dataKey="actual"
                stroke="#64748b"
                strokeWidth={3}
                dot={{ fill: "#64748b", strokeWidth: 2, r: 4 }}
                connectNulls={false}
                name="Historical"
              />
              
              {/* Predicted data */}
              <Line
                type="monotone"
                dataKey="predicted"
                stroke="#0ea5e9"
                strokeWidth={3}
                strokeDasharray="8 4"
                dot={{ fill: "#0ea5e9", strokeWidth: 2, r: 4 }}
                connectNulls={false}
                name="Forecast"
              />
            </LineChart>
          </ResponsiveContainer>
        </div>

        {/* Scenario Analysis */}
        <div className="mt-4 space-y-3">
          <h4 className="font-medium text-sm">Scenario Analysis</h4>
          <div className="grid gap-2">
            {scenarios.map((scenario) => (
              <div 
                key={scenario.name}
                className={`p-3 rounded-lg border cursor-pointer transition-all ${
                  selectedScenario === scenario.name 
                    ? 'border-brand-500 bg-brand-50 dark:bg-brand-900/20' 
                    : 'border-border hover:bg-accent/50'
                }`}
                onClick={() => setSelectedScenario(scenario.name)}
              >
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    <div 
                      className="w-3 h-3 rounded-full" 
                      style={{ backgroundColor: scenario.color }}
                    />
                    <span className="font-medium text-sm">{scenario.name}</span>
                    <Badge variant="outline" className="text-xs">
                      {scenario.probability}%
                    </Badge>
                  </div>
                  <div className={`text-sm font-medium ${
                    scenario.impact > 0 ? 'text-danger-600' : 
                    scenario.impact < 0 ? 'text-success-600' : 'text-muted-foreground'
                  }`}>
                    {scenario.impact > 0 ? '+' : ''}{scenario.impact.toFixed(1)}%
                  </div>
                </div>
                <div className="text-xs text-muted-foreground">
                  {scenario.description}
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Key Insights */}
        <div className="mt-4 grid grid-cols-3 gap-4 text-sm">
          <div className="text-center">
            <div className="font-medium text-lg text-brand-600">
              {forecastData[forecastData.length - 1]?.predicted.toFixed(1)}%
            </div>
            <div className="text-muted-foreground">6-Month Forecast</div>
          </div>
          <div className="text-center">
            <div className="font-medium text-lg text-success-600">
              {avgConfidence.toFixed(0)}%
            </div>
            <div className="text-muted-foreground">Avg Confidence</div>
          </div>
          <div className="text-center">
            <div className={`font-medium text-lg ${forecastTrend > 0 ? 'text-danger-600' : 'text-success-600'}`}>
              {forecastTrend > 0 ? '+' : ''}{forecastTrend?.toFixed(1)}%
            </div>
            <div className="text-muted-foreground">Expected Change</div>
          </div>
        </div>

        {/* AI Insights */}
        <div className="mt-4 p-3 bg-brand-50 dark:bg-brand-900/20 rounded-lg border border-brand-200 dark:border-brand-800">
          <h4 className="font-medium text-sm text-brand-700 dark:text-brand-300 mb-2 flex items-center gap-1">
            <Brain className="w-4 h-4" />
            AI-Generated Insights
          </h4>
          <div className="text-xs text-brand-600 dark:text-brand-400 space-y-1">
            <div>• Model predicts 14.2% tariff rate by June 2024 (79% confidence)</div>
            <div>• Seasonal pattern shows Q2 typically has higher rates</div>
            <div>• Policy uncertainty is the primary driver of rate volatility</div>
            <div>• Recommend monitoring trade negotiations for early indicators</div>
          </div>
        </div>

        {/* Model Performance */}
        <div className="mt-4 flex items-center justify-between text-xs text-muted-foreground">
          <div className="flex items-center gap-1">
            <AlertCircle className="w-3 h-3" />
            <span>Model accuracy: 87.3% (last 12 months)</span>
          </div>
          <div>Last updated: 2 hours ago</div>
        </div>
      </CardContent>
    </Card>
  )
}