import axios, { AxiosInstance, AxiosResponse, AxiosError, InternalAxiosRequestConfig } from 'axios';
import { tokenStorage } from '@/lib/tokenStorage';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

// Create axios instance
export const api: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Track ongoing refresh request to prevent multiple simultaneous refreshes
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value?: any) => void;
  reject: (error?: any) => void;
}> = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error);
    } else {
      resolve(token);
    }
  });
  
  failedQueue = [];
};

// Request interceptor for authentication
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = tokenStorage.getAccessToken();
    if (token && tokenStorage.validateStoredTokens()) {
      config.headers = config.headers || {};
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error: AxiosError) => {
    return Promise.reject(error);
  }
);

// Response interceptor for token refresh and error handling
api.interceptors.response.use(
  (response: AxiosResponse) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // Handle 401 Unauthorized errors
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // If already refreshing, queue this request
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(token => {
          if (originalRequest.headers) {
            originalRequest.headers.Authorization = `Bearer ${token}`;
          }
          return api(originalRequest);
        }).catch(err => {
          return Promise.reject(err);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        // Attempt to refresh token
        const refreshToken = tokenStorage.getRefreshToken();
        if (!refreshToken) {
          throw new Error('No refresh token available');
        }

        const response = await axios.post(`${API_BASE_URL}/auth/refresh`, {
          refreshToken
        });

        const { accessToken, refreshToken: newRefreshToken, expiresIn } = response.data;
        
        // Update stored tokens
        tokenStorage.setTokens(accessToken, newRefreshToken, expiresIn);
        
        // Update the authorization header for the original request
        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        }

        processQueue(null, accessToken);
        
        // Retry the original request
        return api(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        
        // Clear tokens and redirect to login
        tokenStorage.clearTokens();
        
        // Trigger logout in the application
        if (typeof window !== 'undefined') {
          window.dispatchEvent(new CustomEvent('auth:logout'));
        }
        
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    // Handle other error cases
    if (error.response?.status === 403) {
      // Forbidden - user doesn't have permission
      console.warn('Access forbidden:', error.response.data);
      
      // Dispatch custom event for role-based access errors
      if (typeof window !== 'undefined') {
        window.dispatchEvent(new CustomEvent('auth:forbidden', {
          detail: { 
            message: error.response.data?.message || 'Access denied',
            requiredRole: error.response.data?.requiredRole 
          }
        }));
      }
    } else if (error.response?.status === 429) {
      // Rate limiting
      console.warn('Rate limit exceeded:', error.response.data);
      
      if (typeof window !== 'undefined') {
        window.dispatchEvent(new CustomEvent('api:rateLimit', {
          detail: { 
            message: error.response.data?.message || 'Too many requests',
            retryAfter: error.response.headers['retry-after']
          }
        }));
      }
    } else if (error.response && error.response.status >= 500) {
      // Server errors
      console.error('Server error:', error.response.data);
      
      if (typeof window !== 'undefined') {
        window.dispatchEvent(new CustomEvent('api:serverError', {
          detail: { 
            message: error.response.data?.message || 'Server error occurred',
            status: error.response.status
          }
        }));
      }
    }

    return Promise.reject(error);
  }
);

// Tariff API endpoints
export const tariffApi = {
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
  
  getCountries: () => api.get('/countries'),
  
  getHsCodes: (query?: string) => api.get('/hs-codes', { 
    params: { q: query } 
  }),
  
  getTradeRoutes: () => api.get('/trade-routes'),
  
  getAnalytics: (params?: {
    startDate?: string
    endDate?: string
    country?: string
  }) => api.get('/analytics', { params }),

  // User-specific endpoints
  getUserCalculations: (params?: {
    page?: number
    limit?: number
  }) => api.get('/user/calculations', { params }),

  saveCalculation: (calculation: any) => api.post('/user/calculations', calculation),

  deleteCalculation: (id: string) => api.delete(`/user/calculations/${id}`),
}

// Auth API endpoints
export const authApi = {
  login: (credentials: { email: string; password: string }) =>
    api.post('/auth/login', credentials),
  
  register: (userData: { 
    email: string;
    password: string;
    name: string;
    role?: string;
  }) => api.post('/auth/register', userData),
  
  logout: () => {
    const refreshToken = tokenStorage.getRefreshToken();
    return api.post('/auth/logout', { refreshToken });
  },
  
  refreshToken: () => {
    const refreshToken = tokenStorage.getRefreshToken();
    return api.post('/auth/refresh', { refreshToken });
  },
  
  getProfile: () => api.get('/auth/me'),
  
  verifyEmail: (token: string) => api.get(`/auth/verify?token=${token}`),
  
  forgotPassword: (email: string) => api.post('/auth/forgot-password', { email }),
  
  resetPassword: (token: string, password: string) => 
    api.post('/auth/reset-password', { token, password }),
  
  changePassword: (currentPassword: string, newPassword: string) =>
    api.post('/auth/change-password', { currentPassword, newPassword }),
};

export default api