import React from 'react'
import { motion } from 'framer-motion'
import { Clock, ExternalLink, MoreHorizontal } from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { formatCurrency, formatDate, getCountryFlag } from '@/lib/utils'

const recentCalculations = [
  {
    id: 1,
    product: 'Tesla Model Y',
    hsCode: '8703.80.10',
    origin: { code: 'CN', name: 'China' },
    destination: { code: 'US', name: 'United States' },
    value: 45000,
    tariffRate: 0.125,
    tariffAmount: 5625,
    totalCost: 50625,
    timestamp: new Date(Date.now() - 2 * 60 * 60 * 1000), // 2 hours ago
    status: 'completed'
  },
  {
    id: 2,
    product: 'BMW iX Battery Pack',
    hsCode: '8507.60.00',
    origin: { code: 'DE', name: 'Germany' },
    destination: { code: 'US', name: 'United States' },
    value: 12000,
    tariffRate: 0.082,
    tariffAmount: 984,
    totalCost: 12984,
    timestamp: new Date(Date.now() - 4 * 60 * 60 * 1000), // 4 hours ago
    status: 'completed'
  },
  {
    id: 3,
    product: 'Nissan Leaf Components',
    hsCode: '8708.99.81',
    origin: { code: 'JP', name: 'Japan' },
    destination: { code: 'GB', name: 'United Kingdom' },
    value: 8500,
    tariffRate: 0.151,
    tariffAmount: 1283.5,
    totalCost: 9783.5,
    timestamp: new Date(Date.now() - 6 * 60 * 60 * 1000), // 6 hours ago
    status: 'pending'
  },
  {
    id: 4,
    product: 'Hyundai IONIQ 5',
    hsCode: '8703.80.20',
    origin: { code: 'KR', name: 'South Korea' },
    destination: { code: 'DE', name: 'Germany' },
    value: 38000,
    tariffRate: 0.068,
    tariffAmount: 2584,
    totalCost: 40584,
    timestamp: new Date(Date.now() - 8 * 60 * 60 * 1000), // 8 hours ago
    status: 'completed'
  }
]

export function RecentCalculations() {
  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <div className="p-2 bg-success-100 dark:bg-success-900/20 rounded-lg">
              <Clock className="w-5 h-5 text-success-600" />
            </div>
            <div>
              <CardTitle>Recent Calculations</CardTitle>
              <CardDescription>
                Your latest tariff calculations and estimates
              </CardDescription>
            </div>
          </div>
          <Button variant="ghost" size="icon">
            <MoreHorizontal className="w-4 h-4" />
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {recentCalculations.map((calc, index) => (
            <motion.div
              key={calc.id}
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.3, delay: index * 0.1 }}
              className="flex items-center justify-between p-3 rounded-lg border hover:bg-accent/50 transition-colors cursor-pointer group"
            >
              <div className="flex items-center space-x-3 flex-1">
                {/* Route Flags */}
                <div className="flex items-center space-x-1">
                  <span className="text-lg">{getCountryFlag(calc.origin.code)}</span>
                  <span className="text-xs text-muted-foreground">→</span>
                  <span className="text-lg">{getCountryFlag(calc.destination.code)}</span>
                </div>

                {/* Calculation Details */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center space-x-2 mb-1">
                    <p className="text-sm font-medium truncate">{calc.product}</p>
                    <Badge 
                      variant={calc.status === 'completed' ? 'success' : 'warning'}
                      className="text-xs"
                    >
                      {calc.status}
                    </Badge>
                  </div>
                  <div className="flex items-center space-x-4 text-xs text-muted-foreground">
                    <span>HS: {calc.hsCode}</span>
                    <span>{formatDate(calc.timestamp, 'time')}</span>
                    <span className="font-medium text-brand-600">
                      {(calc.tariffRate * 100).toFixed(1)}% rate
                    </span>
                  </div>
                </div>

                {/* Cost Information */}
                <div className="text-right">
                  <p className="text-sm font-medium">
                    {formatCurrency(calc.totalCost)}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    +{formatCurrency(calc.tariffAmount)} tariff
                  </p>
                </div>

                {/* Action Button */}
                <Button 
                  variant="ghost" 
                  size="icon"
                  className="opacity-0 group-hover:opacity-100 transition-opacity"
                >
                  <ExternalLink className="w-4 h-4" />
                </Button>
              </div>
            </motion.div>
          ))}
        </div>

        <div className="mt-4 pt-4 border-t">
          <Button variant="outline" className="w-full">
            View All Calculations
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}