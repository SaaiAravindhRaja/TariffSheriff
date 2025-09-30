import React from 'react';
import { motion } from 'framer-motion';
import { Moon, Sun, User, Bell } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useNavigate } from 'react-router-dom';
import { CountrySearch } from '@/components/search/CountrySearch';
import { useTheme } from '@/hooks/useTheme';
import { cn } from '@/lib/utils';
import safeLocalStorage from '@/lib/safeLocalStorage';
import * as jwt_decode from 'jwt-decode';

interface JwtPayload {
  sub: string;
  roles: string;
  exp: number;
}

interface Profile {
  name: string;
  role: string;
  avatar?: string;
}

interface HeaderProps {
  className?: string;
}

export function Header({ className }: HeaderProps) {
  const { resolvedTheme, toggleTheme } = useTheme();
  const navigate = useNavigate();
  const [profile, setProfile] = React.useState<Profile | null>(null);

  React.useEffect(() => {
    const token = safeLocalStorage.get<string>('token');
    const rawProfile = safeLocalStorage.get<string>('app_profile');

    if (token) {
      try {
        const decoded = jwt_decode<JwtPayload>(token);
        const now = Date.now() / 1000;
        if (decoded.exp > now) {
          if (rawProfile) {
            setProfile(JSON.parse(rawProfile));
          } else {
            setProfile({ name: decoded.sub, role: 'USER' });
          }
        } else {
          safeLocalStorage.remove('token');
          safeLocalStorage.remove('app_profile');
          setProfile(null);
        }
      } catch {
        safeLocalStorage.remove('token');
        safeLocalStorage.remove('app_profile');
        setProfile(null);
      }
    } else {
      setProfile(null);
    }

    const handler = () => {
      const updated = safeLocalStorage.get<string>('app_profile');
      if (updated) {
        try {
          setProfile(JSON.parse(updated));
        } catch {}
      } else {
        setProfile(null);
      }
    };

    window.addEventListener('profile:updated', handler);
    return () => window.removeEventListener('profile:updated', handler);
  }, []);

  const handleLogout = () => {
    safeLocalStorage.remove('token');
    safeLocalStorage.remove('app_profile');
    setProfile(null);
    navigate('/');
  };

  return (
    <motion.header
      role="banner"
      initial={{ y: -100, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      transition={{ duration: 0.5 }}
      className={cn(
        'sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60',
        className
      )}
    >
      <div className="container flex h-16 items-center justify-between px-4">
        {/* Logo */}
        <motion.div className="flex items-center space-x-3">
          <a
            onClick={(e) => {
              e.preventDefault();
              navigate('/');
            }}
            href="/"
            aria-label="Go to home"
            className="flex items-center rounded-md focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-brand-500"
          ></a>
          <div className="flex flex-col">
            <h1 className="text-xl font-bold bg-gradient-to-r from-brand-600 to-brand-800 bg-clip-text text-transparent">
              TariffSheriff
            </h1>
            <p className="text-xs text-muted-foreground -mt-1">
              Trade Intelligence Platform
            </p>
          </div>
        </motion.div>

        {/* Country Search */}
        <div className="flex-1 max-w-md mx-8">
          <CountrySearch placeholder="Search countries for trade data..." className="w-full" />
        </div>

        {/* Actions */}
        <div className="flex items-center space-x-2">
          <Button variant="ghost" size="icon" className="relative" aria-label="Notifications">
            <Bell className="w-5 h-5" />
            <span className="absolute -top-1 -right-1 w-3 h-3 bg-danger-500 rounded-full text-xs flex items-center justify-center">
              <span className="w-1.5 h-1.5 bg-white rounded-full"></span>
            </span>
          </Button>

          {/* Theme Toggle */}
          <Button
            variant="ghost"
            size="icon"
            onClick={toggleTheme}
            aria-label={`Toggle theme, currently ${resolvedTheme}`}
          >
            {resolvedTheme === 'dark' ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
          </Button>

          {/* User Info */}
          {profile ? (
            <div className="flex items-center space-x-2 pl-2 border-l">
              <button
                onClick={() => navigate('/profile')}
                className="flex items-center space-x-2 focus:outline-none"
              >
                <div className="flex flex-col text-right">
                  <span className="text-sm font-medium">{profile.name}</span>
                  <span className="text-xs text-muted-foreground">{profile.role}</span>
                </div>
                <div className="w-8 h-8 rounded-full bg-gradient-to-br from-brand-400 to-brand-600 flex items-center justify-center ml-2 overflow-hidden">
                  {profile.avatar ? (
                    <img src={profile.avatar} alt={`${profile.name} avatar`} className="w-full h-full object-cover" />
                  ) : (
                    <User className="w-4 h-4 text-white" />
                  )}
                </div>
              </button>
              <Button variant="ghost" size="sm" onClick={handleLogout}>
                Sign out
              </Button>
            </div>
          ) : (
            <Button variant="outline" size="sm" onClick={() => navigate('/login')}>
              Sign in
            </Button>
          )}
        </div>
      </div>
    </motion.header>
  );
}
