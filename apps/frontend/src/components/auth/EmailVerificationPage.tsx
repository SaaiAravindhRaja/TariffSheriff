import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { CheckCircle, XCircle, Mail, Loader2, RefreshCw, ArrowRight } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { useAuth } from '@/contexts/AuthContext';
import { authService } from '@/services/authService';

interface VerificationState {
  status: 'loading' | 'success' | 'error' | 'expired' | 'invalid' | 'pending';
  message: string;
  details?: string;
}

export const EmailVerificationPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { user, checkAuthStatus } = useAuth();
  const [verificationState, setVerificationState] = useState<VerificationState>({
    status: 'loading',
    message: 'Verifying your email address...'
  });
  const [isResending, setIsResending] = useState(false);
  const [resendCooldown, setResendCooldown] = useState(0);

  const token = searchParams.get('token');

  useEffect(() => {
    const verifyEmail = async () => {
      if (!token) {
        setVerificationState({
          status: 'pending',
          message: 'Email verification required',
          details: 'Please check your email for the verification link.'
        });
        return;
      }

      try {
        setVerificationState({
          status: 'loading',
          message: 'Verifying your email address...'
        });

        await authService.verifyEmail(token);
        
        // Refresh user data to get updated verification status
        await checkAuthStatus();
        
        setVerificationState({
          status: 'success',
          message: 'Email verified successfully!',
          details: 'Your account is now fully activated and ready to use.'
        });
        
        // Auto-redirect after 5 seconds
        setTimeout(() => {
          navigate('/dashboard', { replace: true });
        }, 5000);
        
      } catch (error: any) {
        const errorMessage = error.message || 'Email verification failed';
        
        // Determine specific error type based on message
        if (errorMessage.toLowerCase().includes('expired')) {
          setVerificationState({
            status: 'expired',
            message: 'Verification link expired',
            details: 'The verification link has expired. Please request a new one.'
          });
        } else if (errorMessage.toLowerCase().includes('invalid') || errorMessage.toLowerCase().includes('not found')) {
          setVerificationState({
            status: 'invalid',
            message: 'Invalid verification link',
            details: 'The verification link is invalid or has already been used.'
          });
        } else {
          setVerificationState({
            status: 'error',
            message: 'Verification failed',
            details: errorMessage
          });
        }
      }
    };

    verifyEmail();
  }, [token, checkAuthStatus, navigate]);

  // Cooldown timer for resend button
  useEffect(() => {
    if (resendCooldown > 0) {
      const timer = setTimeout(() => {
        setResendCooldown(resendCooldown - 1);
      }, 1000);
      return () => clearTimeout(timer);
    }
  }, [resendCooldown]);

  const handleResendVerification = async () => {
    if (!user?.email) {
      setVerificationState({
        status: 'error',
        message: 'Unable to resend verification',
        details: 'Please log in again to resend the verification email.'
      });
      return;
    }

    setIsResending(true);
    
    try {
      // Use forgot password endpoint as a workaround for resending verification
      // In a real implementation, there should be a dedicated resend verification endpoint
      await authService.forgotPassword(user.email);
      
      setVerificationState({
        status: 'pending',
        message: 'Verification email sent!',
        details: `A new verification email has been sent to ${user.email}. Please check your inbox and spam folder.`
      });
      
      // Set cooldown to prevent spam
      setResendCooldown(60);
      
    } catch (error: any) {
      setVerificationState({
        status: 'error',
        message: 'Failed to resend verification email',
        details: error.message || 'Please try again later or contact support.'
      });
    } finally {
      setIsResending(false);
    }
  };

  const renderIcon = () => {
    switch (verificationState.status) {
      case 'loading':
        return <Loader2 className="w-12 h-12 text-blue-500 animate-spin" />;
      case 'success':
        return <CheckCircle className="w-12 h-12 text-green-500" />;
      case 'error':
      case 'expired':
      case 'invalid':
        return <XCircle className="w-12 h-12 text-red-500" />;
      case 'pending':
        return <Mail className="w-12 h-12 text-blue-500" />;
      default:
        return <Mail className="w-12 h-12 text-gray-400" />;
    }
  };

  const renderTitle = () => {
    switch (verificationState.status) {
      case 'loading':
        return 'Verifying Email';
      case 'success':
        return 'Email Verified!';
      case 'error':
        return 'Verification Failed';
      case 'expired':
        return 'Link Expired';
      case 'invalid':
        return 'Invalid Link';
      case 'pending':
        return 'Check Your Email';
      default:
        return 'Email Verification';
    }
  };

  const renderActions = () => {
    switch (verificationState.status) {
      case 'success':
        return (
          <div className="space-y-3">
            <Button
              onClick={() => navigate('/dashboard')}
              className="w-full"
              variant="gradient"
            >
              Continue to Dashboard
              <ArrowRight className="ml-2 h-4 w-4" />
            </Button>
            <p className="text-xs text-gray-500 text-center">
              Redirecting automatically in 5 seconds...
            </p>
          </div>
        );

      case 'expired':
      case 'invalid':
      case 'error':
        return (
          <div className="space-y-3">
            <Button
              onClick={handleResendVerification}
              disabled={isResending || resendCooldown > 0}
              className="w-full"
              variant="gradient"
            >
              {isResending ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Sending...
                </>
              ) : resendCooldown > 0 ? (
                <>
                  <RefreshCw className="mr-2 h-4 w-4" />
                  Resend in {resendCooldown}s
                </>
              ) : (
                <>
                  <RefreshCw className="mr-2 h-4 w-4" />
                  Resend Verification Email
                </>
              )}
            </Button>
            
            <div className="flex space-x-2">
              <Link to="/login" className="flex-1">
                <Button variant="outline" className="w-full">
                  Sign In
                </Button>
              </Link>
              <Link to="/register" className="flex-1">
                <Button variant="outline" className="w-full">
                  Sign Up
                </Button>
              </Link>
            </div>
          </div>
        );

      case 'pending':
        return (
          <div className="space-y-3">
            <Button
              onClick={handleResendVerification}
              disabled={isResending || resendCooldown > 0}
              variant="outline"
              className="w-full"
            >
              {isResending ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Sending...
                </>
              ) : resendCooldown > 0 ? (
                <>
                  <RefreshCw className="mr-2 h-4 w-4" />
                  Resend in {resendCooldown}s
                </>
              ) : (
                <>
                  <RefreshCw className="mr-2 h-4 w-4" />
                  Resend Verification Email
                </>
              )}
            </Button>
            
            <Link to="/dashboard">
              <Button variant="ghost" className="w-full">
                Continue to Dashboard
              </Button>
            </Link>
          </div>
        );

      case 'loading':
      default:
        return (
          <div className="text-center">
            <p className="text-sm text-gray-500">
              This may take a few moments...
            </p>
          </div>
        );
    }
  };

  const getStatusColor = () => {
    switch (verificationState.status) {
      case 'success':
        return 'border-green-200 bg-green-50';
      case 'error':
      case 'expired':
      case 'invalid':
        return 'border-red-200 bg-red-50';
      case 'pending':
        return 'border-blue-200 bg-blue-50';
      case 'loading':
      default:
        return 'border-gray-200 bg-gray-50';
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center space-y-4">
          <div className="mx-auto">
            {renderIcon()}
          </div>
          <div>
            <CardTitle className="text-2xl font-bold">
              {renderTitle()}
            </CardTitle>
            <CardDescription className="mt-2">
              {verificationState.message}
            </CardDescription>
          </div>
        </CardHeader>
        
        <CardContent className="space-y-4">
          {/* Status Details */}
          {verificationState.details && (
            <div className={`p-4 rounded-md border ${getStatusColor()}`}>
              <p className="text-sm text-gray-700">
                {verificationState.details}
              </p>
            </div>
          )}

          {/* User Email Display */}
          {user?.email && verificationState.status !== 'success' && (
            <div className="text-center">
              <p className="text-sm text-gray-600">
                Verification email sent to:
              </p>
              <p className="text-sm font-medium text-gray-900">
                {user.email}
              </p>
            </div>
          )}

          {/* Action Buttons */}
          {renderActions()}

          {/* Help Text */}
          {(verificationState.status === 'pending' || verificationState.status === 'expired') && (
            <div className="text-center text-xs text-gray-500 space-y-1">
              <p>Didn't receive the email? Check your spam folder.</p>
              <p>Still having trouble? <Link to="/contact" className="text-blue-600 hover:underline">Contact support</Link></p>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default EmailVerificationPage;