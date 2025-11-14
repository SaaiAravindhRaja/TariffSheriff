import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { GlobalTradeRoutes } from '@/components/dashboard/GlobalTradeRoutes'
import { Search, ChevronDown, ChevronRight, Calculator, Battery, Zap, Cpu, ThermometerSun, Box, Gauge, Eye, Lightbulb, Magnet } from 'lucide-react'
import api from '@/services/api'

interface TariffRate {
  id: number
  hsCode: string
  description: string
  importerIso3: string
  originIso3: string
  mfnRate: number
  preferentialRate: number
  effectiveDate: string
}

interface HsCodeCategory {
  name: string
  icon: any
  description: string
  subcategories: {
    name: string
    codes: string[]
  }[]
}

const hsCodeCategories: HsCodeCategory[] = [
  {
    name: 'Whole Vehicles',
    icon: Box,
    description: 'Complete electric vehicles (excluding 8703.80)',
    subcategories: []
  },
  {
    name: 'Traction Battery System',
    icon: Battery,
    description: 'Battery packs and accumulator systems',
    subcategories: [
      {
        name: 'Lithium-ion accumulators',
        codes: ['8507.60']
      },
      {
        name: 'Nickel-metal hydride accumulators',
        codes: ['8507.50']
      },
      {
        name: 'Other lead-acid accumulators',
        codes: ['8507.20']
      },
      {
        name: 'Other accumulators',
        codes: ['8507.80']
      },
      {
        name: 'Parts of electric accumulators',
        codes: ['8507.90']
      }
    ]
  },
  {
    name: 'Electric Drive (Traction Motors & Parts)',
    icon: Zap,
    description: 'Motors, generators, and related components',
    subcategories: [
      {
        name: 'DC motors & generators',
        codes: ['8501.31', '8501.32', '8501.33', '8501.34']
      },
      {
        name: 'AC motors',
        codes: ['8501.40', '8501.51', '8501.52', '8501.53']
      },
      {
        name: 'Parts of motors/generators',
        codes: ['8503.00']
      }
    ]
  },
  {
    name: 'Power Electronics & Charging',
    icon: Cpu,
    description: 'Inverters, converters, EVSE, and control systems',
    subcategories: [
      {
        name: 'Static converters',
        codes: ['8504.40']
      },
      {
        name: 'Inductors',
        codes: ['8504.50']
      },
      {
        name: 'Parts of transformers/converters',
        codes: ['8504.90']
      },
      {
        name: 'Control/distribution boards',
        codes: ['8537.10']
      },
      {
        name: 'Fuses ≤1 kV',
        codes: ['8536.10']
      },
      {
        name: 'Relays & switches',
        codes: ['8536.41', '8536.50']
      },
      {
        name: 'Charging connectors',
        codes: ['8536.69']
      },
      {
        name: 'Vehicle wiring harnesses',
        codes: ['8544.30']
      },
      {
        name: 'Charging cables',
        codes: ['8544.42', '8544.49']
      },
      {
        name: 'Printed circuits',
        codes: ['8534.00']
      },
      {
        name: 'Integrated circuits',
        codes: ['8542.31', '8542.32', '8542.39']
      },
      {
        name: 'Transistors (IGBTs)',
        codes: ['8541.29']
      }
    ]
  },
  {
    name: 'Thermal Management',
    icon: ThermometerSun,
    description: 'Battery cooling, HVAC, and heat exchange systems',
    subcategories: [
      {
        name: 'Heat-exchange units',
        codes: ['8419.50']
      },
      {
        name: 'Vehicle air-conditioning / heat-pump HVAC',
        codes: ['8415.20']
      },
      {
        name: 'Radiators & parts',
        codes: ['8708.91']
      }
    ]
  },
  {
    name: 'Chassis & Body',
    icon: Gauge,
    description: 'General EV chassis and body components',
    subcategories: [
      {
        name: 'Brakes & servo-brakes',
        codes: ['8708.30']
      },
      {
        name: 'Gear boxes (reduction gear)',
        codes: ['8708.40']
      },
      {
        name: 'Drive-axles & non-driving axles',
        codes: ['8708.50']
      },
      {
        name: 'Road wheels',
        codes: ['8708.70']
      },
      {
        name: 'Suspension systems',
        codes: ['8708.80']
      },
      {
        name: 'Steering parts, airbags, other',
        codes: ['8708.94', '8708.95', '8708.99']
      }
    ]
  },
  {
    name: 'Magnets for Traction Motors',
    icon: Magnet,
    description: 'Permanent magnets (NdFeB, etc.)',
    subcategories: [
      {
        name: 'Permanent magnets of metal',
        codes: ['8505.11']
      }
    ]
  },
  {
    name: 'ADAS / Sensors',
    icon: Eye,
    description: 'Advanced driver assistance systems and sensors',
    subcategories: [
      {
        name: 'Radar apparatus',
        codes: ['8526.10']
      }
    ]
  },
  {
    name: 'Lighting & Signalling',
    icon: Lightbulb,
    description: 'Lighting and visual signalling equipment',
    subcategories: [
      {
        name: 'Lighting or visual signalling equipment',
        codes: ['8512.20']
      }
    ]
  }
]

export function TariffDatabase() {
  const navigate = useNavigate()
  const [expandedCategories, setExpandedCategories] = useState<Set<string>>(new Set())
  const [searchQuery, setSearchQuery] = useState('')
  const [tariffRates, setTariffRates] = useState<TariffRate[]>([])
  const [filteredRates, setFilteredRates] = useState<TariffRate[]>([])
  const [loading, setLoading] = useState(false)
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null)

  useEffect(() => {
    fetchTariffRates()
  }, [])

  useEffect(() => {
    filterRates()
  }, [searchQuery, selectedCategory, tariffRates])

  const fetchTariffRates = async () => {
    try {
      setLoading(true)
      // Fetch all tariff rates
      const response = await api.get('/tariff-rate/all')
      setTariffRates(response.data)
    } catch (error) {
      console.error('Failed to fetch tariff rates:', error)
    } finally {
      setLoading(false)
    }
  }

  const filterRates = () => {
    let filtered = tariffRates

    // Filter by selected category
    if (selectedCategory) {
      const category = hsCodeCategories.find(c => c.name === selectedCategory)
      if (category) {
        const categoryCodes = category.subcategories.flatMap(sub => sub.codes)
        filtered = filtered.filter(rate => 
          categoryCodes.some(code => rate.hsCode.startsWith(code))
        )
      }
    }

    // Filter by search query
    if (searchQuery) {
      filtered = filtered.filter(rate =>
        rate.hsCode.toLowerCase().includes(searchQuery.toLowerCase()) ||
        rate.description?.toLowerCase().includes(searchQuery.toLowerCase()) ||
        rate.importerIso3.toLowerCase().includes(searchQuery.toLowerCase()) ||
        rate.originIso3?.toLowerCase().includes(searchQuery.toLowerCase())
      )
    }

    setFilteredRates(filtered)
  }

  const toggleCategory = (categoryName: string) => {
    const newExpanded = new Set(expandedCategories)
    if (newExpanded.has(categoryName)) {
      newExpanded.delete(categoryName)
      if (selectedCategory === categoryName) {
        setSelectedCategory(null)
      }
    } else {
      newExpanded.add(categoryName)
    }
    setExpandedCategories(newExpanded)
  }

  const selectCategory = (categoryName: string) => {
    setSelectedCategory(categoryName === selectedCategory ? null : categoryName)
  }

  const transferToCalculator = (rate: TariffRate) => {
    // Store the selected tariff rate in sessionStorage
    sessionStorage.setItem('prefilledTariff', JSON.stringify({
      hsCode: rate.hsCode,
      description: rate.description,
      importerIso3: rate.importerIso3,
      originIso3: rate.originIso3,
      mfnRate: rate.mfnRate,
      preferentialRate: rate.preferentialRate
    }))
    
    // Navigate to calculator
    navigate('/calculator')
  }

  return (
    <div className="flex-1 space-y-6 p-6">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
      >
        <h1 className="text-3xl font-bold tracking-tight">Tariff Database</h1>
        <p className="text-muted-foreground">
          Explore global trade routes and EV component tariff rates
        </p>
      </motion.div>

      {/* Global Trade Routes Map */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.1 }}
      >
        <GlobalTradeRoutes onRouteSelect={() => { const el = document.getElementById("ev-categories"); if (el) { el.classList.remove("route-jump-animate"); el.scrollIntoView({ behavior: "smooth", block: "start" }); requestAnimationFrame(() => { el.classList.add("route-jump-animate"); setTimeout(() => el.classList.remove("route-jump-animate"), 500); }); } }} />
      </motion.div>

      {/* HS Code Categories Section */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.2 }}
      >
        <Card>
          <CardHeader>
            <CardTitle id="ev-categories">EV Component Categories</CardTitle>
            <CardDescription>
              Browse tariff rates by component category
            </CardDescription>
            
            {/* Search Bar */}
            <div className="relative mt-4">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                type="text"
                placeholder="Search by HS code, description, or country..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-10 pr-4 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
          </CardHeader>
          
          <CardContent className="space-y-4">
            {/* Category List */}
            <div className="grid gap-3">
              {hsCodeCategories.map((category) => {
                const Icon = category.icon
                const isExpanded = expandedCategories.has(category.name)
                const isSelected = selectedCategory === category.name
                
                return (
                  <div key={category.name} className="border border-gray-200 dark:border-gray-700 rounded-lg overflow-hidden">
                    {/* Category Header */}
                    <button
                      onClick={() => toggleCategory(category.name)}
                      className="w-full flex items-center justify-between p-4 hover:bg-accent/50 dark:hover:bg-background transition-colors"
                    >
                      <div className="flex items-center gap-3">
                        <div className={`p-2 rounded-lg ${isSelected ? 'bg-blue-100 dark:bg-brand-900/20' : 'bg-gray-100 dark:bg-gray-800'}`}>
                          <Icon className={`w-5 h-5 ${isSelected ? 'text-blue-600 dark:text-brand-300' : 'text-gray-600 dark:text-gray-400'}`} />
                        </div>
                        <div className="text-left">
                          <h3 className="font-semibold text-gray-900 dark:text-white">{category.name}</h3>
                          <p className="text-xs text-gray-500 dark:text-gray-400">{category.description}</p>
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        {category.subcategories.length > 0 && (
                          <span className="text-xs bg-gray-100 dark:bg-gray-800 px-2 py-1 rounded-full text-gray-600 dark:text-gray-400">
                            {category.subcategories.length} subcategories
                          </span>
                        )}
                        {isExpanded ? (
                          <ChevronDown className="w-5 h-5 text-gray-400" />
                        ) : (
                          <ChevronRight className="w-5 h-5 text-gray-400" />
                        )}
                      </div>
                    </button>

                    {/* Subcategories */}
                    {isExpanded && category.subcategories.length > 0 && (
                      <div className="border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-background/50 p-4">
                        <div className="space-y-2">
                          {category.subcategories.map((subcategory) => (
                            <div key={subcategory.name} className="bg-card dark:bg-card rounded-lg p-3 border border-gray-200 dark:border-gray-700">
                              <div className="flex items-center justify-between">
                                <div>
                                  <h4 className="text-sm font-medium text-gray-900 dark:text-white">{subcategory.name}</h4>
                                  <div className="flex flex-wrap gap-1 mt-1">
                                    {subcategory.codes.map((code) => (
                                      <span key={code} className="text-xs bg-blue-100 dark:bg-brand-900/20 text-blue-700 dark:text-brand-300 px-2 py-0.5 rounded">
                                        {code}
                                      </span>
                                    ))}
                                  </div>
                                </div>
                                <Button
                                  size="sm"
                                  variant={isSelected ? "default" : "outline"}
                                  onClick={() => selectCategory(category.name)}
                                >
                                  {isSelected ? 'Selected' : 'View Rates'}
                                </Button>
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                )
              })}
            </div>

            {/* Tariff Rates Table */}
            {(selectedCategory || searchQuery) && (
              <div className="mt-6">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                    {selectedCategory ? `${selectedCategory} - Tariff Rates` : 'Search Results'}
                  </h3>
                  <span className="text-sm text-gray-500 dark:text-gray-400">
                    {filteredRates.length} rates found
                  </span>
                </div>

                {loading ? (
                  <div className="flex items-center justify-center py-12">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600" />
                  </div>
                ) : filteredRates.length === 0 ? (
                  <div className="text-center py-12 text-gray-500 dark:text-gray-400">
                    No tariff rates found
                  </div>
                ) : (
                  <div className="overflow-x-auto rounded-lg border border-gray-200 dark:border-gray-700 bg-card dark:bg-card">
                    <table className="w-full text-sm">
                      <thead className="bg-gray-50 dark:bg-card border-b border-gray-200 dark:border-gray-700">
                        <tr>
                          <th className="px-4 py-3 text-left font-medium text-gray-700 dark:text-gray-300">HS Code</th>
                          <th className="px-4 py-3 text-left font-medium text-gray-700 dark:text-gray-300">Description</th>
                          <th className="px-4 py-3 text-left font-medium text-gray-700 dark:text-gray-300">Route</th>
                          <th className="px-4 py-3 text-left font-medium text-gray-700 dark:text-gray-300">MFN Rate</th>
                          <th className="px-4 py-3 text-left font-medium text-gray-700 dark:text-gray-300">Pref. Rate</th>
                          <th className="px-4 py-3 text-left font-medium text-gray-700 dark:text-gray-300">Action</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-gray-200 dark:divide-gray-700 bg-card dark:bg-card">
                        {filteredRates.slice(0, 50).map((rate) => (
                          <tr key={rate.id} className="hover:bg-gray-50 dark:hover:bg-gray-800">
                            <td className="px-4 py-3 font-mono text-blue-600 dark:text-blue-400">{rate.hsCode}</td>
                            <td className="px-4 py-3 text-gray-900 dark:text-white max-w-xs truncate">{rate.description}</td>
                            <td className="px-4 py-3 text-gray-600 dark:text-gray-400">
                              {rate.originIso3 || 'Any'} → {rate.importerIso3}
                            </td>
                            <td className="px-4 py-3 text-gray-900 dark:text-white">{rate.mfnRate}%</td>
                            <td className="px-4 py-3 text-green-600 dark:text-green-400">{rate.preferentialRate}%</td>
                            <td className="px-4 py-3">
                              <Button
                                size="sm"
                                variant="outline"
                                onClick={() => transferToCalculator(rate)}
                                className="flex items-center gap-1"
                              >
                                <Calculator className="w-3 h-3" />
                                Calculate
                              </Button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            )}
          </CardContent>
        </Card>
      </motion.div>
    </div>
  )
}

