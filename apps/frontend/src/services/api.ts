import axios from 'axios'
import safeLocalStorage from '@/lib/safeLocalStorage'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

export const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor for auth
api.interceptors.request.use(
  (config) => {
    // Get token directly as string (JWT tokens are not JSON)
    const token = typeof window !== 'undefined' ? window.localStorage.getItem('auth_token') : null
    if (token) {
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore - axios types allow headers as any in runtime
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor for error handling
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      if (typeof window !== 'undefined') {
        window.localStorage.removeItem('auth_token')
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

// Tariff API endpoints
export const tariffApi = {
  // Backend tariff calculation endpoint
  calculateTariff: (data: {
    mfnRate: number
    prefRate: number
    rvc: number
    agreementId?: number
    quantity: number
    totalValue: number
    materialCost: number
    labourCost: number
    overheadCost: number
    profit: number
    otherCosts: number
    fob: number
    nonOriginValue: number
  }) => api.post('/tariff-rate/calculate', data),
  
  // Legacy calculate method (deprecated)
  calculate: (data: {
    productCode: string
    originCountry: string
    destinationCountry: string
    value: number
    quantity: number
    date?: string
  }) => api.post('/tariffs/calculate', data),
  
  getRules: (params?: {
    country?: string
    productCode?: string
    page?: number
    limit?: number
  }) => api.get('/tariffs/rules', { params }),
  
  getCountries: (params?: { q?: string; page?: number; size?: number }) => api.get('/countries', { params }),
  getAgreements: (params?: { page?: number; size?: number }) => api.get('/agreements', { params }),
  getAgreementsByCountry: (countryIso2: string) => api.get(`/agreements/by-country/${countryIso2}`),
  getTariffRates: () => api.get('/tariff-rate/'),
  getTariffRateLookup: (params: { importerIso2: string; originIso2?: string; hsCode: string }) => 
    api.get('/tariff-rate/lookup', { params }),
  
  getHsCodes: (query?: string) => api.get('/hs-codes', { 
    params: { q: query } 
  }),
  
  getTradeRoutes: () => api.get('/trade-routes'),
  
  getAnalytics: (params?: {
    startDate?: string
    endDate?: string
    country?: string
  }) => api.get('/analytics', { params }),
}

// Auth API endpoints
export const authApi = {
  login: (credentials: { email: string; password: string }) =>
    api.post('/auth/login', credentials),
  
  register: (userData: { 
    email: string
    password: string
    firstName: string
    lastName: string 
  }) => api.post('/auth/register', userData),
  
  logout: () => api.post('/auth/logout'),
  
  refreshToken: () => api.post('/auth/refresh'),
  
  getProfile: () => api.get('/auth/profile'),
}

export default api