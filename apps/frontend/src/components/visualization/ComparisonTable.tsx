import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import { 
  ArrowUpDown, 
  ArrowUp, 
  ArrowDown, 
  Download,
  Filter,
  Search,
  TrendingUp,
  TrendingDown,
  Minus,
  CheckCircle,
  XCircle,
  AlertCircle
} from 'lucide-react';

export interface TableColumn {
  key: string;
  label: string;
  type: 'text' | 'number' | 'currency' | 'percentage' | 'date' | 'boolean' | 'status' | 'trend';
  sortable?: boolean;
  formatter?: (value: any) => string;
  className?: string;
  width?: string;
}

export interface TableRow {
  id: string;
  [key: string]: any;
}

export interface ComparisonTableConfig {
  title: string;
  description?: string;
  columns: TableColumn[];
  data: TableRow[];
  highlightBest?: string[]; // Column keys to highlight best values
  highlightWorst?: string[]; // Column keys to highlight worst values
  showSearch?: boolean;
  showFilter?: boolean;
  showExport?: boolean;
  defaultSort?: {
    column: string;
    direction: 'asc' | 'desc';
  };
}

interface ComparisonTableProps {
  config: ComparisonTableConfig;
  className?: string;
  onRowClick?: (row: TableRow) => void;
  onExport?: (format: 'csv' | 'excel' | 'pdf') => void;
}

type SortDirection = 'asc' | 'desc' | null;

const formatValue = (value: any, column: TableColumn): React.ReactNode => {
  if (value === null || value === undefined) {
    return <span className="text-muted-foreground">—</span>;
  }

  if (column.formatter) {
    return column.formatter(value);
  }

  switch (column.type) {
    case 'currency':
      return `$${Number(value).toLocaleString()}`;
    
    case 'percentage':
      return `${Number(value).toFixed(1)}%`;
    
    case 'number':
      return Number(value).toLocaleString();
    
    case 'date':
      return new Date(value).toLocaleDateString();
    
    case 'boolean':
      return value ? (
        <CheckCircle size={16} className="text-green-600" />
      ) : (
        <XCircle size={16} className="text-red-600" />
      );
    
    case 'status':
      const statusColors = {
        active: 'bg-green-100 text-green-800',
        inactive: 'bg-gray-100 text-gray-800',
        pending: 'bg-yellow-100 text-yellow-800',
        error: 'bg-red-100 text-red-800',
      };
      return (
        <Badge className={statusColors[value as keyof typeof statusColors] || statusColors.inactive}>
          {value}
        </Badge>
      );
    
    case 'trend':
      const trendValue = Number(value);
      if (trendValue > 0) {
        return (
          <div className="flex items-center gap-1 text-green-600">
            <TrendingUp size={14} />
            <span>+{trendValue.toFixed(1)}%</span>
          </div>
        );
      } else if (trendValue < 0) {
        return (
          <div className="flex items-center gap-1 text-red-600">
            <TrendingDown size={14} />
            <span>{trendValue.toFixed(1)}%</span>
          </div>
        );
      } else {
        return (
          <div className="flex items-center gap-1 text-muted-foreground">
            <Minus size={14} />
            <span>0%</span>
          </div>
        );
      }
    
    default:
      return String(value);
  }
};

const getBestWorstValues = (data: TableRow[], column: string, type: 'best' | 'worst') => {
  if (data.length === 0) return new Set();
  
  const values = data.map(row => Number(row[column])).filter(v => !isNaN(v));
  if (values.length === 0) return new Set();
  
  const targetValue = type === 'best' ? Math.max(...values) : Math.min(...values);
  
  return new Set(
    data
      .filter(row => Number(row[column]) === targetValue)
      .map(row => row.id)
  );
};

export const ComparisonTable: React.FC<ComparisonTableProps> = ({
  config,
  className,
  onRowClick,
  onExport,
}) => {
  const [sortColumn, setSortColumn] = useState<string | null>(
    config.defaultSort?.column || null
  );
  const [sortDirection, setSortDirection] = useState<SortDirection>(
    config.defaultSort?.direction || null
  );
  const [searchTerm, setSearchTerm] = useState('');
  const [filteredData, setFilteredData] = useState(config.data);

  const {
    title,
    description,
    columns,
    data,
    highlightBest = [],
    highlightWorst = [],
    showSearch = true,
    showFilter = false,
    showExport = true,
  } = config;

  // Calculate best/worst values for highlighting
  const bestValues = new Map();
  const worstValues = new Map();
  
  highlightBest.forEach(column => {
    bestValues.set(column, getBestWorstValues(data, column, 'best'));
  });
  
  highlightWorst.forEach(column => {
    worstValues.set(column, getBestWorstValues(data, column, 'worst'));
  });

  const handleSort = (columnKey: string) => {
    const column = columns.find(col => col.key === columnKey);
    if (!column?.sortable) return;

    if (sortColumn === columnKey) {
      setSortDirection(
        sortDirection === 'asc' ? 'desc' : sortDirection === 'desc' ? null : 'asc'
      );
      if (sortDirection === 'desc') {
        setSortColumn(null);
      }
    } else {
      setSortColumn(columnKey);
      setSortDirection('asc');
    }
  };

  const sortedData = React.useMemo(() => {
    let result = [...filteredData];

    // Apply search filter
    if (searchTerm) {
      result = result.filter(row =>
        columns.some(column =>
          String(row[column.key]).toLowerCase().includes(searchTerm.toLowerCase())
        )
      );
    }

    // Apply sorting
    if (sortColumn && sortDirection) {
      result.sort((a, b) => {
        const aValue = a[sortColumn];
        const bValue = b[sortColumn];
        
        // Handle null/undefined values
        if (aValue == null && bValue == null) return 0;
        if (aValue == null) return sortDirection === 'asc' ? 1 : -1;
        if (bValue == null) return sortDirection === 'asc' ? -1 : 1;
        
        // Numeric comparison
        const aNum = Number(aValue);
        const bNum = Number(bValue);
        if (!isNaN(aNum) && !isNaN(bNum)) {
          return sortDirection === 'asc' ? aNum - bNum : bNum - aNum;
        }
        
        // String comparison
        const aStr = String(aValue).toLowerCase();
        const bStr = String(bValue).toLowerCase();
        if (sortDirection === 'asc') {
          return aStr.localeCompare(bStr);
        } else {
          return bStr.localeCompare(aStr);
        }
      });
    }

    return result;
  }, [filteredData, searchTerm, sortColumn, sortDirection, columns]);

  const getSortIcon = (columnKey: string) => {
    if (sortColumn !== columnKey) {
      return <ArrowUpDown size={14} className="opacity-50" />;
    }
    
    if (sortDirection === 'asc') {
      return <ArrowUp size={14} className="text-primary" />;
    } else if (sortDirection === 'desc') {
      return <ArrowDown size={14} className="text-primary" />;
    }
    
    return <ArrowUpDown size={14} className="opacity-50" />;
  };

  const getCellClassName = (row: TableRow, column: TableColumn) => {
    let className = column.className || '';
    
    // Highlight best/worst values
    if (bestValues.has(column.key) && bestValues.get(column.key).has(row.id)) {
      className += ' bg-green-50 text-green-900 font-medium';
    } else if (worstValues.has(column.key) && worstValues.get(column.key).has(row.id)) {
      className += ' bg-red-50 text-red-900 font-medium';
    }
    
    return className;
  };

  return (
    <Card className={cn('w-full', className)}>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle className="text-lg">{title}</CardTitle>
            {description && (
              <p className="text-sm text-muted-foreground mt-1">{description}</p>
            )}
          </div>
          
          <div className="flex items-center gap-2">
            {showExport && onExport && (
              <Button
                variant="outline"
                size="sm"
                onClick={() => onExport('csv')}
                className="h-8"
              >
                <Download size={14} className="mr-1" />
                Export
              </Button>
            )}
          </div>
        </div>
        
        {/* Search and filters */}
        {(showSearch || showFilter) && (
          <div className="flex items-center gap-2 mt-4">
            {showSearch && (
              <div className="relative flex-1 max-w-sm">
                <Search size={14} className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground" />
                <input
                  type="text"
                  placeholder="Search..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="w-full pl-9 pr-3 py-2 text-sm border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary"
                />
              </div>
            )}
            
            {showFilter && (
              <Button variant="outline" size="sm" className="h-9">
                <Filter size={14} className="mr-1" />
                Filter
              </Button>
            )}
          </div>
        )}
      </CardHeader>
      
      <CardContent className="p-0">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-muted/50 border-b">
              <tr>
                {columns.map((column) => (
                  <th
                    key={column.key}
                    className={cn(
                      'px-4 py-3 text-left text-sm font-medium text-muted-foreground',
                      column.sortable && 'cursor-pointer hover:text-foreground transition-colors',
                      column.width && `w-${column.width}`
                    )}
                    onClick={() => column.sortable && handleSort(column.key)}
                  >
                    <div className="flex items-center gap-2">
                      {column.label}
                      {column.sortable && getSortIcon(column.key)}
                    </div>
                  </th>
                ))}
              </tr>
            </thead>
            
            <tbody>
              {sortedData.map((row, index) => (
                <tr
                  key={row.id}
                  className={cn(
                    'border-b hover:bg-muted/30 transition-colors',
                    onRowClick && 'cursor-pointer',
                    index % 2 === 0 && 'bg-muted/10'
                  )}
                  onClick={() => onRowClick?.(row)}
                >
                  {columns.map((column) => (
                    <td
                      key={column.key}
                      className={cn(
                        'px-4 py-3 text-sm',
                        getCellClassName(row, column)
                      )}
                    >
                      {formatValue(row[column.key], column)}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
          
          {sortedData.length === 0 && (
            <div className="text-center py-8 text-muted-foreground">
              <AlertCircle size={48} className="mx-auto mb-4 opacity-50" />
              <p>No data matches your search criteria</p>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
};

// Utility function to create common table configurations
export const createTariffComparisonTable = (data: any[]): ComparisonTableConfig => ({
  title: 'Tariff Rate Comparison',
  description: 'Compare tariff rates across different countries and products',
  columns: [
    { key: 'country', label: 'Country', type: 'text', sortable: true },
    { key: 'product', label: 'Product', type: 'text', sortable: true },
    { key: 'hsCode', label: 'HS Code', type: 'text', sortable: true },
    { key: 'mfnRate', label: 'MFN Rate', type: 'percentage', sortable: true },
    { key: 'preferentialRate', label: 'Preferential Rate', type: 'percentage', sortable: true },
    { key: 'savings', label: 'Savings', type: 'trend', sortable: true },
    { key: 'status', label: 'Status', type: 'status', sortable: true },
  ],
  data,
  highlightBest: ['savings'],
  highlightWorst: ['mfnRate'],
  showSearch: true,
  showExport: true,
  defaultSort: { column: 'savings', direction: 'desc' },
});

ComparisonTable.displayName = 'ComparisonTable';