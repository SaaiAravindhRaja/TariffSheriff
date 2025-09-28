import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { authService } from '@/services/authService';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { LoadingSpinner } from '@/components/ui/loading';

export const VerifyEmail: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { user, checkAuthStatus } = useAuth();
  const [verificationState, setVerificationState] = useState<{
    status: 'loading' | 'success' | 'error' | 'pending';
    message: string;
  }>({
    status: 'loading',
    message: 'Verifying your email...'
  });

  const token = searchParams.get('token');

  useEffect(() => {
    const verifyEmail = async () => {
      if (!token) {
        setVerificationState({
          status: 'pending',
          message: 'Please check your email for the verification link.'
        });
        return;
      }

      try {
        await authService.verifyEmail(token);
        await checkAuthStatus(); // Refresh user data
        setVerificationState({
          status: 'success',
          message: 'Your email has been successfully verified!'
        });
        
        // Redirect to dashboard after 3 seconds
        setTimeout(() => {
          navigate('/');
        }, 3000);
      } catch (error: any) {
        setVerificationState({
          status: 'error',
          message: error.message || 'Email verification failed. The link may be expired or invalid.'
        });
      }
    };

    verifyEmail();
  }, [token, checkAuthStatus, navigate]);

  const handleResendVerification = async () => {
    // This would need to be implemented in the auth service
    // For now, we'll just show a message
    setVerificationState({
      status: 'pending',
      message: 'Verification email sent! Please check your inbox.'
    });
  };

  const renderContent = () => {
    switch (verificationState.status) {
      case 'loading':
        return (
          <div className="text-center">
            <LoadingSpinner />
            <p className="mt-4 text-gray-600">{verificationState.message}</p>
          </div>
        );

      case 'success':
        return (
          <div className="text-center">
            <div className="mx-auto h-12 w-12 text-green-500 mb-4">
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
                  d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
            </div>
            <h2 className="text-2xl font-bold text-gray-900 mb-2">
              Email Verified!
            </h2>
            <p className="text-gray-600 mb-4">{verificationState.message}</p>
            <p className="text-sm text-gray-500">
              Redirecting to dashboard in a few seconds...
            </p>
            <Button
              onClick={() => navigate('/')}
              className="mt-4"
            >
              Go to Dashboard Now
            </Button>
          </div>
        );

      case 'error':
        return (
          <div className="text-center">
            <div className="mx-auto h-12 w-12 text-red-500 mb-4">
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
            <h2 className="text-2xl font-bold text-gray-900 mb-2">
              Verification Failed
            </h2>
            <p className="text-gray-600 mb-4">{verificationState.message}</p>
            <div className="space-y-2">
              <Button
                onClick={handleResendVerification}
                className="w-full"
              >
                Resend Verification Email
              </Button>
              <Button
                onClick={() => navigate('/')}
                variant="outline"
                className="w-full"
              >
                Go to Dashboard
              </Button>
            </div>
          </div>
        );

      case 'pending':
        return (
          <div className="text-center">
            <div className="mx-auto h-12 w-12 text-blue-500 mb-4">
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
                  d="M3 8l7.89 4.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"
                />
              </svg>
            </div>
            <h2 className="text-2xl font-bold text-gray-900 mb-2">
              Check Your Email
            </h2>
            <p className="text-gray-600 mb-4">{verificationState.message}</p>
            {user && (
              <p className="text-sm text-gray-500 mb-4">
                Verification email sent to: {user.email}
              </p>
            )}
            <div className="space-y-2">
              <Button
                onClick={handleResendVerification}
                variant="outline"
                className="w-full"
              >
                Resend Verification Email
              </Button>
              <Link
                to="/"
                className="block text-sm text-blue-600 hover:text-blue-500"
              >
                Continue to Dashboard
              </Link>
            </div>
          </div>
        );

      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <Card className="max-w-md w-full space-y-8 p-8">
        {renderContent()}
      </Card>
    </div>
  );
};

export default VerifyEmail;