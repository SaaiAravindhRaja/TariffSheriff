import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
// import { Slider } from '@/components/ui/slider';
import { cn } from '@/lib/utils';
import { 
  Play, 
  RotateCcw, 
  TrendingUp, 
  TrendingDown,
  Calculator,
  Target,
  AlertTriangle,
  CheckCircle,
  Settings,
  BarChart3
} from 'lucide-react';

export interface ScenarioVariable {
  key: string;
  label: string;
  type: 'slider' | 'select' | 'toggle';
  min?: number;
  max?: number;
  step?: number;
  defaultValue: any;
  currentValue: any;
  unit?: string;
  description?: string;
  options?: { value: any; label: string }[];
}

export interface ScenarioResult {
  key: string;
  label: string;
  value: number;
  unit: string;
  change?: number; // percentage change from baseline
  status: 'positive' | 'negative' | 'neutral';
  description?: string;
}

export interface ScenarioConfig {
  title: string;
  description?: string;
  variables: ScenarioVariable[];
  baselineResults: ScenarioResult[];
  calculator: (variables: Record<string, any>) => ScenarioResult[];
  insights?: {
    positive: string[];
    negative: string[];
    neutral: string[];
  };
}

interface ScenarioModelingProps {
  config: ScenarioConfig;
  className?: string;
  onScenarioChange?: (variables: Record<string, any>, results: ScenarioResult[]) => void;
}



export const ScenarioModeling: React.FC<ScenarioModelingProps> = ({
  config,
  className,
  onScenarioChange,
}) => {
  const [variables, setVariables] = useState<Record<string, any>>(() => {
    const initial: Record<string, any> = {};
    config.variables.forEach(variable => {
      initial[variable.key] = variable.defaultValue;
    });
    return initial;
  });

  const [results, setResults] = useState<ScenarioResult[]>(config.baselineResults);
  const [isRunning, setIsRunning] = useState(false);

  const runScenario = () => {
    setIsRunning(true);
    
    // Simulate calculation delay
    setTimeout(() => {
      const newResults = config.calculator(variables);
      setResults(newResults);
      setIsRunning(false);
      
      if (onScenarioChange) {
        onScenarioChange(variables, newResults);
      }
    }, 500);
  };

  const resetToBaseline = () => {
    const resetVariables: Record<string, any> = {};
    config.variables.forEach(variable => {
      resetVariables[variable.key] = variable.defaultValue;
    });
    setVariables(resetVariables);
    setResults(config.baselineResults);
  };

  const updateVariable = (key: string, value: any) => {
    setVariables(prev => ({ ...prev, [key]: value }));
  };

  const hasChanges = () => {
    return config.variables.some(variable => 
      variables[variable.key] !== variable.defaultValue
    );
  };

  const getResultIcon = (status: string) => {
    switch (status) {
      case 'positive':
        return <TrendingUp size={16} className="text-green-600" />;
      case 'negative':
        return <TrendingDown size={16} className="text-red-600" />;
      default:
        return <Target size={16} className="text-muted-foreground" />;
    }
  };

  const getResultColor = (status: string) => {
    switch (status) {
      case 'positive':
        return 'text-green-600';
      case 'negative':
        return 'text-red-600';
      default:
        return 'text-muted-foreground';
    }
  };

  const renderVariableControl = (variable: ScenarioVariable) => {
    switch (variable.type) {
      case 'slider':
        return (
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <label className="text-sm font-medium">{variable.label}</label>
              <span className="text-sm text-muted-foreground">
                {variables[variable.key]}{variable.unit}
              </span>
            </div>
            <input
              type="range"
              min={variable.min}
              max={variable.max}
              step={variable.step}
              value={variables[variable.key]}
              onChange={(e) => updateVariable(variable.key, Number(e.target.value))}
              className="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer"
            />
            {variable.description && (
              <p className="text-xs text-muted-foreground">{variable.description}</p>
            )}
          </div>
        );

      case 'select':
        return (
          <div className="space-y-2">
            <label className="text-sm font-medium">{variable.label}</label>
            <select
              value={variables[variable.key]}
              onChange={(e) => updateVariable(variable.key, e.target.value)}
              className="w-full px-3 py-2 text-sm border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary"
            >
              {variable.options?.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            {variable.description && (
              <p className="text-xs text-muted-foreground">{variable.description}</p>
            )}
          </div>
        );

      case 'toggle':
        return (
          <div className="flex items-center justify-between">
            <div>
              <label className="text-sm font-medium">{variable.label}</label>
              {variable.description && (
                <p className="text-xs text-muted-foreground">{variable.description}</p>
              )}
            </div>
            <button
              onClick={() => updateVariable(variable.key, !variables[variable.key])}
              className={cn(
                "relative inline-flex h-6 w-11 items-center rounded-full transition-colors",
                variables[variable.key] ? "bg-primary" : "bg-muted"
              )}
            >
              <span
                className={cn(
                  "inline-block h-4 w-4 transform rounded-full bg-white transition-transform",
                  variables[variable.key] ? "translate-x-6" : "translate-x-1"
                )}
              />
            </button>
          </div>
        );

      default:
        return null;
    }
  };

  return (
    <div className={cn('space-y-6', className)}>
      {/* Header */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="flex items-center gap-2">
                <Calculator size={20} />
                {config.title}
              </CardTitle>
              {config.description && (
                <p className="text-sm text-muted-foreground mt-1">{config.description}</p>
              )}
            </div>
            
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={resetToBaseline}
                disabled={!hasChanges() || isRunning}
              >
                <RotateCcw size={14} className="mr-1" />
                Reset
              </Button>
              
              <Button
                onClick={runScenario}
                disabled={isRunning}
                size="sm"
              >
                {isRunning ? (
                  <>
                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-1" />
                    Running...
                  </>
                ) : (
                  <>
                    <Play size={14} className="mr-1" />
                    Run Scenario
                  </>
                )}
              </Button>
            </div>
          </div>
        </CardHeader>
      </Card>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Variables Panel */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <Settings size={16} />
              Scenario Variables
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-6">
            {config.variables.map((variable) => (
              <div key={variable.key}>
                {renderVariableControl(variable)}
              </div>
            ))}
          </CardContent>
        </Card>

        {/* Results Panel */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <BarChart3 size={16} />
              Scenario Results
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {results.map((result) => (
              <div key={result.key} className="flex items-center justify-between p-3 rounded-lg border">
                <div className="flex items-center gap-3">
                  {getResultIcon(result.status)}
                  <div>
                    <div className="font-medium text-sm">{result.label}</div>
                    {result.description && (
                      <div className="text-xs text-muted-foreground">{result.description}</div>
                    )}
                  </div>
                </div>
                
                <div className="text-right">
                  <div className={cn("font-semibold", getResultColor(result.status))}>
                    {result.value.toLocaleString()}{result.unit}
                  </div>
                  {result.change !== undefined && (
                    <div className={cn(
                      "text-xs",
                      result.change > 0 ? "text-green-600" : result.change < 0 ? "text-red-600" : "text-muted-foreground"
                    )}>
                      {result.change > 0 ? '+' : ''}{result.change.toFixed(1)}%
                    </div>
                  )}
                </div>
              </div>
            ))}
          </CardContent>
        </Card>
      </div>

      {/* Insights */}
      {config.insights && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Scenario Insights</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {config.insights.positive.length > 0 && (
              <div>
                <h4 className="text-sm font-medium text-green-700 mb-2 flex items-center gap-2">
                  <CheckCircle size={14} />
                  Positive Impacts
                </h4>
                <ul className="space-y-1">
                  {config.insights.positive.map((insight, index) => (
                    <li key={index} className="text-sm text-muted-foreground flex items-start gap-2">
                      <span className="w-1.5 h-1.5 rounded-full bg-green-500 mt-2 flex-shrink-0" />
                      {insight}
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {config.insights.negative.length > 0 && (
              <div>
                <h4 className="text-sm font-medium text-red-700 mb-2 flex items-center gap-2">
                  <AlertTriangle size={14} />
                  Considerations
                </h4>
                <ul className="space-y-1">
                  {config.insights.negative.map((insight, index) => (
                    <li key={index} className="text-sm text-muted-foreground flex items-start gap-2">
                      <span className="w-1.5 h-1.5 rounded-full bg-red-500 mt-2 flex-shrink-0" />
                      {insight}
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {config.insights.neutral.length > 0 && (
              <div>
                <h4 className="text-sm font-medium text-muted-foreground mb-2">Additional Notes</h4>
                <ul className="space-y-1">
                  {config.insights.neutral.map((insight, index) => (
                    <li key={index} className="text-sm text-muted-foreground flex items-start gap-2">
                      <span className="w-1.5 h-1.5 rounded-full bg-muted-foreground mt-2 flex-shrink-0" />
                      {insight}
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
};

// Utility function to create common scenario configurations
export const createTariffScenarioConfig = (): ScenarioConfig => ({
  title: 'Tariff Impact Analysis',
  description: 'Model the impact of different tariff rates and trade volumes on your business',
  variables: [
    {
      key: 'tariffRate',
      label: 'Tariff Rate',
      type: 'slider',
      min: 0,
      max: 50,
      step: 0.5,
      defaultValue: 10,
      currentValue: 10,
      unit: '%',
      description: 'Current or proposed tariff rate'
    },
    {
      key: 'volume',
      label: 'Import Volume',
      type: 'slider',
      min: 1000,
      max: 100000,
      step: 1000,
      defaultValue: 10000,
      currentValue: 10000,
      unit: ' units',
      description: 'Number of units to import'
    },
    {
      key: 'unitCost',
      label: 'Unit Cost',
      type: 'slider',
      min: 10,
      max: 1000,
      step: 10,
      defaultValue: 100,
      currentValue: 100,
      unit: '$',
      description: 'Cost per unit before tariffs'
    },
    {
      key: 'preferentialTrade',
      label: 'Preferential Trade Agreement',
      type: 'toggle',
      defaultValue: false,
      currentValue: false,
      description: 'Apply preferential rates if available'
    }
  ],
  baselineResults: [
    {
      key: 'totalCost',
      label: 'Total Import Cost',
      value: 1100000,
      unit: '$',
      status: 'neutral'
    },
    {
      key: 'tariffAmount',
      label: 'Tariff Amount',
      value: 100000,
      unit: '$',
      status: 'negative'
    },
    {
      key: 'effectiveRate',
      label: 'Effective Rate',
      value: 10,
      unit: '%',
      status: 'neutral'
    }
  ],
  calculator: (variables) => {
    const { tariffRate, volume, unitCost, preferentialTrade } = variables;
    const effectiveRate = preferentialTrade ? tariffRate * 0.5 : tariffRate;
    const baseCost = volume * unitCost;
    const tariffAmount = baseCost * (effectiveRate / 100);
    const totalCost = baseCost + tariffAmount;

    return [
      {
        key: 'totalCost',
        label: 'Total Import Cost',
        value: totalCost,
        unit: '$',
        status: 'neutral',
        change: ((totalCost - 1100000) / 1100000) * 100
      },
      {
        key: 'tariffAmount',
        label: 'Tariff Amount',
        value: tariffAmount,
        unit: '$',
        status: 'negative',
        change: ((tariffAmount - 100000) / 100000) * 100
      },
      {
        key: 'effectiveRate',
        label: 'Effective Rate',
        value: effectiveRate,
        unit: '%',
        status: preferentialTrade ? 'positive' : 'neutral',
        change: ((effectiveRate - 10) / 10) * 100
      }
    ];
  },
  insights: {
    positive: [
      'Preferential trade agreements can significantly reduce effective tariff rates',
      'Higher volumes may qualify for additional discounts'
    ],
    negative: [
      'Tariff increases directly impact total import costs',
      'Consider alternative sourcing countries with lower rates'
    ],
    neutral: [
      'Monitor trade policy changes that may affect rates',
      'Evaluate timing of imports around rate changes'
    ]
  }
});

ScenarioModeling.displayName = 'ScenarioModeling';