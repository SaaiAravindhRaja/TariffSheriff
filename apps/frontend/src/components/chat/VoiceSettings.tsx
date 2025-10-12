import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import { 
  Settings, 
  Volume2, 
  Mic, 
  Globe, 
  Zap,
  RotateCcw,
  Save,
  TestTube
} from 'lucide-react';
import { voiceService, VoiceSettings as VoiceSettingsType } from '@/services/VoiceService';

interface VoiceSettingsProps {
  className?: string;
  onSettingsChange?: (settings: VoiceSettingsType) => void;
}

const languages = [
  { code: 'en-US', name: 'English (US)' },
  { code: 'en-GB', name: 'English (UK)' },
  { code: 'es-ES', name: 'Spanish (Spain)' },
  { code: 'es-MX', name: 'Spanish (Mexico)' },
  { code: 'fr-FR', name: 'French (France)' },
  { code: 'de-DE', name: 'German (Germany)' },
  { code: 'it-IT', name: 'Italian (Italy)' },
  { code: 'pt-BR', name: 'Portuguese (Brazil)' },
  { code: 'ja-JP', name: 'Japanese (Japan)' },
  { code: 'ko-KR', name: 'Korean (South Korea)' },
  { code: 'zh-CN', name: 'Chinese (Simplified)' },
  { code: 'zh-TW', name: 'Chinese (Traditional)' },
];

export const VoiceSettings: React.FC<VoiceSettingsProps> = ({
  className,
  onSettingsChange,
}) => {
  const [settings, setSettings] = useState<VoiceSettingsType>(voiceService.getSettings());
  const [availableVoices, setAvailableVoices] = useState<SpeechSynthesisVoice[]>([]);
  const [hasChanges, setHasChanges] = useState(false);
  const [isTestingVoice, setIsTestingVoice] = useState(false);

  useEffect(() => {
    // Load available voices
    const loadVoices = () => {
      const voices = voiceService.getAvailableVoices();
      setAvailableVoices(voices);
    };

    loadVoices();
    
    // Some browsers load voices asynchronously
    if (speechSynthesis.onvoiceschanged !== undefined) {
      speechSynthesis.onvoiceschanged = loadVoices;
    }

    return () => {
      if (speechSynthesis.onvoiceschanged !== undefined) {
        speechSynthesis.onvoiceschanged = null;
      }
    };
  }, []);

  const handleSettingChange = (key: keyof VoiceSettingsType, value: any) => {
    const newSettings = { ...settings, [key]: value };
    setSettings(newSettings);
    setHasChanges(true);
  };

  const handleSaveSettings = () => {
    voiceService.updateSettings(settings);
    setHasChanges(false);
    onSettingsChange?.(settings);
  };

  const handleResetSettings = () => {
    const defaultSettings: VoiceSettingsType = {
      language: 'en-US',
      continuous: false,
      interimResults: true,
      maxAlternatives: 3,
      voiceRate: 1,
      voicePitch: 1,
      voiceVolume: 1,
    };
    setSettings(defaultSettings);
    setHasChanges(true);
  };

  const handleTestVoice = async () => {
    setIsTestingVoice(true);
    try {
      const testText = "This is a test of the voice synthesis settings. The rate, pitch, and volume are configured as shown.";
      await voiceService.speak(testText, {
        rate: settings.voiceRate,
        pitch: settings.voicePitch,
        volume: settings.voiceVolume,
        lang: settings.language,
      });
    } catch (error) {
      console.error('Voice test failed:', error);
    } finally {
      setIsTestingVoice(false);
    }
  };

  const getVoicesForLanguage = (languageCode: string) => {
    return availableVoices.filter(voice => 
      voice.lang.startsWith(languageCode.split('-')[0])
    );
  };

  const selectedLanguageVoices = getVoicesForLanguage(settings.language);

  return (
    <Card className={className}>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Settings size={20} />
          Voice Settings
        </CardTitle>
        <p className="text-sm text-muted-foreground">
          Configure voice recognition and speech synthesis preferences
        </p>
      </CardHeader>
      
      <CardContent className="space-y-6">
        {/* Speech Recognition Settings */}
        <div>
          <Label className="text-sm font-medium flex items-center gap-2 mb-3">
            <Mic size={16} />
            Speech Recognition
          </Label>
          
          <div className="space-y-4">
            {/* Language Selection */}
            <div>
              <Label className="text-sm">Recognition Language</Label>
              <select
                value={settings.language}
                onChange={(e) => handleSettingChange('language', e.target.value)}
                className="w-full mt-1 px-3 py-2 text-sm border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary"
              >
                {languages.map((lang) => (
                  <option key={lang.code} value={lang.code}>
                    {lang.name}
                  </option>
                ))}
              </select>
            </div>

            {/* Recognition Options */}
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <div>
                  <Label className="text-sm">Continuous Recognition</Label>
                  <p className="text-xs text-muted-foreground">
                    Keep listening after each result
                  </p>
                </div>
                <button
                  onClick={() => handleSettingChange('continuous', !settings.continuous)}
                  className={cn(
                    "relative inline-flex h-6 w-11 items-center rounded-full transition-colors",
                    settings.continuous ? "bg-primary" : "bg-muted"
                  )}
                >
                  <span
                    className={cn(
                      "inline-block h-4 w-4 transform rounded-full bg-white transition-transform",
                      settings.continuous ? "translate-x-6" : "translate-x-1"
                    )}
                  />
                </button>
              </div>

              <div className="flex items-center justify-between">
                <div>
                  <Label className="text-sm">Interim Results</Label>
                  <p className="text-xs text-muted-foreground">
                    Show results while speaking
                  </p>
                </div>
                <button
                  onClick={() => handleSettingChange('interimResults', !settings.interimResults)}
                  className={cn(
                    "relative inline-flex h-6 w-11 items-center rounded-full transition-colors",
                    settings.interimResults ? "bg-primary" : "bg-muted"
                  )}
                >
                  <span
                    className={cn(
                      "inline-block h-4 w-4 transform rounded-full bg-white transition-transform",
                      settings.interimResults ? "translate-x-6" : "translate-x-1"
                    )}
                  />
                </button>
              </div>
            </div>

            {/* Max Alternatives */}
            <div>
              <Label className="text-sm">Alternative Results</Label>
              <div className="flex items-center gap-2 mt-1">
                <input
                  type="range"
                  min="1"
                  max="5"
                  value={settings.maxAlternatives}
                  onChange={(e) => handleSettingChange('maxAlternatives', parseInt(e.target.value))}
                  className="flex-1 h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer"
                />
                <Badge variant="secondary" className="text-xs min-w-[2rem] text-center">
                  {settings.maxAlternatives}
                </Badge>
              </div>
              <p className="text-xs text-muted-foreground mt-1">
                Number of alternative transcriptions to consider
              </p>
            </div>
          </div>
        </div>

        {/* Speech Synthesis Settings */}
        <div>
          <Label className="text-sm font-medium flex items-center gap-2 mb-3">
            <Volume2 size={16} />
            Speech Synthesis
          </Label>
          
          <div className="space-y-4">
            {/* Voice Selection */}
            {selectedLanguageVoices.length > 0 && (
              <div>
                <Label className="text-sm">Voice</Label>
                <select
                  value={settings.preferredVoice || ''}
                  onChange={(e) => handleSettingChange('preferredVoice', e.target.value || undefined)}
                  className="w-full mt-1 px-3 py-2 text-sm border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary"
                >
                  <option value="">Default Voice</option>
                  {selectedLanguageVoices.map((voice) => (
                    <option key={voice.name} value={voice.name}>
                      {voice.name} {voice.localService ? '(Local)' : '(Network)'}
                    </option>
                  ))}
                </select>
              </div>
            )}

            {/* Voice Rate */}
            <div>
              <Label className="text-sm">Speech Rate</Label>
              <div className="flex items-center gap-2 mt-1">
                <input
                  type="range"
                  min="0.5"
                  max="2"
                  step="0.1"
                  value={settings.voiceRate}
                  onChange={(e) => handleSettingChange('voiceRate', parseFloat(e.target.value))}
                  className="flex-1 h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer"
                />
                <Badge variant="secondary" className="text-xs min-w-[3rem] text-center">
                  {settings.voiceRate.toFixed(1)}x
                </Badge>
              </div>
            </div>

            {/* Voice Pitch */}
            <div>
              <Label className="text-sm">Speech Pitch</Label>
              <div className="flex items-center gap-2 mt-1">
                <input
                  type="range"
                  min="0.5"
                  max="2"
                  step="0.1"
                  value={settings.voicePitch}
                  onChange={(e) => handleSettingChange('voicePitch', parseFloat(e.target.value))}
                  className="flex-1 h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer"
                />
                <Badge variant="secondary" className="text-xs min-w-[3rem] text-center">
                  {settings.voicePitch.toFixed(1)}
                </Badge>
              </div>
            </div>

            {/* Voice Volume */}
            <div>
              <Label className="text-sm">Speech Volume</Label>
              <div className="flex items-center gap-2 mt-1">
                <input
                  type="range"
                  min="0.1"
                  max="1"
                  step="0.1"
                  value={settings.voiceVolume}
                  onChange={(e) => handleSettingChange('voiceVolume', parseFloat(e.target.value))}
                  className="flex-1 h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer"
                />
                <Badge variant="secondary" className="text-xs min-w-[3rem] text-center">
                  {Math.round(settings.voiceVolume * 100)}%
                </Badge>
              </div>
            </div>

            {/* Test Voice Button */}
            <Button
              variant="outline"
              onClick={handleTestVoice}
              disabled={isTestingVoice}
              className="w-full"
            >
              {isTestingVoice ? (
                <>
                  <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-current mr-2" />
                  Testing Voice...
                </>
              ) : (
                <>
                  <TestTube size={16} className="mr-2" />
                  Test Voice Settings
                </>
              )}
            </Button>
          </div>
        </div>

        {/* System Information */}
        <div className="p-3 bg-muted/50 rounded-lg">
          <Label className="text-sm font-medium mb-2 block">System Information</Label>
          <div className="grid grid-cols-2 gap-2 text-xs">
            <div>
              <span className="text-muted-foreground">Recognition:</span>
              <span className="ml-1 font-medium">
                {voiceService.isVoiceSupported() ? 'Supported' : 'Not Supported'}
              </span>
            </div>
            <div>
              <span className="text-muted-foreground">Synthesis:</span>
              <span className="ml-1 font-medium">
                {voiceService.isSpeechSynthesisSupported() ? 'Supported' : 'Not Supported'}
              </span>
            </div>
            <div>
              <span className="text-muted-foreground">Available Voices:</span>
              <span className="ml-1 font-medium">{availableVoices.length}</span>
            </div>
            <div>
              <span className="text-muted-foreground">Language Voices:</span>
              <span className="ml-1 font-medium">{selectedLanguageVoices.length}</span>
            </div>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex gap-3 pt-4 border-t">
          <Button
            variant="outline"
            onClick={handleResetSettings}
            className="flex-1"
          >
            <RotateCcw size={16} className="mr-2" />
            Reset to Defaults
          </Button>
          
          <Button
            onClick={handleSaveSettings}
            disabled={!hasChanges}
            className="flex-1"
          >
            <Save size={16} className="mr-2" />
            {hasChanges ? 'Save Changes' : 'Saved'}
          </Button>
        </div>

        {hasChanges && (
          <div className="text-xs text-muted-foreground text-center">
            Changes will be applied immediately after saving
          </div>
        )}
      </CardContent>
    </Card>
  );
};

VoiceSettings.displayName = 'VoiceSettings';