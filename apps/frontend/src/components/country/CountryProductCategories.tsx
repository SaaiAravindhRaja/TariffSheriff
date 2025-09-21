import React from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Treemap, Cell } from 'recharts';
import { Package, TrendingUp, Zap } from 'lucide-react';
import { formatCurrency } from '@/lib/utils';

interface CountryProductCategoriesProps {
  countryCode: string;
}

// Mock product categories data
const generateProductData = (_countryCode: string) => [
  { 
    category: 'Electronics', 
    volume: 1200000000, 
    share: 32.4, 
    change: '+15.2%', 
    tariffRate: 12.5,
    topProducts: ['Smartphones', 'Laptops', 'Semiconductors']
  },
  { 
    category: 'Automotive', 
    volume: 890000000, 
    share: 24.1, 
    change: '+8.7%', 
    tariffRate: 8.3,
    topProducts: ['Electric Vehicles', 'Auto Parts', 'Engines']
  },
  { 
    category: 'Machinery', 
    volume: 650000000, 
    share: 17.6, 
    change: '+12.1%', 
    tariffRate: 6.2,
    topProducts: ['Industrial Equipment', 'Construction Machinery', 'Tools']
  },
  { 
    category: 'Chemicals', 
    volume: 420000000, 
    share: 11.4, 
    change: '+5.8%', 
    tariffRate: 9.8,
    topProducts: ['Pharmaceuticals', 'Plastics', 'Fertilizers']
  },
  { 
    category: 'Textiles', 
    volume: 320000000, 
    share: 8.7, 
    change: '+3.2%', 
    tariffRate: 15.7,
    topProducts: ['Clothing', 'Fabrics', 'Footwear']
  },
  { 
    category: 'Food & Beverages', 
    volume: 210000000, 
    share: 5.7, 
    change: '+7.4%', 
    tariffRate: 18.4,
    topProducts: ['Processed Foods', 'Beverages', 'Agricultural Products']
  }
];

const treemapData = [
  { name: 'Electronics', size: 32.4, color: '#3b82f6' },
  { name: 'Automotive', size: 24.1, color: '#ef4444' },
  { name: 'Machinery', size: 17.6, color: '#f59e0b' },
  { name: 'Chemicals', size: 11.4, color: '#10b981' },
  { name: 'Textiles', size: 8.7, color: '#8b5cf6' },
  { name: 'Food & Beverages', size: 5.7, color: '#f97316' }
];

export const CountryProductCategories: React.FC<CountryProductCategoriesProps> = ({ countryCode }) => {
  const productData = generateProductData(countryCode);
  const totalVolume = productData.reduce((sum, product) => sum + product.volume, 0);
  const avgGrowth = (productData.reduce((sum, p) => sum + parseFloat(p.change.replace('%', '').replace('+', '')), 0) / productData.length).toFixed(1)

  return (
    <div className="space-y-6">
      {/* Product Categories Overview */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {productData.map((product) => (
          <Card key={product.category} className="card-hover">
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-medium flex items-center justify-between">
                <span className="flex items-center">
                  <Package className="w-4 h-4 mr-2" />
                  {product.category}
                </span>
                <Badge variant="secondary" className="text-xs">
                  {product.share}%
                </Badge>
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-2">
                <div className="text-2xl font-bold">
                  {formatCurrency(product.volume, 'USD')}
                </div>
                <div className="flex items-center justify-between text-sm">
                  <span className={`flex items-center ${
                    product.change.startsWith('+') ? 'text-green-600' : 'text-red-600'
                  }`}>
                    <TrendingUp className="w-3 h-3 mr-1" />
                    {product.change}
                  </span>
                  <span className="text-muted-foreground">
                    {product.tariffRate}% tariff
                  </span>
                </div>
                <div className="pt-2 border-t">
                  <div className="text-xs text-muted-foreground mb-1">Top Products:</div>
                  <div className="flex flex-wrap gap-1">
                    {product.topProducts.slice(0, 2).map((item, idx) => (
                      <Badge key={idx} variant="outline" className="text-xs">
                        {item}
                      </Badge>
                    ))}
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Detailed Analytics */}
      <div className="grid gap-6 lg:grid-cols-2">
        {/* Volume Chart */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center">
              <BarChart className="w-5 h-5 mr-2" />
              Trade Volume by Category
            </CardTitle>
            <CardDescription>
              Export and import volumes across product categories
            </CardDescription>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={productData} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" className="opacity-30" />
                <XAxis 
                  dataKey="category" 
                  className="text-xs"
                  tick={{ fontSize: 10 }}
                  angle={-45}
                  textAnchor="end"
                  height={80}
                />
                <YAxis 
                  className="text-xs"
                  tick={{ fontSize: 12 }}
                  tickFormatter={(value) => `$${(value / 1000000000).toFixed(1)}B`}
                />
                <Tooltip 
                  formatter={(value: number) => [formatCurrency(value, 'USD'), 'Trade Volume']}
                  contentStyle={{
                    backgroundColor: 'hsl(var(--card))',
                    border: '1px solid hsl(var(--border))',
                    borderRadius: '8px'
                  }}
                />
                <Bar dataKey="volume" fill="#3b82f6" />
              </BarChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        {/* Market Share Treemap */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center">
              <Zap className="w-5 h-5 mr-2" />
              Market Share Visualization
            </CardTitle>
            <CardDescription>
              Proportional representation of trade categories
            </CardDescription>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={300}>
              <Treemap
                data={treemapData}
                dataKey="size"
                aspectRatio={4/3}
                stroke="#fff"
                fill="#8884d8"
              >
                {treemapData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={entry.color} />
                ))}
              </Treemap>
            </ResponsiveContainer>
            
            <div className="mt-4 grid grid-cols-2 gap-2">
              {treemapData.map((item, index) => (
                <div key={index} className="flex items-center space-x-2 text-sm">
                  <div 
                    className="w-3 h-3 rounded-full" 
                    style={{ backgroundColor: item.color }}
                  />
                  <span className="text-muted-foreground">{item.name}</span>
                  <span className="text-xs">({item.size}%)</span>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Summary Statistics */}
      <Card>
        <CardHeader>
          <CardTitle>Category Performance Summary</CardTitle>
          <CardDescription>
            Key insights across all product categories
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-4">
            <div className="text-center p-4 bg-blue-50 dark:bg-blue-900/20 rounded-lg">
              <div className="text-2xl font-bold text-blue-600 dark:text-blue-400">
                {formatCurrency(totalVolume, 'USD')}
              </div>
              <div className="text-sm text-muted-foreground">Total Volume</div>
            </div>
            <div className="text-center p-4 bg-green-50 dark:bg-green-900/20 rounded-lg">
              <div className="text-2xl font-bold text-green-600 dark:text-green-400">
                {productData.length}
              </div>
              <div className="text-sm text-muted-foreground">Categories</div>
            </div>
            <div className="text-center p-4 bg-purple-50 dark:bg-purple-900/20 rounded-lg">
              <div className="text-2xl font-bold text-purple-600 dark:text-purple-400">
                {(productData.reduce((sum, p) => sum + p.tariffRate, 0) / productData.length).toFixed(1)}%
              </div>
              <div className="text-sm text-muted-foreground">Avg Tariff</div>
            </div>
            <div className="text-center p-4 bg-orange-50 dark:bg-orange-900/20 rounded-lg">
                <div className="text-2xl font-bold text-orange-600 dark:text-orange-400">
                  {`+${avgGrowth}%`}
                </div>
              <div className="text-sm text-muted-foreground">Avg Growth</div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};