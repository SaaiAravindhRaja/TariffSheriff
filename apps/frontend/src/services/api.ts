import axios from 'axios'

export interface TariffRateOption {
  id: number
  basis: string
  adValoremRate: number | null
  specificAmount: number | null
  specificUnit: string | null
  agreementId: number | null
  agreementName: string | null
  rvcThreshold: number | null
}

export interface TariffLookupResponse {
  importerIso2: string
  originIso2: string | null
  hsCode: string
  rates: TariffRateOption[]
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

export const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use(
  (config) => {
    const token = typeof window !== 'undefined' ? window.localStorage.getItem('auth_token') : null
    if (token) {
      config.headers = config.headers ?? {}
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && typeof window !== 'undefined') {
      window.localStorage.removeItem('auth_token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  },
)

export const tariffApi = {
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
  getCountries: (params?: { q?: string; page?: number; size?: number }) =>
    api.get('/countries', { params }),
  getAgreements: (params?: { page?: number; size?: number }) =>
    api.get('/agreements', { params }),
  getAgreementsByCountry: (countryIso2: string) =>
    api.get(`/agreements/by-country/${countryIso2}`),
  getTariffRates: () => api.get('/tariff-rate/'),
  getTariffRateLookup: (params: {
    importerIso2: string
    originIso2?: string
    hsCode: string
  }) => api.get<TariffLookupResponse>('/tariff-rate/lookup', { params }),
}

export const authApi = {
  login: (credentials: { email: string; password: string }) =>
    api.post('/auth/login', credentials),
  register: (userData: { name: string; email: string; password: string; aboutMe?: string }) =>
    api.post('/auth/register', userData),
}

export default api
