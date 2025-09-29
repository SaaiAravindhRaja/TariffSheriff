import React, { useState } from 'react'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { User, Mail, Briefcase, MapPin, Edit, Save, X, Shield } from 'lucide-react'
import { useAuth } from '@/contexts/AuthContext'
import { UserRole, UserStatus } from '@/types/auth'
import { LoadingSpinner } from '@/components/ui/loading'
import { ChangePasswordForm } from '@/components/auth/ChangePasswordForm'

export default function Profile() {
  const { user, logout, isLoading } = useAuth()
  const [isEditing, setIsEditing] = useState(false)
  const [editedName, setEditedName] = useState('')
  const [showChangePassword, setShowChangePassword] = useState(false)

  // Initialize edited name when user data is available
  React.useEffect(() => {
    if (user && !editedName) {
      setEditedName(user.name)
    }
  }, [user, editedName])

  const handleSaveProfile = async () => {
    // TODO: Implement profile update API call
    // For now, just close editing mode
    setIsEditing(false)
    console.log('Profile update would be implemented here:', { name: editedName })
  }

  const handleCancelEdit = () => {
    setEditedName(user?.name || '')
    setIsEditing(false)
  }

  const handleLogout = async () => {
    try {
      await logout()
    } catch (error) {
      console.error('Logout failed:', error)
    }
  }

  const getRoleDisplayName = (role: UserRole): string => {
    switch (role) {
      case UserRole.ADMIN:
        return 'Administrator'
      case UserRole.ANALYST:
        return 'Trade Analyst'
      case UserRole.USER:
        return 'User'
      default:
        return 'User'
    }
  }

  const getStatusDisplayName = (status: UserStatus): string => {
    switch (status) {
      case UserStatus.ACTIVE:
        return 'Active'
      case UserStatus.PENDING:
        return 'Pending Verification'
      case UserStatus.SUSPENDED:
        return 'Suspended'
      case UserStatus.LOCKED:
        return 'Locked'
      default:
        return 'Unknown'
    }
  }

  const getStatusColor = (status: UserStatus): string => {
    switch (status) {
      case UserStatus.ACTIVE:
        return 'text-green-600 bg-green-50 border-green-200'
      case UserStatus.PENDING:
        return 'text-yellow-600 bg-yellow-50 border-yellow-200'
      case UserStatus.SUSPENDED:
      case UserStatus.LOCKED:
        return 'text-red-600 bg-red-50 border-red-200'
      default:
        return 'text-gray-600 bg-gray-50 border-gray-200'
    }
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <LoadingSpinner />
      </div>
    )
  }

  if (!user) {
    return (
      <div className="p-8 max-w-4xl mx-auto">
        <Card>
          <CardContent className="p-8 text-center">
            <p className="text-muted-foreground">No user data available</p>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="p-8 max-w-4xl mx-auto space-y-6">
      {/* Profile Information Card */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between w-full">
            <div className="flex items-center gap-4">
              <div className="w-20 h-20 rounded-full bg-gradient-to-br from-brand-400 to-brand-600 flex items-center justify-center text-white text-2xl overflow-hidden">
                <User className="w-8 h-8" />
              </div>
              <div className="flex-1">
                {isEditing ? (
                  <div className="space-y-2">
                    <Input
                      value={editedName}
                      onChange={(e) => setEditedName(e.target.value)}
                      className="text-2xl font-bold"
                      placeholder="Enter your name"
                    />
                    <CardDescription>{getRoleDisplayName(user.role)}</CardDescription>
                  </div>
                ) : (
                  <div>
                    <CardTitle className="text-2xl">{user.name}</CardTitle>
                    <CardDescription>{getRoleDisplayName(user.role)}</CardDescription>
                  </div>
                )}
              </div>
            </div>
            <div className="flex items-center gap-2">
              {isEditing ? (
                <>
                  <Button variant="outline" size="sm" onClick={handleCancelEdit}>
                    <X className="w-4 h-4 mr-2" />
                    Cancel
                  </Button>
                  <Button size="sm" onClick={handleSaveProfile}>
                    <Save className="w-4 h-4 mr-2" />
                    Save
                  </Button>
                </>
              ) : (
                <>
                  <Button variant="outline" size="sm" onClick={() => setIsEditing(true)}>
                    <Edit className="w-4 h-4 mr-2" />
                    Edit Profile
                  </Button>
                  <Button variant="destructive" size="sm" onClick={handleLogout}>
                    Sign Out
                  </Button>
                </>
              )}
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-4">
              <h3 className="text-lg font-semibold">Contact Information</h3>
              <div className="flex items-center gap-3 text-sm">
                <Mail className="w-4 h-4 text-muted-foreground" />
                <div>
                  <div className="font-medium">{user.email}</div>
                  <div className="text-xs text-muted-foreground">
                    {user.emailVerified ? 'Verified email' : 'Email not verified'}
                  </div>
                </div>
              </div>
              
              <div className="flex items-center gap-3 text-sm">
                <Shield className="w-4 h-4 text-muted-foreground" />
                <div>
                  <div className="font-medium">Account Status</div>
                  <div className={`text-xs px-2 py-1 rounded-full border inline-block ${getStatusColor(user.status)}`}>
                    {getStatusDisplayName(user.status)}
                  </div>
                </div>
              </div>
            </div>

            <div className="space-y-4">
              <h3 className="text-lg font-semibold">Account Details</h3>
              <div className="space-y-3 text-sm">
                <div className="flex items-center gap-3">
                  <Briefcase className="w-4 h-4 text-muted-foreground" />
                  <div>
                    <div className="font-medium">Role</div>
                    <div className="text-muted-foreground">{getRoleDisplayName(user.role)}</div>
                  </div>
                </div>
                
                <div className="flex items-center gap-3">
                  <User className="w-4 h-4 text-muted-foreground" />
                  <div>
                    <div className="font-medium">Member Since</div>
                    <div className="text-muted-foreground">
                      {new Date(user.createdAt).toLocaleDateString('en-US', {
                        year: 'numeric',
                        month: 'long',
                        day: 'numeric'
                      })}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Security Settings Card */}
      <Card>
        <CardHeader>
          <CardTitle>Security Settings</CardTitle>
          <CardDescription>
            Manage your account security and password
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h4 className="font-medium">Password</h4>
                <p className="text-sm text-muted-foreground">
                  Change your account password
                </p>
              </div>
              <Button
                variant="outline"
                onClick={() => setShowChangePassword(!showChangePassword)}
              >
                {showChangePassword ? 'Cancel' : 'Change Password'}
              </Button>
            </div>
            
            {showChangePassword && (
              <div className="border-t pt-4">
                <ChangePasswordForm
                  onSuccess={() => setShowChangePassword(false)}
                  onCancel={() => setShowChangePassword(false)}
                />
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
