import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { LoadingSpinner } from '@/components/ui/loading';

interface PublicRouteProps {
  children: React.ReactNode;
  redirectTo?: string;
  redirectIfAuthenticated?: boolean;
}

/**
 * PublicRoute component for routes that should only be accessible to unauthenticated users
 * (like login, register pages) or routes that are accessible to everyone
 */
export const PublicRoute: React.FC<PublicRouteProps> = ({
  children,
  redirectTo = '/',
  redirectIfAuthenticated = true,
}) => {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  // Show loading spinner while checking authentication
  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <LoadingSpinner />
      </div>
    );
  }

  // If user is authenticated and we should redirect authenticated users
  if (isAuthenticated && redirectIfAuthenticated) {
    // Check if there's a redirect location from the login flow
    const from = location.state?.from?.pathname || redirectTo;
    return <Navigate to={from} replace />;
  }

  return <>{children}</>;
};

export default PublicRoute;