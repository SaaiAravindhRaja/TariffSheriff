import { authApi } from './api';
import { tokenStorage } from '@/lib/tokenStorage';
import { LoginRequest, RegisterRequest, AuthResponse, User } from '@/types/auth';

export class AuthService {
  /**
   * Login user with email and password
   */
  async login(credentials: LoginRequest): Promise<AuthResponse> {
    try {
      const response = await authApi.login(credentials);
      const authData: AuthResponse = response.data;
      
      // Store tokens
      tokenStorage.setTokens(
        authData.accessToken,
        authData.refreshToken,
        authData.expiresIn
      );
      
      return authData;
    } catch (error: any) {
      const message = error.response?.data?.message || 'Login failed';
      throw new Error(message);
    }
  }

  /**
   * Register new user
   */
  async register(userData: RegisterRequest): Promise<AuthResponse> {
    try {
      const response = await authApi.register(userData);
      const authData: AuthResponse = response.data;
      
      // Store tokens
      tokenStorage.setTokens(
        authData.accessToken,
        authData.refreshToken,
        authData.expiresIn
      );
      
      return authData;
    } catch (error: any) {
      const message = error.response?.data?.message || 'Registration failed';
      throw new Error(message);
    }
  }

  /**
   * Logout user
   */
  async logout(): Promise<void> {
    try {
      // Attempt server logout (best effort)
      await authApi.logout();
    } catch (error) {
      console.warn('Server logout failed:', error);
    } finally {
      // Always clear local tokens
      tokenStorage.clearTokens();
    }
  }

  /**
   * Refresh authentication tokens
   */
  async refreshTokens(): Promise<AuthResponse> {
    try {
      const response = await authApi.refreshToken();
      const authData: AuthResponse = response.data;
      
      // Update stored tokens
      tokenStorage.setTokens(
        authData.accessToken,
        authData.refreshToken,
        authData.expiresIn
      );
      
      return authData;
    } catch (error: any) {
      // Clear tokens on refresh failure
      tokenStorage.clearTokens();
      const message = error.response?.data?.message || 'Token refresh failed';
      throw new Error(message);
    }
  }

  /**
   * Get current user profile
   */
  async getCurrentUser(): Promise<User> {
    try {
      const response = await authApi.getProfile();
      return response.data;
    } catch (error: any) {
      const message = error.response?.data?.message || 'Failed to get user profile';
      throw new Error(message);
    }
  }

  /**
   * Verify email with token
   */
  async verifyEmail(token: string): Promise<void> {
    try {
      await authApi.verifyEmail(token);
    } catch (error: any) {
      const message = error.response?.data?.message || 'Email verification failed';
      throw new Error(message);
    }
  }

  /**
   * Request password reset
   */
  async forgotPassword(email: string): Promise<void> {
    try {
      await authApi.forgotPassword(email);
    } catch (error: any) {
      const message = error.response?.data?.message || 'Password reset request failed';
      throw new Error(message);
    }
  }

  /**
   * Reset password with token
   */
  async resetPassword(token: string, password: string): Promise<void> {
    try {
      await authApi.resetPassword(token, password);
    } catch (error: any) {
      const message = error.response?.data?.message || 'Password reset failed';
      throw new Error(message);
    }
  }

  /**
   * Change password for authenticated user
   */
  async changePassword(currentPassword: string, newPassword: string): Promise<void> {
    try {
      await authApi.changePassword(currentPassword, newPassword);
    } catch (error: any) {
      const message = error.response?.data?.message || 'Password change failed';
      throw new Error(message);
    }
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    return tokenStorage.hasValidTokens();
  }

  /**
   * Check if tokens need refresh
   */
  shouldRefreshToken(): boolean {
    return tokenStorage.shouldRefreshToken();
  }

  /**
   * Get current access token
   */
  getAccessToken(): string | null {
    return tokenStorage.getAccessToken();
  }

  /**
   * Clear all authentication data
   */
  clearAuth(): void {
    tokenStorage.clearTokens();
  }
}

// Export singleton instance
export const authService = new AuthService();
export default authService;