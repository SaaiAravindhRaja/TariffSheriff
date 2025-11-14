import axios from 'axios'

export interface TariffRateOption {
  id: number
  basis: string
  adValoremRate: number | null
  nonAdValorem: boolean
  nonAdValoremText: string | null
  agreementId: number | null
  agreementName: string | null
  rvcThreshold: number | null
}

export interface TariffLookupResponse {
  importerIso3: string
  originIso3: string | null
  hsCode: string
  rates: TariffRateOption[]
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

export const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000, // Increased to 30 seconds for news API calls
  headers: {
    'Content-Type': 'application/json',
  },
})

// Global token setter - will be called by useAuth hook
let getAccessTokenSilently: ((options?: any) => Promise<string>) | null = null

export const setAuth0TokenGetter = (getter: (options?: any) => Promise<string>) => {
  getAccessTokenSilently = getter
}

api.interceptors.request.use(
  async (config) => {
    // Skip auth for public endpoints
    const publicEndpoints = ['/tariff-rate', '/countries', '/hs-products']
    const isPublicEndpoint = publicEndpoints.some(endpoint => config.url?.startsWith(endpoint))
    
    console.log(`API Request: ${config.method?.toUpperCase()} ${config.url}`, {
      isPublic: isPublicEndpoint,
      hasTokenGetter: !!getAccessTokenSilently
    })
    
    if (!isPublicEndpoint) {
      try {
        if (getAccessTokenSilently) {
          const token = await getAccessTokenSilently({
            authorizationParams: {
              audience: import.meta.env.VITE_AUTH0_AUDIENCE || 'https://api.tariffsheriff.com',
            }
          })
          config.headers = config.headers ?? {}
          if (token) {
            config.headers.Authorization = `Bearer ${token}`
          }
          console.log(`✅ Auth token attached for ${config.url}`, {
            tokenLength: token?.length,
            tokenPreview: token?.substring(0, 20) + '...'
          })
        } else {
          console.warn(`⚠️ No token getter available for ${config.url}`)
        }
      } catch (error) {
        console.error(`❌ Failed to get Auth0 token for ${config.url}:`, error)
      }
    }
    return config
  },
  (error) => Promise.reject(error),
)

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && typeof window !== 'undefined') {
      console.error(`🔒 401 Unauthorized for ${error.config?.url}`, {
        hasAuthHeader: !!error.config?.headers?.Authorization,
        responseData: error.response?.data
      })
      // Auth0 will handle re-authentication
      console.error('Unauthorized request - please login again')
    } else if (error.response?.status === 403) {
      console.error(`🚫 403 Forbidden for ${error.config?.url}`, {
        hasAuthHeader: !!error.config?.headers?.Authorization,
        authHeaderPreview: error.config?.headers?.Authorization?.substring(0, 30) + '...',
        responseData: error.response?.data
      })
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
  getAgreementsByCountry: (countryIso3: string) =>
    api.get(`/agreements/by-country/${countryIso3}`),
  getTariffRates: () => api.get('/tariff-rate/'),
  getTariffRateLookup: (params: {
    importerIso3: string
    originIso3?: string
    hsCode: string
  }) => api.get<TariffLookupResponse>('/tariff-rate/lookup', { params }),
  getTariffSubcategories: (params: {
    importerIso3: string
    originIso3?: string
    hsCode: string
    limit?: number
  }) => api.get<TariffLookupResponse[]>('/tariff-rate/subcategories', { params }),
  searchHsProducts: (params: { q: string; limit?: number; importerIso3?: string }) =>
    api.get<{ hsCode: string; hsLabel: string }[]>('/hs-products/search', { params }),
}

export interface SavedTariffSummary {
  id: number
  name: string | null
  createdAt: string
  totalTariff: number | null // saved as total cost
  totalValue: number | null
  rateUsed: string | null
  appliedRate: number | null
  rvcComputed: number | null
  rvcThreshold: number | null
  hsCode: string | null
  importerIso3: string | null
  originIso3: string | null
  agreementName: string | null
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export const savedTariffsApi = {
  list: (params?: { page?: number; size?: number }) =>
    api.get<PageResponse<SavedTariffSummary>>('/tariff-calculations', { params }),
  get: (id: number) => api.get(`/tariff-calculations/${id}`),
  delete: (id: number) => api.delete(`/tariff-calculations/${id}`),
  save: (payload: any) => api.post('/tariff-calculations', payload),
}

export interface ChatConversationSummary {
  conversationId: string
  createdAt: string
  updatedAt: string
}

export interface ChatMessageDetail {
  role: 'user' | 'assistant'
  content: string
  createdAt: string
}

export interface ChatConversationDetail {
  conversationId: string
  createdAt: string
  updatedAt: string
  messages: ChatMessageDetail[]
}

export const chatbotApi = {
  listConversations: () => api.get<ChatConversationSummary[]>('/chatbot/conversations'),
  getConversation: (conversationId: string) =>
    api.get<ChatConversationDetail>(`/chatbot/conversations/${conversationId}`),
  deleteConversation: (conversationId: string) =>
    api.delete(`/chatbot/conversations/${conversationId}`),
}

export interface Article {
  title: string
  url: string
  content: string
  queryContext?: string
  source?: 'db' | 'api' | 'error'
  publishedAt?: string
  imageUrl?: string
}

export interface NewsQueryResponse {
  synthesizedAnswer: string
  source: 'db' | 'api' | 'error'
  articles: Article[]
  conversationId?: number | null
}

export const newsApi = {
  getAllArticles: (page = 0, limit = 12) =>
    api.get<Article[]>('/news/articles', { params: { page, limit } }),
  queryNews: (payload: { query: string; username?: string; conversationId?: number | null }) => {
    const { query, ...rest } = payload
    const body = Object.keys(rest).length ? rest : undefined
    return api.post<NewsQueryResponse>('/news/query', body, {
      params: { query },
    })
  },
}

export default api
