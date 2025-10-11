import React, { useRef, useEffect, useState } from 'react';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { ChatMessage, ChatMessage as ChatMessageType } from './ChatMessage';
import { ProgressIndicator } from './ProgressIndicator';
import { Send, Loader2 } from 'lucide-react';

interface ChatWindowProps {
  messages: ChatMessageType[];
  isLoading: boolean;
  isTyping: boolean;
  onSendMessage: (message: string) => void;
  onRetryMessage?: (messageId: string) => void;
  className?: string;
  placeholder?: string;
  disabled?: boolean;
}

export const ChatWindow: React.FC<ChatWindowProps> = ({
  messages,
  isLoading,
  isTyping,
  onSendMessage,
  onRetryMessage,
  className,
  placeholder = "Ask me about tariffs, trade agreements, or product classifications...",
  disabled = false,
}) => {
  const [inputValue, setInputValue] = useState('');
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const messagesContainerRef = useRef<HTMLDivElement>(null);

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

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (inputValue.trim() && !isLoading && !disabled) {
      onSendMessage(inputValue.trim());
      setInputValue('');
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
                <ChatMessage message={message} />
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

        {/* Progress indicator for long-running queries */}
        <ProgressIndicator isVisible={isLoading || isTyping} />

        {/* Scroll anchor */}
        <div ref={messagesEndRef} />
      </div>

      {/* Input form */}
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

        {/* Input hints */}
        <div className="mt-2 text-xs text-muted-foreground">
          Press Enter to send, Shift+Enter for new line
        </div>
      </div>
    </div>
  );
};

ChatWindow.displayName = 'ChatWindow';