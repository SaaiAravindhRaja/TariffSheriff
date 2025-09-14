
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell } from 'recharts'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { formatCurrency, getCountryFlag } from '@/lib/utils'

const countryData = [
  { 
    country: 'United States', 
    code: 'US',
    tariffRate: 12.5, 
    tradeVolume: 1200000000, 
    growth: 8.2,
    disputes: 15,
    compliance: 94.2
  },
  { 
    country: 'China', 
    code: 'CN',
    tariffRate: 15.8, 
    tradeVolume: 987000000, 
    growth: 12.8,
    disputes: 28,
    compliance: 91.5
  },
  { 
    country: 'Germany', 
    code: 'DE',
    tariffRate: 8.2, 
    tradeVolume: 654000000, 
    growth: -2.1,
    disputes: 8,
    compliance: 96.8
  },
  { 
    country: 'Japan', 
    code: 'JP',
    tariffRate: 6.8, 
    tradeVolume: 432000000, 
    growth: 5.7,
    disputes: 5,
    compliance: 97.2
  },
  { 
    country: 'United Kingdom', 
    code: 'GB',
    tariffRate: 10.1, 
    tradeVolume: 321000000, 
    growth: 3.4,
    disputes: 12,
    compliance: 95.1
  },
  { 
    country: 'South Korea', 
    code: 'KR',
    tariffRate: 9.5, 
    tradeVolume: 298000000, 
    growth: 7.9,
    disputes: 6,
    compliance: 96.3
  }
]

export function CountryAnalysisChart() {
  const getBarColor = (tariffRate: number) => {
    if (tariffRate < 8) return '#22c55e' // Low tariff - green
    if (tariffRate < 12) return '#f59e0b' // Medium tariff - yellow
    return '#ef4444' // High tariff - red
  }

  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload
      return (
        <div className="bg-background border border-border rounded-lg shadow-lg p-3">
          <div className="flex items-center gap-2 mb-2">
            <span className="text-lg">{getCountryFlag(data.code)}</span>
            <p className="font-medium text-foreground">{data.country}</p>
          </div>
          <div className="space-y-1 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Tariff Rate:</span>
              <span className="font-medium">{data.tariffRate.toFixed(1)}%</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Trade Volume:</span>
              <span className="font-medium">{formatCurrency(data.tradeVolume)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Growth:</span>
              <span className={`font-medium ${data.growth > 0 ? 'text-success-600' : 'text-danger-600'}`}>
                {data.growth > 0 ? '+' : ''}{data.growth.toFixed(1)}%
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Compliance:</span>
              <span className="font-medium">{data.compliance.toFixed(1)}%</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Disputes:</span>
              <span className="font-medium">{data.disputes}</span>
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
        <CardTitle>Country Analysis</CardTitle>
        <CardDescription>
          Tariff rates and trade performance by country
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="h-80">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={countryData} margin={{ top: 20, right: 30, left: 20, bottom: 60 }}>
              <CartesianGrid strokeDasharray="3 3" className="opacity-30" />
              <XAxis 
                dataKey="country" 
                axisLine={false}
                tickLine={false}
                className="text-xs text-muted-foreground"
                angle={-45}
                textAnchor="end"
                height={80}
                interval={0}
                tick={{ fontSize: 10 }}
              />
              <YAxis 
                axisLine={false}
                tickLine={false}
                className="text-xs text-muted-foreground"
                tickFormatter={(value) => `${value}%`}
              />
              <Tooltip content={<CustomTooltip />} />
              <Bar dataKey="tariffRate" radius={[4, 4, 0, 0]}>
                {countryData.map((entry, index) => (
                  <Cell 
                    key={`cell-${index}`} 
                    fill={getBarColor(entry.tariffRate)} 
                  />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Country Rankings */}
        <div className="mt-4 space-y-2">
          <h4 className="font-medium text-sm">Top Performers</h4>
          <div className="grid gap-2">
            {countryData
              .sort((a, b) => b.compliance - a.compliance)
              .slice(0, 3)
              .map((country, index) => (
                <div key={country.code} className="flex items-center justify-between p-2 rounded border">
                  <div className="flex items-center gap-2">
                    <span className="text-lg">{getCountryFlag(country.code)}</span>
                    <span className="text-sm font-medium">{country.country}</span>
                    <Badge variant="outline" className="text-xs">
                      #{index + 1}
                    </Badge>
                  </div>
                  <div className="flex items-center gap-2 text-sm">
                    <span className="text-muted-foreground">Compliance:</span>
                    <span className="font-medium text-success-600">
                      {country.compliance.toFixed(1)}%
                    </span>
                  </div>
                </div>
              ))}
          </div>
        </div>
      </CardContent>
    </Card>
  )
}