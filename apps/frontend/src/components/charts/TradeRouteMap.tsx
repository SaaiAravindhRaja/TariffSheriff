import React from 'react'
import { motion } from 'framer-motion'
import { Badge } from '@/components/ui/badge'
import { getCountryFlag } from '@/lib/utils'

const tradeRoutes = [
  {
    id: 1,
    from: { code: 'US', name: 'United States', x: 20, y: 40 },
    to: { code: 'CN', name: 'China', x: 75, y: 35 },
    volume: '$1.2B',
    tariffRate: '12.5%',
    status: 'active',
    products: ['Electric Vehicles', 'Batteries', 'Components']
  },
  {
    id: 2,
    from: { code: 'DE', name: 'Germany', x: 50, y: 30 },
    to: { code: 'US', name: 'United States', x: 20, y: 40 },
    volume: '$890M',
    tariffRate: '8.2%',
    status: 'active',
    products: ['Luxury EVs', 'Auto Parts']
  },
  {
    id: 3,
    from: { code: 'JP', name: 'Japan', x: 80, y: 38 },
    to: { code: 'GB', name: 'United Kingdom', x: 48, y: 28 },
    volume: '$654M',
    tariffRate: '15.1%',
    status: 'warning',
    products: ['Hybrid Vehicles', 'Technology']
  },
  {
    id: 4,
    from: { code: 'KR', name: 'South Korea', x: 78, y: 42 },
    to: { code: 'DE', name: 'Germany', x: 50, y: 30 },
    volume: '$432M',
    tariffRate: '6.8%',
    status: 'active',
    products: ['Battery Tech', 'EVs']
  }
]

const countries = [
  { code: 'US', name: 'United States', x: 20, y: 40, volume: '$2.1B' },
  { code: 'CN', name: 'China', x: 75, y: 35, volume: '$1.8B' },
  { code: 'DE', name: 'Germany', x: 50, y: 30, volume: '$1.2B' },
  { code: 'JP', name: 'Japan', x: 80, y: 38, volume: '$890M' },
  { code: 'GB', name: 'United Kingdom', x: 48, y: 28, volume: '$654M' },
  { code: 'KR', name: 'South Korea', x: 78, y: 42, volume: '$432M' }
]

export function TradeRouteMap() {
  const [selectedRoute, setSelectedRoute] = React.useState<number | null>(null)

  return (
    <div className="relative w-full h-96 bg-gradient-to-br from-slate-50 to-slate-100 dark:from-slate-900 dark:to-slate-800 rounded-lg overflow-hidden">
      {/* World Map Background */}
      <div className="absolute inset-0 opacity-10 dark:opacity-5">
        <svg viewBox="0 0 100 60" className="w-full h-full">
          {/* Simplified world map paths */}
          <path
            d="M15,25 Q20,20 25,25 L30,30 Q35,25 40,30 L45,25 Q50,30 55,25 L60,30 Q65,25 70,30 L75,25 Q80,30 85,25"
            fill="none"
            stroke="currentColor"
            strokeWidth="0.5"
            className="text-muted-foreground"
          />
          <path
            d="M10,35 Q15,30 20,35 L25,40 Q30,35 35,40 L40,35 Q45,40 50,35 L55,40 Q60,35 65,40 L70,35 Q75,40 80,35 L85,40"
            fill="none"
            stroke="currentColor"
            strokeWidth="0.5"
            className="text-muted-foreground"
          />
        </svg>
      </div>

      {/* Trade Routes */}
      <svg className="absolute inset-0 w-full h-full">
        {tradeRoutes.map((route, index) => (
          <motion.g
            key={route.id}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.5, delay: index * 0.2 }}
          >
            {/* Route Line */}
            <motion.line
              x1={`${route.from.x}%`}
              y1={`${route.from.y}%`}
              x2={`${route.to.x}%`}
              y2={`${route.to.y}%`}
              stroke={
                route.status === 'active' 
                  ? '#22c55e' 
                  : route.status === 'warning' 
                  ? '#f59e0b' 
                  : '#ef4444'
              }
              strokeWidth="2"
              strokeDasharray="5,5"
              className="cursor-pointer"
              onClick={() => setSelectedRoute(selectedRoute === route.id ? null : route.id)}
              whileHover={{ strokeWidth: 3 }}
            />
            
            {/* Animated Flow */}
            <motion.circle
              r="2"
              fill={
                route.status === 'active' 
                  ? '#22c55e' 
                  : route.status === 'warning' 
                  ? '#f59e0b' 
                  : '#ef4444'
              }
              animate={{
                cx: [`${route.from.x}%`, `${route.to.x}%`],
                cy: [`${route.from.y}%`, `${route.to.y}%`],
              }}
              transition={{
                duration: 3,
                repeat: Infinity,
                ease: "linear"
              }}
            />
          </motion.g>
        ))}
      </svg>

      {/* Country Nodes */}
      {countries.map((country, index) => (
        <motion.div
          key={country.code}
          initial={{ scale: 0, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ duration: 0.5, delay: index * 0.1 }}
          className="absolute transform -translate-x-1/2 -translate-y-1/2 cursor-pointer group"
          style={{ left: `${country.x}%`, top: `${country.y}%` }}
        >
          <div className="relative">
            {/* Country Flag/Icon */}
            <div className="w-8 h-8 rounded-full bg-white dark:bg-slate-800 border-2 border-brand-500 flex items-center justify-center text-lg shadow-lg group-hover:scale-110 transition-transform">
              {getCountryFlag(country.code)}
            </div>
            
            {/* Tooltip */}
            <div className="absolute bottom-full left-1/2 transform -translate-x-1/2 mb-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none">
              <div className="bg-popover text-popover-foreground text-xs rounded-md shadow-lg p-2 whitespace-nowrap border">
                <div className="font-medium">{country.name}</div>
                <div className="text-muted-foreground">Volume: {country.volume}</div>
              </div>
            </div>
          </div>
        </motion.div>
      ))}

      {/* Route Details Panel */}
      {selectedRoute && (
        <motion.div
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          exit={{ opacity: 0, x: 20 }}
          className="absolute top-4 right-4 bg-background border rounded-lg shadow-lg p-4 max-w-xs"
        >
          {(() => {
            const route = tradeRoutes.find(r => r.id === selectedRoute)
            if (!route) return null
            
            return (
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <h4 className="font-medium text-sm">Trade Route</h4>
                  <Badge 
                    variant={route.status === 'active' ? 'success' : 'warning'}
                    className="text-xs"
                  >
                    {route.status}
                  </Badge>
                </div>
                
                <div className="flex items-center space-x-2 text-sm">
                  <span>{getCountryFlag(route.from.code)}</span>
                  <span className="text-muted-foreground">→</span>
                  <span>{getCountryFlag(route.to.code)}</span>
                </div>
                
                <div className="space-y-1 text-xs">
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Volume:</span>
                    <span className="font-medium">{route.volume}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-muted-foreground">Tariff Rate:</span>
                    <span className="font-medium">{route.tariffRate}</span>
                  </div>
                </div>
                
                <div>
                  <div className="text-xs text-muted-foreground mb-1">Products:</div>
                  <div className="flex flex-wrap gap-1">
                    {route.products.map((product) => (
                      <Badge key={product} variant="secondary" className="text-xs">
                        {product}
                      </Badge>
                    ))}
                  </div>
                </div>
              </div>
            )
          })()}
        </motion.div>
      )}

      {/* Legend */}
      <div className="absolute bottom-4 left-4 bg-background/80 backdrop-blur-sm border rounded-lg p-3">
        <div className="text-xs font-medium mb-2">Route Status</div>
        <div className="space-y-1 text-xs">
          <div className="flex items-center space-x-2">
            <div className="w-3 h-0.5 bg-success-500"></div>
            <span>Active</span>
          </div>
          <div className="flex items-center space-x-2">
            <div className="w-3 h-0.5 bg-warning-500"></div>
            <span>Warning</span>
          </div>
          <div className="flex items-center space-x-2">
            <div className="w-3 h-0.5 bg-danger-500"></div>
            <span>Blocked</span>
          </div>
        </div>
      </div>
    </div>
  )
}