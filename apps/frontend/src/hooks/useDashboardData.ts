import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { tariffApi } from '@/services/api';
import { useAuth } from '@/contexts/AuthContext';

// Mock data as fallback
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
  const { isAuthenticated } = useAuth();

  return useQuery({
    queryKey: ['dashboard-stats'],
    queryFn: async () => {
      try {
        // Try to fetch real analytics data
        const response = await tariffApi.getAnalytics({
          startDate: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString(), // Last 30 days
          endDate: new Date().toISOString()
        });
        
        // Transform API response to expected format
        return response.data.stats || mockStatsData;
      } catch (error) {
        console.warn('Failed to fetch dashboard stats, using mock data:', error);
        return mockStatsData;
      }
    },
    enabled: isAuthenticated,
    staleTime: 1000 * 60 * 5, // 5 minutes
    retry: 1, // Only retry once before falling back to mock data
  });
};

export const useTopCountries = () => {
  const { isAuthenticated } = useAuth();

  return useQuery({
    queryKey: ['top-countries'],
    queryFn: async () => {
      try {
        // Try to fetch real countries data
        const response = await tariffApi.getCountries();
        
        // Transform API response to expected format
        const countries = response.data;
        if (Array.isArray(countries)) {
          return countries.slice(0, 5).map((country: any) => ({
            code: country.code,
            name: country.name,
            volume: country.tradeVolume || '$0',
            change: country.volumeChange || '0%'
          }));
        }
        
        return mockTopCountries;
      } catch (error) {
        console.warn('Failed to fetch top countries, using mock data:', error);
        return mockTopCountries;
      }
    },
    enabled: isAuthenticated,
    staleTime: 1000 * 60 * 10, // 10 minutes
    retry: 1, // Only retry once before falling back to mock data
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