import React, { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { Clock, ExternalLink, MoreHorizontal } from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { formatCurrency, formatDate, getCountryFlag } from '@/lib/utils'
import { useNavigate } from 'react-router-dom'
import api from '@/services/api'

interface Calculation {
  id: number
  name: string
  createdAt: string
  totalTariff: number
  totalValue: number
  rateUsed: string
  appliedRate: number
  hsCode: string
  importerIso3: string
  originIso3: string
  agreementName: string | null
}

export function RecentCalculations() {
  const navigate = useNavigate()
  const [calculations, setCalculations] = useState<Calculation[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchRecentCalculations()
  }, [])

  const fetchRecentCalculations = async () => {
    try {
      setLoading(true)
      const response = await api.get('/tariff-calculations', {
        params: { page: 0, size: 3 }
      })
      setCalculations(response.data.content || [])
    } catch (error) {
      console.error('Failed to fetch recent calculations:', error)
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
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
          </div>
        </CardHeader>
        <CardContent>
          <div className="flex h-48 items-center justify-center text-sm text-muted-foreground">
            Loading calculations...
          </div>
        </CardContent>
      </Card>
    )
  }

  if (calculations.length === 0) {
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
          </div>
        </CardHeader>
        <CardContent>
          <div className="flex h-48 items-center justify-center rounded-md border border-dashed text-sm text-muted-foreground">
            No calculations yet. Start by creating your first tariff calculation!
          </div>
          <div className="mt-4 pt-4 border-t">
            <Button variant="outline" className="w-full" onClick={() => navigate('/calculator')}>
              Create Calculation
            </Button>
          </div>
        </CardContent>
      </Card>
    )
  }

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
          {calculations.map((calc, index) => (
            <motion.div
              key={calc.id}
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.3, delay: index * 0.1 }}
              className="flex items-center justify-between p-3 rounded-lg border hover:bg-accent/50 transition-colors cursor-pointer group"
              onClick={() => navigate(`/saved-tariffs/${calc.id}`)}
            >
              <div className="flex items-center space-x-3 flex-1">
                {/* Route Flags */}
                <div className="flex items-center space-x-1">
                  <span className="text-lg">{getCountryFlag(calc.originIso3 || 'XX')}</span>
                  <span className="text-xs text-muted-foreground">→</span>
                  <span className="text-lg">{getCountryFlag(calc.importerIso3 || 'XX')}</span>
                </div>

                {/* Calculation Details */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center space-x-2 mb-1">
                    <p className="text-sm font-medium truncate">{calc.name || 'Unnamed Calculation'}</p>
                    <Badge 
                      variant={calc.rateUsed === 'PREF' ? 'success' : 'default'}
                      className="text-xs"
                    >
                      {calc.rateUsed}
                    </Badge>
                  </div>
                  <div className="flex items-center space-x-4 text-xs text-muted-foreground">
                    <span>HS: {calc.hsCode || 'N/A'}</span>
                    <span>{formatDate(new Date(calc.createdAt), 'time')}</span>
                    <span className="font-medium text-brand-600">
                      {((calc.appliedRate || 0) * 100).toFixed(1)}% rate
                    </span>
                  </div>
                </div>

                {/* Cost Information */}
                <div className="text-right">
                  <p className="text-sm font-medium">
                    {formatCurrency((calc.totalValue || 0) + (calc.totalTariff || 0))}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    +{formatCurrency(calc.totalTariff || 0)} tariff
                  </p>
                </div>

                {/* Action Button */}
                <Button 
                  variant="ghost" 
                  size="icon"
                  className="opacity-0 group-hover:opacity-100 transition-opacity"
                  onClick={(e) => {
                    e.stopPropagation()
                    navigate(`/saved-tariffs/${calc.id}`)
                  }}
                >
                  <ExternalLink className="w-4 h-4" />
                </Button>
              </div>
            </motion.div>
          ))}
        </div>

        <div className="mt-4 pt-4 border-t">
          <Button variant="outline" className="w-full" onClick={() => navigate('/saved-tariffs')}>
            View All Calculations
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}
