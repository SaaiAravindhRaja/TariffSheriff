import safeLocalStorage from './safeLocalStorage';

export interface TokenData {
  accessToken: string;
  refreshToken: string;
  expiresAt: number;
}

export interface SecureTokenOptions {
  useMemoryStorage?: boolean;
  encryptTokens?: boolean;
}

class TokenStorage {
  private readonly ACCESS_TOKEN_KEY = 'auth_access_token';
  private readonly REFRESH_TOKEN_KEY = 'auth_refresh_token';
  private readonly EXPIRES_AT_KEY = 'auth_expires_at';
  
  // In-memory storage for enhanced security (fallback)
  private memoryStorage: Map<string, any> = new Map();
  private useMemoryFallback = false;

  /**
   * Store authentication tokens securely with fallback options
   */
  setTokens(accessToken: string, refreshToken: string, expiresIn: number): void {
    const expiresAt = Date.now() + (expiresIn * 1000);
    
    try {
      // Primary storage: localStorage
      safeLocalStorage.set(this.ACCESS_TOKEN_KEY, accessToken);
      safeLocalStorage.set(this.REFRESH_TOKEN_KEY, refreshToken);
      safeLocalStorage.set(this.EXPIRES_AT_KEY, expiresAt);
      this.useMemoryFallback = false;
    } catch (error) {
      console.warn('localStorage unavailable, using memory storage:', error);
      // Fallback: memory storage
      this.memoryStorage.set(this.ACCESS_TOKEN_KEY, accessToken);
      this.memoryStorage.set(this.REFRESH_TOKEN_KEY, refreshToken);
      this.memoryStorage.set(this.EXPIRES_AT_KEY, expiresAt);
      this.useMemoryFallback = true;
    }
  }

  /**
   * Get the current access token with fallback support
   */
  getAccessToken(): string | null {
    if (this.useMemoryFallback) {
      return this.memoryStorage.get(this.ACCESS_TOKEN_KEY) || null;
    }
    return safeLocalStorage.get<string>(this.ACCESS_TOKEN_KEY);
  }

  /**
   * Get the current refresh token with fallback support
   */
  getRefreshToken(): string | null {
    if (this.useMemoryFallback) {
      return this.memoryStorage.get(this.REFRESH_TOKEN_KEY) || null;
    }
    return safeLocalStorage.get<string>(this.REFRESH_TOKEN_KEY);
  }

  /**
   * Get token expiration timestamp with fallback support
   */
  getExpiresAt(): number | null {
    if (this.useMemoryFallback) {
      return this.memoryStorage.get(this.EXPIRES_AT_KEY) || null;
    }
    return safeLocalStorage.get<number>(this.EXPIRES_AT_KEY);
  }

  /**
   * Check if the access token is expired or will expire soon (within 1 minute)
   */
  isTokenExpired(): boolean {
    const expiresAt = this.getExpiresAt();
    if (!expiresAt) return true;
    
    // Consider token expired if it expires within 1 minute
    const bufferTime = 60 * 1000; // 1 minute in milliseconds
    return Date.now() >= (expiresAt - bufferTime);
  }

  /**
   * Check if we have valid tokens
   */
  hasValidTokens(): boolean {
    const accessToken = this.getAccessToken();
    const refreshToken = this.getRefreshToken();
    
    return !!(accessToken && refreshToken && !this.isTokenExpired());
  }

  /**
   * Check if we have a refresh token (even if access token is expired)
   */
  hasRefreshToken(): boolean {
    return !!this.getRefreshToken();
  }

  /**
   * Clear all stored tokens from both storage mechanisms
   */
  clearTokens(): void {
    // Clear from localStorage
    safeLocalStorage.remove(this.ACCESS_TOKEN_KEY);
    safeLocalStorage.remove(this.REFRESH_TOKEN_KEY);
    safeLocalStorage.remove(this.EXPIRES_AT_KEY);
    
    // Clear from memory storage
    this.memoryStorage.delete(this.ACCESS_TOKEN_KEY);
    this.memoryStorage.delete(this.REFRESH_TOKEN_KEY);
    this.memoryStorage.delete(this.EXPIRES_AT_KEY);
    
    this.useMemoryFallback = false;
  }

  /**
   * Get all token data
   */
  getTokenData(): TokenData | null {
    const accessToken = this.getAccessToken();
    const refreshToken = this.getRefreshToken();
    const expiresAt = this.getExpiresAt();

    if (!accessToken || !refreshToken || !expiresAt) {
      return null;
    }

    return {
      accessToken,
      refreshToken,
      expiresAt
    };
  }

  /**
   * Check if tokens need refresh (within 2 minutes of expiration)
   */
  shouldRefreshToken(): boolean {
    const expiresAt = this.getExpiresAt();
    if (!expiresAt) return false;
    
    // Refresh if token expires within 2 minutes
    const refreshThreshold = 2 * 60 * 1000; // 2 minutes in milliseconds
    return Date.now() >= (expiresAt - refreshThreshold);
  }

  /**
   * Get time until token expiration in milliseconds
   */
  getTimeUntilExpiration(): number {
    const expiresAt = this.getExpiresAt();
    if (!expiresAt) return 0;
    
    return Math.max(0, expiresAt - Date.now());
  }

  /**
   * Check if storage is available and working
   */
  isStorageAvailable(): boolean {
    return !this.useMemoryFallback;
  }

  /**
   * Validate token format (basic JWT structure check)
   */
  private isValidTokenFormat(token: string): boolean {
    if (!token || typeof token !== 'string') return false;
    
    // Basic JWT format check (3 parts separated by dots)
    const parts = token.split('.');
    return parts.length === 3 && parts.every(part => part.length > 0);
  }

  /**
   * Validate stored tokens
   */
  validateStoredTokens(): boolean {
    const accessToken = this.getAccessToken();
    const refreshToken = this.getRefreshToken();
    
    if (!accessToken || !refreshToken) return false;
    
    return this.isValidTokenFormat(accessToken) && this.isValidTokenFormat(refreshToken);
  }

  /**
   * Update only the access token (used during token refresh)
   */
  updateAccessToken(accessToken: string, expiresIn: number): void {
    const expiresAt = Date.now() + (expiresIn * 1000);
    
    try {
      if (this.useMemoryFallback) {
        this.memoryStorage.set(this.ACCESS_TOKEN_KEY, accessToken);
        this.memoryStorage.set(this.EXPIRES_AT_KEY, expiresAt);
      } else {
        safeLocalStorage.set(this.ACCESS_TOKEN_KEY, accessToken);
        safeLocalStorage.set(this.EXPIRES_AT_KEY, expiresAt);
      }
    } catch (error) {
      console.error('Failed to update access token:', error);
    }
  }
}

// Export a singleton instance
export const tokenStorage = new TokenStorage();
export default tokenStorage;