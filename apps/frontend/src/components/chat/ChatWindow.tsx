import React, { useRef, useEffect, useState } from 'react';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { ChatMessage as ChatMessageComponent, ChatMessage as ChatMessageType } from './ChatMessage';
import { ChatSuggestions, getContextualSuggestions } from './ChatSuggestions';
// import { ProgressIndicator } from './ProgressIndicator';
import { Send, Loader2, Mic, MicOff } from 'lucide-react';

interface ChatWindowProps {
  messages: ChatMessageType[];
  isLoading: boolean;
  isTyping: boolean;
  onSendMessage: (message: string) => void;
  onRetryMessage?: (messageId: string) => void;
  onShareMessage?: (messageId: string) => void;
  onBookmarkMessage?: (messageId: string) => void;
  onExportMessage?: (messageId: string, format: 'pdf' | 'html' | 'json') => void;
  className?: string;
  placeholder?: string;
  disabled?: boolean;
  showSuggestions?: boolean;
  voiceEnabled?: boolean;
}

export const ChatWindow: React.FC<ChatWindowProps> = ({
  messages,
  isLoading,
  isTyping,
  onSendMessage,
  onRetryMessage,
  onShareMessage,
  onBookmarkMessage,
  onExportMessage,
  className,
  placeholder = "Ask me about tariffs, trade agreements, or product classifications...",
  disabled = false,
  showSuggestions = true,
  voiceEnabled = false,
}) => {
  const [inputValue, setInputValue] = useState('');
  const [isListening, setIsListening] = useState(false);
  const [showSuggestionsPanel, setShowSuggestionsPanel] = useState(true);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const messagesContainerRef = useRef<HTMLDivElement>(null);
  const recognitionRef = useRef<SpeechRecognition | null>(null);

  // Auto-scroll to bottom when new messages arrive
  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, isTyping]);

  // Auto-resize textarea based on content
  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = `${Math.min(textareaRef.current.scrollHeight, 120)}px`;
    }
  }, [inputValue]);

  // Initialize speech recognition
  useEffect(() => {
    if (!voiceEnabled || typeof window === 'undefined') {
      return () => undefined;
    }

    const SpeechRecognitionCtor =
      (window as typeof window & { webkitSpeechRecognition?: any }).webkitSpeechRecognition ||
      (window as typeof window & { SpeechRecognition?: any }).SpeechRecognition;

    if (!SpeechRecognitionCtor) {
      return () => undefined;
    }

    let recognition: SpeechRecognition | null = null;
    try {
      const recognitionInstance: SpeechRecognition = new SpeechRecognitionCtor();
      recognitionInstance.continuous = false;
      recognitionInstance.interimResults = false;
      recognitionInstance.lang = 'en-US';

      recognitionInstance.onresult = (event) => {
        const transcript = event.results[0][0].transcript;
        setInputValue((prev) => prev + transcript);
        setIsListening(false);
      };

      recognitionInstance.onerror = () => {
        setIsListening(false);
      };

      recognitionInstance.onend = () => {
        setIsListening(false);
      };

      recognitionRef.current = recognitionInstance;
      recognition = recognitionInstance;
    } catch (error) {
      console.warn('Speech recognition is unavailable in this browser.', error);
      recognitionRef.current = null;
      setIsListening(false);
      return () => undefined;
    }

    return () => {
      if (recognition) {
        recognition.stop();
      }
    };
  }, [voiceEnabled]);

  // Hide suggestions when user starts typing
  useEffect(() => {
    if (inputValue.trim()) {
      setShowSuggestionsPanel(false);
    } else if (messages.length === 0) {
      setShowSuggestionsPanel(true);
    }
  }, [inputValue, messages.length]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (inputValue.trim() && !isLoading && !disabled) {
      onSendMessage(inputValue.trim());
      setInputValue('');
      setShowSuggestionsPanel(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e);
    }
  };

  const handleRetry = (messageId: string) => {
    if (onRetryMessage) {
      onRetryMessage(messageId);
    }
  };

  const handleSuggestionClick = (suggestion: any) => {
    setInputValue(suggestion.text);
    setShowSuggestionsPanel(false);
    // Auto-focus the textarea
    if (textareaRef.current) {
      textareaRef.current.focus();
    }
  };

  const handleVoiceToggle = () => {
    if (!voiceEnabled || !recognitionRef.current) return;

    if (isListening) {
      recognitionRef.current.stop();
      setIsListening(false);
    } else {
      recognitionRef.current.start();
      setIsListening(true);
    }
  };

  const suggestions = getContextualSuggestions(messages);

  return (
    <div className={cn('flex h-full flex-col', className)}>
      {/* Messages container */}
      <div
        ref={messagesContainerRef}
        className="flex-1 overflow-y-auto scroll-smooth px-4 py-4"
        role="log"
        aria-live="polite"
        aria-label="Chat messages"
      >
        {messages.length === 0 ? (
          <div className="flex h-full items-center justify-center">
            <div className="text-center text-muted-foreground">
              <div className="mb-4 text-4xl">🤖</div>
              <h3 className="mb-2 text-lg font-medium">AI Trade Copilot</h3>
              <p className="text-sm">
                Ask me anything about tariffs, trade agreements, or product classifications.
                <br />
                I'm here to help you navigate international trade data.
              </p>
            </div>
          </div>
        ) : (
          <div className="space-y-4">
            {messages.map((message) => (
              <div key={message.id} className="relative">
                <ChatMessageComponent 
                  message={message}
                  onShare={onShareMessage}
                  onBookmark={onBookmarkMessage}
                  onExport={onExportMessage}
                />
                {message.status === 'error' && onRetryMessage && (
                  <div className="flex justify-end px-4">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleRetry(message.id)}
                      className="text-xs"
                    >
                      Retry
                    </Button>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}

        {/* Progress indicator for long-running queries - Temporarily disabled */}
        {/* <ProgressIndicator isVisible={isLoading || isTyping} /> */}

        {/* Simple typing indicator */}
        {(isLoading || isTyping) && (
          <div className="flex items-center gap-3 p-4">
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-primary-foreground">
              <Loader2 size={16} className="animate-spin" />
            </div>
            <div className="flex items-center space-x-1 rounded-lg bg-muted px-4 py-3 text-sm text-muted-foreground">
              <span>AI is thinking</span>
              <div className="flex space-x-1">
                <div className="h-1 w-1 animate-pulse rounded-full bg-current"></div>
                <div className="h-1 w-1 animate-pulse rounded-full bg-current delay-75"></div>
                <div className="h-1 w-1 animate-pulse rounded-full bg-current delay-150"></div>
              </div>
            </div>
          </div>
        )}

        {/* Scroll anchor */}
        <div ref={messagesEndRef} />
      </div>

      {/* Suggestions panel */}
      {showSuggestions && showSuggestionsPanel && (
        <div className="px-4">
          <ChatSuggestions
            suggestions={suggestions}
            onSuggestionClick={handleSuggestionClick}
            visible={showSuggestionsPanel}
          />
        </div>
      )}

      {/* Enhanced input form */}
      <div className="border-t bg-background p-4">
        <form onSubmit={handleSubmit} className="flex gap-2">
          <div className="flex-1">
            <Textarea
              ref={textareaRef}
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder={placeholder}
              disabled={disabled || isLoading}
              className="min-h-[44px] max-h-[120px] resize-none"
              rows={1}
              aria-label="Type your message"
            />
          </div>
          
          {/* Voice input button */}
          {voiceEnabled && (
            <Button
              type="button"
              variant="outline"
              size="icon"
              className="h-11 w-11 flex-shrink-0"
              onClick={handleVoiceToggle}
              disabled={disabled || isLoading}
              aria-label={isListening ? "Stop voice input" : "Start voice input"}
            >
              {isListening ? (
                <MicOff size={18} className="text-red-500" />
              ) : (
                <Mic size={18} />
              )}
            </Button>
          )}
          
          <Button
            type="submit"
            disabled={!inputValue.trim() || isLoading || disabled}
            size="icon"
            className="h-11 w-11 flex-shrink-0"
            aria-label="Send message"
          >
            {isLoading ? (
              <Loader2 size={18} className="animate-spin" />
            ) : (
              <Send size={18} />
            )}
          </Button>
        </form>

        {/* Enhanced input hints */}
        <div className="mt-2 flex items-center justify-between text-xs text-muted-foreground">
          <span>Press Enter to send, Shift+Enter for new line</span>
          {voiceEnabled && (
            <span className="flex items-center gap-1">
              <Mic size={12} />
              Voice input {isListening ? 'active' : 'available'}
            </span>
          )}
        </div>
      </div>
    </div>
  );
};

ChatWindow.displayName = 'ChatWindow';