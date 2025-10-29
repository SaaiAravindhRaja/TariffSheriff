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

export const tariffApi = {
  // Backend tariff calculation endpoint
  calculateTariff: (data: {
    mfnRate: number
    prefRate: number
    rvcThreshold?: number
    rvc?: number
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
  getCountries: (params?: { q?: string; page?: number; size?: number }) => api.get('/countries', { params }),
  getAgreements: (params?: { page?: number; size?: number }) => api.get('/agreements', { params }),
  getAgreementsByCountry: (countryIso2: string) => api.get(`/agreements/by-country/${countryIso2}`),
  getTariffRates: () => api.get('/tariff-rate/'),
  getTariffRateLookup: (params: { importerIso2: string; originIso2?: string; hsCode: string }) => 
    api.get('/tariff-rate/lookup', { params }),
}

// Auth API endpoints
// Optional: provide auth API helpers if you want to use axios instead of fetch
export const authApi = {
  login: (credentials: { email: string; password: string }) => api.post('/auth/login', credentials),
  register: (userData: { name: string; email: string; password: string; aboutMe?: string }) => api.post('/auth/register', userData),
}

export default api
