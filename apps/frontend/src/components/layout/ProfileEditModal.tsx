import React, { useState, useEffect } from 'react'
import { Dialog, DialogContent, DialogTrigger } from '@radix-ui/react-dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { useToast } from '@/components/ui/toast'
import api from '@/services/api'
import axios from 'axios'

type Profile = {
  id?: number
  name: string
  email: string
  aboutMe?: string
}

export function ProfileEditModal({ children, profile: initialProfile }: { children: React.ReactNode; profile: Profile }) {
  const [open, setOpen] = useState(false)
  const [profile, setProfile] = useState<Profile>(initialProfile)
  const [saving, setSaving] = useState(false)
  const { showToast } = useToast()

  useEffect(() => {
    setProfile(initialProfile)
  }, [initialProfile])

  const save = async () => {
    setSaving(true)
    try {
      await api.put('/profile', {
        name: profile.name,
        aboutMe: profile.aboutMe || ''
      })
      
      // Show success toast
      showToast('Profile updated successfully!', 'success')
      
      setOpen(false)
      // Dispatch event to refresh profile
      window.dispatchEvent(new Event('profile:updated'))
    } catch (error: any) {
      console.error('Failed to update profile:', error)
      
      if (axios.isAxiosError(error) && error.response?.status === 403) {
        showToast('Authentication error: Please log out and log in again.', 'error')
      } else {
        showToast(error.response?.data?.message || 'Failed to update profile. Please try again.', 'error')
      }
    } finally {
      setSaving(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>{children}</DialogTrigger>
      <DialogContent className="max-w-lg">
        <div className="mb-2">
          <h3 className="text-lg font-semibold">Edit profile</h3>
          <p className="text-sm text-muted-foreground">Update your name, role, contact details and avatar.</p>
        </div>
        <div className="space-y-4 mt-4">
          <label className="block">
            <div className="text-sm text-muted-foreground">Full name</div>
            <Input 
              value={profile.name} 
              onChange={(e) => setProfile({ ...profile, name: e.target.value })} 
            />
          </label>
          <label className="block">
            <div className="text-sm text-muted-foreground">Email (read-only)</div>
            <Input 
              value={profile.email} 
              disabled 
            />
          </label>
          <label className="block">
            <div className="text-sm text-muted-foreground">About me (optional)</div>
            <Textarea 
              value={profile.aboutMe || ''} 
              onChange={(e) => setProfile({ ...profile, aboutMe: e.target.value })} 
              placeholder="Tell us about yourself..."
              rows={4}
            />
          </label>
          <div className="flex justify-end gap-2 mt-4">
            <Button variant="secondary" onClick={() => setOpen(false)} disabled={saving}>
              Cancel
            </Button>
            <Button onClick={save} disabled={saving}>
              {saving ? 'Saving...' : 'Save'}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}

export default ProfileEditModal
