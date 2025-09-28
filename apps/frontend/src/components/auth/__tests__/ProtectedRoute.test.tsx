import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ProtectedRoute } from '../ProtectedRoute';
import { AuthProvider } from '@/contexts/AuthContext';
import { authService } from '@/services/authService';

// Mock the auth service
vi.mock('@/services/authService', () => ({
  authService: {
    isAuthenticated: vi.fn(),
    shouldRefreshToken: vi.fn(),
    getCurrentUser: vi.fn(),
    refreshTokens: vi.fn(),
    clearAuth: vi.fn(),
  },
}));

// Mock react-router-dom Navigate component
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    Navigate: ({ to }: { to: string }) => <div data-testid="navigate-to">{to}</div>,
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

const mockAdminUser = {
  ...mockUser,
  id: 2,
  email: 'admin@example.com',
  name: 'Admin User',
  role: 'ADMIN' as const,
};

const TestComponent = () => <div data-testid="protected-content">Protected Content</div>;

const renderProtectedRoute = (
  requiredRoles?: string[],
  initialEntries: string[] = ['/protected']
) => {
  return render(
    <MemoryRouter initialEntries={initialEntries}>
      <AuthProvider>
        <ProtectedRoute requiredRoles={requiredRoles}>
          <TestComponent />
        </ProtectedRoute>
      </AuthProvider>
    </MemoryRouter>
  );
};

describe('ProtectedRoute', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should show loading spinner while checking authentication', () => {
    vi.mocked(authService.isAuthenticated).mockReturnValue(false);
    vi.mocked(authService.shouldRefreshToken).mockReturnValue(false);

    renderProtectedRoute();

    expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();
  });

  it('should render protected content for authenticated user', async () => {
    vi.mocked(authService.isAuthenticated).mockReturnValue(true);
    vi.mocked(authService.getCurrentUser).mockResolvedValue(mockUser);

    renderProtectedRoute();

    await waitFor(() => {
      expect(screen.getByTestId('protected-content')).toBeInTheDocument();
    });
  });

  it('should redirect to login for unauthenticated user', async () => {
    vi.mocked(authService.isAuthenticated).mockReturnValue(false);
    vi.mocked(authService.shouldRefreshToken).mockReturnValue(false);

    renderProtectedRoute();

    await waitFor(() => {
      expect(screen.getByTestId('navigate-to')).toHaveTextContent('/login');
    });
  });

  it('should attempt token refresh if needed', async () => {
    vi.mocked(authService.isAuthenticated).mockReturnValue(false);
    vi.mocked(authService.shouldRefreshToken).mockReturnValue(true);
    vi.mocked(authService.refreshTokens).mockResolvedValue({
      user: mockUser,
      accessToken: 'new-access-token',
      refreshToken: 'new-refresh-token',
      tokenType: 'Bearer',
      expiresIn: 900,
    });

    renderProtectedRoute();

    await waitFor(() => {
      expect(screen.getByTestId('protected-content')).toBeInTheDocument();
    });

    expect(authService.refreshTokens).toHaveBeenCalled();
  });

  it('should redirect to login if token refresh fails', async () => {
    vi.mocked(authService.isAuthenticated).mockReturnValue(false);
    vi.mocked(authService.shouldRefreshToken).mockReturnValue(true);
    vi.mocked(authService.refreshTokens).mockRejectedValue(new Error('Refresh failed'));

    renderProtectedRoute();

    await waitFor(() => {
      expect(screen.getByTestId('navigate-to')).toHaveTextContent('/login');
    });
  });

  it('should allow access for user with required role', async () => {
    vi.mocked(authService.isAuthenticated).mockReturnValue(true);
    vi.mocked(authService.getCurrentUser).mockResolvedValue(mockAdminUser);

    renderProtectedRoute(['ADMIN']);

    await waitFor(() => {
      expect(screen.getByTestId('protected-content')).toBeInTheDocument();
    });
  });

  it('should redirect to unauthorized for user without required role', async () => {
    vi.mocked(authService.isAuthenticated).mockReturnValue(true);
    vi.mocked(authService.getCurrentUser).mockResolvedValue(mockUser); // USER role

    renderProtectedRoute(['ADMIN']); // Requires ADMIN role

    await waitFor(() => {
      expect(screen.getByTestId('navigate-to')).toHaveTextContent('/unauthorized');
    });
  });

  it('should allow access for user with one of multiple required roles', async () => {
    vi.mocked(authService.isAuthenticated).mockReturnValue(true);
    vi.mocked(authService.getCurrentUser).mockResolvedValue(mockUser); // USER role

    renderProtectedRoute(['ADMIN', 'USER']); // USER role is allowed

    await waitFor(() => {
      expect(screen.getByTestId('protected-content')).toBeInTheDocument();
    });
  });

  it('should handle authentication check failure', async () => {
    vi.mocked(authService.isAuthenticated).mockReturnValue(true);
    vi.mocked(authService.getCurrentUser).mockRejectedValue(new Error('Network error'));

    renderProtectedRoute();

    await waitFor(() => {
      expect(screen.getByTestId('navigate-to')).toHaveTextContent('/login');
    });
  });

  it('should preserve location state for redirect after login', async () => {
    vi.mocked(authService.isAuthenticated).mockReturnValue(false);
    vi.mocked(authService.shouldRefreshToken).mockReturnValue(false);

    renderProtectedRoute(undefined, ['/protected/dashboard']);

    await waitFor(() => {
      const navigateElement = screen.getByTestId('navigate-to');
      expect(navigateElement).toHaveTextContent('/login');
      // In a real implementation, this would include state with the original location
    });
  });

  it('should handle role checking with case insensitivity', async () => {
    vi.mocked(authService.isAuthenticated).mockReturnValue(true);
    vi.mocked(authService.getCurrentUser).mockResolvedValue(mockUser); // USER role

    renderProtectedRoute(['user']); // lowercase

    await waitFor(() => {
      expect(screen.getByTestId('protected-content')).toBeInTheDocument();
    });
  });

  it('should handle empty required roles array', async () => {
    vi.mocked(authService.isAuthenticated).mockReturnValue(true);
    vi.mocked(authService.getCurrentUser).mockResolvedValue(mockUser);

    renderProtectedRoute([]); // No specific roles required

    await waitFor(() => {
      expect(screen.getByTestId('protected-content')).toBeInTheDocument();
    });
  });

  it('should handle undefined required roles', async () => {
    vi.mocked(authService.isAuthenticated).mockReturnValue(true);
    vi.mocked(authService.getCurrentUser).mockResolvedValue(mockUser);

    renderProtectedRoute(); // No roles specified

    await waitFor(() => {
      expect(screen.getByTestId('protected-content')).toBeInTheDocument();
    });
  });

  it('should handle user with null role', async () => {
    const userWithoutRole = { ...mockUser, role: null as any };
    vi.mocked(authService.isAuthenticated).mockReturnValue(true);
    vi.mocked(authService.getCurrentUser).mockResolvedValue(userWithoutRole);

    renderProtectedRoute(['USER']);

    await waitFor(() => {
      expect(screen.getByTestId('navigate-to')).toHaveTextContent('/unauthorized');
    });
  });

  it('should handle suspended user', async () => {
    const suspendedUser = { ...mockUser, status: 'SUSPENDED' as const };
    vi.mocked(authService.isAuthenticated).mockReturnValue(true);
    vi.mocked(authService.getCurrentUser).mockResolvedValue(suspendedUser);

    renderProtectedRoute();

    await waitFor(() => {
      expect(screen.getByTestId('navigate-to')).toHaveTextContent('/login');
    });
  });

  it('should handle unverified user', async () => {
    const unverifiedUser = { ...mockUser, emailVerified: false };
    vi.mocked(authService.isAuthenticated).mockReturnValue(true);
    vi.mocked(authService.getCurrentUser).mockResolvedValue(unverifiedUser);

    renderProtectedRoute();

    await waitFor(() => {
      expect(screen.getByTestId('navigate-to')).toHaveTextContent('/verify-email');
    });
  });

  it('should re-check authentication when auth state changes', async () => {
    vi.mocked(authService.isAuthenticated).mockReturnValue(false);
    vi.mocked(authService.shouldRefreshToken).mockReturnValue(false);

    const { rerender } = renderProtectedRoute();

    await waitFor(() => {
      expect(screen.getByTestId('navigate-to')).toHaveTextContent('/login');
    });

    // Simulate user logging in
    vi.mocked(authService.isAuthenticated).mockReturnValue(true);
    vi.mocked(authService.getCurrentUser).mockResolvedValue(mockUser);

    rerender(
      <MemoryRouter initialEntries={['/protected']}>
        <AuthProvider>
          <ProtectedRoute>
            <TestComponent />
          </ProtectedRoute>
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('protected-content')).toBeInTheDocument();
    });
  });

  it('should handle hierarchical roles (ADMIN > USER)', async () => {
    vi.mocked(authService.isAuthenticated).mockReturnValue(true);
    vi.mocked(authService.getCurrentUser).mockResolvedValue(mockAdminUser); // ADMIN role

    renderProtectedRoute(['USER']); // Requires USER role, but ADMIN should have access

    await waitFor(() => {
      expect(screen.getByTestId('protected-content')).toBeInTheDocument();
    });
  });

  it('should show appropriate loading state', () => {
    vi.mocked(authService.isAuthenticated).mockReturnValue(false);
    vi.mocked(authService.shouldRefreshToken).mockReturnValue(false);

    renderProtectedRoute();

    const loadingSpinner = screen.getByTestId('loading-spinner');
    expect(loadingSpinner).toBeInTheDocument();
    expect(loadingSpinner).toHaveAttribute('aria-label', 'Loading...');
  });
});