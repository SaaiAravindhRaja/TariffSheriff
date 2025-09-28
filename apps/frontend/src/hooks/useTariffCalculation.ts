import { useState, useCallback } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { tariffService, TariffCalculationRequest, TariffCalculationResult } from '@/services/tariffService';
import { useAuth } from '@/contexts/AuthContext';

export const useTariffCalculation = () => {
  const { isAuthenticated } = useAuth();
  const [calculationHistory, setCalculationHistory] = useState<TariffCalculationResult[]>([]);

  const calculateMutation = useMutation({
    mutationFn: (request: TariffCalculationRequest) => tariffService.calculateTariff(request),
    onSuccess: (result) => {
      // Add to calculation history
      setCalculationHistory(prev => [result, ...prev.slice(0, 9)]); // Keep last 10
    },
  });

  const saveCalculationMutation = useMutation({
    mutationFn: (calculation: TariffCalculationResult) => tariffService.saveCalculation(calculation),
  });

  const calculate = useCallback((request: TariffCalculationRequest) => {
    if (!isAuthenticated) {
      throw new Error('Authentication required for tariff calculations');
    }
    return calculateMutation.mutateAsync(request);
  }, [isAuthenticated, calculateMutation]);

  const saveCalculation = useCallback((calculation: TariffCalculationResult) => {
    if (!isAuthenticated) {
      throw new Error('Authentication required to save calculations');
    }
    return saveCalculationMutation.mutateAsync(calculation);
  }, [isAuthenticated, saveCalculationMutation]);

  return {
    calculate,
    saveCalculation,
    calculationHistory,
    isCalculating: calculateMutation.isPending,
    isSaving: saveCalculationMutation.isPending,
    calculationError: calculateMutation.error,
    saveError: saveCalculationMutation.error,
    reset: () => {
      calculateMutation.reset();
      saveCalculationMutation.reset();
    }
  };
};

export const useTariffRules = (params?: {
  country?: string;
  productCode?: string;
  page?: number;
  limit?: number;
}) => {
  const { isAuthenticated } = useAuth();

  return useQuery({
    queryKey: ['tariff-rules', params],
    queryFn: () => tariffService.getTariffRules(params),
    enabled: isAuthenticated,
    staleTime: 1000 * 60 * 10, // 10 minutes
  });
};

export const useHSCodeSearch = (query: string) => {
  const { isAuthenticated } = useAuth();

  return useQuery({
    queryKey: ['hs-codes', query],
    queryFn: () => tariffService.searchHSCodes(query),
    enabled: isAuthenticated && query.length >= 3,
    staleTime: 1000 * 60 * 30, // 30 minutes
  });
};

export const useTradeRoutes = () => {
  const { isAuthenticated } = useAuth();

  return useQuery({
    queryKey: ['trade-routes'],
    queryFn: () => tariffService.getTradeRoutes(),
    enabled: isAuthenticated,
    staleTime: 1000 * 60 * 15, // 15 minutes
  });
};

export const useUserCalculations = (params?: {
  page?: number;
  limit?: number;
}) => {
  const { isAuthenticated } = useAuth();

  return useQuery({
    queryKey: ['user-calculations', params],
    queryFn: () => tariffService.getUserCalculations(params),
    enabled: isAuthenticated,
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
};