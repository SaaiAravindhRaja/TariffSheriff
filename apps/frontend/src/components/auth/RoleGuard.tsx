import React from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { UserRole } from '@/types/auth';

interface RoleGuardProps {
  children: React.ReactNode;
  requiredRoles: UserRole[];
  fallback?: React.ReactNode;
  requireAll?: boolean;
}

/**
 * RoleGuard component for conditionally rendering content based on user roles
 * This is different from ProtectedRoute as it doesn't redirect, just shows/hides content
 */
export const RoleGuard: React.FC<RoleGuardProps> = ({
  children,
  requiredRoles,
  fallback = null,
  requireAll = false,
}) => {
  const { user, isAuthenticated } = useAuth();

  // If not authenticated, don't show content
  if (!isAuthenticated || !user) {
    return <>{fallback}</>;
  }

  // Check role requirements
  const hasAccess = requireAll 
    ? hasAllRoles(user.role, requiredRoles)
    : hasAnyRole(user.role, requiredRoles);

  if (!hasAccess) {
    return <>{fallback}</>;
  }

  return <>{children}</>;
};

/**
 * Check if user has any of the required roles
 */
function hasAnyRole(userRole: UserRole, requiredRoles: UserRole[]): boolean {
  if (requiredRoles.length === 0) return true;
  
  // Define role hierarchy
  const roleHierarchy: Record<UserRole, number> = {
    [UserRole.USER]: 1,
    [UserRole.ANALYST]: 2,
    [UserRole.ADMIN]: 3,
  };

  const userRoleLevel = roleHierarchy[userRole];
  
  // Check if user has any of the required roles or higher
  return requiredRoles.some(role => userRoleLevel >= roleHierarchy[role]);
}

/**
 * Check if user has all of the required roles (for strict role checking)
 */
function hasAllRoles(userRole: UserRole, requiredRoles: UserRole[]): boolean {
  if (requiredRoles.length === 0) return true;
  
  // For simplicity, we'll use the same hierarchy logic
  // In a more complex system, you might have multiple roles per user
  return hasAnyRole(userRole, requiredRoles);
}

export default RoleGuard;