/// <reference path="../types/speech.d.ts" />

export interface VoiceSettings {
  language: string;
  continuous: boolean;
  interimResults: boolean;
  maxAlternatives: number;
  voiceRate: number;
  voicePitch: number;
  voiceVolume: number;
  preferredVoice?: string;
}

export interface VoiceRecognitionResult {
  transcript: string;
  confidence: number;
  isFinal: boolean;
  alternatives?: string[];
}

export interface VoiceCommand {
  command: string;
  action: () => void;
  description: string;
  patterns: RegExp[];
}

class VoiceService {
  private recognition: SpeechRecognition | null = null;
  private synthesis: SpeechSynthesis | null = null;
  private isListening = false;
  private isSupported = false;
  private settings: VoiceSettings;
  private voiceCommands: VoiceCommand[] = [];
  private onResultCallback?: (result: VoiceRecognitionResult) => void;
  private onErrorCallback?: (error: string) => void;
  private onStatusChangeCallback?: (status: 'listening' | 'stopped' | 'error') => void;

  constructor() {
    this.settings = {
      language: 'en-US',
      continuous: false,
      interimResults: true,
      maxAlternatives: 3,
      voiceRate: 1,
      voicePitch: 1,
      voiceVolume: 1,
    };

    this.initializeServices();
    this.setupVoiceCommands();
  }

  private initializeServices() {
    // Check for speech recognition support
    if ('webkitSpeechRecognition' in window || 'SpeechRecognition' in window) {
      const SpeechRecognition = window.webkitSpeechRecognition || window.SpeechRecognition;
      this.recognition = new SpeechRecognition();
      this.setupRecognition();
      this.isSupported = true;
    }

    // Check for speech synthesis support
    if ('speechSynthesis' in window) {
      this.synthesis = window.speechSynthesis;
    }
  }

  private setupRecognition() {
    if (!this.recognition) return;

    this.recognition.continuous = this.settings.continuous;
    this.recognition.interimResults = this.settings.interimResults;
    this.recognition.lang = this.settings.language;
    this.recognition.maxAlternatives = this.settings.maxAlternatives;

    this.recognition.onstart = () => {
      this.isListening = true;
      this.onStatusChangeCallback?.('listening');
    };

    this.recognition.onend = () => {
      this.isListening = false;
      this.onStatusChangeCallback?.('stopped');
    };

    this.recognition.onerror = (event) => {
      this.isListening = false;
      const errorMessage = this.getErrorMessage(event.error);
      this.onErrorCallback?.(errorMessage);
      this.onStatusChangeCallback?.('error');
    };

    this.recognition.onresult = (event) => {
      const results = Array.from(event.results);
      const lastResult = results[results.length - 1];
      
      if (lastResult) {
        const transcript = lastResult[0].transcript;
        const confidence = lastResult[0].confidence;
        const isFinal = lastResult.isFinal;
        
        const alternatives = Array.from(lastResult)
          .slice(1)
          .map(alt => alt.transcript);

        const result: VoiceRecognitionResult = {
          transcript,
          confidence,
          isFinal,
          alternatives
        };

        // Check for voice commands
        if (isFinal) {
          this.processVoiceCommand(transcript);
        }

        this.onResultCallback?.(result);
      }
    };
  }

  private setupVoiceCommands() {
    this.voiceCommands = [
      {
        command: 'clear chat',
        action: () => this.triggerCommand('clear-chat'),
        description: 'Clear the current conversation',
        patterns: [/clear chat/i, /clear conversation/i, /start over/i]
      },
      {
        command: 'stop listening',
        action: () => this.stopListening(),
        description: 'Stop voice recognition',
        patterns: [/stop listening/i, /stop recording/i, /cancel/i]
      },
      {
        command: 'help',
        action: () => this.triggerCommand('show-help'),
        description: 'Show available voice commands',
        patterns: [/help/i, /what can you do/i, /voice commands/i]
      },
      {
        command: 'export conversation',
        action: () => this.triggerCommand('export-conversation'),
        description: 'Export the current conversation',
        patterns: [/export conversation/i, /download chat/i, /save conversation/i]
      },
      {
        command: 'bookmark this',
        action: () => this.triggerCommand('bookmark-last'),
        description: 'Bookmark the last AI response',
        patterns: [/bookmark this/i, /save this/i, /remember this/i]
      }
    ];
  }

  private processVoiceCommand(transcript: string) {
    const command = this.voiceCommands.find(cmd =>
      cmd.patterns.some(pattern => pattern.test(transcript))
    );

    if (command) {
      command.action();
      this.speak(`Executing ${command.command}`);
    }
  }

  private triggerCommand(commandType: string) {
    // Dispatch custom event for command handling
    window.dispatchEvent(new CustomEvent('voice-command', {
      detail: { command: commandType }
    }));
  }

  private getErrorMessage(error: string): string {
    switch (error) {
      case 'no-speech':
        return 'No speech was detected. Please try again.';
      case 'audio-capture':
        return 'Audio capture failed. Please check your microphone.';
      case 'not-allowed':
        return 'Microphone access was denied. Please enable microphone permissions.';
      case 'network':
        return 'Network error occurred during speech recognition.';
      case 'service-not-allowed':
        return 'Speech recognition service is not allowed.';
      default:
        return `Speech recognition error: ${error}`;
    }
  }

  // Public methods
  isVoiceSupported(): boolean {
    return this.isSupported;
  }

  isSpeechSynthesisSupported(): boolean {
    return this.synthesis !== null;
  }

  startListening(): Promise<void> {
    return new Promise((resolve, reject) => {
      if (!this.recognition) {
        reject(new Error('Speech recognition not supported'));
        return;
      }

      if (this.isListening) {
        resolve();
        return;
      }

      try {
        this.recognition.start();
        resolve();
      } catch (error) {
        reject(error);
      }
    });
  }

  stopListening(): void {
    if (this.recognition && this.isListening) {
      this.recognition.stop();
    }
  }

  speak(text: string, options?: Partial<SpeechSynthesisUtterance>): Promise<void> {
    return new Promise((resolve, reject) => {
      if (!this.synthesis) {
        reject(new Error('Speech synthesis not supported'));
        return;
      }

      const utterance = new SpeechSynthesisUtterance(text);
      
      // Apply settings
      utterance.rate = options?.rate || this.settings.voiceRate;
      utterance.pitch = options?.pitch || this.settings.voicePitch;
      utterance.volume = options?.volume || this.settings.voiceVolume;
      utterance.lang = options?.lang || this.settings.language;

      // Set preferred voice if available
      if (this.settings.preferredVoice) {
        const voices = this.synthesis.getVoices();
        const preferredVoice = voices.find(voice => 
          voice.name === this.settings.preferredVoice
        );
        if (preferredVoice) {
          utterance.voice = preferredVoice;
        }
      }

      utterance.onend = () => resolve();
      utterance.onerror = (event) => reject(new Error(`Speech synthesis error: ${event.error}`));

      this.synthesis.speak(utterance);
    });
  }

  stopSpeaking(): void {
    if (this.synthesis) {
      this.synthesis.cancel();
    }
  }

  getAvailableVoices(): SpeechSynthesisVoice[] {
    if (!this.synthesis) return [];
    return this.synthesis.getVoices();
  }

  updateSettings(newSettings: Partial<VoiceSettings>): void {
    this.settings = { ...this.settings, ...newSettings };
    
    if (this.recognition) {
      this.recognition.lang = this.settings.language;
      this.recognition.continuous = this.settings.continuous;
      this.recognition.interimResults = this.settings.interimResults;
      this.recognition.maxAlternatives = this.settings.maxAlternatives;
    }
  }

  getSettings(): VoiceSettings {
    return { ...this.settings };
  }

  getVoiceCommands(): VoiceCommand[] {
    return [...this.voiceCommands];
  }

  addVoiceCommand(command: VoiceCommand): void {
    this.voiceCommands.push(command);
  }

  removeVoiceCommand(commandName: string): void {
    this.voiceCommands = this.voiceCommands.filter(cmd => cmd.command !== commandName);
  }

  // Event handlers
  onResult(callback: (result: VoiceRecognitionResult) => void): void {
    this.onResultCallback = callback;
  }

  onError(callback: (error: string) => void): void {
    this.onErrorCallback = callback;
  }

  onStatusChange(callback: (status: 'listening' | 'stopped' | 'error') => void): void {
    this.onStatusChangeCallback = callback;
  }

  // Utility methods
  isCurrentlyListening(): boolean {
    return this.isListening;
  }

  isSpeaking(): boolean {
    return this.synthesis ? this.synthesis.speaking : false;
  }

  // Accessibility features
  announceMessage(message: string): void {
    // Announce important messages for accessibility
    this.speak(message, { rate: 0.9, pitch: 1.1 });
  }

  // Keyboard shortcuts integration
  setupKeyboardShortcuts(): void {
    document.addEventListener('keydown', (event) => {
      // Ctrl/Cmd + Shift + V to toggle voice recognition
      if ((event.ctrlKey || event.metaKey) && event.shiftKey && event.key === 'V') {
        event.preventDefault();
        if (this.isListening) {
          this.stopListening();
        } else {
          this.startListening().catch(console.error);
        }
      }

      // Escape to stop listening
      if (event.key === 'Escape' && this.isListening) {
        this.stopListening();
      }

      // Ctrl/Cmd + Shift + S to stop speaking
      if ((event.ctrlKey || event.metaKey) && event.shiftKey && event.key === 'S') {
        event.preventDefault();
        this.stopSpeaking();
      }
    });
  }
}

// Export singleton instance
export const voiceService = new VoiceService();
export default voiceService;