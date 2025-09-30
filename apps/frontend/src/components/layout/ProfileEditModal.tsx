import React, { useState, useEffect } from 'react';
import { Dialog, DialogContent, DialogTrigger } from '@radix-ui/react-dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

type Profile = {
  name: string;
  role: string;
  email: string;
  location: string;
  avatar?: string;
};

const STORAGE_KEY = 'app_profile';

export default function ProfileEditModal({ children }: { children: React.ReactNode }) {
  const [open, setOpen] = useState(false);
  const [profile, setProfile] = useState<Profile | null>(null); // no user initially

  useEffect(() => {
    const raw = typeof window !== 'undefined' && localStorage.getItem(STORAGE_KEY);
    if (raw) {
      try {
        setProfile(JSON.parse(raw));
      } catch (e) {}
    }
  }, []);

  const save = () => {
    if (!profile) return;
    localStorage.setItem(STORAGE_KEY, JSON.stringify(profile));
    setOpen(false);
    window.dispatchEvent(new Event('profile:updated'));
  };

  const onAvatar = (file?: File) => {
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      setProfile((p) => (p ? { ...p, avatar: reader.result as string } : null));
    };
    reader.readAsDataURL(file);
  };

  if (!profile) {
    return (
      <div className="p-4 border rounded-md bg-muted">
        <p className="text-sm text-muted-foreground mb-2">
          No user signed in.
        </p>
        <Button
          onClick={() => {
            const newUser: Profile = {
              name: 'New User',
              role: 'Member',
              email: 'user@example.com',
              location: 'Unknown'
            };
            setProfile(newUser);
            localStorage.setItem(STORAGE_KEY, JSON.stringify(newUser));
          }}
        >
          Login
        </Button>
      </div>
    );
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>{children}</DialogTrigger>
      <DialogContent className="max-w-lg">
        <div className="mb-2">
          <h3 className="text-lg font-semibold">Edit profile</h3>
          <p className="text-sm text-muted-foreground">
            Update your name, role, contact details and avatar.
          </p>
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
            <div className="text-sm text-muted-foreground">Role</div>
            <Input
              value={profile.role}
              onChange={(e) => setProfile({ ...profile, role: e.target.value })}
            />
          </label>
          <label className="block">
            <div className="text-sm text-muted-foreground">Email</div>
            <Input
              value={profile.email}
              onChange={(e) => setProfile({ ...profile, email: e.target.value })}
            />
          </label>
          <label className="block">
            <div className="text-sm text-muted-foreground">Location</div>
            <Input
              value={profile.location}
              onChange={(e) => setProfile({ ...profile, location: e.target.value })}
            />
          </label>
          <label className="block">
            <div className="text-sm text-muted-foreground">Avatar</div>
            <input
              type="file"
              accept="image/*"
              onChange={(e) => onAvatar(e.target.files?.[0])}
            />
            {profile.avatar && (
              <img
                src={profile.avatar}
                alt="avatar"
                className="w-20 h-20 rounded-full mt-2"
              />
            )}
          </label>
          <div className="flex justify-end gap-2 mt-4">
            <Button variant="secondary" onClick={() => setOpen(false)}>
              Cancel
            </Button>
            <Button onClick={save}>Save</Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
