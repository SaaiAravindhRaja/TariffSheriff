import React from 'react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { formatCurrency, getCountryFlag } from '@/lib/utils'
import { Thermometer, TrendingUp, TrendingDown } from 'lucide-react'

const regionData = [
  {
    region: 'North America',
    countries: [
      { name: 'United States', code: 'US', tariffRate: 12.5, volume: 1200, intensity: 85 },
      { name: 'Canada', code: 'CA', tariffRate: 7.5, volume: 245, intensity: 45 },
      { name: 'Mexico', code: 'MX', tariffRate: 8.0, volume: 189, intensity: 38 }
    ]
  },
  {
    region: 'Europe',
    countries: [
      { name: 'Germany', code: 'DE', tariffRate: 8.2, volume: 654, intensity: 65 },
      { name: 'United Kingdom', code: 'GB', tariffRate: 10.1, volume: 321, intensity: 52 },
      { name: 'France', code: 'FR', tariffRate: 9.8, volume: 287, intensity: 48 },
      { name: 'Netherlands', code: 'NL', tariffRate: 8.5, volume: 198, intensity: 42 }
    ]
  },
  {
    region: 'Asia Pacific',
    countries: [
      { name: 'China', code: 'CN', tariffRate: 15.8, volume: 987, intensity: 95 },
      { name: 'Japan', code: 'JP', tariffRate: 6.8, volume: 432, intensity: 58 },
      { name: 'South Korea', code: 'KR', tariffRate: 9.5, volume: 298, intensity: 48 },
      { name: 'Australia', code: 'AU', tariffRate: 5.2, volume: 156, intensity: 32 }
    ]
  }
]

const tradeFlows = [
  { from: 'CN', to: 'US', volume: 450, tariff: 15.8, growth: 8.2 },
  { from: 'DE', to: 'US', volume: 320, tariff: 8.2, growth: 12.5 },
  { from: 'JP', to: 'GB', volume: 280, tariff: 6.8, growth: -2.1 },
  { from: 'KR', to: 'DE', volume: 195, tariff: 9.5, growth: 15.3 },
  { from: 'US', to: 'CA', volume: 165, tariff: 7.5, growth: 5.7 }
]

export function RegionalHeatmap() {
  const getIntensityColor = (intensity: number) => {
    if (intensity >= 80) return 'bg-red-500'
    if (intensity >= 60) return 'bg-orange-500'
    if (intensity >= 40) return 'bg-yellow-500'
    if (intensity >= 20) return 'bg-green-500'
    return 'bg-blue-500'
  }

  const getIntensityLabel = (intensity: number) => {
    if (intensity >= 80) return 'Very High'
    if (intensity >= 60) return 'High'
    if (intensity >= 40) return 'Medium'
    if (intensity >= 20) return 'Low'
    return 'Very Low'
  }

  const totalVolume = regionData.reduce((sum, region) => 
    sum + region.countries.reduce((regionSum, country) => regionSum + country.volume, 0), 0
  )

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle className="flex items-center gap-2">
              <Thermometer className="w-5 h-5 text-brand-600" />
              Regional Trade Heatmap
            </CardTitle>
            <CardDescription>
              Trade intensity and tariff rates by region and country
            </CardDescription>
          </div>
          <Badge variant="info" className="text-xs">
            ${(totalVolume / 1000).toFixed(1)}B Total Volume
          </Badge>
        </div>
      </CardHeader>
      <CardContent>
        {/* Regional Heatmap */}
        <div className="space-y-6">
          {regionData.map((region) => (
            <div key={region.region} className="space-y-3">
              <h4 className="font-medium text-sm flex items-center justify-between">
                {region.region}
                <Badge variant="outline" className="text-xs">
                  {region.countries.length} countries
                </Badge>
              </h4>
              
              <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                {region.countries.map((country) => (
                  <div
                    key={country.code}
                    className="relative p-3 rounded-lg border hover:shadow-md transition-all cursor-pointer group"
                    style={{
                      background: `linear-gradient(135deg, ${
                        country.intensity >= 80 ? '#fef2f2' : 
                        country.intensity >= 60 ? '#fefbeb' : 
                        country.intensity >= 40 ? '#fefce8' : 
                        country.intensity >= 20 ? '#f0fdf4' : '#eff6ff'
                      }, ${
                        country.intensity >= 80 ? '#fee2e2' : 
                        country.intensity >= 60 ? '#fef3c7' : 
                        country.intensity >= 40 ? '#fef08a' : 
                        country.intensity >= 20 ? '#dcfce7' : '#dbeafe'
                      })`
                    }}
                  >
                    {/* Intensity indicator */}
                    <div className="absolute top-2 right-2">
                      <div 
                        className={`w-3 h-3 rounded-full ${getIntensityColor(country.intensity)}`}
                        title={`${getIntensityLabel(country.intensity)} intensity`}
                      />
                    </div>
                    
                    {/* Country info */}
                    <div className="space-y-2">
                      <div className="flex items-center gap-2">
                        <span className="text-lg">{getCountryFlag(country.code)}</span>
                        <span className="font-medium text-sm">{country.name}</span>
                      </div>
                      
                      <div className="space-y-1 text-xs">
                        <div className="flex justify-between">
                          <span className="text-muted-foreground">Tariff:</span>
                          <span className="font-medium">{country.tariffRate.toFixed(1)}%</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-muted-foreground">Volume:</span>
                          <span className="font-medium">${country.volume}M</span>
                        </div>
                        <div className="flex justify-between">
                          <span className="text-muted-foreground">Intensity:</span>
                          <span className="font-medium">{country.intensity}%</span>
                        </div>
                      </div>
                    </div>

                    {/* Hover tooltip */}
                    <div className="absolute bottom-full left-1/2 transform -translate-x-1/2 mb-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none">
                      <div className="bg-popover text-popover-foreground text-xs rounded-md shadow-lg p-2 whitespace-nowrap border">
                        <div className="font-medium">{country.name}</div>
                        <div>Trade Intensity: {getIntensityLabel(country.intensity)}</div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>

        {/* Major Trade Flows */}
        <div className="mt-6 space-y-3">
          <h4 className="font-medium text-sm">Major Trade Flows</h4>
          <div className="space-y-2">
            {tradeFlows.map((flow, index) => (
              <div key={index} className="flex items-center justify-between p-3 rounded-lg border">
                <div className="flex items-center gap-3">
                  <div className="flex items-center gap-1">
                    <span className="text-lg">{getCountryFlag(flow.from)}</span>
                    <span className="text-muted-foreground text-sm">→</span>
                    <span className="text-lg">{getCountryFlag(flow.to)}</span>
                  </div>
                  <div className="text-sm">
                    <div className="font-medium">${flow.volume}M</div>
                    <div className="text-xs text-muted-foreground">
                      {flow.tariff.toFixed(1)}% tariff
                    </div>
                  </div>
                </div>
                
                <div className="flex items-center gap-2">
                  {flow.growth > 0 ? (
                    <TrendingUp className="w-4 h-4 text-success-500" />
                  ) : (
                    <TrendingDown className="w-4 h-4 text-danger-500" />
                  )}
                  <span className={`text-sm font-medium ${
                    flow.growth > 0 ? 'text-success-600' : 'text-danger-600'
                  }`}>
                    {flow.growth > 0 ? '+' : ''}{flow.growth.toFixed(1)}%
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Legend */}
        <div className="mt-6 p-3 bg-muted/50 rounded-lg">
          <h4 className="font-medium text-sm mb-3">Trade Intensity Legend</h4>
          <div className="grid grid-cols-5 gap-2 text-xs">
            <div className="flex items-center gap-2">
              <div className="w-3 h-3 rounded-full bg-blue-500"></div>
              <span>Very Low (0-20%)</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-3 h-3 rounded-full bg-green-500"></div>
              <span>Low (20-40%)</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-3 h-3 rounded-full bg-yellow-500"></div>
              <span>Medium (40-60%)</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-3 h-3 rounded-full bg-orange-500"></div>
              <span>High (60-80%)</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-3 h-3 rounded-full bg-red-500"></div>
              <span>Very High (80%+)</span>
            </div>
          </div>
        </div>

        {/* Regional Summary */}
        <div className="mt-4 grid grid-cols-3 gap-4 text-sm">
          <div className="text-center">
            <div className="font-medium text-lg text-brand-600">
              {regionData.reduce((sum, region) => sum + region.countries.length, 0)}
            </div>
            <div className="text-muted-foreground">Countries Tracked</div>
          </div>
          <div className="text-center">
            <div className="font-medium text-lg text-success-600">
              {regionData.reduce((sum, region) => 
                sum + region.countries.reduce((avg, country) => avg + country.tariffRate, 0) / region.countries.length, 0
              ).toFixed(1)}%
            </div>
            <div className="text-muted-foreground">Avg Tariff Rate</div>
          </div>
          <div className="text-center">
            <div className="font-medium text-lg text-warning-600">
              {Math.round(regionData.reduce((sum, region) => 
                sum + region.countries.reduce((avg, country) => avg + country.intensity, 0) / region.countries.length, 0
              ) / regionData.length)}%
            </div>
            <div className="text-muted-foreground">Avg Intensity</div>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}