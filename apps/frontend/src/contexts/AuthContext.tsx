import React, { createContext, useContext, useState, useEffect, ReactNode, useCallback } from 'react';
import { User, AuthState, AuthContextType, RegisterRequest } from '@/types/auth';
import { authService } from '@/services/authService';

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  // TEMPORARY: Mock authenticated state - remove this block to re-enable auth
  const [state, setState] = useState<AuthState>({
    user: {
      id: 'temp-user',
      email: 'demo@example.com',
      name: 'Demo User',
      role: 'USER' as any,
      emailVerified: true,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    },
    isAuthenticated: true,
    isLoading: false,
    error: null,
  });

  /* COMMENTED OUT - ORIGINAL STATE (uncomment to re-enable)
  const [state, setState] = useState<AuthState>({
    user: null,
    isAuthenticated: false,
    isLoading: true,
    error: null,
  });
  */

  const clearError = useCallback(() => {
    setState(prev => ({ ...prev, error: null }));
  }, []);

  const setLoading = useCallback((isLoading: boolean) => {
    setState(prev => ({ ...prev, isLoading }));
  }, []);

  const setError = useCallback((error: string) => {
    setState(prev => ({ ...prev, error, isLoading: false }));
  }, []);

  const setAuthenticated = useCallback((user: User) => {
    setState({
      user,
      isAuthenticated: true,
      isLoading: false,
      error: null,
    });
  }, []);

  const setUnauthenticated = useCallback(() => {
    setState({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,
    });
    authService.clearAuth();
  }, []);

  const refreshToken = useCallback(async (): Promise<void> => {
    try {
      const authData = await authService.refreshTokens();
      setAuthenticated(authData.user);
    } catch (error) {
      console.error('Token refresh failed:', error);
      setUnauthenticated();
      throw error;
    }
  }, [setAuthenticated, setUnauthenticated]);

  const login = useCallback(async (email: string, password: string): Promise<void> => {
    try {
      setLoading(true);
      clearError();

      const authData = await authService.login({ email, password });
      setAuthenticated(authData.user);
    } catch (error: any) {
      const errorMessage = error.message || 'Login failed. Please try again.';
      setError(errorMessage);
      throw error;
    }
  }, [setLoading, clearError, setAuthenticated, setError]);

  const register = useCallback(async (data: RegisterRequest): Promise<void> => {
    try {
      setLoading(true);
      clearError();

      const authData = await authService.register(data);
      setAuthenticated(authData.user);
    } catch (error: any) {
      const errorMessage = error.message || 'Registration failed. Please try again.';
      setError(errorMessage);
      throw error;
    }
  }, [setLoading, clearError, setAuthenticated, setError]);

  const logout = useCallback(async (): Promise<void> => {
    try {
      await authService.logout();
    } catch (error) {
      console.warn('Logout error:', error);
    } finally {
      setUnauthenticated();
    }
  }, [setUnauthenticated]);

  const checkAuthStatus = useCallback(async (): Promise<void> => {
    try {
      setLoading(true);

      // Check if we have valid tokens
      if (authService.isAuthenticated()) {
        // Try to get user profile with current token
        const user = await authService.getCurrentUser();
        setAuthenticated(user);
        return;
      }

      // If access token is expired but we have refresh token, try to refresh
      if (authService.shouldRefreshToken()) {
        await refreshToken();
        return;
      }

      // No valid tokens, user is not authenticated
      setUnauthenticated();
    } catch (error) {
      console.error('Auth status check failed:', error);
      setUnauthenticated();
    }
  }, [setLoading, setAuthenticated, setUnauthenticated, refreshToken]);

  // TEMPORARY: Authentication check disabled
  /* COMMENTED OUT - AUTH CHECK (uncomment to re-enable)
  // Check authentication status on mount
  useEffect(() => {
    checkAuthStatus();
  }, [checkAuthStatus]);
  */

  // TEMPORARY: Token refresh disabled
  /* COMMENTED OUT - TOKEN REFRESH (uncomment to re-enable)
  // Set up automatic token refresh
  useEffect(() => {
    if (!state.isAuthenticated) return;

    const checkTokenExpiration = () => {
      if (authService.shouldRefreshToken()) {
        refreshToken().catch(() => {
          // Token refresh failed, user will be logged out
        });
      }
    };

    // Check token expiration every minute
    const interval = setInterval(checkTokenExpiration, 60 * 1000);

    return () => clearInterval(interval);
  }, [state.isAuthenticated, refreshToken]);
  */

  // Listen for logout events from the API interceptor and other API errors
  useEffect(() => {
    const handleLogout = () => {
      setUnauthenticated();
    };

    const handleForbidden = (event: CustomEvent) => {
      const { message, requiredRole } = event.detail;
      console.warn('Access forbidden:', message);
      
      if (requiredRole) {
        console.info(`Required role: ${requiredRole}, Current role: ${state.user?.role}`);
      }
    };

    const handleRateLimit = (event: CustomEvent) => {
      const { message, retryAfter } = event.detail;
      console.warn('Rate limit exceeded:', message);
      
      if (retryAfter) {
        console.info(`Please try again in ${retryAfter} seconds`);
      }
    };

    const handleServerError = (event: CustomEvent) => {
      const { message, status } = event.detail;
      console.error('Server error:', message, status);
    };

    window.addEventListener('auth:logout', handleLogout);
    window.addEventListener('auth:forbidden', handleForbidden as EventListener);
    window.addEventListener('api:rateLimit', handleRateLimit as EventListener);
    window.addEventListener('api:serverError', handleServerError as EventListener);
    
    return () => {
      window.removeEventListener('auth:logout', handleLogout);
      window.removeEventListener('auth:forbidden', handleForbidden as EventListener);
      window.removeEventListener('api:rateLimit', handleRateLimit as EventListener);
      window.removeEventListener('api:serverError', handleServerError as EventListener);
    };
  }, [setUnauthenticated, state.user]);

  const contextValue: AuthContextType = {
    ...state,
    login,
    register,
    logout,
    refreshToken,
    clearError,
    checkAuthStatus,
  };

  return (
    <AuthContext.Provider value={contextValue}>
      {children}
    </AuthContext.Provider>
  );
};