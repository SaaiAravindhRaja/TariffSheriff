import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { authService } from '../authService';
import { api } from '../api';

// Mock the API service
vi.mock('../api', () => ({
  api: {
    post: vi.fn(),
    get: vi.fn(),
    defaults: {
      headers: {
        common: {},
      },
    },
  },
}));

// Mock token storage
vi.mock('@/lib/tokenStorage', () => ({
  tokenStorage: {
    getAccessToken: vi.fn(),
    getRefreshToken: vi.fn(),
    setTokens: vi.fn(),
    clearTokens: vi.fn(),
    isTokenExpired: vi.fn(),
  },
}));

import { tokenStorage } from '@/lib/tokenStorage';

const mockUser = {
  id: 1,
  email: 'test@example.com',
  name: 'Test User',
  role: 'USER' as const,
  status: 'ACTIVE' as const,
  emailVerified: true,
  createdAt: '2023-01-01T00:00:00Z',
  updatedAt: '2023-01-01T00:00:00Z',
};

const mockAuthResponse = {
  success: true,
  message: 'Login successful',
  accessToken: 'access-token-123',
  refreshToken: 'refresh-token-123',
  tokenType: 'Bearer',
  expiresIn: 900,
  user: mockUser,
};

const mockRegisterResponse = {
  success: true,
  message: 'Registration successful',
  user: mockUser,
};

describe('AuthService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Clear localStorage
    localStorage.clear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('login', () => {
    it('should login successfully and store tokens', async () => {
      vi.mocked(api.post).mockResolvedValue({ data: mockAuthResponse });

      const result = await authService.login({
        email: 'test@example.com',
        password: 'password123',
      });

      expect(api.post).toHaveBeenCalledWith('/auth/login', {
        email: 'test@example.com',
        password: 'password123',
      });

      expect(tokenStorage.setTokens).toHaveBeenCalledWith(
        'access-token-123',
        'refresh-token-123'
      );

      expect(result).toEqual({
        user: mockUser,
        accessToken: 'access-token-123',
        refreshToken: 'refresh-token-123',
        tokenType: 'Bearer',
        expiresIn: 900,
      });
    });

    it('should handle login failure', async () => {
      const errorResponse = {
        response: {
          data: {
            success: false,
            message: 'Invalid credentials',
          },
        },
      };

      vi.mocked(api.post).mockRejectedValue(errorResponse);

      await expect(authService.login({
        email: 'test@example.com',
        password: 'wrongpassword',
      })).rejects.toThrow('Invalid credentials');
    });

    it('should handle network errors', async () => {
      vi.mocked(api.post).mockRejectedValue(new Error('Network Error'));

      await expect(authService.login({
        email: 'test@example.com',
        password: 'password123',
      })).rejects.toThrow('Network Error');
    });
  });

  describe('register', () => {
    it('should register successfully', async () => {
      vi.mocked(api.post).mockResolvedValue({ data: mockRegisterResponse });

      const result = await authService.register({
        name: 'Test User',
        email: 'test@example.com',
        password: 'password123',
        role: 'USER',
      });

      expect(api.post).toHaveBeenCalledWith('/auth/register', {
        name: 'Test User',
        email: 'test@example.com',
        password: 'password123',
        role: 'USER',
      });

      expect(result).toEqual({
        user: mockUser,
        accessToken: null,
        refreshToken: null,
        tokenType: 'Bearer',
        expiresIn: 0,
      });
    });

    it('should handle registration failure', async () => {
      const errorResponse = {
        response: {
          data: {
            success: false,
            message: 'Email already exists',
          },
        },
      };

      vi.mocked(api.post).mockRejectedValue(errorResponse);

      await expect(authService.register({
        name: 'Test User',
        email: 'existing@example.com',
        password: 'password123',
        role: 'USER',
      })).rejects.toThrow('Email already exists');
    });
  });

  describe('logout', () => {
    it('should logout successfully and clear tokens', async () => {
      vi.mocked(tokenStorage.getAccessToken).mockReturnValue('access-token-123');
      vi.mocked(api.post).mockResolvedValue({ data: { success: true } });

      await authService.logout();

      expect(api.post).toHaveBeenCalledWith('/auth/logout');
      expect(tokenStorage.clearTokens).toHaveBeenCalled();
      expect(api.defaults.headers.common['Authorization']).toBeUndefined();
    });

    it('should clear tokens even if API call fails', async () => {
      vi.mocked(tokenStorage.getAccessToken).mockReturnValue('access-token-123');
      vi.mocked(api.post).mockRejectedValue(new Error('Network error'));

      await authService.logout();

      expect(tokenStorage.clearTokens).toHaveBeenCalled();
      expect(api.defaults.headers.common['Authorization']).toBeUndefined();
    });

    it('should handle logout when no tokens exist', async () => {
      vi.mocked(tokenStorage.getAccessToken).mockReturnValue(null);

      await authService.logout();

      expect(api.post).not.toHaveBeenCalled();
      expect(tokenStorage.clearTokens).toHaveBeenCalled();
    });
  });

  describe('refreshTokens', () => {
    it('should refresh tokens successfully', async () => {
      vi.mocked(tokenStorage.getRefreshToken).mockReturnValue('refresh-token-123');
      vi.mocked(api.post).mockResolvedValue({ data: mockAuthResponse });

      const result = await authService.refreshTokens();

      expect(api.post).toHaveBeenCalledWith('/auth/refresh', {
        refreshToken: 'refresh-token-123',
      });

      expect(tokenStorage.setTokens).toHaveBeenCalledWith(
        'access-token-123',
        'refresh-token-123'
      );

      expect(result).toEqual({
        user: mockUser,
        accessToken: 'access-token-123',
        refreshToken: 'refresh-token-123',
        tokenType: 'Bearer',
        expiresIn: 900,
      });
    });

    it('should handle refresh failure', async () => {
      vi.mocked(tokenStorage.getRefreshToken).mockReturnValue('invalid-refresh-token');
      vi.mocked(api.post).mockRejectedValue(new Error('Invalid refresh token'));

      await expect(authService.refreshTokens()).rejects.toThrow('Invalid refresh token');
      expect(tokenStorage.clearTokens).toHaveBeenCalled();
    });

    it('should throw error when no refresh token exists', async () => {
      vi.mocked(tokenStorage.getRefreshToken).mockReturnValue(null);

      await expect(authService.refreshTokens()).rejects.toThrow('No refresh token available');
    });
  });

  describe('getCurrentUser', () => {
    it('should get current user successfully', async () => {
      vi.mocked(api.get).mockResolvedValue({ data: mockUser });

      const result = await authService.getCurrentUser();

      expect(api.get).toHaveBeenCalledWith('/auth/me');
      expect(result).toEqual(mockUser);
    });

    it('should handle get user failure', async () => {
      vi.mocked(api.get).mockRejectedValue(new Error('Unauthorized'));

      await expect(authService.getCurrentUser()).rejects.toThrow('Unauthorized');
    });
  });

  describe('forgotPassword', () => {
    it('should send forgot password request successfully', async () => {
      const response = { success: true, message: 'Reset link sent' };
      vi.mocked(api.post).mockResolvedValue({ data: response });

      const result = await authService.forgotPassword('test@example.com');

      expect(api.post).toHaveBeenCalledWith('/auth/forgot-password', {
        email: 'test@example.com',
      });

      expect(result).toEqual(response);
    });
  });

  describe('resetPassword', () => {
    it('should reset password successfully', async () => {
      const response = { success: true, message: 'Password reset successfully' };
      vi.mocked(api.post).mockResolvedValue({ data: response });

      const result = await authService.resetPassword('reset-token', 'newpassword123');

      expect(api.post).toHaveBeenCalledWith('/auth/reset-password', {
        token: 'reset-token',
        newPassword: 'newpassword123',
      });

      expect(result).toEqual(response);
    });
  });

  describe('changePassword', () => {
    it('should change password successfully', async () => {
      const response = { success: true, message: 'Password changed successfully' };
      vi.mocked(api.post).mockResolvedValue({ data: response });

      const result = await authService.changePassword('currentpassword', 'newpassword123');

      expect(api.post).toHaveBeenCalledWith('/auth/change-password', {
        currentPassword: 'currentpassword',
        newPassword: 'newpassword123',
      });

      expect(result).toEqual(response);
    });
  });

  describe('verifyEmail', () => {
    it('should verify email successfully', async () => {
      const response = { success: true, message: 'Email verified successfully' };
      vi.mocked(api.get).mockResolvedValue({ data: response });

      const result = await authService.verifyEmail('verification-token');

      expect(api.get).toHaveBeenCalledWith('/auth/verify?token=verification-token');
      expect(result).toEqual(response);
    });
  });

  describe('isAuthenticated', () => {
    it('should return true when access token exists and is not expired', () => {
      vi.mocked(tokenStorage.getAccessToken).mockReturnValue('access-token-123');
      vi.mocked(tokenStorage.isTokenExpired).mockReturnValue(false);

      expect(authService.isAuthenticated()).toBe(true);
    });

    it('should return false when no access token exists', () => {
      vi.mocked(tokenStorage.getAccessToken).mockReturnValue(null);

      expect(authService.isAuthenticated()).toBe(false);
    });

    it('should return false when access token is expired', () => {
      vi.mocked(tokenStorage.getAccessToken).mockReturnValue('access-token-123');
      vi.mocked(tokenStorage.isTokenExpired).mockReturnValue(true);

      expect(authService.isAuthenticated()).toBe(false);
    });
  });

  describe('shouldRefreshToken', () => {
    it('should return true when access token is expired but refresh token exists', () => {
      vi.mocked(tokenStorage.getAccessToken).mockReturnValue('access-token-123');
      vi.mocked(tokenStorage.isTokenExpired).mockReturnValue(true);
      vi.mocked(tokenStorage.getRefreshToken).mockReturnValue('refresh-token-123');

      expect(authService.shouldRefreshToken()).toBe(true);
    });

    it('should return false when access token is not expired', () => {
      vi.mocked(tokenStorage.getAccessToken).mockReturnValue('access-token-123');
      vi.mocked(tokenStorage.isTokenExpired).mockReturnValue(false);

      expect(authService.shouldRefreshToken()).toBe(false);
    });

    it('should return false when no refresh token exists', () => {
      vi.mocked(tokenStorage.getAccessToken).mockReturnValue('access-token-123');
      vi.mocked(tokenStorage.isTokenExpired).mockReturnValue(true);
      vi.mocked(tokenStorage.getRefreshToken).mockReturnValue(null);

      expect(authService.shouldRefreshToken()).toBe(false);
    });
  });

  describe('clearAuth', () => {
    it('should clear all authentication data', () => {
      authService.clearAuth();

      expect(tokenStorage.clearTokens).toHaveBeenCalled();
      expect(api.defaults.headers.common['Authorization']).toBeUndefined();
    });
  });

  describe('setAuthHeader', () => {
    it('should set authorization header with token', () => {
      authService.setAuthHeader('access-token-123');

      expect(api.defaults.headers.common['Authorization']).toBe('Bearer access-token-123');
    });

    it('should remove authorization header when token is null', () => {
      authService.setAuthHeader(null);

      expect(api.defaults.headers.common['Authorization']).toBeUndefined();
    });
  });

  describe('error handling', () => {
    it('should handle API errors with custom messages', async () => {
      const errorResponse = {
        response: {
          status: 400,
          data: {
            success: false,
            message: 'Custom error message',
            validationErrors: {
              email: 'Invalid email format',
            },
          },
        },
      };

      vi.mocked(api.post).mockRejectedValue(errorResponse);

      await expect(authService.login({
        email: 'invalid-email',
        password: 'password123',
      })).rejects.toThrow('Custom error message');
    });

    it('should handle network errors gracefully', async () => {
      vi.mocked(api.post).mockRejectedValue(new Error('Network Error'));

      await expect(authService.login({
        email: 'test@example.com',
        password: 'password123',
      })).rejects.toThrow('Network Error');
    });

    it('should handle 401 errors by clearing tokens', async () => {
      const errorResponse = {
        response: {
          status: 401,
          data: {
            success: false,
            message: 'Unauthorized',
          },
        },
      };

      vi.mocked(api.get).mockRejectedValue(errorResponse);

      await expect(authService.getCurrentUser()).rejects.toThrow('Unauthorized');
    });

    it('should handle 403 errors appropriately', async () => {
      const errorResponse = {
        response: {
          status: 403,
          data: {
            success: false,
            message: 'Access denied',
          },
        },
      };

      vi.mocked(api.get).mockRejectedValue(errorResponse);

      await expect(authService.getCurrentUser()).rejects.toThrow('Access denied');
    });

    it('should handle 429 rate limit errors', async () => {
      const errorResponse = {
        response: {
          status: 429,
          data: {
            success: false,
            message: 'Too many requests',
          },
          headers: {
            'retry-after': '60',
          },
        },
      };

      vi.mocked(api.post).mockRejectedValue(errorResponse);

      await expect(authService.login({
        email: 'test@example.com',
        password: 'password123',
      })).rejects.toThrow('Too many requests');
    });

    it('should handle 500 server errors', async () => {
      const errorResponse = {
        response: {
          status: 500,
          data: {
            success: false,
            message: 'Internal server error',
          },
        },
      };

      vi.mocked(api.post).mockRejectedValue(errorResponse);

      await expect(authService.login({
        email: 'test@example.com',
        password: 'password123',
      })).rejects.toThrow('Internal server error');
    });
  });

  describe('token management', () => {
    it('should automatically set auth header after successful login', async () => {
      vi.mocked(api.post).mockResolvedValue({ data: mockAuthResponse });

      await authService.login({
        email: 'test@example.com',
        password: 'password123',
      });

      expect(api.defaults.headers.common['Authorization']).toBe('Bearer access-token-123');
    });

    it('should automatically set auth header after successful token refresh', async () => {
      vi.mocked(tokenStorage.getRefreshToken).mockReturnValue('refresh-token-123');
      vi.mocked(api.post).mockResolvedValue({ data: mockAuthResponse });

      await authService.refreshTokens();

      expect(api.defaults.headers.common['Authorization']).toBe('Bearer access-token-123');
    });

    it('should clear auth header after logout', async () => {
      vi.mocked(tokenStorage.getAccessToken).mockReturnValue('access-token-123');
      vi.mocked(api.post).mockResolvedValue({ data: { success: true } });

      await authService.logout();

      expect(api.defaults.headers.common['Authorization']).toBeUndefined();
    });
  });
});