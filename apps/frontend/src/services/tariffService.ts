import { tariffApi } from './api';

export interface TariffCalculationRequest {
  productCode: string;
  originCountry: string;
  destinationCountry: string;
  value: number;
  quantity: number;
  date?: string;
}

export interface TariffCalculationResult {
  baseValue: number;
  dutiableValue: number;
  tariffRate: number;
  tariffAmount: number;
  additionalDuties: number;
  taxes: {
    vat: number;
    excise: number;
    other: number;
  };
  fees: {
    processing: number;
    inspection: number;
    storage: number;
    other: number;
  };
  totalCost: number;
  effectiveRate: number;
  breakdown: Array<{
    type: string;
    category: 'duty' | 'tax' | 'fee';
    rate: number;
    amount: number;
    description: string;
    legal_basis: string;
  }>;
  appliedRules: Array<{
    ruleId: string;
    description: string;
    source: string;
    validFrom: string;
    validTo: string;
    confidence: number;
    tradeAgreement?: string;
  }>;
  warnings: Array<{
    type: 'info' | 'warning' | 'error';
    message: string;
    recommendation?: string;
  }>;
  alternativeRoutes: Array<{
    country: string;
    countryName: string;
    tariffRate: number;
    totalCost: number;
    savings: number;
    savingsPercentage: number;
    tradeAgreement?: string;
    transitTime?: number;
  }>;
  compliance: {
    requiredDocuments: string[];
    certificates: string[];
    restrictions: string[];
    prohibitions: string[];
  };
}

export class TariffService {
  /**
   * Calculate tariff for a given product and trade route
   */
  async calculateTariff(request: TariffCalculationRequest): Promise<TariffCalculationResult> {
    try {
      const response = await tariffApi.calculate(request);
      return response.data;
    } catch (error: any) {
      const message = error.response?.data?.message || 'Tariff calculation failed';
      throw new Error(message);
    }
  }

  /**
   * Get tariff rules for a specific country and product
   */
  async getTariffRules(params?: {
    country?: string;
    productCode?: string;
    page?: number;
    limit?: number;
  }) {
    try {
      const response = await tariffApi.getRules(params);
      return response.data;
    } catch (error: any) {
      const message = error.response?.data?.message || 'Failed to fetch tariff rules';
      throw new Error(message);
    }
  }

  /**
   * Get list of countries with trade data
   */
  async getCountries() {
    try {
      const response = await tariffApi.getCountries();
      return response.data;
    } catch (error: any) {
      const message = error.response?.data?.message || 'Failed to fetch countries';
      throw new Error(message);
    }
  }

  /**
   * Search HS codes
   */
  async searchHSCodes(query: string) {
    try {
      const response = await tariffApi.getHsCodes(query);
      return response.data;
    } catch (error: any) {
      const message = error.response?.data?.message || 'Failed to search HS codes';
      throw new Error(message);
    }
  }

  /**
   * Get trade routes
   */
  async getTradeRoutes() {
    try {
      const response = await tariffApi.getTradeRoutes();
      return response.data;
    } catch (error: any) {
      const message = error.response?.data?.message || 'Failed to fetch trade routes';
      throw new Error(message);
    }
  }

  /**
   * Get analytics data
   */
  async getAnalytics(params?: {
    startDate?: string;
    endDate?: string;
    country?: string;
  }) {
    try {
      const response = await tariffApi.getAnalytics(params);
      return response.data;
    } catch (error: any) {
      const message = error.response?.data?.message || 'Failed to fetch analytics';
      throw new Error(message);
    }
  }

  /**
   * Get user's saved calculations
   */
  async getUserCalculations(params?: {
    page?: number;
    limit?: number;
  }) {
    try {
      const response = await tariffApi.getUserCalculations(params);
      return response.data;
    } catch (error: any) {
      const message = error.response?.data?.message || 'Failed to fetch user calculations';
      throw new Error(message);
    }
  }

  /**
   * Save a calculation for the user
   */
  async saveCalculation(calculation: any) {
    try {
      const response = await tariffApi.saveCalculation(calculation);
      return response.data;
    } catch (error: any) {
      const message = error.response?.data?.message || 'Failed to save calculation';
      throw new Error(message);
    }
  }

  /**
   * Delete a user's calculation
   */
  async deleteCalculation(id: string) {
    try {
      const response = await tariffApi.deleteCalculation(id);
      return response.data;
    } catch (error: any) {
      const message = error.response?.data?.message || 'Failed to delete calculation';
      throw new Error(message);
    }
  }
}

// Export singleton instance
export const tariffService = new TariffService();
export default tariffService;