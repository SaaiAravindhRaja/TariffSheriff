import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Eye, EyeOff, Mail, Lock, User, Loader2, Check, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { useAuth } from '@/contexts/AuthContext';
import { RegisterRequest, UserRole } from '@/types/auth';

interface RegisterFormProps {
  onSuccess?: () => void;
  redirectTo?: string;
}

interface FormData extends RegisterRequest {
  confirmPassword: string;
  acceptTerms: boolean;
}

interface FormErrors {
  name?: string;
  email?: string;
  password?: string;
  confirmPassword?: string;
  acceptTerms?: string;
  general?: string;
}

interface PasswordStrength {
  score: number;
  feedback: string[];
  isValid: boolean;
}

export const RegisterForm: React.FC<RegisterFormProps> = ({ onSuccess, redirectTo }) => {
  const { register, isLoading, error, clearError } = useAuth();
  const [formData, setFormData] = useState<FormData>({
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
    role: UserRole.USER,
    acceptTerms: false,
  });
  const [formErrors, setFormErrors] = useState<FormErrors>({});
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [passwordStrength, setPasswordStrength] = useState<PasswordStrength>({
    score: 0,
    feedback: [],
    isValid: false,
  });

  const validateName = (name: string): string | undefined => {
    if (!name.trim()) {
      return 'Name is required';
    }
    if (name.trim().length < 2) {
      return 'Name must be at least 2 characters long';
    }
    if (name.trim().length > 50) {
      return 'Name must be less than 50 characters';
    }
    return undefined;
  };

  const validateEmail = (email: string): string | undefined => {
    if (!email) {
      return 'Email is required';
    }
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      return 'Please enter a valid email address';
    }
    return undefined;
  };

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

  const validateTerms = (acceptTerms: boolean): string | undefined => {
    if (!acceptTerms) {
      return 'You must accept the terms and conditions';
    }
    return undefined;
  };

  const validateForm = (): boolean => {
    const errors: FormErrors = {};
    
    errors.name = validateName(formData.name);
    errors.email = validateEmail(formData.email);
    errors.password = validatePassword(formData.password);
    errors.confirmPassword = validateConfirmPassword(formData.confirmPassword, formData.password);
    errors.acceptTerms = validateTerms(formData.acceptTerms);

    setFormErrors(errors);
    return !errors.name && !errors.email && !errors.password && !errors.confirmPassword && !errors.acceptTerms;
  };

  const handleInputChange = (field: keyof FormData) => (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>
  ) => {
    const value = field === 'acceptTerms' ? (e.target as HTMLInputElement).checked : e.target.value;
    
    setFormData(prev => ({
      ...prev,
      [field]: value,
    }));

    // Update password strength in real-time
    if (field === 'password') {
      const strength = calculatePasswordStrength(value as string);
      setPasswordStrength(strength);
    }

    // Clear field-specific error when user starts typing
    if (formErrors[field as keyof FormErrors]) {
      setFormErrors(prev => ({
        ...prev,
        [field]: undefined,
      }));
    }

    // Clear general error when user makes changes
    if (error) {
      clearError();
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    setIsSubmitting(true);
    
    try {
      const registerData: RegisterRequest = {
        name: formData.name.trim(),
        email: formData.email.trim().toLowerCase(),
        password: formData.password,
        role: formData.role,
      };

      await register(registerData);
      onSuccess?.();
    } catch (err: any) {
      // Error is handled by the auth context
      setFormErrors({
        general: err.message || 'Registration failed. Please try again.',
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

  const isFormLoading = isLoading || isSubmitting;

  return (
    <Card className="w-full max-w-md mx-auto">
      <CardHeader className="space-y-1">
        <CardTitle className="text-2xl font-bold text-center">
          Create Account
        </CardTitle>
        <CardDescription className="text-center">
          Join TariffSheriff to access advanced tariff analysis tools
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Name Field */}
          <div className="space-y-2">
            <label htmlFor="name" className="text-sm font-medium">
              Full Name
            </label>
            <div className="relative">
              <User className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
              <Input
                id="name"
                type="text"
                placeholder="Enter your full name"
                value={formData.name}
                onChange={handleInputChange('name')}
                className={`pl-10 ${formErrors.name ? 'border-red-500 focus:border-red-500' : ''}`}
                disabled={isFormLoading}
                autoComplete="name"
                required
              />
            </div>
            {formErrors.name && (
              <p className="text-sm text-red-600">{formErrors.name}</p>
            )}
          </div>

          {/* Email Field */}
          <div className="space-y-2">
            <label htmlFor="email" className="text-sm font-medium">
              Email Address
            </label>
            <div className="relative">
              <Mail className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
              <Input
                id="email"
                type="email"
                placeholder="Enter your email"
                value={formData.email}
                onChange={handleInputChange('email')}
                className={`pl-10 ${formErrors.email ? 'border-red-500 focus:border-red-500' : ''}`}
                disabled={isFormLoading}
                autoComplete="email"
                required
              />
            </div>
            {formErrors.email && (
              <p className="text-sm text-red-600">{formErrors.email}</p>
            )}
          </div>

          {/* Password Field */}
          <div className="space-y-2">
            <label htmlFor="password" className="text-sm font-medium">
              Password
            </label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
              <Input
                id="password"
                type={showPassword ? 'text' : 'password'}
                placeholder="Create a strong password"
                value={formData.password}
                onChange={handleInputChange('password')}
                className={`pl-10 pr-10 ${formErrors.password ? 'border-red-500 focus:border-red-500' : ''}`}
                disabled={isFormLoading}
                autoComplete="new-password"
                required
              />
              <button
                type="button"
                onClick={() => togglePasswordVisibility('password')}
                className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
                disabled={isFormLoading}
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
              Confirm Password
            </label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
              <Input
                id="confirmPassword"
                type={showConfirmPassword ? 'text' : 'password'}
                placeholder="Confirm your password"
                value={formData.confirmPassword}
                onChange={handleInputChange('confirmPassword')}
                className={`pl-10 pr-10 ${formErrors.confirmPassword ? 'border-red-500 focus:border-red-500' : ''}`}
                disabled={isFormLoading}
                autoComplete="new-password"
                required
              />
              <button
                type="button"
                onClick={() => togglePasswordVisibility('confirmPassword')}
                className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
                disabled={isFormLoading}
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

          {/* Terms and Conditions */}
          <div className="space-y-2">
            <div className="flex items-start space-x-2">
              <input
                id="acceptTerms"
                type="checkbox"
                checked={formData.acceptTerms}
                onChange={handleInputChange('acceptTerms')}
                className={`mt-1 h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded ${
                  formErrors.acceptTerms ? 'border-red-500' : ''
                }`}
                disabled={isFormLoading}
                required
              />
              <label htmlFor="acceptTerms" className="text-sm text-gray-600">
                I agree to the{' '}
                <Link
                  to="/terms"
                  className="text-blue-600 hover:text-blue-800 hover:underline"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Terms of Service
                </Link>{' '}
                and{' '}
                <Link
                  to="/privacy"
                  className="text-blue-600 hover:text-blue-800 hover:underline"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Privacy Policy
                </Link>
              </label>
            </div>
            {formErrors.acceptTerms && (
              <p className="text-sm text-red-600">{formErrors.acceptTerms}</p>
            )}
          </div>

          {/* Error Messages */}
          {(error || formErrors.general) && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-md">
              <p className="text-sm text-red-600">
                {error || formErrors.general}
              </p>
            </div>
          )}

          {/* Submit Button */}
          <Button
            type="submit"
            className="w-full"
            disabled={isFormLoading || !passwordStrength.isValid}
            variant="gradient"
          >
            {isFormLoading ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Creating account...
              </>
            ) : (
              'Create Account'
            )}
          </Button>

          {/* Email Verification Notice */}
          <div className="p-3 bg-blue-50 border border-blue-200 rounded-md">
            <p className="text-sm text-blue-700">
              📧 After registration, please check your email to verify your account before signing in.
            </p>
          </div>

          {/* Sign In Link */}
          <div className="text-center">
            <p className="text-sm text-gray-600">
              Already have an account?{' '}
              <Link
                to="/login"
                className="text-blue-600 hover:text-blue-800 hover:underline font-medium"
              >
                Sign in
              </Link>
            </p>
          </div>
        </form>
      </CardContent>
    </Card>
  );
};

export default RegisterForm;