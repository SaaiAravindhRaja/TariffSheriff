import React, { useState, useEffect } from 'react'
import { ComposableMap, Geographies, Geography, Line, Marker } from 'react-simple-maps'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Globe, Search, X } from 'lucide-react'
import api from '@/services/api'

const geoUrl = "https://cdn.jsdelivr.net/npm/world-atlas@2/countries-110m.json"

interface TradeRoute {
  importer: string
  origin: string
  importerCoords: [number, number]
  originCoords: [number, number]
  count: number
}

interface RouteInfo {
  importer: string
  origin: string
  count: number
}

// Country ISO3 to name mapping
const countryNames: Record<string, string> = {
  'USA': 'United States',
  'CHN': 'China',
  'DEU': 'Germany',
  'JPN': 'Japan',
  'GBR': 'United Kingdom',
  'FRA': 'France',
  'IND': 'India',
  'ITA': 'Italy',
  'BRA': 'Brazil',
  'CAN': 'Canada',
  'KOR': 'South Korea',
  'MEX': 'Mexico',
  'ESP': 'Spain',
  'AUS': 'Australia',
  'NLD': 'Netherlands',
  'SGP': 'Singapore',
  'CHE': 'Switzerland',
  'SWE': 'Sweden',
  'POL': 'Poland',
  'BEL': 'Belgium',
}

// Country ISO3 to coordinates mapping (major trade hubs)
const countryCoordinates: Record<string, [number, number]> = {
  'USA': [-95, 38],
  'CHN': [105, 35],
  'DEU': [10, 51],
  'JPN': [138, 36],
  'GBR': [-2, 54],
  'FRA': [2, 46],
  'IND': [78, 22],
  'ITA': [12, 42],
  'BRA': [-47, -15],
  'CAN': [-106, 56],
  'KOR': [127, 37],
  'MEX': [-102, 23],
  'ESP': [-4, 40],
  'AUS': [133, -27],
  'NLD': [5, 52],
  'SGP': [103, 1],
  'CHE': [8, 47],
  'SWE': [15, 62],
  'POL': [19, 52],
  'BEL': [4, 50],
}

export function GlobalTradeRoutes() {
  const [routes, setRoutes] = useState<TradeRoute[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedRoute, setSelectedRoute] = useState<RouteInfo | null>(null)
  const [hoveredRoute, setHoveredRoute] = useState<RouteInfo | null>(null)
  const [selectedCountry, setSelectedCountry] = useState<string | null>(null)
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState<string[]>([])
  const [showSearchResults, setShowSearchResults] = useState(false)

  useEffect(() => {
    fetchTradeRoutes()
  }, [])

  const fetchTradeRoutes = async () => {
    try {
      setLoading(true)
      // Fetch distinct trade routes from tariff_rate table
      const response = await api.get('/tariff-rate/routes')
      
      // Map to coordinates
      const routesWithCoords = response.data
        .map((route: any) => {
          const importerCoords = countryCoordinates[route.importerIso3]
          const originCoords = route.originIso3 ? countryCoordinates[route.originIso3] : null
          
          if (importerCoords && originCoords) {
            return {
              importer: route.importerIso3,
              origin: route.originIso3,
              importerCoords,
              originCoords,
              count: route.count || 1
            }
          }
          return null
        })
        .filter(Boolean)
        .slice(0, 50) // Limit to top 50 routes to avoid clutter
      
      setRoutes(routesWithCoords)
    } catch (error) {
      console.error('Failed to fetch trade routes:', error)
    } finally {
      setLoading(false)
    }
  }

  // Filter routes based on selected country
  const filteredRoutes = selectedCountry 
    ? routes.filter(route => route.importer === selectedCountry || route.origin === selectedCountry)
    : routes

  // Get country name helper
  const getCountryName = (iso3: string) => countryNames[iso3] || iso3

  // Check if a country has routes
  const hasRoutes = (countryCode: string) => {
    return routes.some(route => route.importer === countryCode || route.origin === countryCode)
  }

  // Get all countries with routes for search
  const countriesWithRoutes = Array.from(new Set(routes.flatMap(r => [r.importer, r.origin])))

  // Handle search
  const handleSearch = (query: string) => {
    setSearchQuery(query)
    if (query.length > 0) {
      const results = countriesWithRoutes.filter(countryCode => {
        const name = getCountryName(countryCode).toLowerCase()
        return name.includes(query.toLowerCase()) || countryCode.toLowerCase().includes(query.toLowerCase())
      })
      setSearchResults(results)
      setShowSearchResults(true)
    } else {
      setSearchResults([])
      setShowSearchResults(false)
    }
  }

  // Select country from search
  const selectCountryFromSearch = (countryCode: string) => {
    setSelectedCountry(countryCode)
    setSearchQuery(getCountryName(countryCode))
    setShowSearchResults(false)
  }

  // Clear search
  const clearSearch = () => {
    setSearchQuery('')
    setSearchResults([])
    setShowSearchResults(false)
    setSelectedCountry(null)
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between gap-4">
          <div className="flex items-center space-x-2">
            <div className="p-2 bg-blue-100 dark:bg-blue-900/20 rounded-lg">
              <Globe className="w-5 h-5 text-blue-600" />
            </div>
            <div>
              <CardTitle>Global Trade Routes</CardTitle>
              <CardDescription>
                Active tariff routes available in database
              </CardDescription>
            </div>
          </div>
          
          {/* Search Bar */}
          <div className="relative w-80">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                type="text"
                placeholder="Search country..."
                value={searchQuery}
                onChange={(e) => handleSearch(e.target.value)}
                onFocus={() => searchQuery && setShowSearchResults(true)}
                className="w-full pl-10 pr-10 py-2 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
              {searchQuery && (
                <button
                  onClick={clearSearch}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
                >
                  <X className="w-4 h-4" />
                </button>
              )}
            </div>
            
            {/* Search Results Dropdown */}
            {showSearchResults && searchResults.length > 0 && (
              <div className="absolute top-full mt-1 w-full bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg max-h-60 overflow-y-auto z-10">
                {searchResults.map((countryCode) => {
                  const countryRouteCount = routes.filter(r => r.importer === countryCode || r.origin === countryCode).length
                  return (
                    <button
                      key={countryCode}
                      onClick={() => selectCountryFromSearch(countryCode)}
                      className="w-full px-4 py-2 text-left text-sm hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors border-b border-gray-100 dark:border-gray-700 last:border-b-0"
                    >
                      <div className="font-medium text-gray-900 dark:text-white">
                        {getCountryName(countryCode)}
                      </div>
                      <div className="text-xs text-gray-500 dark:text-gray-400">
                        {countryRouteCount} routes
                      </div>
                    </button>
                  )
                })}
              </div>
            )}
            
            {showSearchResults && searchResults.length === 0 && searchQuery && (
              <div className="absolute top-full mt-1 w-full bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg p-4 z-10">
                <p className="text-sm text-gray-500 dark:text-gray-400">No countries found</p>
              </div>
            )}
          </div>
        </div>
      </CardHeader>
      <CardContent className="p-0">
        {loading ? (
          <div className="flex h-[500px] items-center justify-center text-sm text-muted-foreground">
            Loading trade routes...
          </div>
        ) : routes.length === 0 ? (
          <div className="flex h-[500px] items-center justify-center rounded-md border border-dashed text-sm text-muted-foreground">
            No trade routes data available
          </div>
        ) : (
          <div className="w-full h-[500px] relative bg-gray-900 rounded-lg overflow-hidden">
            <ComposableMap
              projection="geoMercator"
              projectionConfig={{
                scale: 160,
                center: [0, 20],
              }}
              className="w-full h-full"
            >
              <Geographies geography={geoUrl}>
                {({ geographies }: { geographies: any[] }) =>
                  geographies.map((geo: any) => {
                    const countryCode = geo.properties.ISO_A3
                    const isActive = hasRoutes(countryCode)
                    const isSelected = selectedCountry === countryCode
                    
                    return (
                      <Geography
                        key={geo.rsmKey}
                        geography={geo}
                        fill="currentColor"
                        className={`
                          outline-none transition-all
                          ${isActive ? 'cursor-pointer' : 'cursor-default'}
                          ${isSelected 
                            ? 'text-blue-500 dark:text-blue-400' 
                            : isActive 
                              ? 'text-gray-300 dark:text-gray-600 hover:text-blue-400 dark:hover:text-blue-500' 
                              : 'text-gray-200 dark:text-gray-700'
                          }
                        `}
                        stroke="currentColor"
                        strokeWidth={0.5}
                        style={{
                          default: { outline: 'none' },
                          hover: { outline: 'none' },
                          pressed: { outline: 'none' },
                        }}
                        onClick={() => {
                          if (isActive) {
                            setSelectedCountry(isSelected ? null : countryCode)
                            setSelectedRoute(null)
                          }
                        }}
                      />
                    )
                  })
                }
              </Geographies>

              {/* Draw trade route lines */}
              {filteredRoutes.map((route, idx) => (
                <Line
                  key={`route-${idx}`}
                  from={route.originCoords}
                  to={route.importerCoords}
                  stroke="#3b82f6"
                  strokeWidth={hoveredRoute?.importer === route.importer && hoveredRoute?.origin === route.origin ? 2 : 1}
                  strokeLinecap="round"
                  className="opacity-60 hover:opacity-100 transition-all cursor-pointer"
                  onClick={() => setSelectedRoute({
                    importer: route.importer,
                    origin: route.origin,
                    count: route.count
                  })}
                  onMouseEnter={() => setHoveredRoute({
                    importer: route.importer,
                    origin: route.origin,
                    count: route.count
                  })}
                  onMouseLeave={() => setHoveredRoute(null)}
                />
              ))}

              {/* Draw markers for major trade hubs */}
              {Array.from(new Set(filteredRoutes.flatMap(r => [r.importer, r.origin]))).map((country) => {
                const coords = countryCoordinates[country]
                if (!coords) return null
                
                return (
                  <Marker key={country} coordinates={coords}>
                    <circle
                      r={3}
                      fill="#3b82f6"
                      className="animate-pulse"
                      stroke="#fff"
                      strokeWidth={1}
                    />
                  </Marker>
                )
              })}
            </ComposableMap>
            
            {/* Country selection panel */}
            {selectedCountry && !selectedRoute && (
              <div className="absolute top-4 left-4 bg-white dark:bg-gray-800 px-4 py-3 rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 max-w-sm">
                <div className="flex items-center justify-between mb-2">
                  <h4 className="font-semibold text-sm text-gray-900 dark:text-white">
                    {getCountryName(selectedCountry)}
                  </h4>
                  <button
                    onClick={() => setSelectedCountry(null)}
                    className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </button>
                </div>
                <div className="space-y-2 text-sm">
                  <div className="flex items-center gap-2">
                    <span className="text-gray-600 dark:text-gray-400">Active Routes:</span>
                    <span className="font-semibold text-blue-600 dark:text-blue-400">
                      {filteredRoutes.length}
                    </span>
                  </div>
                  <div className="pt-2 border-t border-gray-200 dark:border-gray-700">
                    <p className="text-xs text-gray-500 dark:text-gray-400">
                      Click a route to see details, or click the country again to deselect
                    </p>
                  </div>
                </div>
              </div>
            )}
            
            {/* Route information panel */}
            {selectedRoute && (
              <div className="absolute top-4 left-4 bg-white dark:bg-gray-800 px-4 py-3 rounded-lg shadow-lg border border-gray-200 dark:border-gray-700 max-w-xs">
                <div className="flex items-center justify-between mb-2">
                  <h4 className="font-semibold text-sm text-gray-900 dark:text-white">
                    Trade Route Details
                  </h4>
                  <button
                    onClick={() => setSelectedRoute(null)}
                    className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </button>
                </div>
                <div className="space-y-2 text-sm">
                  <div className="flex items-center gap-2">
                    <span className="text-gray-600 dark:text-gray-400">From:</span>
                    <span className="font-medium text-gray-900 dark:text-white">
                      {countryNames[selectedRoute.origin] || selectedRoute.origin}
                    </span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-gray-600 dark:text-gray-400">To:</span>
                    <span className="font-medium text-gray-900 dark:text-white">
                      {countryNames[selectedRoute.importer] || selectedRoute.importer}
                    </span>
                  </div>
                  <div className="flex items-center gap-2 pt-2 border-t border-gray-200 dark:border-gray-700">
                    <span className="text-gray-600 dark:text-gray-400">Tariff Rates:</span>
                    <span className="font-semibold text-blue-600 dark:text-blue-400">
                      {selectedRoute.count}
                    </span>
                  </div>
                </div>
              </div>
            )}
            
            {/* Hover tooltip */}
            {hoveredRoute && !selectedRoute && (
              <div className="absolute top-4 left-4 bg-gray-900 dark:bg-gray-700 text-white px-3 py-2 rounded-lg shadow-lg text-xs pointer-events-none">
                <div className="font-medium">
                  {countryNames[hoveredRoute.origin] || hoveredRoute.origin} → {countryNames[hoveredRoute.importer] || hoveredRoute.importer}
                </div>
                <div className="text-gray-300 dark:text-gray-400">
                  {hoveredRoute.count} tariff rates
                </div>
              </div>
            )}
            
            <div className="absolute bottom-4 right-4 bg-white dark:bg-gray-800 px-3 py-2 rounded-lg shadow-md border text-xs">
              {selectedCountry ? (
                <div>
                  <p className="font-medium text-blue-600 dark:text-blue-400">
                    {filteredRoutes.length} routes for {getCountryName(selectedCountry)}
                  </p>
                  <p className="text-gray-500 dark:text-gray-400 text-[10px] mt-1">
                    {routes.length} total routes available
                  </p>
                </div>
              ) : (
                <p className="font-medium">{routes.length} active trade routes</p>
              )}
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  )
}
