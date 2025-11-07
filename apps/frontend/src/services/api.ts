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

// Global token setter - will be called by useAuth hook
let getAccessTokenSilently: (() => Promise<string>) | null = null

export const setAuth0TokenGetter = (getter: () => Promise<string>) => {
  getAccessTokenSilently = getter
}

api.interceptors.request.use(
  async (config) => {
    try {
      if (getAccessTokenSilently) {
        const token = await getAccessTokenSilently()
        config.headers = config.headers ?? {}
        config.headers.Authorization = `Bearer ${token}`
      }
    } catch (error) {
      console.error('Failed to get Auth0 token:', error)
    }
    return config
  },
  (error) => Promise.reject(error),
)

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && typeof window !== 'undefined') {
      // Auth0 will handle re-authentication
      console.error('Unauthorized request - please login again')
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
  }, config?: { signal?: AbortSignal }) => api.post('/tariff-rate/calculate', data, config),
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

export default api
