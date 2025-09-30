import React from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { FileText, Calendar, Users, CheckCircle, Clock, AlertTriangle, TrendingDown } from 'lucide-react';
import { formatDate } from '@/lib/utils';

interface CountryTradeAgreementsProps {
  countryCode: string;
}

// Agreements will be fetched later from backend/DB. For now, use empty list.
const generateTradeAgreements = (_countryCode: string) => [] as Array<{
  name: string;
  acronym: string;
  status: string;
  effectiveDate: string | null;
  expiryDate: string | null;
  participants: string[];
  benefits: string[];
  tariffReduction: number;
}>;

const getStatusColor = (status: string) => {
  switch (status) {
    case 'active': return 'success';
    case 'negotiating': return 'warning';
    case 'pending': return 'secondary';
    case 'expired': return 'destructive';
    default: return 'secondary';
  }
};

const getStatusIcon = (status: string) => {
  switch (status) {
    case 'active': return CheckCircle;
    case 'negotiating': return Clock;
    case 'pending': return Clock;
    case 'expired': return AlertTriangle;
    default: return FileText;
  }
};

export const CountryTradeAgreements: React.FC<CountryTradeAgreementsProps> = ({ countryCode }) => {
  const agreements = generateTradeAgreements(countryCode);
  
  const activeAgreements = agreements.filter(a => a.status === 'active').length;
  const totalParticipants = new Set(agreements.flatMap(a => a.participants)).size;
  const avgTariffReduction = agreements.length > 0
    ? agreements.reduce((sum, a) => sum + a.tariffReduction, 0) / agreements.length
    : 0;

  return (
    <div className="space-y-6">
      {/* Summary Cards */}
      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium flex items-center">
              <CheckCircle className="w-4 h-4 mr-2 text-green-600" />
              Active Agreements
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{activeAgreements}</div>
            <p className="text-xs text-muted-foreground">
              Currently in effect
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium flex items-center">
              <Users className="w-4 h-4 mr-2 text-blue-600" />
              Partner Countries
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{totalParticipants}</div>
            <p className="text-xs text-muted-foreground">
              Unique trading partners
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium flex items-center">
              <TrendingDown className="w-4 h-4 mr-2 text-purple-600" />
              Avg Tariff Reduction
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{avgTariffReduction.toFixed(0)}%</div>
            <p className="text-xs text-muted-foreground">
              Across all agreements
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Agreements List */}
      <div className="space-y-4">
        {agreements.length === 0 ? (
          <div className="text-center py-8">
            <FileText className="w-10 h-10 text-muted-foreground mx-auto mb-2" />
            <div className="text-sm text-muted-foreground">No agreements found</div>
          </div>
        ) : (
          agreements.map((agreement, index) => {
            const StatusIcon = getStatusIcon(agreement.status);
            
            return (
              <Card key={index} className="card-hover">
                <CardHeader>
                  <div className="flex items-start justify-between">
                    <div className="space-y-1">
                      <CardTitle className="text-lg flex items-center">
                        <StatusIcon className="w-5 h-5 mr-2" />
                        {agreement.name}
                      </CardTitle>
                      <div className="flex items-center space-x-2">
                        <Badge variant="outline">{agreement.acronym}</Badge>
                        <Badge variant={getStatusColor(agreement.status)}>
                          {agreement.status.charAt(0).toUpperCase() + agreement.status.slice(1)}
                        </Badge>
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="text-2xl font-bold text-blue-600 dark:text-blue-400">
                        {agreement.tariffReduction}%
                      </div>
                      <div className="text-xs text-muted-foreground">
                        Tariff Reduction
                      </div>
                    </div>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="grid gap-4 md:grid-cols-2">
                    <div>
                      <h4 className="font-medium mb-2 flex items-center">
                        <Calendar className="w-4 h-4 mr-2" />
                        Timeline
                      </h4>
                      <div className="space-y-1 text-sm">
                        {agreement.effectiveDate && (
                          <div>
                            <span className="text-muted-foreground">Effective: </span>
                            {formatDate(agreement.effectiveDate)}
                          </div>
                        )}
                        {agreement.expiryDate && (
                          <div>
                            <span className="text-muted-foreground">Expires: </span>
                            {formatDate(agreement.expiryDate)}
                          </div>
                        )}
                        {!agreement.effectiveDate && agreement.status === 'negotiating' && (
                          <div className="text-amber-600 dark:text-amber-400">
                            Under negotiation
                          </div>
                        )}
                      </div>
                    </div>

                    <div>
                      <h4 className="font-medium mb-2 flex items-center">
                        <Users className="w-4 h-4 mr-2" />
                        Participants ({agreement.participants.length})
                      </h4>
                      <div className="flex flex-wrap gap-1">
                        {agreement.participants.slice(0, 8).map((country, idx) => (
                          <Badge key={idx} variant="secondary" className="text-xs">
                            {country}
                          </Badge>
                        ))}
                        {agreement.participants.length > 8 && (
                          <Badge variant="outline" className="text-xs">
                            +{agreement.participants.length - 8} more
                          </Badge>
                        )}
                      </div>
                    </div>
                  </div>

                  <div className="mt-4">
                    <h4 className="font-medium mb-2">Key Benefits</h4>
                    <div className="grid gap-2 md:grid-cols-2 lg:grid-cols-3">
                      {agreement.benefits.map((benefit, idx) => (
                        <div key={idx} className="flex items-start space-x-2 text-sm">
                          <CheckCircle className="w-3 h-3 text-green-600 mt-0.5 flex-shrink-0" />
                          <span>{benefit}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                </CardContent>
              </Card>
            );
          })
        )}
      </div>

      {/* Agreement Status Overview */}
      <Card>
        <CardHeader>
          <CardTitle>Agreement Status Overview</CardTitle>
          <CardDescription>
            Current status of all trade agreements
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-4">
            {['active', 'negotiating', 'pending', 'expired'].map((status) => {
              const count = agreements.filter(a => a.status === status).length;
              const StatusIcon = getStatusIcon(status);
              
              return (
                <div key={status} className="text-center p-4 bg-gray-50 dark:bg-gray-800/50 rounded-lg">
                  <StatusIcon className="w-8 h-8 mx-auto mb-2 text-muted-foreground" />
                  <div className="text-2xl font-bold">{count}</div>
                  <div className="text-sm text-muted-foreground capitalize">{status}</div>
                </div>
              );
            })}
          </div>
        </CardContent>
      </Card>
    </div>
  );
};