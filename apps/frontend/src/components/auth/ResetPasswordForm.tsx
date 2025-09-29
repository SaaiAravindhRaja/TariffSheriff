import React, { useState, useEffect } from 'react';
import { Link, useSearchParams, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, Lock, Loader2, CheckCircle, X, Check } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { authService } from '@/services/authService';

interface ResetPasswordFormProps {
  onSuccess?: () => void;
}

interface FormData {
  password: string;
  confirmPassword: string;
}

interface FormErrors {
  password?: string;
  confirmPassword?: string;
  general?: string;
}

interface PasswordStrength {
  score: number;
  feedback: string[];
  isValid: boolean;
}

export const ResetPasswordForm: React.FC<ResetPasswordFormProps> = ({ onSuccess }) => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token');

  const [formData, setFormData] = useState<FormData>({
    password: '',
    confirmPassword: '',
  });
  const [formErrors, setFormErrors] = useState<FormErrors>({});
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [passwordStrength, setPasswordStrength] = useState<PasswordStrength>({
    score: 0,
    feedback: [],
    isValid: false,
  });

  // Redirect if no token is provided
  useEffect(() => {
    if (!token) {
      navigate('/forgot-password', { replace: true });
    }
  }, [token, navigate]);

  const calculatePasswordStrength = (password: string): PasswordStrength => {
    const feedback: string[] = [];
    let score = 0;

    if (password.length >= 8) {
      score += 1;
    } else {
      feedback.push('At least 8 characters');
    }

    if (/[a-z]/.test(password)) {
      score += 1;
    } else {
      feedback.push('One lowercase letter');
    }

    if (/[A-Z]/.test(password)) {
      score += 1;
    } else {
      feedback.push('One uppercase letter');
    }

    if (/\d/.test(password)) {
      score += 1;
    } else {
      feedback.push('One number');
    }

    if (/[!@#$%^&*(),.?":{}|<>]/.test(password)) {
      score += 1;
    } else {
      feedback.push('One special character');
    }

    return {
      score,
      feedback,
      isValid: score >= 4,
    };
  };

  const validatePassword = (password: string): string | undefined => {
    if (!password) {
      return 'Password is required';
    }
    
    const strength = calculatePasswordStrength(password);
    if (!strength.isValid) {
      return 'Password does not meet security requirements';
    }
    
    return undefined;
  };

  const validateConfirmPassword = (confirmPassword: string, password: string): string | undefined => {
    if (!confirmPassword) {
      return 'Please confirm your password';
    }
    if (confirmPassword !== password) {
      return 'Passwords do not match';
    }
    return undefined;
  };

  const validateForm = (): boolean => {
    const errors: FormErrors = {};
    
    errors.password = validatePassword(formData.password);
    errors.confirmPassword = validateConfirmPassword(formData.confirmPassword, formData.password);

    setFormErrors(errors);
    return !errors.password && !errors.confirmPassword;
  };

  const handleInputChange = (field: keyof FormData) => (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    const { value } = e.target;
    
    setFormData(prev => ({
      ...prev,
      [field]: value,
    }));

    // Update password strength in real-time
    if (field === 'password') {
      const strength = calculatePasswordStrength(value);
      setPasswordStrength(strength);
    }

    // Clear field-specific error when user starts typing
    if (formErrors[field]) {
      setFormErrors(prev => ({
        ...prev,
        [field]: undefined,
      }));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!token) {
      setFormErrors({ general: 'Invalid or missing reset token' });
      return;
    }

    if (!validateForm()) {
      return;
    }

    setIsSubmitting(true);
    setFormErrors({});
    
    try {
      await authService.resetPassword(token, formData.password);
      setIsSuccess(true);
      onSuccess?.();
    } catch (err: any) {
      setFormErrors({
        general: err.message || 'Failed to reset password. The link may have expired.',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const togglePasswordVisibility = (field: 'password' | 'confirmPassword') => {
    if (field === 'password') {
      setShowPassword(!showPassword);
    } else {
      setShowConfirmPassword(!showConfirmPassword);
    }
  };

  const getPasswordStrengthColor = (score: number): string => {
    if (score <= 1) return 'bg-red-500';
    if (score <= 2) return 'bg-orange-500';
    if (score <= 3) return 'bg-yellow-500';
    if (score <= 4) return 'bg-green-500';
    return 'bg-green-600';
  };

  const getPasswordStrengthText = (score: number): string => {
    if (score <= 1) return 'Very Weak';
    if (score <= 2) return 'Weak';
    if (score <= 3) return 'Fair';
    if (score <= 4) return 'Good';
    return 'Strong';
  };

  if (!token) {
    return null; // Will redirect via useEffect
  }

  if (isSuccess) {
    return (
      <Card className="w-full max-w-md mx-auto">
        <CardHeader className="space-y-1 text-center">
          <div className="mx-auto mb-4 w-12 h-12 bg-green-100 rounded-full flex items-center justify-center">
            <CheckCircle className="w-6 h-6 text-green-600" />
          </div>
          <CardTitle className="text-2xl font-bold">
            Password Reset Successful
          </CardTitle>
          <CardDescription>
            Your password has been successfully updated
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="p-4 bg-green-50 border border-green-200 rounded-md">
            <p className="text-sm text-green-700">
              ✅ Your password has been reset successfully. You can now sign in with your new password.
            </p>
          </div>

          <Link to="/login">
            <Button className="w-full" variant="gradient">
              Continue to Sign In
            </Button>
          </Link>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="w-full max-w-md mx-auto">
      <CardHeader className="space-y-1">
        <CardTitle className="text-2xl font-bold text-center">
          Set New Password
        </CardTitle>
        <CardDescription className="text-center">
          Enter your new password below
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Password Field */}
          <div className="space-y-2">
            <label htmlFor="password" className="text-sm font-medium">
              New Password
            </label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
              <Input
                id="password"
                type={showPassword ? 'text' : 'password'}
                placeholder="Enter your new password"
                value={formData.password}
                onChange={handleInputChange('password')}
                className={`pl-10 pr-10 ${formErrors.password ? 'border-red-500 focus:border-red-500' : ''}`}
                disabled={isSubmitting}
                autoComplete="new-password"
                autoFocus
                required
              />
              <button
                type="button"
                onClick={() => togglePasswordVisibility('password')}
                className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
                disabled={isSubmitting}
              >
                {showPassword ? (
                  <EyeOff className="h-4 w-4" />
                ) : (
                  <Eye className="h-4 w-4" />
                )}
              </button>
            </div>
            
            {/* Password Strength Indicator */}
            {formData.password && (
              <div className="space-y-2">
                <div className="flex items-center space-x-2">
                  <div className="flex-1 bg-gray-200 rounded-full h-2">
                    <div
                      className={`h-2 rounded-full transition-all duration-300 ${getPasswordStrengthColor(passwordStrength.score)}`}
                      style={{ width: `${(passwordStrength.score / 5) * 100}%` }}
                    />
                  </div>
                  <span className="text-xs font-medium">
                    {getPasswordStrengthText(passwordStrength.score)}
                  </span>
                </div>
                
                {passwordStrength.feedback.length > 0 && (
                  <div className="text-xs text-gray-600">
                    <p className="font-medium mb-1">Password must include:</p>
                    <ul className="space-y-1">
                      {passwordStrength.feedback.map((item, index) => (
                        <li key={index} className="flex items-center space-x-1">
                          <X className="h-3 w-3 text-red-500" />
                          <span>{item}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
                
                {passwordStrength.isValid && (
                  <div className="flex items-center space-x-1 text-xs text-green-600">
                    <Check className="h-3 w-3" />
                    <span>Password meets security requirements</span>
                  </div>
                )}
              </div>
            )}
            
            {formErrors.password && (
              <p className="text-sm text-red-600">{formErrors.password}</p>
            )}
          </div>

          {/* Confirm Password Field */}
          <div className="space-y-2">
            <label htmlFor="confirmPassword" className="text-sm font-medium">
              Confirm New Password
            </label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
              <Input
                id="confirmPassword"
                type={showConfirmPassword ? 'text' : 'password'}
                placeholder="Confirm your new password"
                value={formData.confirmPassword}
                onChange={handleInputChange('confirmPassword')}
                className={`pl-10 pr-10 ${formErrors.confirmPassword ? 'border-red-500 focus:border-red-500' : ''}`}
                disabled={isSubmitting}
                autoComplete="new-password"
                required
              />
              <button
                type="button"
                onClick={() => togglePasswordVisibility('confirmPassword')}
                className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
                disabled={isSubmitting}
              >
                {showConfirmPassword ? (
                  <EyeOff className="h-4 w-4" />
                ) : (
                  <Eye className="h-4 w-4" />
                )}
              </button>
            </div>
            {formErrors.confirmPassword && (
              <p className="text-sm text-red-600">{formErrors.confirmPassword}</p>
            )}
          </div>

          {/* Error Messages */}
          {formErrors.general && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-md">
              <p className="text-sm text-red-600">{formErrors.general}</p>
            </div>
          )}

          {/* Submit Button */}
          <Button
            type="submit"
            className="w-full"
            disabled={isSubmitting || !passwordStrength.isValid}
            variant="gradient"
          >
            {isSubmitting ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Resetting Password...
              </>
            ) : (
              'Reset Password'
            )}
          </Button>

          {/* Back to Login Link */}
          <div className="text-center">
            <Link
              to="/login"
              className="text-sm text-gray-600 hover:text-gray-800 hover:underline"
            >
              Back to Sign In
            </Link>
          </div>
        </form>
      </CardContent>
    </Card>
  );
};

export default ResetPasswordForm;