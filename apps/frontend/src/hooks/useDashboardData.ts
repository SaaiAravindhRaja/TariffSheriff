import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';

// Mock data - replace with actual API calls
const mockStatsData = [
  {
    title: 'Total Tariff Revenue',
    value: '$2,847,392',
    change: '+12.5%',
    trend: 'up' as const,
    description: 'This month'
  },
  {
    title: 'Active Trade Routes',
    value: '847',
    change: '+3.2%',
    trend: 'up' as const,
    description: 'Currently monitored'
  },
  {
    title: 'Calculations Today',
    value: '1,247',
    change: '-2.1%',
    trend: 'down' as const,
    description: 'vs yesterday'
  },
  {
    title: 'Avg Processing Time',
    value: '0.8s',
    change: '-15.3%',
    trend: 'up' as const,
    description: 'Response time'
  }
];

const mockTopCountries = [
  { code: 'US', name: 'United States', volume: '$1.2B', change: '+5.2%' },
  { code: 'CN', name: 'China', volume: '$987M', change: '+12.8%' },
  { code: 'DE', name: 'Germany', volume: '$654M', change: '-2.1%' },
  { code: 'JP', name: 'Japan', volume: '$432M', change: '+8.7%' },
  { code: 'GB', name: 'United Kingdom', volume: '$321M', change: '+3.4%' }
];

export const useDashboardStats = () => {
  return useQuery({
    queryKey: ['dashboard-stats'],
    queryFn: async () => {
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 500));
      return mockStatsData;
    },
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
};

export const useTopCountries = () => {
  return useQuery({
    queryKey: ['top-countries'],
    queryFn: async () => {
      // Simulate API call
      await new Promise(resolve => setTimeout(resolve, 300));
      return mockTopCountries;
    },
    staleTime: 1000 * 60 * 10, // 10 minutes
  });
};

export const useDashboardData = () => {
  const statsQuery = useDashboardStats();
  const countriesQuery = useTopCountries();

  const isLoading = statsQuery.isLoading || countriesQuery.isLoading;
  const isError = statsQuery.isError || countriesQuery.isError;

  const memoizedData = useMemo(() => ({
    stats: statsQuery.data || [],
    countries: countriesQuery.data || [],
    isLoading,
    isError,
    refetch: () => {
      statsQuery.refetch();
      countriesQuery.refetch();
    }
  }), [statsQuery.data, countriesQuery.data, isLoading, isError]);

  return memoizedData;
};