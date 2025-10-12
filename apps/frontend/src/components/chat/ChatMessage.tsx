import React, { useState } from 'react';
import ReactMarkdown from 'react-markdown';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark } from 'react-syntax-highlighter/dist/esm/styles/prism';
import remarkGfm from 'remark-gfm';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Copy, Check, User, Bot, Share, Bookmark, Download, Table, BarChart3 } from 'lucide-react';

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
    hasTable?: boolean;
    hasChart?: boolean;
    hasCode?: boolean;
    confidence?: number;
    sources?: string[];
  };
  richContent?: {
    type: 'table' | 'chart' | 'comparison' | 'analysis';
    data: any;
  };
}

interface ChatMessageProps {
  message: ChatMessage;
  className?: string;
  onShare?: (messageId: string) => void;
  onBookmark?: (messageId: string) => void;
  onExport?: (messageId: string, format: 'pdf' | 'html' | 'json') => void;
}

export const ChatMessage: React.FC<ChatMessageProps> = ({ 
  message, 
  className,
  onShare,
  onBookmark,
  onExport
}) => {
  const [copied, setCopied] = useState(false);
  const [showActions, setShowActions] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(message.content);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (error) {
      console.error('Failed to copy message:', error);
    }
  };

  const handleShare = () => {
    if (onShare) {
      onShare(message.id);
    }
  };

  const handleBookmark = () => {
    if (onBookmark) {
      onBookmark(message.id);
    }
  };

  const handleExport = (format: 'pdf' | 'html' | 'json') => {
    if (onExport) {
      onExport(message.id, format);
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

  // Custom renderers for markdown
  const markdownComponents = {
    code({ node, inline, className, children, ...props }: any) {
      const match = /language-(\w+)/.exec(className || '');
      return !inline && match ? (
        <div className="my-4">
          <div className="flex items-center justify-between bg-muted px-4 py-2 rounded-t-lg border-b">
            <span className="text-sm font-medium text-muted-foreground">
              {match[1].toUpperCase()}
            </span>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => navigator.clipboard.writeText(String(children))}
              className="h-6 px-2 text-xs"
            >
              <Copy size={12} />
            </Button>
          </div>
          <SyntaxHighlighter
            style={oneDark}
            language={match[1]}
            PreTag="div"
            className="rounded-t-none"
            {...props}
          >
            {String(children).replace(/\n$/, '')}
          </SyntaxHighlighter>
        </div>
      ) : (
        <code className="bg-muted px-1.5 py-0.5 rounded text-sm font-mono" {...props}>
          {children}
        </code>
      );
    },
    table({ children }: any) {
      return (
        <div className="my-4 overflow-x-auto">
          <table className="w-full border-collapse border border-border rounded-lg">
            {children}
          </table>
        </div>
      );
    },
    thead({ children }: any) {
      return <thead className="bg-muted">{children}</thead>;
    },
    th({ children }: any) {
      return (
        <th className="border border-border px-4 py-2 text-left font-semibold">
          {children}
        </th>
      );
    },
    td({ children }: any) {
      return (
        <td className="border border-border px-4 py-2">
          {children}
        </td>
      );
    },
    blockquote({ children }: any) {
      return (
        <blockquote className="border-l-4 border-primary pl-4 my-4 italic text-muted-foreground">
          {children}
        </blockquote>
      );
    },
  };

  const renderRichContent = () => {
    if (!message.richContent) return null;

    switch (message.richContent.type) {
      case 'table':
        return (
          <Card className="mt-3">
            <CardContent className="p-4">
              <div className="flex items-center gap-2 mb-3">
                <Table size={16} />
                <span className="text-sm font-medium">Data Table</span>
              </div>
              {/* Table rendering would go here based on data structure */}
              <div className="text-sm text-muted-foreground">
                Table data visualization
              </div>
            </CardContent>
          </Card>
        );
      case 'chart':
        return (
          <Card className="mt-3">
            <CardContent className="p-4">
              <div className="flex items-center gap-2 mb-3">
                <BarChart3 size={16} />
                <span className="text-sm font-medium">Chart Analysis</span>
              </div>
              {/* Chart rendering would go here */}
              <div className="text-sm text-muted-foreground">
                Chart visualization
              </div>
            </CardContent>
          </Card>
        );
      default:
        return null;
    }
  };

  return (
    <div
      className={cn(
        'flex w-full gap-3 p-4',
        isUser ? 'justify-end' : 'justify-start',
        className
      )}
      onMouseEnter={() => setShowActions(true)}
      onMouseLeave={() => setShowActions(false)}
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
          'flex max-w-[85%] flex-col gap-2',
          isUser ? 'items-end' : 'items-start'
        )}
      >
        {/* Message bubble */}
        <div
          className={cn(
            'group relative rounded-lg px-4 py-3 text-sm',
            isUser
              ? 'bg-primary text-primary-foreground'
              : 'bg-muted text-foreground',
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

          {/* Enhanced message content with markdown support */}
          <div className="prose prose-sm max-w-none dark:prose-invert">
            {isUser ? (
              <div className="whitespace-pre-wrap">{message.content}</div>
            ) : (
              <ReactMarkdown
                remarkPlugins={[remarkGfm]}
                components={markdownComponents}
              >
                {message.content}
              </ReactMarkdown>
            )}
          </div>

          {/* Message actions */}
          <div
            className={cn(
              'absolute -right-2 -top-2 flex gap-1 opacity-0 transition-opacity',
              (showActions || copied) && 'opacity-100'
            )}
          >
            <Button
              variant="ghost"
              size="icon"
              className="h-6 w-6 bg-background/80 backdrop-blur-sm hover:bg-background"
              onClick={handleCopy}
              aria-label="Copy message"
            >
              {copied ? <Check size={12} /> : <Copy size={12} />}
            </Button>
            
            {!isUser && onShare && (
              <Button
                variant="ghost"
                size="icon"
                className="h-6 w-6 bg-background/80 backdrop-blur-sm hover:bg-background"
                onClick={handleShare}
                aria-label="Share message"
              >
                <Share size={12} />
              </Button>
            )}
            
            {!isUser && onBookmark && (
              <Button
                variant="ghost"
                size="icon"
                className="h-6 w-6 bg-background/80 backdrop-blur-sm hover:bg-background"
                onClick={handleBookmark}
                aria-label="Bookmark message"
              >
                <Bookmark size={12} />
              </Button>
            )}
            
            {!isUser && onExport && (
              <Button
                variant="ghost"
                size="icon"
                className="h-6 w-6 bg-background/80 backdrop-blur-sm hover:bg-background"
                onClick={() => handleExport('html')}
                aria-label="Export message"
              >
                <Download size={12} />
              </Button>
            )}
          </div>
        </div>

        {/* Rich content rendering */}
        {renderRichContent()}

        {/* Content indicators */}
        {!isUser && (message.metadata?.hasTable || message.metadata?.hasChart || message.metadata?.hasCode) && (
          <div className="flex gap-1 mt-1">
            {message.metadata.hasTable && (
              <Badge variant="secondary" className="text-xs">
                <Table size={10} className="mr-1" />
                Table
              </Badge>
            )}
            {message.metadata.hasChart && (
              <Badge variant="secondary" className="text-xs">
                <BarChart3 size={10} className="mr-1" />
                Chart
              </Badge>
            )}
            {message.metadata.hasCode && (
              <Badge variant="secondary" className="text-xs">
                Code
              </Badge>
            )}
          </div>
        )}

        {/* Enhanced message metadata */}
        <div
          className={cn(
            'flex flex-wrap items-center gap-2 text-xs text-muted-foreground',
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

          {/* Confidence score for assistant messages */}
          {!isUser && message.metadata?.confidence && (
            <Badge 
              variant={message.metadata.confidence > 0.8 ? "default" : "secondary"} 
              className="text-xs"
            >
              {Math.round(message.metadata.confidence * 100)}% confident
            </Badge>
          )}

          {/* Tools used indicator for assistant messages */}
          {!isUser && message.metadata?.toolsUsed && message.metadata.toolsUsed.length > 0 && (
            <div className="flex flex-wrap gap-1">
              {message.metadata.toolsUsed.map((tool, index) => (
                <Badge key={index} variant="outline" className="text-xs">
                  {tool}
                </Badge>
              ))}
            </div>
          )}

          {/* Processing time for assistant messages */}
          {!isUser && message.metadata?.processingTime && (
            <span className="text-xs text-muted-foreground/70">
              {(message.metadata.processingTime / 1000).toFixed(1)}s
              {message.metadata.cached && ' (cached)'}
            </span>
          )}

          {/* Sources indicator */}
          {!isUser && message.metadata?.sources && message.metadata.sources.length > 0 && (
            <span className="text-xs text-muted-foreground/70">
              Sources: {message.metadata.sources.length}
            </span>
          )}

          {/* Cached indicator */}
          {!isUser && message.metadata?.cached && !message.metadata?.processingTime && (
            <Badge variant="secondary" className="text-xs">
              Cached
            </Badge>
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