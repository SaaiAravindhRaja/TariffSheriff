import React, { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import { 
  MapPin, 
  Route, 
  Truck, 
  Ship, 
  Plane, 
  Clock,
  DollarSign,
  AlertTriangle,
  Info
} from 'lucide-react';

export interface TradeRoute {
  id: string;
  origin: {
    country: string;
    city: string;
    coordinates: [number, number];
  };
  destination: {
    country: string;
    city: string;
    coordinates: [number, number];
  };
  transportMode: 'sea' | 'air' | 'land' | 'multimodal';
  distance: number;
  estimatedTime: number; // in days
  cost: number;
  tariffRate: number;
  riskLevel: 'low' | 'medium' | 'high';
  advantages: string[];
  disadvantages: string[];
}

interface TradeRouteMapProps {
  routes: TradeRoute[];
  selectedRoute?: string;
  onRouteSelect?: (routeId: string) => void;
  className?: string;
  showComparison?: boolean;
}

const transportIcons = {
  sea: Ship,
  air: Plane,
  land: Truck,
  multimodal: Route,
};

const riskColors = {
  low: 'bg-green-100 text-green-800 border-green-200',
  medium: 'bg-yellow-100 text-yellow-800 border-yellow-200',
  high: 'bg-red-100 text-red-800 border-red-200',
};

const RouteCard: React.FC<{
  route: TradeRoute;
  isSelected: boolean;
  onSelect: () => void;
  showComparison: boolean;
}> = ({ route, isSelected, onSelect, showComparison }) => {
  const TransportIcon = transportIcons[route.transportMode];
  
  return (
    <Card 
      className={cn(
        'cursor-pointer transition-all duration-200 hover:shadow-md',
        isSelected && 'ring-2 ring-primary border-primary'
      )}
      onClick={onSelect}
    >
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <TransportIcon size={16} className="text-primary" />
            <CardTitle className="text-sm">
              {route.origin.country} → {route.destination.country}
            </CardTitle>
          </div>
          <Badge className={cn('text-xs', riskColors[route.riskLevel])}>
            {route.riskLevel} risk
          </Badge>
        </div>
      </CardHeader>
      
      <CardContent className="space-y-3">
        {/* Route details */}
        <div className="grid grid-cols-2 gap-3 text-sm">
          <div className="flex items-center gap-2">
            <Clock size={14} className="text-muted-foreground" />
            <span>{route.estimatedTime} days</span>
          </div>
          <div className="flex items-center gap-2">
            <DollarSign size={14} className="text-muted-foreground" />
            <span>${route.cost.toLocaleString()}</span>
          </div>
          <div className="flex items-center gap-2">
            <Route size={14} className="text-muted-foreground" />
            <span>{route.distance.toLocaleString()} km</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-muted-foreground">Tariff:</span>
            <span>{route.tariffRate}%</span>
          </div>
        </div>

        {/* Cities */}
        <div className="space-y-1 text-xs text-muted-foreground">
          <div className="flex items-center gap-2">
            <MapPin size={12} />
            <span>From: {route.origin.city}, {route.origin.country}</span>
          </div>
          <div className="flex items-center gap-2">
            <MapPin size={12} />
            <span>To: {route.destination.city}, {route.destination.country}</span>
          </div>
        </div>

        {/* Advantages/Disadvantages */}
        {showComparison && (
          <div className="space-y-2">
            {route.advantages.length > 0 && (
              <div>
                <h5 className="text-xs font-medium text-green-700 mb-1">Advantages</h5>
                <ul className="text-xs text-muted-foreground space-y-0.5">
                  {route.advantages.slice(0, 2).map((advantage, index) => (
                    <li key={index} className="flex items-start gap-1">
                      <span className="w-1 h-1 rounded-full bg-green-500 mt-1.5 flex-shrink-0" />
                      {advantage}
                    </li>
                  ))}
                </ul>
              </div>
            )}
            
            {route.disadvantages.length > 0 && (
              <div>
                <h5 className="text-xs font-medium text-red-700 mb-1">Considerations</h5>
                <ul className="text-xs text-muted-foreground space-y-0.5">
                  {route.disadvantages.slice(0, 2).map((disadvantage, index) => (
                    <li key={index} className="flex items-start gap-1">
                      <span className="w-1 h-1 rounded-full bg-red-500 mt-1.5 flex-shrink-0" />
                      {disadvantage}
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
};

export const TradeRouteMap: React.FC<TradeRouteMapProps> = ({
  routes,
  selectedRoute,
  onRouteSelect,
  className,
  showComparison = true,
}) => {
  const [viewMode, setViewMode] = useState<'list' | 'comparison'>('list');

  const handleRouteSelect = (routeId: string) => {
    if (onRouteSelect) {
      onRouteSelect(routeId);
    }
  };

  const getBestRoute = () => {
    if (routes.length === 0) return null;
    
    // Simple scoring: lower cost + lower risk + faster time = better
    return routes.reduce((best, current) => {
      const currentScore = (1 / current.cost) + (1 / current.estimatedTime) + 
                          (current.riskLevel === 'low' ? 3 : current.riskLevel === 'medium' ? 2 : 1);
      const bestScore = (1 / best.cost) + (1 / best.estimatedTime) + 
                       (best.riskLevel === 'low' ? 3 : best.riskLevel === 'medium' ? 2 : 1);
      
      return currentScore > bestScore ? current : best;
    });
  };

  const bestRoute = getBestRoute();

  if (routes.length === 0) {
    return (
      <Card className={className}>
        <CardContent className="flex items-center justify-center h-64">
          <div className="text-center text-muted-foreground">
            <MapPin size={48} className="mx-auto mb-4 opacity-50" />
            <p>No trade routes available</p>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className={cn('space-y-4', className)}>
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Route size={20} className="text-primary" />
          <h3 className="text-lg font-semibold">Trade Route Analysis</h3>
        </div>
        
        <div className="flex items-center gap-2">
          <Button
            variant={viewMode === 'list' ? 'default' : 'outline'}
            size="sm"
            onClick={() => setViewMode('list')}
          >
            List View
          </Button>
          <Button
            variant={viewMode === 'comparison' ? 'default' : 'outline'}
            size="sm"
            onClick={() => setViewMode('comparison')}
          >
            Compare
          </Button>
        </div>
      </div>

      {/* Best route recommendation */}
      {bestRoute && (
        <Card className="border-primary/20 bg-primary/5">
          <CardHeader className="pb-3">
            <div className="flex items-center gap-2">
              <Badge variant="default" className="text-xs">
                Recommended
              </Badge>
              <span className="text-sm font-medium">
                {bestRoute.origin.country} → {bestRoute.destination.country}
              </span>
            </div>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-3 gap-4 text-sm">
              <div className="text-center">
                <div className="font-semibold text-primary">${bestRoute.cost.toLocaleString()}</div>
                <div className="text-xs text-muted-foreground">Total Cost</div>
              </div>
              <div className="text-center">
                <div className="font-semibold text-primary">{bestRoute.estimatedTime} days</div>
                <div className="text-xs text-muted-foreground">Transit Time</div>
              </div>
              <div className="text-center">
                <div className="font-semibold text-primary">{bestRoute.tariffRate}%</div>
                <div className="text-xs text-muted-foreground">Tariff Rate</div>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Routes grid */}
      <div className={cn(
        'grid gap-4',
        viewMode === 'comparison' ? 'grid-cols-1 md:grid-cols-2 lg:grid-cols-3' : 'grid-cols-1'
      )}>
        {routes.map((route) => (
          <RouteCard
            key={route.id}
            route={route}
            isSelected={selectedRoute === route.id}
            onSelect={() => handleRouteSelect(route.id)}
            showComparison={viewMode === 'comparison'}
          />
        ))}
      </div>

      {/* Summary statistics */}
      {routes.length > 1 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-sm flex items-center gap-2">
              <Info size={16} />
              Route Comparison Summary
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
              <div>
                <div className="font-medium">Fastest Route</div>
                <div className="text-muted-foreground">
                  {Math.min(...routes.map(r => r.estimatedTime))} days
                </div>
              </div>
              <div>
                <div className="font-medium">Cheapest Route</div>
                <div className="text-muted-foreground">
                  ${Math.min(...routes.map(r => r.cost)).toLocaleString()}
                </div>
              </div>
              <div>
                <div className="font-medium">Lowest Tariff</div>
                <div className="text-muted-foreground">
                  {Math.min(...routes.map(r => r.tariffRate))}%
                </div>
              </div>
              <div>
                <div className="font-medium">Low Risk Routes</div>
                <div className="text-muted-foreground">
                  {routes.filter(r => r.riskLevel === 'low').length} of {routes.length}
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
};

TradeRouteMap.displayName = 'TradeRouteMap';