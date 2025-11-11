import React, { useEffect, useState } from 'react'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { User, Mail } from 'lucide-react'
import ProfileEditModal from '@/components/layout/ProfileEditModal'
import { useAuth } from '@/hooks/useAuth'
import api from '@/services/api'

type ProfileType = {
  id?: number
  name: string
  email: string
  aboutMe?: string
}

export default function Profile() {
  const { user, logout } = useAuth()
  const [profile, setProfile] = useState<ProfileType | null>(null)
  const [loading, setLoading] = useState(true)

  // Fetch profile from backend
  const fetchProfile = async () => {
    try {
      const response = await api.get('/profile')
      setProfile(response.data)
    } catch (error) {
      console.error('Failed to fetch profile:', error)
      // Fallback to Auth0 user data
      if (user) {
        setProfile({
          name: user.name || user.email || 'User',
          email: user.email || '',
          aboutMe: ''
        })
      }
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchProfile()
  }, [user])

  // Listen for profile updates
  useEffect(() => {
    const handler = () => {
      fetchProfile()
    }
    window.addEventListener('profile:updated', handler)
    return () => window.removeEventListener('profile:updated', handler)
  }, [])

  const handleSignOut = () => {
    logout()
  }

  if (loading) {
    return (
      <div className="p-8 max-w-4xl mx-auto flex items-center justify-center">
        <div className="text-muted-foreground">Loading profile...</div>
      </div>
    )
  }

  if (!profile) {
    return (
      <div className="p-8 max-w-4xl mx-auto flex items-center justify-center">
        <div className="text-muted-foreground">Profile not found</div>
      </div>
    )
  }

  return (
    <div className="p-8 max-w-4xl mx-auto">
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between w-full">
            <div className="flex items-center gap-4">
              <div className="w-20 h-20 rounded-full bg-gradient-to-br from-brand-400 to-brand-600 flex items-center justify-center text-white text-2xl overflow-hidden">
                <User className="w-8 h-8" />
              </div>
              <div>
                <CardTitle className="text-2xl">{profile.name}</CardTitle>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <ProfileEditModal profile={profile}>
                <Button variant="secondary">Edit Profile</Button>
              </ProfileEditModal>
              <Button onClick={handleSignOut} variant="outline">Sign Out</Button>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-4">
              <h3 className="text-lg font-semibold">Contact</h3>
              <div className="flex items-center gap-3 text-sm text-muted-foreground">
                <Mail className="w-4 h-4" />
                <div>
                  <div className="font-medium">{profile.email}</div>
                  <div className="text-xs">Primary email</div>
                </div>
              </div>
            </div>

            {profile.aboutMe && (
              <div className="space-y-4">
                <h3 className="text-lg font-semibold">About</h3>
                <p className="text-sm text-muted-foreground">
                  {profile.aboutMe}
                </p>
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
