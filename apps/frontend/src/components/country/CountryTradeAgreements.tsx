import React, { useEffect, useMemo, useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { FileText, Calendar, Users, CheckCircle, Clock, AlertTriangle, Shield } from 'lucide-react';
import { formatDate } from '@/lib/utils';
import { tariffApi } from '@/services/api';

interface CountryTradeAgreementsProps {
  countryCode: string;
}

type AgreementDto = {
  id: number;
  name: string;
  type: string;
  status: string;
  enteredIntoForce?: string | null;
  rvcThreshold?: number | null;
};

type TariffRateDto = {
  id: number;
  importer?: { iso2?: string };
  origin?: { iso2?: string } | null;
  basis?: string;
};

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
  const [agreements, setAgreements] = useState<AgreementDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [hasMfn, setHasMfn] = useState<boolean>(false);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const [agreementsRes, ratesRes] = await Promise.all([
          tariffApi.getAgreements({ page: 0, size: 250 }),
          tariffApi.getTariffRates(),
        ]);

        const agreementsData: AgreementDto[] = Array.isArray(agreementsRes.data)
          ? agreementsRes.data
          : (Array.isArray(agreementsRes.data?.content) ? agreementsRes.data.content : []);
        if (!cancelled) setAgreements(agreementsData);

        const rates: TariffRateDto[] = Array.isArray(ratesRes.data) ? ratesRes.data : [];
        const mfnForImporter = rates.find(r =>
          (r.importer?.iso2 || '').toUpperCase() === countryCode.toUpperCase() &&
          (r.basis || '').toUpperCase() === 'MFN' &&
          (!r.origin || !r.origin.iso2)
        );
        if (!cancelled) setHasMfn(Boolean(mfnForImporter));
      } catch (e: any) {
        if (!cancelled) setError(e?.message || 'Failed to load agreements');
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => { cancelled = true };
  }, [countryCode]);

  const activeAgreements = useMemo(() => agreements.filter(a => (a.status || '').toLowerCase() === 'active').length, [agreements]);

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
              Total Agreements
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{agreements.length}</div>
            <p className="text-xs text-muted-foreground">Across database</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium flex items-center">
              <Shield className="w-4 h-4 mr-2 text-purple-600" />
              MFN Available
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{hasMfn ? 'Yes' : 'No'}</div>
            <p className="text-xs text-muted-foreground">Based on tariff-rate endpoint</p>
          </CardContent>
        </Card>
      </div>

      {/* Agreements List */}
      <div className="space-y-4">
        {loading ? (
          <div className="text-center py-8">
            <FileText className="w-10 h-10 text-muted-foreground mx-auto mb-2" />
            <div className="text-sm text-muted-foreground">Loading...</div>
          </div>
        ) : error ? (
          <div className="text-center py-8">
            <div className="text-sm text-red-600">{error}</div>
          </div>
        ) : agreements.length === 0 ? (
          <div className="text-center py-8">
            <FileText className="w-10 h-10 text-muted-foreground mx-auto mb-2" />
            <div className="text-sm text-muted-foreground">No agreements found</div>
          </div>
        ) : (
          agreements.map((agreement, index) => {
            const StatusIcon = getStatusIcon((agreement.status || '').toLowerCase());
            
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
                        <Badge variant="outline">{(agreement as any).type}</Badge>
                        <Badge variant={getStatusColor((agreement.status || '').toLowerCase())}>
                          {(agreement.status || '').charAt(0).toUpperCase() + (agreement.status || '').slice(1)}
                        </Badge>
                      </div>
                    </div>
                    <div className="text-right text-xs text-muted-foreground">
                      {(agreement as any).rvcThreshold != null && (
                        <span>RVC Threshold: {(agreement as any).rvcThreshold}%</span>
                      )}
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
                        {(agreement as any).enteredIntoForce && (
                          <div>
                            <span className="text-muted-foreground">Effective: </span>
                            {formatDate((agreement as any).enteredIntoForce)}
                          </div>
                        )}
                        {!(agreement as any).enteredIntoForce && (agreement.status || '') === 'negotiating' && (
                          <div className="text-amber-600 dark:text-amber-400">
                            Under negotiation
                          </div>
                        )}
                      </div>
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
              const count = agreements.filter(a => (a.status || '').toLowerCase() === status).length;
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