import React from 'react';
import {
  LineChart,
  Line,
  AreaChart,
  Area,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  ComposedChart,
  ScatterChart,
  Scatter,
} from 'recharts';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import { 
  Download, 
  Maximize2, 
  BarChart3, 
  LineChart as LineChartIcon, 
  PieChart as PieChartIcon,
  TrendingUp,
  TrendingDown,
  Minus
} from 'lucide-react';

export interface ChartData {
  [key: string]: any;
}

export interface VisualizationConfig {
  type: 'line' | 'area' | 'bar' | 'pie' | 'composed' | 'scatter' | 'comparison' | 'trend';
  title: string;
  description?: string;
  data: ChartData[];
  xAxis?: string;
  yAxis?: string | string[];
  colors?: string[];
  showLegend?: boolean;
  showGrid?: boolean;
  height?: number;
  width?: string;
  formatters?: {
    [key: string]: (value: any) => string;
  };
  insights?: string[];
  metadata?: {
    source?: string;
    lastUpdated?: string;
    confidence?: number;
  };
}

interface VisualizationEngineProps {
  config: VisualizationConfig;
  className?: string;
  onExport?: (format: 'png' | 'svg' | 'pdf') => void;
  onExpand?: () => void;
}

const CHART_COLORS = [
  'hsl(var(--primary))',
  '#22c55e',
  '#f59e0b',
  '#ef4444',
  '#8b5cf6',
  '#06b6d4',
  '#f97316',
  '#84cc16',
];

const CustomTooltip = ({ active, payload, label, formatters }: any) => {
  if (active && payload && payload.length) {
    return (
      <div className="bg-background border border-border rounded-lg shadow-lg p-3 max-w-xs">
        <p className="font-medium text-foreground mb-2">{label}</p>
        {payload.map((entry: any, index: number) => (
          <div key={index} className="flex items-center space-x-2 text-sm">
            <div 
              className="w-3 h-3 rounded-full" 
              style={{ backgroundColor: entry.color }}
            />
            <span className="text-muted-foreground">{entry.name}:</span>
            <span className="font-medium text-foreground">
              {formatters && formatters[entry.dataKey] 
                ? formatters[entry.dataKey](entry.value)
                : entry.value
              }
            </span>
          </div>
        ))}
      </div>
    );
  }
  return null;
};

const TrendIndicator: React.FC<{ value: number; label: string }> = ({ value, label }) => {
  const isPositive = value > 0;
  const isNeutral = value === 0;
  
  return (
    <div className="flex items-center gap-2">
      {isNeutral ? (
        <Minus size={16} className="text-muted-foreground" />
      ) : isPositive ? (
        <TrendingUp size={16} className="text-green-600" />
      ) : (
        <TrendingDown size={16} className="text-red-600" />
      )}
      <span className={cn(
        "text-sm font-medium",
        isNeutral ? "text-muted-foreground" : isPositive ? "text-green-600" : "text-red-600"
      )}>
        {Math.abs(value).toFixed(1)}% {label}
      </span>
    </div>
  );
};

export const VisualizationEngine: React.FC<VisualizationEngineProps> = ({
  config,
  className,
  onExport,
  onExpand,
}) => {
  const {
    type,
    title,
    description,
    data,
    xAxis = 'name',
    yAxis = 'value',
    colors = CHART_COLORS,
    showLegend = true,
    showGrid = true,
    height = 300,
    formatters,
    insights,
    metadata,
  } = config;

  const renderChart = () => {
    const commonProps = {
      data,
      margin: { top: 5, right: 30, left: 20, bottom: 5 },
    };

    switch (type) {
      case 'line':
        return (
          <LineChart {...commonProps}>
            {showGrid && <CartesianGrid strokeDasharray="3 3" className="opacity-30" />}
            <XAxis 
              dataKey={xAxis} 
              axisLine={false}
              tickLine={false}
              className="text-xs text-muted-foreground"
            />
            <YAxis 
              axisLine={false}
              tickLine={false}
              className="text-xs text-muted-foreground"
              tickFormatter={formatters?.[yAxis as string]}
            />
            <Tooltip content={<CustomTooltip formatters={formatters} />} />
            {showLegend && <Legend />}
            {Array.isArray(yAxis) ? (
              yAxis.map((key, index) => (
                <Line
                  key={key}
                  type="monotone"
                  dataKey={key}
                  stroke={colors[index % colors.length]}
                  strokeWidth={2}
                  dot={{ r: 4 }}
                  activeDot={{ r: 6 }}
                />
              ))
            ) : (
              <Line
                type="monotone"
                dataKey={yAxis}
                stroke={colors[0]}
                strokeWidth={2}
                dot={{ r: 4 }}
                activeDot={{ r: 6 }}
              />
            )}
          </LineChart>
        );

      case 'area':
        return (
          <AreaChart {...commonProps}>
            <defs>
              <linearGradient id="areaGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor={colors[0]} stopOpacity={0.3}/>
                <stop offset="95%" stopColor={colors[0]} stopOpacity={0}/>
              </linearGradient>
            </defs>
            {showGrid && <CartesianGrid strokeDasharray="3 3" className="opacity-30" />}
            <XAxis 
              dataKey={xAxis} 
              axisLine={false}
              tickLine={false}
              className="text-xs text-muted-foreground"
            />
            <YAxis 
              axisLine={false}
              tickLine={false}
              className="text-xs text-muted-foreground"
              tickFormatter={formatters?.[yAxis as string]}
            />
            <Tooltip content={<CustomTooltip formatters={formatters} />} />
            {showLegend && <Legend />}
            <Area
              type="monotone"
              dataKey={yAxis}
              stroke={colors[0]}
              strokeWidth={2}
              fill="url(#areaGradient)"
            />
          </AreaChart>
        );

      case 'bar':
        return (
          <BarChart {...commonProps}>
            {showGrid && <CartesianGrid strokeDasharray="3 3" className="opacity-30" />}
            <XAxis 
              dataKey={xAxis} 
              axisLine={false}
              tickLine={false}
              className="text-xs text-muted-foreground"
            />
            <YAxis 
              axisLine={false}
              tickLine={false}
              className="text-xs text-muted-foreground"
              tickFormatter={formatters?.[yAxis as string]}
            />
            <Tooltip content={<CustomTooltip formatters={formatters} />} />
            {showLegend && <Legend />}
            {Array.isArray(yAxis) ? (
              yAxis.map((key, index) => (
                <Bar
                  key={key}
                  dataKey={key}
                  fill={colors[index % colors.length]}
                  radius={[2, 2, 0, 0]}
                />
              ))
            ) : (
              <Bar
                dataKey={yAxis}
                fill={colors[0]}
                radius={[2, 2, 0, 0]}
              />
            )}
          </BarChart>
        );

      case 'pie':
        return (
          <PieChart>
            <Pie
              data={data}
              cx="50%"
              cy="50%"
              labelLine={false}
              label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
              outerRadius={80}
              fill="#8884d8"
              dataKey={yAxis}
            >
              {data.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={colors[index % colors.length]} />
              ))}
            </Pie>
            <Tooltip content={<CustomTooltip formatters={formatters} />} />
          </PieChart>
        );

      case 'composed':
        return (
          <ComposedChart {...commonProps}>
            {showGrid && <CartesianGrid strokeDasharray="3 3" className="opacity-30" />}
            <XAxis 
              dataKey={xAxis} 
              axisLine={false}
              tickLine={false}
              className="text-xs text-muted-foreground"
            />
            <YAxis 
              axisLine={false}
              tickLine={false}
              className="text-xs text-muted-foreground"
            />
            <Tooltip content={<CustomTooltip formatters={formatters} />} />
            {showLegend && <Legend />}
            <Bar dataKey="volume" fill={colors[1]} />
            <Line type="monotone" dataKey="rate" stroke={colors[0]} strokeWidth={2} />
          </ComposedChart>
        );

      default:
        return <div className="text-center text-muted-foreground">Unsupported chart type</div>;
    }
  };

  const getChartIcon = () => {
    switch (type) {
      case 'line':
      case 'area':
        return <LineChartIcon size={16} />;
      case 'bar':
      case 'composed':
        return <BarChart3 size={16} />;
      case 'pie':
        return <PieChartIcon size={16} />;
      default:
        return <BarChart3 size={16} />;
    }
  };

  return (
    <Card className={cn('w-full', className)}>
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            {getChartIcon()}
            <CardTitle className="text-lg">{title}</CardTitle>
            {metadata?.confidence && (
              <Badge variant={metadata.confidence > 0.8 ? "default" : "secondary"}>
                {Math.round(metadata.confidence * 100)}% confident
              </Badge>
            )}
          </div>
          <div className="flex items-center gap-1">
            {onExport && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => onExport('png')}
                className="h-8 w-8 p-0"
              >
                <Download size={14} />
              </Button>
            )}
            {onExpand && (
              <Button
                variant="ghost"
                size="sm"
                onClick={onExpand}
                className="h-8 w-8 p-0"
              >
                <Maximize2 size={14} />
              </Button>
            )}
          </div>
        </div>
        {description && (
          <p className="text-sm text-muted-foreground">{description}</p>
        )}
      </CardHeader>
      
      <CardContent>
        <div style={{ width: '100%', height }}>
          <ResponsiveContainer>
            {renderChart()}
          </ResponsiveContainer>
        </div>

        {/* Insights section */}
        {insights && insights.length > 0 && (
          <div className="mt-4 space-y-2">
            <h4 className="text-sm font-medium text-foreground">Key Insights</h4>
            <ul className="space-y-1">
              {insights.map((insight, index) => (
                <li key={index} className="text-sm text-muted-foreground flex items-start gap-2">
                  <span className="w-1.5 h-1.5 rounded-full bg-primary mt-2 flex-shrink-0" />
                  {insight}
                </li>
              ))}
            </ul>
          </div>
        )}

        {/* Metadata */}
        {metadata && (
          <div className="mt-4 pt-3 border-t border-border">
            <div className="flex items-center justify-between text-xs text-muted-foreground">
              {metadata.source && (
                <span>Source: {metadata.source}</span>
              )}
              {metadata.lastUpdated && (
                <span>Updated: {metadata.lastUpdated}</span>
              )}
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
};

// Utility functions for creating common chart configurations
export const createTariffTrendChart = (data: any[]): VisualizationConfig => ({
  type: 'line',
  title: 'Tariff Rate Trends',
  description: 'Historical tariff rates over time',
  data,
  xAxis: 'period',
  yAxis: 'rate',
  formatters: {
    rate: (value: number) => `${value.toFixed(1)}%`,
  },
  insights: [
    'Tariff rates have shown volatility over the past year',
    'Consider timing imports during lower rate periods',
  ],
});

export const createTradeVolumeChart = (data: any[]): VisualizationConfig => ({
  type: 'bar',
  title: 'Trade Volume Analysis',
  description: 'Import/export volumes by country',
  data,
  xAxis: 'country',
  yAxis: 'volume',
  formatters: {
    volume: (value: number) => `$${(value / 1000000).toFixed(1)}M`,
  },
});

export const createCostComparisonChart = (data: any[]): VisualizationConfig => ({
  type: 'composed',
  title: 'Cost Comparison Analysis',
  description: 'Total landed costs across different routes',
  data,
  xAxis: 'route',
  yAxis: ['cost', 'savings'],
  colors: ['#0ea5e9', '#22c55e'],
  formatters: {
    cost: (value: number) => `$${value.toLocaleString()}`,
    savings: (value: number) => `${value.toFixed(1)}%`,
  },
});

VisualizationEngine.displayName = 'VisualizationEngine';