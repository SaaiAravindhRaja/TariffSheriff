import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from '@/contexts/AuthContext';
import { LoginForm } from '@/components/auth/LoginForm';
import { RegisterForm } from '@/components/auth/RegisterForm';
import { ProtectedRoute } from '@/components/auth/ProtectedRoute';
import { authService } from '@/services/authService';

// Mock the auth service
vi.mock('@/services/authService', () => ({
  authService: {
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    refreshTokens: vi.fn(),
    getCurrentUser: vi.fn(),
    isAuthenticated: vi.fn(),
    shouldRefreshToken: vi.fn(),
    clearAuth: vi.fn(),
    forgotPassword: vi.fn(),
    resetPassword: vi.fn(),
    changePassword: vi.fn(),
    verifyEmail: vi.fn(),
    setAuthHeader: vi.fn(),
  },
}));

// Mock react-router-dom Navigate component
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    Navigate: ({ to }: { to: string }) => <div data-testid="navigate-to">{to}</div>,
    useNavigate: () => vi.fn(),
  };
});

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

const mockAuthData = {
  user: mockUser,
  accessToken: 'access-token-123',
  refreshToken: 'refresh-token-123',
  tokenType: 'Bearer',
  expiresIn: 900,
};

const TestApp = ({ initialRoute = '/login' }: { initialRoute?: string }) => {
  return (
    <MemoryRouter initialEntries={[initialRoute]}>
      <AuthProvider>
        <div>
          {initialRoute === '/login' && <LoginForm />}
          {initialRoute === '/register' && <RegisterForm />}
          {initialRoute === '/protected' && (
            <ProtectedRoute>
              <div data-testid="protected-content">Protected Content</div>
            </ProtectedRoute>
          )}
        </div>
      </AuthProvider>
    </MemoryRouter>
  );
};

describe('Authentication E2E Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Mock console methods to avoid noise in tests
    vi.spyOn(console, 'error').mockImplementation(() => {});
    vi.spyOn(console, 'warn').mockImplementation(() => {});
    vi.spyOn(console, 'info').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('Complete Login Flow', () => {
    it('should complete successful login flow', async () => {
      const user = userEvent.setup();
      
      // Mock initial unauthenticated state
      vi.mocked(authService.isAuthenticated).mockReturnValue(false);
      vi.mocked(authService.shouldRefreshToken).mockReturnValue(false);
      vi.mocked(authService.login).mockResolvedValue(mockAuthData);

      render(<TestApp initialRoute="/login" />);

      // Wait for form to load
      await waitFor(() => {
        expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
      });

      // Fill and submit login form
      const emailInput = screen.getByLabelText(/email/i);
      const passwordInput = screen.getByLabelText(/password/i);
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(emailInput, 'test@example.com');
      await user.type(passwordInput, 'password123');
      await user.click(submitButton);

      // Verify login was called
      await waitFor(() => {
        expect(authService.login).toHaveBeenCalledWith({
          email: 'test@example.com',
          password: 'password123',
        });
      });

      // Verify success state (would typically redirect in real app)
      expect(screen.queryByText(/invalid credentials/i)).not.toBeInTheDocument();
    });

    it('should handle login failure gracefully', async () => {
      const user = userEvent.setup();
      
      vi.mocked(authService.isAuthenticated).mockReturnValue(false);
      vi.mocked(authService.shouldRefreshToken).mockReturnValue(false);
      vi.mocked(authService.login).mockRejectedValue(new Error('Invalid credentials'));

      render(<TestApp initialRoute="/login" />);

      await waitFor(() => {
        expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
      });

      const emailInput = screen.getByLabelText(/email/i);
      const passwordInput = screen.getByLabelText(/password/i);
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(emailInput, 'test@example.com');
      await user.type(passwordInput, 'wrongpassword');
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/invalid credentials/i)).toBeInTheDocument();
      });
    });

    it('should show loading state during login', async () => {
      const user = userEvent.setup();
      
      vi.mocked(authService.isAuthenticated).mockReturnValue(false);
      vi.mocked(authService.shouldRefreshToken).mockReturnValue(false);
      vi.mocked(authService.login).mockImplementation(() => 
        new Promise(resolve => setTimeout(() => resolve(mockAuthData), 100))
      );

      render(<TestApp initialRoute="/login" />);

      await waitFor(() => {
        expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
      });

      const emailInput = screen.getByLabelText(/email/i);
      const passwordInput = screen.getByLabelText(/password/i);
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(emailInput, 'test@example.com');
      await user.type(passwordInput, 'password123');
      await user.click(submitButton);

      // Check loading state
      expect(screen.getByText(/signing in/i)).toBeInTheDocument();
      expect(submitButton).toBeDisabled();

      // Wait for completion
      await waitFor(() => {
        expect(screen.queryByText(/signing in/i)).not.toBeInTheDocument();
      });
    });
  });

  describe('Complete Registration Flow', () => {
    it('should complete successful registration flow', async () => {
      const user = userEvent.setup();
      
      vi.mocked(authService.isAuthenticated).mockReturnValue(false);
      vi.mocked(authService.shouldRefreshToken).mockReturnValue(false);
      vi.mocked(authService.register).mockResolvedValue({
        user: { ...mockUser, status: 'PENDING', emailVerified: false },
        accessToken: null,
        refreshToken: null,
        tokenType: 'Bearer',
        expiresIn: 0,
      });

      render(<TestApp initialRoute="/register" />);

      await waitFor(() => {
        expect(screen.getByLabelText(/full name/i)).toBeInTheDocument();
      });

      // Fill registration form
      const nameInput = screen.getByLabelText(/full name/i);
      const emailInput = screen.getByLabelText(/email/i);
      const passwordInput = screen.getByLabelText(/^password$/i);
      const confirmPasswordInput = screen.getByLabelText(/confirm password/i);
      const termsCheckbox = screen.getByLabelText(/terms and conditions/i);
      const submitButton = screen.getByRole('button', { name: /create account/i });

      await user.type(nameInput, 'Test User');
      await user.type(emailInput, 'test@example.com');
      await user.type(passwordInput, 'Password123!');
      await user.type(confirmPasswordInput, 'Password123!');
      await user.click(termsCheckbox);
      await user.click(submitButton);

      await waitFor(() => {
        expect(authService.register).toHaveBeenCalledWith({
          name: 'Test User',
          email: 'test@example.com',
          password: 'Password123!',
          role: 'USER',
        });
      });
    });

    it('should validate all registration fields', async () => {
      const user = userEvent.setup();
      
      vi.mocked(authService.isAuthenticated).mockReturnValue(false);
      vi.mocked(authService.shouldRefreshToken).mockReturnValue(false);

      render(<TestApp initialRoute="/register" />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /create account/i })).toBeInTheDocument();
      });

      const submitButton = screen.getByRole('button', { name: /create account/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/name is required/i)).toBeInTheDocument();
        expect(screen.getByText(/email is required/i)).toBeInTheDocument();
        expect(screen.getByText(/password is required/i)).toBeInTheDocument();
      });
    });

    it('should validate password confirmation', async () => {
      const user = userEvent.setup();
      
      vi.mocked(authService.isAuthenticated).mockReturnValue(false);
      vi.mocked(authService.shouldRefreshToken).mockReturnValue(false);

      render(<TestApp initialRoute="/register" />);

      await waitFor(() => {
        expect(screen.getByLabelText(/^password$/i)).toBeInTheDocument();
      });

      const passwordInput = screen.getByLabelText(/^password$/i);
      const confirmPasswordInput = screen.getByLabelText(/confirm password/i);
      const submitButton = screen.getByRole('button', { name: /create account/i });

      await user.type(passwordInput, 'Password123!');
      await user.type(confirmPasswordInput, 'DifferentPassword123!');
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/passwords do not match/i)).toBeInTheDocument();
      });
    });
  });

  describe('Protected Route Access', () => {
    it('should redirect unauthenticated users to login', async () => {
      vi.mocked(authService.isAuthenticated).mockReturnValue(false);
      vi.mocked(authService.shouldRefreshToken).mockReturnValue(false);

      render(<TestApp initialRoute="/protected" />);

      await waitFor(() => {
        expect(screen.getByTestId('navigate-to')).toHaveTextContent('/login');
      });
    });

    it('should allow authenticated users to access protected content', async () => {
      vi.mocked(authService.isAuthenticated).mockReturnValue(true);
      vi.mocked(authService.getCurrentUser).mockResolvedValue(mockUser);

      render(<TestApp initialRoute="/protected" />);

      await waitFor(() => {
        expect(screen.getByTestId('protected-content')).toBeInTheDocument();
      });
    });

    it('should attempt token refresh for expired tokens', async () => {
      vi.mocked(authService.isAuthenticated).mockReturnValue(false);
      vi.mocked(authService.shouldRefreshToken).mockReturnValue(true);
      vi.mocked(authService.refreshTokens).mockResolvedValue(mockAuthData);

      render(<TestApp initialRoute="/protected" />);

      await waitFor(() => {
        expect(screen.getByTestId('protected-content')).toBeInTheDocument();
      });

      expect(authService.refreshTokens).toHaveBeenCalled();
    });

    it('should redirect to login if token refresh fails', async () => {
      vi.mocked(authService.isAuthenticated).mockReturnValue(false);
      vi.mocked(authService.shouldRefreshToken).mockReturnValue(true);
      vi.mocked(authService.refreshTokens).mockRejectedValue(new Error('Refresh failed'));

      render(<TestApp initialRoute="/protected" />);

      await waitFor(() => {
        expect(screen.getByTestId('navigate-to')).toHaveTextContent('/login');
      });
    });
  });

  describe('Authentication State Management', () => {
    it('should maintain authentication state across components', async () => {
      const user = userEvent.setup();
      
      // Start unauthenticated
      vi.mocked(authService.isAuthenticated).mockReturnValue(false);
      vi.mocked(authService.shouldRefreshToken).mockReturnValue(false);
      vi.mocked(authService.login).mockResolvedValue(mockAuthData);

      const { rerender } = render(<TestApp initialRoute="/login" />);

      await waitFor(() => {
        expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
      });

      // Login
      const emailInput = screen.getByLabelText(/email/i);
      const passwordInput = screen.getByLabelText(/password/i);
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(emailInput, 'test@example.com');
      await user.type(passwordInput, 'password123');
      await user.click(submitButton);

      await waitFor(() => {
        expect(authService.login).toHaveBeenCalled();
      });

      // Simulate navigation to protected route after login
      vi.mocked(authService.isAuthenticated).mockReturnValue(true);
      vi.mocked(authService.getCurrentUser).mockResolvedValue(mockUser);

      rerender(<TestApp initialRoute="/protected" />);

      await waitFor(() => {
        expect(screen.getByTestId('protected-content')).toBeInTheDocument();
      });
    });

    it('should handle authentication errors gracefully', async () => {
      vi.mocked(authService.isAuthenticated).mockReturnValue(true);
      vi.mocked(authService.getCurrentUser).mockRejectedValue(new Error('Token expired'));

      render(<TestApp initialRoute="/protected" />);

      await waitFor(() => {
        expect(screen.getByTestId('navigate-to')).toHaveTextContent('/login');
      });
    });

    it('should clear authentication state on logout', async () => {
      // Start authenticated
      vi.mocked(authService.isAuthenticated).mockReturnValue(true);
      vi.mocked(authService.getCurrentUser).mockResolvedValue(mockUser);

      const { rerender } = render(<TestApp initialRoute="/protected" />);

      await waitFor(() => {
        expect(screen.getByTestId('protected-content')).toBeInTheDocument();
      });

      // Simulate logout event
      vi.mocked(authService.isAuthenticated).mockReturnValue(false);
      vi.mocked(authService.shouldRefreshToken).mockReturnValue(false);

      // Trigger logout event
      window.dispatchEvent(new CustomEvent('auth:logout'));

      // Rerender to simulate route change
      rerender(<TestApp initialRoute="/login" />);

      await waitFor(() => {
        expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
      });
    });
  });

  describe('Form Validation and UX', () => {
    it('should provide real-time validation feedback', async () => {
      const user = userEvent.setup();
      
      vi.mocked(authService.isAuthenticated).mockReturnValue(false);
      vi.mocked(authService.shouldRefreshToken).mockReturnValue(false);

      render(<TestApp initialRoute="/login" />);

      await waitFor(() => {
        expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
      });

      const emailInput = screen.getByLabelText(/email/i);
      
      // Type invalid email
      await user.type(emailInput, 'invalid-email');
      await user.tab(); // Trigger blur event

      await waitFor(() => {
        expect(screen.getByText(/please enter a valid email address/i)).toBeInTheDocument();
      });

      // Fix email
      await user.clear(emailInput);
      await user.type(emailInput, 'valid@example.com');

      await waitFor(() => {
        expect(screen.queryByText(/please enter a valid email address/i)).not.toBeInTheDocument();
      });
    });

    it('should handle form submission with keyboard', async () => {
      const user = userEvent.setup();
      
      vi.mocked(authService.isAuthenticated).mockReturnValue(false);
      vi.mocked(authService.shouldRefreshToken).mockReturnValue(false);
      vi.mocked(authService.login).mockResolvedValue(mockAuthData);

      render(<TestApp initialRoute="/login" />);

      await waitFor(() => {
        expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
      });

      const emailInput = screen.getByLabelText(/email/i);
      const passwordInput = screen.getByLabelText(/password/i);

      await user.type(emailInput, 'test@example.com');
      await user.type(passwordInput, 'password123');
      await user.keyboard('{Enter}');

      await waitFor(() => {
        expect(authService.login).toHaveBeenCalledWith({
          email: 'test@example.com',
          password: 'password123',
        });
      });
    });

    it('should disable form during submission', async () => {
      const user = userEvent.setup();
      
      vi.mocked(authService.isAuthenticated).mockReturnValue(false);
      vi.mocked(authService.shouldRefreshToken).mockReturnValue(false);
      vi.mocked(authService.login).mockImplementation(() => 
        new Promise(resolve => setTimeout(() => resolve(mockAuthData), 100))
      );

      render(<TestApp initialRoute="/login" />);

      await waitFor(() => {
        expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
      });

      const emailInput = screen.getByLabelText(/email/i);
      const passwordInput = screen.getByLabelText(/password/i);
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(emailInput, 'test@example.com');
      await user.type(passwordInput, 'password123');
      await user.click(submitButton);

      // Check that form is disabled during submission
      expect(emailInput).toBeDisabled();
      expect(passwordInput).toBeDisabled();
      expect(submitButton).toBeDisabled();

      await waitFor(() => {
        expect(emailInput).not.toBeDisabled();
        expect(passwordInput).not.toBeDisabled();
        expect(submitButton).not.toBeDisabled();
      });
    });
  });

  describe('Error Handling', () => {
    it('should handle network errors gracefully', async () => {
      const user = userEvent.setup();
      
      vi.mocked(authService.isAuthenticated).mockReturnValue(false);
      vi.mocked(authService.shouldRefreshToken).mockReturnValue(false);
      vi.mocked(authService.login).mockRejectedValue(new Error('Network Error'));

      render(<TestApp initialRoute="/login" />);

      await waitFor(() => {
        expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
      });

      const emailInput = screen.getByLabelText(/email/i);
      const passwordInput = screen.getByLabelText(/password/i);
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(emailInput, 'test@example.com');
      await user.type(passwordInput, 'password123');
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/network error/i)).toBeInTheDocument();
      });
    });

    it('should clear errors when user starts typing', async () => {
      const user = userEvent.setup();
      
      vi.mocked(authService.isAuthenticated).mockReturnValue(false);
      vi.mocked(authService.shouldRefreshToken).mockReturnValue(false);
      vi.mocked(authService.login).mockRejectedValue(new Error('Invalid credentials'));

      render(<TestApp initialRoute="/login" />);

      await waitFor(() => {
        expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
      });

      const emailInput = screen.getByLabelText(/email/i);
      const passwordInput = screen.getByLabelText(/password/i);
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      // Trigger error
      await user.type(emailInput, 'test@example.com');
      await user.type(passwordInput, 'wrongpassword');
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/invalid credentials/i)).toBeInTheDocument();
      });

      // Clear error by typing
      await user.clear(emailInput);
      await user.type(emailInput, 'new@example.com');

      await waitFor(() => {
        expect(screen.queryByText(/invalid credentials/i)).not.toBeInTheDocument();
      });
    });
  });
});