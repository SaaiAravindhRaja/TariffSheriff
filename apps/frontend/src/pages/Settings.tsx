import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { 
  Settings as SettingsIcon, 
  User,
  Bell,
  Globe,
  Shield,
  Database,
  Palette,
  Download,
  Upload,
  Trash2,
  Save,
  RefreshCw,
  CheckCircle,
  AlertTriangle,
  Info
} from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Switch } from '@/components/ui/switch';
import { useCountries } from '@/hooks/useCountries';
import safeLocalStorage from '@/lib/safeLocalStorage';
import { appConfig } from '@/config/app';

interface UserSettings {
  email: string;
  firstName: string;
  lastName: string;
  company: string;
  role: string;
  timezone: string;
  language: string;
  currency: string;
  dateFormat: string;
  notifications: {
    email: boolean;
    push: boolean;
    tariffUpdates: boolean;
    tradeAlerts: boolean;
    weeklyReports: boolean;
  };
  privacy: {
    analytics: boolean;
    dataSharing: boolean;
    marketingEmails: boolean;
  };
  display: {
    animations: boolean;
    compactMode: boolean;
    showFlags: boolean;
    defaultView: string;
  };
}

const defaultSettings: UserSettings = {
  email: '',
  firstName: '',
  lastName: '',
  company: '',
  role: 'Trade Analyst',
  timezone: 'UTC',
  language: 'en-US',
  currency: 'USD',
  dateFormat: 'MM/DD/YYYY',
  notifications: {
    email: true,
    push: true,
    tariffUpdates: true,
    tradeAlerts: true,
    weeklyReports: false,
  },
  privacy: {
    analytics: true,
    dataSharing: false,
    marketingEmails: false,
  },
  display: {
    animations: true,
    compactMode: false,
    showFlags: true,
    defaultView: 'dashboard',
  },
};

export function Settings() {
  const { regions, currencies } = useCountries();
  const [settings, setSettings] = useState<UserSettings>(defaultSettings);
  const [saved, setSaved] = useState(false);
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('general');

  // Load settings on mount
  useEffect(() => {
    const savedSettings = safeLocalStorage.get<UserSettings>('userSettings');
    if (savedSettings) {
      setSettings({ ...defaultSettings, ...savedSettings });
    }
  }, []);

  // Save settings
  const handleSave = async () => {
    setLoading(true);
    try {
      safeLocalStorage.set('userSettings', settings);
      setSaved(true);
      setTimeout(() => setSaved(false), 3000);
    } catch (error) {
      console.error('Failed to save settings:', error);
    } finally {
      setLoading(false);
    }
  };

  // Reset settings
  const handleReset = () => {
    setSettings(defaultSettings);
    safeLocalStorage.remove('userSettings');
  };

  // Export settings
  const handleExport = () => {
    const dataStr = JSON.stringify(settings, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'tariffsheriff-settings.json';
    link.click();
    URL.revokeObjectURL(url);
  };

  // Import settings
  const handleImport = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (e) => {
        try {
          const importedSettings = JSON.parse(e.target?.result as string);
          setSettings({ ...defaultSettings, ...importedSettings });
        } catch (error) {
          console.error('Failed to import settings:', error);
        }
      };
      reader.readAsText(file);
    }
  };

  const updateSettings = (path: string, value: any) => {
    // Defensive: prevent prototype pollution by rejecting dangerous path segments
    const denylist = new Set(['__proto__', 'prototype', 'constructor']);
    if (!path || typeof path !== 'string') {
      console.warn('updateSettings called with invalid path:', path);
      return;
    }

    const keys = path.split('.');
    if (keys.some(k => denylist.has(k))) {
      console.warn('Ignored updateSettings path containing unsafe segment:', path);
      return;
    }

    setSettings(prev => {
      const newSettings = { ...prev } as any;
      let current: any = newSettings;

      for (let i = 0; i < keys.length - 1; i++) {
        const key = keys[i];
        const next = current[key];
        // Only copy plain objects; if something unexpected is encountered, replace with an object
        current[key] = (next && typeof next === 'object' && !Array.isArray(next)) ? { ...next } : {};
        current = current[key];
      }

      const lastKey = keys[keys.length - 1];
      current[lastKey] = value;
      return newSettings;
    });
  };

  return (
    <div className="flex-1 space-y-6 p-6 max-w-4xl mx-auto">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="flex items-center justify-between"
      >
        <div>
          <h1 className="text-3xl font-bold tracking-tight flex items-center gap-3">
            <SettingsIcon className="w-8 h-8 text-brand-600" />
            Settings
          </h1>
          <p className="text-muted-foreground">
            Manage your account preferences and application settings
          </p>
        </div>
        
        <div className="flex items-center space-x-2">
          <Button variant="outline" onClick={handleExport}>
            <Download className="w-4 h-4 mr-2" />
            Export
          </Button>
          <Button variant="outline" onClick={handleReset}>
            <RefreshCw className="w-4 h-4 mr-2" />
            Reset
          </Button>
          <Button onClick={handleSave} disabled={loading}>
            {loading ? (
              <RefreshCw className="w-4 h-4 mr-2 animate-spin" />
            ) : (
              <Save className="w-4 h-4 mr-2" />
            )}
            Save Changes
          </Button>
        </div>
      </motion.div>

      {/* Success Message */}
      {saved && (
        <motion.div
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -10 }}
          className="bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg p-4"
        >
          <div className="flex items-center">
            <CheckCircle className="w-5 h-5 text-green-600 dark:text-green-400 mr-2" />
            <span className="text-green-800 dark:text-green-200">Settings saved successfully!</span>
          </div>
        </motion.div>
      )}

      {/* Settings Tabs */}
      <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
        <TabsList className="grid w-full grid-cols-5">
          <TabsTrigger value="general">General</TabsTrigger>
          <TabsTrigger value="appearance">Appearance</TabsTrigger>
          <TabsTrigger value="notifications">Notifications</TabsTrigger>
          <TabsTrigger value="privacy">Privacy</TabsTrigger>
          <TabsTrigger value="data">Data</TabsTrigger>
        </TabsList>

        {/* General Settings */}
        <TabsContent value="general" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center">
                <User className="w-5 h-5 mr-2" />
                Profile Information
              </CardTitle>
              <CardDescription>
                Update your personal information and preferences
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-2">First Name</label>
                  <input
                    type="text"
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-800"
                    value={settings.firstName}
                    onChange={(e) => updateSettings('firstName', e.target.value)}
                    placeholder="John"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-2">Last Name</label>
                  <input
                    type="text"
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-800"
                    value={settings.lastName}
                    onChange={(e) => updateSettings('lastName', e.target.value)}
                    placeholder="Doe"
                  />
                </div>
              </div>
              
              <div>
                <label className="block text-sm font-medium mb-2">Email Address</label>
                <input
                  type="email"
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-800"
                  value={settings.email}
                  onChange={(e) => updateSettings('email', e.target.value)}
                  placeholder="john.doe@company.com"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-2">Company</label>
                  <input
                    type="text"
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-800"
                    value={settings.company}
                    onChange={(e) => updateSettings('company', e.target.value)}
                    placeholder="Acme Corp"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-2">Role</label>
                  <select
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-800"
                    value={settings.role}
                    onChange={(e) => updateSettings('role', e.target.value)}
                  >
                    <option value="Trade Analyst">Trade Analyst</option>
                    <option value="Import Manager">Import Manager</option>
                    <option value="Export Manager">Export Manager</option>
                    <option value="Compliance Officer">Compliance Officer</option>
                    <option value="Finance Manager">Finance Manager</option>
                    <option value="Operations Manager">Operations Manager</option>
                  </select>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center">
                <Globe className="w-5 h-5 mr-2" />
                Regional Preferences
              </CardTitle>
              <CardDescription>
                Set your timezone, language, and currency preferences
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-3 gap-4">
                <div>
                  <label className="block text-sm font-medium mb-2">Language</label>
                  <select
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-800"
                    value={settings.language}
                    onChange={(e) => updateSettings('language', e.target.value)}
                  >
                    {appConfig.i18n.supportedLocales.map(locale => (
                      <option key={locale} value={locale}>
                        {new Intl.DisplayNames([locale], { type: 'language' }).of(locale)}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium mb-2">Currency</label>
                  <select
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-800"
                    value={settings.currency}
                    onChange={(e) => updateSettings('currency', e.target.value)}
                  >
                    {currencies.map(currency => (
                      <option key={currency} value={currency}>{currency}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium mb-2">Date Format</label>
                  <select
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-800"
                    value={settings.dateFormat}
                    onChange={(e) => updateSettings('dateFormat', e.target.value)}
                  >
                    <option value="MM/DD/YYYY">MM/DD/YYYY</option>
                    <option value="DD/MM/YYYY">DD/MM/YYYY</option>
                    <option value="YYYY-MM-DD">YYYY-MM-DD</option>
                  </select>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        {/* Appearance Settings */}
        <TabsContent value="appearance" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center">
                <Palette className="w-5 h-5 mr-2" />
                Display Preferences
              </CardTitle>
              <CardDescription>
                Adjust layout and interface details to match your workflow
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <div>
                    <label className="text-sm font-medium">Enable Animations</label>
                    <p className="text-sm text-muted-foreground">Smooth transitions and motion effects</p>
                  </div>
                  <Switch
                    checked={settings.display.animations}
                    onCheckedChange={(checked) => updateSettings('display.animations', checked)}
                  />
                </div>

                <div className="flex items-center justify-between">
                  <div>
                    <label className="text-sm font-medium">Compact Mode</label>
                    <p className="text-sm text-muted-foreground">Reduce spacing and padding</p>
                  </div>
                  <Switch
                    checked={settings.display.compactMode}
                    onCheckedChange={(checked) => updateSettings('display.compactMode', checked)}
                  />
                </div>

                <div className="flex items-center justify-between">
                  <div>
                    <label className="text-sm font-medium">Show Country Flags</label>
                    <p className="text-sm text-muted-foreground">Display flag emojis next to country names</p>
                  </div>
                  <Switch
                    checked={settings.display.showFlags}
                    onCheckedChange={(checked) => updateSettings('display.showFlags', checked)}
                  />
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        {/* Notifications Settings */}
        <TabsContent value="notifications" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center">
                <Bell className="w-5 h-5 mr-2" />
                Notification Preferences
              </CardTitle>
              <CardDescription>
                Choose what notifications you want to receive
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              {[
                { key: 'email', label: 'Email Notifications', description: 'Receive notifications via email' },
                { key: 'push', label: 'Push Notifications', description: 'Browser push notifications' },
                { key: 'tariffUpdates', label: 'Tariff Updates', description: 'When tariff rates change' },
                { key: 'tradeAlerts', label: 'Trade Alerts', description: 'Important trade policy changes' },
                { key: 'weeklyReports', label: 'Weekly Reports', description: 'Summary of your trade activity' },
              ].map(({ key, label, description }) => (
                <div key={key} className="flex items-center justify-between">
                  <div>
                    <label className="text-sm font-medium">{label}</label>
                    <p className="text-sm text-muted-foreground">{description}</p>
                  </div>
                  <Switch
                    checked={settings.notifications[key as keyof typeof settings.notifications]}
                    onCheckedChange={(checked) => updateSettings(`notifications.${key}`, checked)}
                  />
                </div>
              ))}
            </CardContent>
          </Card>
        </TabsContent>

        {/* Privacy Settings */}
        <TabsContent value="privacy" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center">
                <Shield className="w-5 h-5 mr-2" />
                Privacy & Security
              </CardTitle>
              <CardDescription>
                Control your privacy and data sharing preferences
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              {[
                { key: 'analytics', label: 'Usage Analytics', description: 'Help improve the app by sharing usage data' },
                { key: 'dataSharing', label: 'Data Sharing', description: 'Share anonymized data for research' },
                { key: 'marketingEmails', label: 'Marketing Emails', description: 'Receive product updates and offers' },
              ].map(({ key, label, description }) => (
                <div key={key} className="flex items-center justify-between">
                  <div>
                    <label className="text-sm font-medium">{label}</label>
                    <p className="text-sm text-muted-foreground">{description}</p>
                  </div>
                  <Switch
                    checked={settings.privacy[key as keyof typeof settings.privacy]}
                    onCheckedChange={(checked) => updateSettings(`privacy.${key}`, checked)}
                  />
                </div>
              ))}
            </CardContent>
          </Card>
        </TabsContent>

        {/* Data Management */}
        <TabsContent value="data" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center">
                <Database className="w-5 h-5 mr-2" />
                Data Management
              </CardTitle>
              <CardDescription>
                Import, export, and manage your application data
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <Button variant="outline" onClick={handleExport} className="h-auto p-4">
                  <div className="text-center">
                    <Download className="w-6 h-6 mx-auto mb-2" />
                    <div className="font-medium">Export Settings</div>
                    <div className="text-sm text-muted-foreground">Download your preferences</div>
                  </div>
                </Button>
                
                <label className="cursor-pointer">
                  <input
                    type="file"
                    accept=".json"
                    onChange={handleImport}
                    className="hidden"
                  />
                  <div className="h-full p-4 border-2 border-dashed border-gray-300 dark:border-gray-600 rounded-lg hover:border-brand-500 transition-colors">
                    <div className="text-center">
                      <Upload className="w-6 h-6 mx-auto mb-2" />
                      <div className="font-medium">Import Settings</div>
                      <div className="text-sm text-muted-foreground">Upload a settings file</div>
                    </div>
                  </div>
                </label>
              </div>

              <div className="border-t pt-4">
                <div className="flex items-center justify-between">
                  <div>
                    <h4 className="font-medium text-red-600 dark:text-red-400">Danger Zone</h4>
                    <p className="text-sm text-muted-foreground">Irreversible actions</p>
                  </div>
                </div>
                <div className="mt-4 space-y-2">
                  <Button variant="outline" onClick={handleReset} className="w-full justify-start text-red-600 hover:text-red-700">
                    <RefreshCw className="w-4 h-4 mr-2" />
                    Reset All Settings
                  </Button>
                  <Button variant="outline" className="w-full justify-start text-red-600 hover:text-red-700">
                    <Trash2 className="w-4 h-4 mr-2" />
                    Clear All Data
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center">
                <Info className="w-5 h-5 mr-2" />
                Application Info
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <span className="text-muted-foreground">Version:</span>
                  <span className="ml-2 font-mono">1.0.0</span>
                </div>
                <div>
                  <span className="text-muted-foreground">Build:</span>
                  <span className="ml-2 font-mono">2024.01.15</span>
                </div>
                <div>
                  <span className="text-muted-foreground">Environment:</span>
                  <Badge variant="secondary">{process.env.NODE_ENV}</Badge>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}
