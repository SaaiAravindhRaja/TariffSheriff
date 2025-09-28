import { useAuth } from '@/contexts/AuthContext';
import { UserRole } from '@/types/auth';

/**
 * Hook for checking authentication and role permissions
 */
export const useAuthGuard = () => {
  const { user, isAuthenticated, isLoading } = useAuth();

  /**
   * Check if user has any of the specified roles
   */
  const hasRole = (roles: UserRole | UserRole[]): boolean => {
    if (!isAuthenticated || !user) return false;
    
    const requiredRoles = Array.isArray(roles) ? roles : [roles];
    if (requiredRoles.length === 0) return true;
    
    // Define role hierarchy
    const roleHierarchy: Record<UserRole, number> = {
      [UserRole.USER]: 1,
      [UserRole.ANALYST]: 2,
      [UserRole.ADMIN]: 3,
    };

    const userRoleLevel = roleHierarchy[user.role];
    
    // Check if user has any of the required roles or higher
    return requiredRoles.some(role => userRoleLevel >= roleHierarchy[role]);
  };

  /**
   * Check if user is admin
   */
  const isAdmin = (): boolean => {
    return hasRole(UserRole.ADMIN);
  };

  /**
   * Check if user is analyst or higher
   */
  const isAnalyst = (): boolean => {
    return hasRole([UserRole.ANALYST, UserRole.ADMIN]);
  };

  /**
   * Check if user's email is verified
   */
  const isEmailVerified = (): boolean => {
    return isAuthenticated && user ? user.emailVerified : false;
  };

  /**
   * Check if user can access a specific feature
   */
  const canAccess = (feature: string): boolean => {
    if (!isAuthenticated || !user) return false;

    // Define feature permissions
    const featurePermissions: Record<string, UserRole[]> = {
      'admin-panel': [UserRole.ADMIN],
      'analytics': [UserRole.ANALYST, UserRole.ADMIN],
      'reports': [UserRole.ANALYST, UserRole.ADMIN],
      'user-management': [UserRole.ADMIN],
      'tariff-calculator': [UserRole.USER, UserRole.ANALYST, UserRole.ADMIN],
      'database-access': [UserRole.USER, UserRole.ANALYST, UserRole.ADMIN],
    };

    const requiredRoles = featurePermissions[feature];
    if (!requiredRoles) return true; // If no specific permissions defined, allow access

    return hasRole(requiredRoles);
  };

  return {
    isAuthenticated,
    isLoading,
    user,
    hasRole,
    isAdmin,
    isAnalyst,
    isEmailVerified,
    canAccess,
  };
};

export default useAuthGuard;