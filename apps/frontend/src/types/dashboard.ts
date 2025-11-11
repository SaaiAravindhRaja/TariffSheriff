export interface DashboardStats {
  totalTariffRevenue: number;
  activeTariffRoutes: number;
  calculationsCount: number;
  mostUsedHsCode: MostUsedHsCode | null;
}

export interface MostUsedHsCode {
  hsCode: string;
  description: string;
  count: number;
}

export type CalculationPeriod = 'today' | 'month' | 'year';
