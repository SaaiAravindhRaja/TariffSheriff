import React from 'react'
import { Sankey, ResponsiveContainer, Tooltip } from 'recharts'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Truck, Factory, Package, AlertTriangle } from 'lucide-react'

const supplyChainData = {
  nodes: [
    // Raw Materials
    { name: 'Lithium (Chile)', category: 'raw', risk: 'medium' },
    { name: 'Cobalt (DRC)', category: 'raw', risk: 'high' },
    { name: 'Nickel (Indonesia)', category: 'raw', risk: 'medium' },
    { name: 'Rare Earths (China)', category: 'raw', risk: 'high' },
    
    // Components
    { name: 'Battery Cells (China)', category: 'component', risk: 'high' },
    { name: 'Motors (Germany)', category: 'component', risk: 'low' },
    { name: 'Electronics (South Korea)', category: 'component', risk: 'medium' },
    { name: 'Chassis (Mexico)', category: 'component', risk: 'low' },
    
    // Assembly
    { name: 'Final Assembly (US)', category: 'assembly', risk: 'low' },
    { name: 'Final Assembly (EU)', category: 'assembly', risk: 'low' }
  ],
  links: [
    // Raw materials to components
    { source: 0, target: 4, value: 25 }, // Lithium to Battery Cells
    { source: 1, target: 4, value: 15 }, // Cobalt to Battery Cells
    { source: 2, target: 4, value: 20 }, // Nickel to Battery Cells
    { source: 3, target: 5, value: 10 }, // Rare Earths to Motors
    { source: 3, target: 6, value: 15 }, // Rare Earths to Electronics
    
    // Components to assembly
    { source: 4, target: 8, value: 30 }, // Battery Cells to US Assembly
    { source: 4, target: 9, value: 30 }, // Battery Cells to EU Assembly
    { source: 5, target: 8, value: 15 }, // Motors to US Assembly
    { source: 5, target: 9, value: 15 }, // Motors to EU Assembly
    { source: 6, target: 8, value: 20 }, // Electronics to US Assembly
    { source: 6, target: 9, value: 20 }, // Electronics to EU Assembly
    { source: 7, target: 8, value: 25 }, // Chassis to US Assembly
    { source: 7, target: 9, value: 10 }  // Chassis to EU Assembly
  ]
}

const riskFactors = [
  {
    factor: 'Geopolitical Risk',
    impact: 'High',
    affectedNodes: ['China', 'DRC'],
    description: 'Trade tensions and political instability',
    mitigation: 'Diversify suppliers'
  },
  {
    factor: 'Supply Concentration',
    impact: 'High',
    affectedNodes: ['China', 'Indonesia'],
    description: 'Over-reliance on single countries',
    mitigation: 'Alternative sourcing'
  },
  {
    factor: 'Transportation Risk',
    impact: 'Medium',
    affectedNodes: ['All maritime routes'],
    description: 'Shipping delays and costs',
    mitigation: 'Multiple logistics partners'
  },
  {
    factor: 'Regulatory Changes',
    impact: 'Medium',
    affectedNodes: ['US', 'EU'],
    description: 'Changing trade policies',
    mitigation: 'Policy monitoring'
  }
]

const alternativeRoutes = [
  {
    component: 'Battery Cells',
    current: { country: 'China', tariff: 15.8, risk: 'High' },
    alternatives: [
      { country: 'South Korea', tariff: 9.5, risk: 'Medium', capacity: '60%' },
      { country: 'Japan', tariff: 6.8, risk: 'Low', capacity: '40%' },
      { country: 'Poland', tariff: 8.2, risk: 'Low', capacity: '25%' }
    ]
  },
  {
    component: 'Rare Earth Magnets',
    current: { country: 'China', tariff: 18.2, risk: 'High' },
    alternatives: [
      { country: 'Australia', tariff: 5.2, risk: 'Low', capacity: '30%' },
      { country: 'Canada', tariff: 7.5, risk: 'Low', capacity: '20%' },
      { country: 'Vietnam', tariff: 12.1, risk: 'Medium', capacity: '45%' }
    ]
  }
]

export function SupplyChainAnalytics() {
  

  const getRiskColor = (risk: string) => {
    switch (risk) {
      case 'high': return 'text-danger-600 bg-danger-50 border-danger-200'
      case 'medium': return 'text-warning-600 bg-warning-50 border-warning-200'
      case 'low': return 'text-success-600 bg-success-50 border-success-200'
      default: return 'text-muted-foreground bg-muted border-border'
    }
  }

  const getRiskBadgeVariant = (risk: string) => {
    switch (risk) {
      case 'High': return 'destructive' as const
      case 'Medium': return 'warning' as const
      case 'Low': return 'success' as const
      default: return 'secondary' as const
    }
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle className="flex items-center gap-2">
              <Truck className="w-5 h-5 text-brand-600" />
              Supply Chain Analytics
            </CardTitle>
            <CardDescription>
              EV supply chain flow analysis and risk assessment
            </CardDescription>
          </div>
          <Badge variant="warning" className="text-xs">
            4 Risk Factors
          </Badge>
        </div>
      </CardHeader>
      <CardContent>
        {/* Supply Chain Flow Visualization */}
        <div className="mb-6">
          <h4 className="font-medium text-sm mb-3">Supply Chain Flow</h4>
          <div className="grid grid-cols-3 gap-6 p-4 bg-muted/30 rounded-lg">
            {/* Raw Materials */}
            <div className="space-y-3">
              <div className="flex items-center gap-2 text-sm font-medium">
                <Package className="w-4 h-4" />
                Raw Materials
              </div>
              {supplyChainData.nodes.slice(0, 4).map((node, index) => (
                <div 
                  key={index}
                  className={`p-2 rounded border text-xs ${getRiskColor(node.risk)}`}
                >
                  {node.name}
                </div>
              ))}
            </div>

            {/* Components */}
            <div className="space-y-3">
              <div className="flex items-center gap-2 text-sm font-medium">
                <Factory className="w-4 h-4" />
                Components
              </div>
              {supplyChainData.nodes.slice(4, 8).map((node, index) => (
                <div 
                  key={index}
                  className={`p-2 rounded border text-xs ${getRiskColor(node.risk)}`}
                >
                  {node.name}
                </div>
              ))}
            </div>

            {/* Final Assembly */}
            <div className="space-y-3">
              <div className="flex items-center gap-2 text-sm font-medium">
                <Truck className="w-4 h-4" />
                Final Assembly
              </div>
              {supplyChainData.nodes.slice(8).map((node, index) => (
                <div 
                  key={index}
                  className={`p-2 rounded border text-xs ${getRiskColor(node.risk)}`}
                >
                  {node.name}
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Risk Factors */}
        <div className="mb-6 space-y-3">
          <h4 className="font-medium text-sm">Risk Assessment</h4>
          <div className="grid gap-3">
            {riskFactors.map((risk, index) => (
              <div key={index} className="p-3 rounded-lg border">
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    <AlertTriangle className="w-4 h-4 text-warning-600" />
                    <span className="font-medium text-sm">{risk.factor}</span>
                  </div>
                  <Badge variant={getRiskBadgeVariant(risk.impact)} className="text-xs">
                    {risk.impact} Impact
                  </Badge>
                </div>
                <div className="text-xs text-muted-foreground mb-2">
                  {risk.description}
                </div>
                <div className="flex items-center justify-between text-xs">
                  <span className="text-muted-foreground">
                    Affected: {Array.isArray(risk.affectedNodes) ? risk.affectedNodes.join(', ') : risk.affectedNodes}
                  </span>
                  <span className="font-medium text-brand-600">
                    Mitigation: {risk.mitigation}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Alternative Sourcing */}
        <div className="space-y-3">
          <h4 className="font-medium text-sm">Alternative Sourcing Options</h4>
          <div className="space-y-4">
            {alternativeRoutes.map((route, index) => (
              <div key={index} className="space-y-3">
                <div className="flex items-center justify-between">
                  <h5 className="font-medium text-sm">{route.component}</h5>
                  <Badge variant="outline" className="text-xs">
                    Current: {route.current.country}
                  </Badge>
                </div>
                
                {/* Current Supplier */}
                <div className="p-3 bg-muted/50 rounded-lg border-l-4 border-l-danger-500">
                  <div className="flex items-center justify-between mb-1">
                    <span className="font-medium text-sm">Current: {route.current.country}</span>
                    <Badge variant="destructive" className="text-xs">
                      {route.current.risk} Risk
                    </Badge>
                  </div>
                  <div className="text-xs text-muted-foreground">
                    Tariff: {route.current.tariff}%
                  </div>
                </div>

                {/* Alternatives */}
                <div className="grid gap-2">
                  {route.alternatives.map((alt, altIndex) => (
                    <div key={altIndex} className="p-2 rounded border hover:bg-accent/50 transition-colors">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <span className="text-sm font-medium">{alt.country}</span>
                          <Badge 
                            variant={getRiskBadgeVariant(alt.risk)}
                            className="text-xs"
                          >
                            {alt.risk}
                          </Badge>
                        </div>
                        <div className="text-xs text-muted-foreground">
                          {alt.tariff}% tariff • {alt.capacity} capacity
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Supply Chain Metrics */}
        <div className="mt-6 grid grid-cols-4 gap-4 text-sm">
          <div className="text-center">
            <div className="font-medium text-lg text-danger-600">68%</div>
            <div className="text-muted-foreground">High Risk Exposure</div>
          </div>
          <div className="text-center">
            <div className="font-medium text-lg text-warning-600">12</div>
            <div className="text-muted-foreground">Critical Suppliers</div>
          </div>
          <div className="text-center">
            <div className="font-medium text-lg text-brand-600">85%</div>
            <div className="text-muted-foreground">Supply Reliability</div>
          </div>
          <div className="text-center">
            <div className="font-medium text-lg text-success-600">3.2 days</div>
            <div className="text-muted-foreground">Avg Lead Time</div>
          </div>
        </div>

        {/* Recommendations */}
        <div className="mt-4 p-3 bg-brand-50 dark:bg-brand-900/20 rounded-lg border border-brand-200 dark:border-brand-800">
          <h4 className="font-medium text-sm text-brand-700 dark:text-brand-300 mb-2">
            Supply Chain Recommendations
          </h4>
          <div className="text-xs text-brand-600 dark:text-brand-400 space-y-1">
            <div>• Reduce China dependency by sourcing 40% of battery cells from South Korea/Japan</div>
            <div>• Establish strategic inventory for critical rare earth materials</div>
            <div>• Develop secondary suppliers in low-risk countries (Australia, Canada)</div>
            <div>• Implement real-time supply chain monitoring and early warning systems</div>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}