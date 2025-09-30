import React, { useEffect, useState } from 'react'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { User, Mail, Briefcase, MapPin } from 'lucide-react'
import ProfileEditModal from '@/components/layout/ProfileEditModal'
import safeLocalStorage from '@/lib/safeLocalStorage'

type ProfileType = {
  name: string
  role: string
  email: string
  location: string
  avatar?: string
}

const STORAGE_KEY = 'app_profile'

export default function Profile() {
  const [profile, setProfile] = useState<ProfileType>({
    name: 'John Doe',
    role: 'Trade Analyst',
    email: 'john.doe@example.com',
    location: 'New York, USA'
  })

  useEffect(() => {
    const raw = safeLocalStorage.get<string>(STORAGE_KEY)
    if (raw) {
      try {
        setProfile(typeof raw === 'string' ? JSON.parse(raw) : raw)
      } catch {
        // ignore
      }
    }

    const handler = () => {
      const updated = safeLocalStorage.get<string>(STORAGE_KEY)
      if (updated) {
        try {
          setProfile(typeof updated === 'string' ? JSON.parse(updated) : updated)
        } catch {
          // ignore
        }
      }
    }
    try {
      if (typeof window !== 'undefined') window.addEventListener('profile:updated', handler)
    } catch {
      // non-browser env
    }
    return () => {
      try {
        if (typeof window !== 'undefined') window.removeEventListener('profile:updated', handler)
      } catch {
        // noop
      }
    }
  }, [])

  return (
    <div className="p-8 max-w-4xl mx-auto">
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between w-full">
            <div className="flex items-center gap-4">
              <div className="w-20 h-20 rounded-full bg-gradient-to-br from-brand-400 to-brand-600 flex items-center justify-center text-white text-2xl overflow-hidden">
                {profile.avatar ? (
                  <img src={profile.avatar} alt="avatar" className="w-full h-full object-cover" />
                ) : (
                  <User className="w-8 h-8" />
                )}
              </div>
              <div>
                <CardTitle className="text-2xl">{profile.name}</CardTitle>
                <CardDescription>{profile.role}</CardDescription>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <ProfileEditModal>
                <Button variant="secondary">Edit Profile</Button>
              </ProfileEditModal>
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
              <div className="flex items-center gap-3 text-sm text-muted-foreground">
                <MapPin className="w-4 h-4" />
                <div>
                  <div className="font-medium">{profile.location}</div>
                  <div className="text-xs">Location</div>
                </div>
              </div>
            </div>

            <div className="space-y-4">
              <h3 className="text-lg font-semibold">About</h3>
              <p className="text-sm text-muted-foreground">
                {profile.name} is a Trade Analyst with a focus on tariff policy and supply chain impacts. They help teams
                understand tariff exposure, compliance risks, and identify cost optimization opportunities.
              </p>
              <div className="mt-4">
                <h4 className="text-sm font-medium">Role</h4>
                <div className="text-sm text-muted-foreground flex items-center gap-2">
                  <Briefcase className="w-4 h-4" /> {profile.role}
                </div>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
