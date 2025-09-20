// Application configuration
export const appConfig = {
  // API Configuration
  api: {
    baseUrl: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
    timeout: 10000,
    retries: 3,
  },

  // Internationalization
  i18n: {
    defaultLocale: 'en-US',
    defaultCurrency: 'USD',
    supportedLocales: ['en-US', 'en-GB', 'de-DE', 'fr-FR', 'es-ES', 'ja-JP', 'zh-CN'],
    supportedCurrencies: ['USD', 'EUR', 'GBP', 'JPY', 'CNY', 'CAD', 'AUD'],
  },

  // UI Configuration
  ui: {
    theme: {
      defaultMode: 'light' as 'light' | 'dark' | 'system',
      enableSystemTheme: true,
    },
    animations: {
      enabled: true,
      duration: {
        fast: 150,
        normal: 300,
        slow: 500,
      },
    },
    pagination: {
      defaultPageSize: 20,
      pageSizeOptions: [10, 20, 50, 100],
    },
  },

  // Feature Flags
  features: {
    enableAnalytics: import.meta.env.VITE_ENABLE_ANALYTICS === 'true',
    enableErrorReporting: import.meta.env.VITE_ENABLE_ERROR_REPORTING === 'true',
    enableDarkMode: true,
    enableExperimentalFeatures: import.meta.env.NODE_ENV === 'development',
  },

  // Business Logic
  tariff: {
    maxCalculationHistory: 100,
    defaultTariffRate: 0.1,
    supportedProductCategories: [
      'Electronics',
      'Automotive',
      'Textiles',
      'Machinery',
      'Chemicals',
      'Food & Beverages',
    ],
  },

  // Performance
  performance: {
    queryStaleTime: 5 * 60 * 1000, // 5 minutes
    queryCacheTime: 10 * 60 * 1000, // 10 minutes
    debounceDelay: 300,
  },
} as const;

// Type-safe configuration access
export type AppConfig = typeof appConfig;

// Helper functions
export const getApiUrl = (endpoint: string): string => {
  return `${appConfig.api.baseUrl}${endpoint.startsWith('/') ? '' : '/'}${endpoint}`;
};

export const isFeatureEnabled = (feature: keyof typeof appConfig.features): boolean => {
  return appConfig.features[feature];
};

export const getLocaleConfig = () => ({
  locale: appConfig.i18n.defaultLocale,
  currency: appConfig.i18n.defaultCurrency,
});

export default appConfig;