import React, { useState } from 'react';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Copy, Check, User, Bot } from 'lucide-react';

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  status: 'sending' | 'sent' | 'error';
  metadata?: {
    toolsUsed?: string[];
    processingTime?: number;
    cached?: boolean;
  };
}

interface ChatMessageProps {
  message: ChatMessage;
  className?: string;
}

export const ChatMessage: React.FC<ChatMessageProps> = ({ message, className }) => {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(message.content);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (error) {
      console.error('Failed to copy message:', error);
    }
  };

  const formatTimestamp = (timestamp: Date) => {
    return new Intl.DateTimeFormat('en-US', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: true,
    }).format(timestamp);
  };

  const isUser = message.role === 'user';
  const isError = message.status === 'error';
  const isSending = message.status === 'sending';

  return (
    <div
      className={cn(
        'flex w-full gap-3 p-4',
        isUser ? 'justify-end' : 'justify-start',
        className
      )}
    >
      {/* Avatar for assistant messages */}
      {!isUser && (
        <div className="flex-shrink-0">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-primary-foreground">
            <Bot size={16} />
          </div>
        </div>
      )}

      {/* Message content */}
      <div
        className={cn(
          'flex max-w-[80%] flex-col gap-2',
          isUser ? 'items-end' : 'items-start'
        )}
      >
        {/* Message bubble */}
        <div
          className={cn(
            'group relative rounded-lg px-4 py-3 text-sm',
            isUser
              ? 'bg-primary text-primary-foreground'
              : 'bg-muted text-muted-foreground',
            isError && 'bg-destructive text-destructive-foreground',
            isSending && 'opacity-70',
            'break-words'
          )}
        >
          {/* Loading indicator for sending messages */}
          {isSending && (
            <div className="absolute inset-0 flex items-center justify-center rounded-lg bg-black/10">
              <div className="flex space-x-1">
                <div className="h-1 w-1 animate-pulse rounded-full bg-current"></div>
                <div className="h-1 w-1 animate-pulse rounded-full bg-current delay-75"></div>
                <div className="h-1 w-1 animate-pulse rounded-full bg-current delay-150"></div>
              </div>
            </div>
          )}

          {/* Message content */}
          <div className="whitespace-pre-wrap">{message.content}</div>

          {/* Copy button */}
          <Button
            variant="ghost"
            size="icon"
            className={cn(
              'absolute -right-2 -top-2 h-6 w-6 opacity-0 transition-opacity group-hover:opacity-100',
              isUser ? 'text-primary-foreground/70 hover:text-primary-foreground' : 'text-muted-foreground/70 hover:text-muted-foreground'
            )}
            onClick={handleCopy}
            aria-label="Copy message"
          >
            {copied ? <Check size={12} /> : <Copy size={12} />}
          </Button>
        </div>

        {/* Message metadata */}
        <div
          className={cn(
            'flex items-center gap-2 text-xs text-muted-foreground',
            isUser ? 'flex-row-reverse' : 'flex-row'
          )}
        >
          {/* Timestamp */}
          <span>{formatTimestamp(message.timestamp)}</span>

          {/* Status indicator */}
          {isError && (
            <span className="text-destructive">Failed to send</span>
          )}
          {isSending && (
            <span>Sending...</span>
          )}

          {/* Tools used indicator for assistant messages */}
          {!isUser && message.metadata?.toolsUsed && message.metadata.toolsUsed.length > 0 && (
            <span className="text-xs text-muted-foreground/70">
              Used: {message.metadata.toolsUsed.join(', ')}
            </span>
          )}

          {/* Processing time for assistant messages */}
          {!isUser && message.metadata?.processingTime && (
            <span className="text-xs text-muted-foreground/70">
              {(message.metadata.processingTime / 1000).toFixed(1)}s
              {message.metadata.cached && ' (cached)'}
            </span>
          )}

          {/* Cached indicator */}
          {!isUser && message.metadata?.cached && !message.metadata?.processingTime && (
            <span className="text-xs text-muted-foreground/70">
              Cached response
            </span>
          )}
        </div>
      </div>

      {/* Avatar for user messages */}
      {isUser && (
        <div className="flex-shrink-0">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-secondary text-secondary-foreground">
            <User size={16} />
          </div>
        </div>
      )}
    </div>
  );
};

ChatMessage.displayName = 'ChatMessage';