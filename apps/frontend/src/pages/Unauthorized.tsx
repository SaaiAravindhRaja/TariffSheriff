import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';

export const Unauthorized: React.FC = () => {
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const handleGoBack = () => {
    navigate(-1);
  };

  const handleGoHome = () => {
    navigate('/');
  };

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <Card className="max-w-md w-full space-y-8 p-8">
        <div className="text-center">
          <div className="mx-auto h-12 w-12 text-red-500">
            <svg
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z"
              />
            </svg>
          </div>
          <h2 className="mt-6 text-3xl font-extrabold text-gray-900">
            Access Denied
          </h2>
          <p className="mt-2 text-sm text-gray-600">
            You don't have permission to access this page.
          </p>
          {user && (
            <p className="mt-2 text-xs text-gray-500">
              Logged in as: {user.email} ({user.role})
            </p>
          )}
        </div>

        <div className="space-y-4">
          <Button
            onClick={handleGoBack}
            variant="outline"
            className="w-full"
          >
            Go Back
          </Button>
          
          <Button
            onClick={handleGoHome}
            className="w-full"
          >
            Go to Dashboard
          </Button>

          <div className="text-center">
            <p className="text-sm text-gray-600">
              Need different permissions?{' '}
              <Link
                to="/contact"
                className="font-medium text-blue-600 hover:text-blue-500"
              >
                Contact support
              </Link>
            </p>
          </div>

          <div className="text-center pt-4 border-t">
            <button
              onClick={handleLogout}
              className="text-sm text-gray-500 hover:text-gray-700"
            >
              Sign out and login with different account
            </button>
          </div>
        </div>
      </Card>
    </div>
  );
};

export default Unauthorized;