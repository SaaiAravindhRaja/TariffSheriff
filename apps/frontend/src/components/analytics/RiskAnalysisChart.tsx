import React from 'react'
import { RadarChart, PolarGrid, PolarAngleAxis, PolarRadiusAxis, Radar, ResponsiveContainer, Tooltip } from 'recharts'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { AlertTriangle, Shield, TrendingUp } from 'lucide-react'

const riskData = [
  {
    category: 'Political Risk',
    current: 65,
    benchmark: 70,
    trend: -5,
    description: 'Trade policy stability'
  },
  {
    category: 'Economic Risk',
    current: 45,
    benchmark: 55,
    trend: -10,
    description: 'Currency and inflation'
  },
  {
    category: 'Regulatory Risk',
    current: 75,
    benchmark: 60,
    trend: 15,
    description: 'Compliance changes'
  },
  {
    category: 'Supply Chain Risk',
    current: 55,
    benchmark: 50,
    trend: 5,
    description: 'Disruption probability'
  },
  {
    category: 'Market Risk',
    current: 40,
    benchmark: 45,
    trend: -5,
    description: 'Demand volatility'
  },
  {
    category: 'Operational Risk',
    current: 35,
    benchmark: 40,
    trend: -5,
    description: 'Process efficiency'
  }
]

const countryRisks = [
  { country: 'China', riskScore: 72, trend: 'increasing', factors: ['Trade tensions', 'Regulatory changes'] },
  { country: 'United States', riskScore: 58, trend: 'stable', factors: ['Policy uncertainty', 'Tariff volatility'] },
  { country: 'Germany', riskScore: 35, trend: 'decreasing', factors: ['EU regulations', 'Economic stability'] },
  { country: 'Japan', riskScore: 28, trend: 'stable', factors: ['Stable policies', 'Low volatility'] },
  { country: 'United Kingdom', riskScore: 45, trend: 'decreasing', factors: ['Brexit impact', 'New trade deals'] }
]

export function RiskAnalysisChart() {
  const overallRisk = riskData.reduce((sum, item) => sum + item.current, 0) / riskData.length
  const riskTrend = riskData.reduce((sum, item) => sum + item.trend, 0) / riskData.length

  const getRiskLevel = (score: number) => {
    if (score >= 70) return { level: 'High', variant: 'destructive' as const, color: '#ef4444' }
    if (score >= 50) return { level: 'Medium', variant: 'warning' as const, color: '#f59e0b' }
    return { level: 'Low', variant: 'success' as const, color: '#22c55e' }
  }

  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload
      return (
        <div className="bg-background border border-border rounded-lg shadow-lg p-3">
          <p className="font-medium text-foreground mb-2">{data.category}</p>
          <div className="space-y-1 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Current Risk:</span>
              <span className="font-medium">{data.current}%</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Benchmark:</span>
              <span className="font-medium">{data.benchmark}%</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Trend:</span>
              <span className={`font-medium ${data.trend > 0 ? 'text-danger-600' : 'text-success-600'}`}>
                {data.trend > 0 ? '+' : ''}{data.trend}%
              </span>
            </div>
            <div className="text-xs text-muted-foreground mt-1">
              {data.description}
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
            <CardTitle>Risk Analysis</CardTitle>
            <CardDescription>
              Multi-dimensional risk assessment across key categories
            </CardDescription>
          </div>
          <Badge 
            variant={getRiskLevel(overallRisk).variant}
            className="flex items-center gap-1"
          >
            <AlertTriangle className="w-3 h-3" />
            {getRiskLevel(overallRisk).level} Risk
          </Badge>
        </div>
      </CardHeader>
      <CardContent>
        <div className="h-80">
          <ResponsiveContainer width="100%" height="100%">
            <RadarChart data={riskData} margin={{ top: 20, right: 30, bottom: 20, left: 30 }}>
              <PolarGrid />
              <PolarAngleAxis 
                dataKey="category" 
                className="text-xs text-muted-foreground"
                tick={{ fontSize: 10 }}
              />
              <PolarRadiusAxis 
                angle={90} 
                domain={[0, 100]}
                className="text-xs text-muted-foreground"
                tick={{ fontSize: 8 }}
              />
              <Radar
                name="Current Risk"
                dataKey="current"
                stroke="#ef4444"
                fill="#ef4444"
                fillOpacity={0.2}
                strokeWidth={2}
              />
              <Radar
                name="Benchmark"
                dataKey="benchmark"
                stroke="#94a3b8"
                fill="transparent"
                strokeWidth={2}
                strokeDasharray="5 5"
              />
              <Tooltip content={<CustomTooltip />} />
            </RadarChart>
          </ResponsiveContainer>
        </div>

        {/* Risk Categories */}
        <div className="mt-4 grid grid-cols-2 gap-4">
          <div className="space-y-2">
            <h4 className="font-medium text-sm">High Risk Areas</h4>
            <div className="space-y-1">
              {riskData
                .filter(item => item.current >= 60)
                .sort((a, b) => b.current - a.current)
                .map((item) => (
                  <div key={item.category} className="flex items-center justify-between p-2 rounded border">
                    <span className="text-sm font-medium">{item.category}</span>
                    <div className="flex items-center gap-2">
                      <Badge variant="destructive" className="text-xs">
                        {item.current}%
                      </Badge>
                      {item.trend > 0 && <TrendingUp className="w-3 h-3 text-danger-500" />}
                    </div>
                  </div>
                ))}
            </div>
          </div>
          
          <div className="space-y-2">
            <h4 className="font-medium text-sm">Country Risk Scores</h4>
            <div className="space-y-1">
              {countryRisks
                .sort((a, b) => b.riskScore - a.riskScore)
                .slice(0, 3)
                .map((country) => (
                  <div key={country.country} className="flex items-center justify-between p-2 rounded border">
                    <span className="text-sm font-medium">{country.country}</span>
                    <div className="flex items-center gap-2">
                      <Badge 
                        variant={getRiskLevel(country.riskScore).variant}
                        className="text-xs"
                      >
                        {country.riskScore}%
                      </Badge>
                      <div className={`w-2 h-2 rounded-full ${
                        country.trend === 'increasing' ? 'bg-danger-500' :
                        country.trend === 'decreasing' ? 'bg-success-500' : 'bg-warning-500'
                      }`} />
                    </div>
                  </div>
                ))}
            </div>
          </div>
        </div>

        {/* Risk Summary */}
        <div className="mt-4 grid grid-cols-3 gap-4 text-sm">
          <div className="text-center">
            <div className={`font-medium text-lg ${getRiskLevel(overallRisk).color}`} style={{ color: getRiskLevel(overallRisk).color }}>
              {overallRisk.toFixed(1)}%
            </div>
            <div className="text-muted-foreground">Overall Risk</div>
          </div>
          <div className="text-center">
            <div className={`font-medium text-lg ${riskTrend > 0 ? 'text-danger-600' : 'text-success-600'}`}>
              {riskTrend > 0 ? '+' : ''}{riskTrend.toFixed(1)}%
            </div>
            <div className="text-muted-foreground">Risk Trend</div>
          </div>
          <div className="text-center">
            <div className="font-medium text-lg">
              {riskData.filter(item => item.current >= 60).length}
            </div>
            <div className="text-muted-foreground">High Risk Areas</div>
          </div>
        </div>

        {/* Risk Mitigation */}
        <div className="mt-4 p-3 bg-danger-50 dark:bg-danger-900/20 rounded-lg border border-danger-200 dark:border-danger-800">
          <h4 className="font-medium text-sm text-danger-700 dark:text-danger-300 mb-2 flex items-center gap-1">
            <Shield className="w-4 h-4" />
            Risk Mitigation Strategies
          </h4>
          <div className="text-xs text-danger-600 dark:text-danger-400 space-y-1">
            <div>• Diversify supply chains to reduce regulatory risk exposure</div>
            <div>• Implement hedging strategies for currency and political risks</div>
            <div>• Monitor regulatory changes in high-risk jurisdictions</div>
            <div>• Establish contingency plans for supply chain disruptions</div>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}