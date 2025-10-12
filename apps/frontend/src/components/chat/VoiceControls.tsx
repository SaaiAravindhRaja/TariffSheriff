import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import { 
  Mic, 
  MicOff, 
  Volume2, 
  VolumeX, 
  Settings, 
  HelpCircle,
  Keyboard,
  Zap,
  AlertCircle,
  CheckCircle
} from 'lucide-react';
import { voiceService, VoiceRecognitionResult } from '@/services/VoiceService';

interface VoiceControlsProps {
  onTranscript?: (transcript: string) => void;
  onVoiceCommand?: (command: string) => void;
  className?: string;
  showCommands?: boolean;
}

export const VoiceControls: React.FC<VoiceControlsProps> = ({
  onTranscript,
  onVoiceCommand,
  className,
  showCommands = false,
}) => {
  const [isListening, setIsListening] = useState(false);
  const [isSpeaking, setIsSpeaking] = useState(false);
  const [isSupported, setIsSupported] = useState(false);
  const [currentTranscript, setCurrentTranscript] = useState('');
  const [confidence, setConfidence] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [status, setStatus] = useState<'idle' | 'listening' | 'processing' | 'error'>('idle');

  useEffect(() => {
    // Check support
    setIsSupported(voiceService.isVoiceSupported());

    // Setup voice service callbacks
    voiceService.onResult((result: VoiceRecognitionResult) => {
      setCurrentTranscript(result.transcript);
      setConfidence(result.confidence);
      
      if (result.isFinal && onTranscript) {
        onTranscript(result.transcript);
        setCurrentTranscript('');
      }
    });

    voiceService.onError((errorMessage: string) => {
      setError(errorMessage);
      setStatus('error');
      setTimeout(() => {
        setError(null);
        setStatus('idle');
      }, 3000);
    });

    voiceService.onStatusChange((voiceStatus) => {
      setIsListening(voiceStatus === 'listening');
      setStatus(voiceStatus === 'listening' ? 'listening' : 
                voiceStatus === 'error' ? 'error' : 'idle');
    });

    // Setup keyboard shortcuts
    voiceService.setupKeyboardShortcuts();

    // Listen for voice commands
    const handleVoiceCommand = (event: CustomEvent) => {
      if (onVoiceCommand) {
        onVoiceCommand(event.detail.command);
      }
    };

    window.addEventListener('voice-command', handleVoiceCommand as EventListener);

    // Check if synthesis is speaking periodically
    const speakingInterval = setInterval(() => {
      setIsSpeaking(voiceService.isSpeaking());
    }, 100);

    return () => {
      clearInterval(speakingInterval);
      window.removeEventListener('voice-command', handleVoiceCommand as EventListener);
    };
  }, [onTranscript, onVoiceCommand]);

  const handleToggleListening = async () => {
    if (!isSupported) return;

    try {
      if (isListening) {
        voiceService.stopListening();
      } else {
        setError(null);
        await voiceService.startListening();
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to start voice recognition');
    }
  };

  const handleStopSpeaking = () => {
    voiceService.stopSpeaking();
  };

  const handleTestVoice = () => {
    voiceService.speak('Voice synthesis is working correctly. You can now use voice commands and hear AI responses.');
  };

  const voiceCommands = voiceService.getVoiceCommands();

  if (!isSupported) {
    return (
      <Card className={cn('border-muted', className)}>
        <CardContent className="p-4">
          <div className="flex items-center gap-3 text-muted-foreground">
            <MicOff size={20} />
            <div>
              <p className="text-sm font-medium">Voice not supported</p>
              <p className="text-xs">Your browser doesn't support voice recognition</p>
            </div>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className={cn('space-y-4', className)}>
      {/* Main Voice Controls */}
      <Card>
        <CardContent className="p-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Button
                variant={isListening ? 'default' : 'outline'}
                size="sm"
                onClick={handleToggleListening}
                className={cn(
                  'transition-all duration-200',
                  isListening && 'animate-pulse bg-red-500 hover:bg-red-600'
                )}
              >
                {isListening ? (
                  <>
                    <MicOff size={16} className="mr-1" />
                    Stop
                  </>
                ) : (
                  <>
                    <Mic size={16} className="mr-1" />
                    Listen
                  </>
                )}
              </Button>

              {isSpeaking && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleStopSpeaking}
                  className="text-orange-600 border-orange-200"
                >
                  <VolumeX size={16} className="mr-1" />
                  Stop Speaking
                </Button>
              )}

              <Button
                variant="ghost"
                size="sm"
                onClick={handleTestVoice}
                disabled={isSpeaking}
              >
                <Volume2 size={16} className="mr-1" />
                Test
              </Button>
            </div>

            <div className="flex items-center gap-2">
              <Badge 
                variant={
                  status === 'listening' ? 'default' : 
                  status === 'error' ? 'destructive' : 
                  'secondary'
                }
                className="text-xs"
              >
                {status === 'listening' && <Mic size={10} className="mr-1" />}
                {status === 'error' && <AlertCircle size={10} className="mr-1" />}
                {status === 'idle' && <CheckCircle size={10} className="mr-1" />}
                {status.charAt(0).toUpperCase() + status.slice(1)}
              </Badge>

              {isSpeaking && (
                <Badge variant="secondary" className="text-xs animate-pulse">
                  <Volume2 size={10} className="mr-1" />
                  Speaking
                </Badge>
              )}
            </div>
          </div>

          {/* Live Transcript */}
          {currentTranscript && (
            <div className="mt-3 p-2 bg-muted/50 rounded text-sm">
              <div className="flex items-center justify-between mb-1">
                <span className="text-xs text-muted-foreground">Live transcript:</span>
                {confidence > 0 && (
                  <Badge variant="outline" className="text-xs">
                    {Math.round(confidence * 100)}% confident
                  </Badge>
                )}
              </div>
              <p className="text-foreground">{currentTranscript}</p>
            </div>
          )}

          {/* Error Display */}
          {error && (
            <div className="mt-3 p-2 bg-destructive/10 border border-destructive/20 rounded text-sm text-destructive">
              <div className="flex items-center gap-2">
                <AlertCircle size={14} />
                {error}
              </div>
            </div>
          )}

          {/* Keyboard Shortcuts */}
          <div className="mt-3 pt-3 border-t border-muted">
            <div className="flex items-center gap-4 text-xs text-muted-foreground">
              <div className="flex items-center gap-1">
                <Keyboard size={12} />
                <span>Ctrl+Shift+V: Toggle listening</span>
              </div>
              <div className="flex items-center gap-1">
                <span>Esc: Stop listening</span>
              </div>
              <div className="flex items-center gap-1">
                <span>Ctrl+Shift+S: Stop speaking</span>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Voice Commands Help */}
      {showCommands && (
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-sm flex items-center gap-2">
              <HelpCircle size={16} />
              Voice Commands
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {voiceCommands.map((command, index) => (
              <div key={index} className="flex items-start gap-3">
                <div className="flex h-6 w-6 items-center justify-center rounded bg-primary/10 text-primary">
                  <Zap size={12} />
                </div>
                <div className="flex-1">
                  <p className="text-sm font-medium">"{command.command}"</p>
                  <p className="text-xs text-muted-foreground">{command.description}</p>
                </div>
              </div>
            ))}
            
            <div className="pt-2 border-t border-muted">
              <p className="text-xs text-muted-foreground">
                Speak naturally and include these phrases in your speech to trigger commands.
              </p>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Voice Settings Preview */}
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-sm flex items-center gap-2">
            <Settings size={16} />
            Voice Settings
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 gap-3 text-xs">
            <div>
              <span className="text-muted-foreground">Language:</span>
              <span className="ml-1 font-medium">English (US)</span>
            </div>
            <div>
              <span className="text-muted-foreground">Synthesis:</span>
              <span className="ml-1 font-medium">
                {voiceService.isSpeechSynthesisSupported() ? 'Enabled' : 'Disabled'}
              </span>
            </div>
            <div>
              <span className="text-muted-foreground">Commands:</span>
              <span className="ml-1 font-medium">{voiceCommands.length} available</span>
            </div>
            <div>
              <span className="text-muted-foreground">Voices:</span>
              <span className="ml-1 font-medium">{voiceService.getAvailableVoices().length} available</span>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

VoiceControls.displayName = 'VoiceControls';