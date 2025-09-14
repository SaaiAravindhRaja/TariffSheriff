
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell, PieChart, Pie } from 'recharts'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { CheckCircle, AlertTriangle, XCircle, Clock } from 'lucide-react'

/* eslint-disable @typescript-eslint/no-explicit-any, @typescript-eslint/no-unused-vars */

const complianceData = [
  { 
    category: 'Documentation', 
    compliant: 94.2, 
    nonCompliant: 5.8, 
    issues: 45,
    avgResolutionTime: 2.3,
    color: '#22c55e'
  },
  { 
    category: 'Tariff Classification', 
    compliant: 91.5, 
    nonCompliant: 8.5, 
    issues: 68,
    avgResolutionTime: 3.1,
    color: '#f59e0b'
  },
  { 
    category: 'Valuation', 
    compliant: 96.8, 
    nonCompliant: 3.2, 
    issues: 25,
    avgResolutionTime: 1.8,
    color: '#0ea5e9'
  },
  { 
    category: 'Origin Verification', 
    compliant: 89.3, 
    nonCompliant: 10.7, 
    issues: 82,
    avgResolutionTime: 4.2,
    color: '#ef4444'
  },
  { 
    category: 'Licensing', 
    compliant: 97.1, 
    nonCompliant: 2.9, 
    issues: 18,
    avgResolutionTime: 1.5,
    color: '#8b5cf6'
  }
]

const issueTypes = [
  { name: 'Missing Documents', value: 35, color: '#ef4444' },
  { name: 'Incorrect Classification', value: 28, color: '#f59e0b' },
  { name: 'Valuation Disputes', value: 18, color: '#0ea5e9' },
  { name: 'Origin Issues', value: 12, color: '#8b5cf6' },
  { name: 'Other', value: 7, color: '#6b7280' }
]

export function ComplianceMetricsChart() {
  const overallCompliance = complianceData.reduce((sum, item) => sum + item.compliant, 0) / complianceData.length
  const totalIssues = complianceData.reduce((sum, item) => sum + item.issues, 0)
  const avgResolutionTime = complianceData.reduce((sum, item) => sum + item.avgResolutionTime, 0) / complianceData.length

  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload
      return (
        <div className="bg-background border border-border rounded-lg shadow-lg p-3">
          <p className="font-medium text-foreground mb-2">{data.category}</p>
          <div className="space-y-1 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Compliance Rate:</span>
              <span className="font-medium text-success-600">{data.compliant.toFixed(1)}%</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Non-Compliance:</span>
              <span className="font-medium text-danger-600">{data.nonCompliant.toFixed(1)}%</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Issues:</span>
              <span className="font-medium">{data.issues}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Avg Resolution:</span>
              <span className="font-medium">{data.avgResolutionTime.toFixed(1)} days</span>
            </div>
          </div>
        </div>
      )
    }
    return null
  }

  const PieTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0]
      return (
        <div className="bg-background border border-border rounded-lg shadow-lg p-3">
          <p className="font-medium text-foreground mb-1">{data.name}</p>
          <div className="text-sm">
            <span className="text-muted-foreground">Issues: </span>
            <span className="font-medium">{data.value}%</span>
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
            <CardTitle>Compliance Metrics</CardTitle>
            <CardDescription>
              Compliance rates and issue analysis by category
            </CardDescription>
          </div>
          <Badge 
            variant={overallCompliance > 95 ? 'success' : overallCompliance > 90 ? 'warning' : 'destructive'}
            className="text-xs"
          >
            {overallCompliance.toFixed(1)}% Overall
          </Badge>
        </div>
      </CardHeader>
      <CardContent>
        <div className="grid gap-6 lg:grid-cols-2">
          {/* Compliance Rates Bar Chart */}
          <div className="h-64">
            <h4 className="font-medium text-sm mb-3">Compliance by Category</h4>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={complianceData} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" className="opacity-30" />
                <XAxis 
                  dataKey="category" 
                  axisLine={false}
                  tickLine={false}
                  className="text-xs text-muted-foreground"
                  angle={-45}
                  textAnchor="end"
                  height={60}
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
                <Bar dataKey="compliant" radius={[4, 4, 0, 0]}>
                  {complianceData.map((entry, index) => (
                    <Cell 
                      key={`cell-${index}`} 
                      fill={entry.compliant > 95 ? '#22c55e' : entry.compliant > 90 ? '#f59e0b' : '#ef4444'} 
                    />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>

          {/* Issue Types Pie Chart */}
          <div className="h-64">
            <h4 className="font-medium text-sm mb-3">Issue Distribution</h4>
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={issueTypes}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                  outerRadius={60}
                  fill="#8884d8"
                  dataKey="value"
                >
                  {issueTypes.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip content={<PieTooltip />} />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Compliance Summary Cards */}
        <div className="mt-6 grid grid-cols-4 gap-4">
          <div className="text-center p-3 bg-success-50 dark:bg-success-900/20 rounded-lg border border-success-200 dark:border-success-800">
            <CheckCircle className="w-6 h-6 text-success-600 mx-auto mb-2" />
            <div className="font-medium text-lg text-success-700 dark:text-success-300">
              {overallCompliance.toFixed(1)}%
            </div>
            <div className="text-xs text-success-600 dark:text-success-400">
              Overall Compliance
            </div>
          </div>
          
          <div className="text-center p-3 bg-warning-50 dark:bg-warning-900/20 rounded-lg border border-warning-200 dark:border-warning-800">
            <AlertTriangle className="w-6 h-6 text-warning-600 mx-auto mb-2" />
            <div className="font-medium text-lg text-warning-700 dark:text-warning-300">
              {totalIssues}
            </div>
            <div className="text-xs text-warning-600 dark:text-warning-400">
              Total Issues
            </div>
          </div>
          
          <div className="text-center p-3 bg-brand-50 dark:bg-brand-900/20 rounded-lg border border-brand-200 dark:border-brand-800">
            <Clock className="w-6 h-6 text-brand-600 mx-auto mb-2" />
            <div className="font-medium text-lg text-brand-700 dark:text-brand-300">
              {avgResolutionTime.toFixed(1)}d
            </div>
            <div className="text-xs text-brand-600 dark:text-brand-400">
              Avg Resolution
            </div>
          </div>
          
          <div className="text-center p-3 bg-danger-50 dark:bg-danger-900/20 rounded-lg border border-danger-200 dark:border-danger-800">
            <XCircle className="w-6 h-6 text-danger-600 mx-auto mb-2" />
            <div className="font-medium text-lg text-danger-700 dark:text-danger-300">
              {(100 - overallCompliance).toFixed(1)}%
            </div>
            <div className="text-xs text-danger-600 dark:text-danger-400">
              Non-Compliance
            </div>
          </div>
        </div>

        {/* Action Items */}
        <div className="mt-4 p-3 bg-muted/50 rounded-lg">
          <h4 className="font-medium text-sm mb-2">Recommended Actions</h4>
          <div className="text-xs text-muted-foreground space-y-1">
            <div>• Focus on Origin Verification improvements (89.3% compliance)</div>
            <div>• Reduce Tariff Classification errors (68 issues this month)</div>
            <div>• Implement automated documentation checks</div>
            <div>• Provide additional training for high-risk categories</div>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}