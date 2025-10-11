import React from 'react';
import { motion } from 'framer-motion';
import { Bot, Sparkles, MessageSquare, Zap } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { ChatWindow } from '@/components/chat/ChatWindow';
import { ConversationHistory } from '@/components/chat/ConversationHistory';
import { useChat } from '@/hooks/useChat';

const features = [
  {
    icon: MessageSquare,
    title: 'Natural Language Queries',
    description: 'Ask questions in plain English about tariffs, trade agreements, and product classifications.'
  },
  {
    icon: Zap,
    title: 'Instant Responses',
    description: 'Get immediate answers powered by AI and real-time trade data.'
  },
  {
    icon: Bot,
    title: 'Smart Assistance',
    description: 'AI understands context and provides relevant, actionable trade intelligence.'
  }
];

const exampleQueries = [
  "What's the tariff for importing avocados from Mexico to the USA?",
  "Find the HS code for electric skateboards",
  "What trade agreements does Japan have?",
  "Compare tariff rates for steel imports from China vs Germany"
];

export function AiAssistantPage() {
  const {
    messages,
    isLoading,
    isTyping,
    error,
    conversationId,
    sendMessage,
    retryMessage,
    clearChat,
    clearError,
    loadConversation
  } = useChat({ persistConversation: true });

  const handleExampleQuery = (query: string) => {
    sendMessage(query);
  };

  return (
    <div className="flex h-full flex-col">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60"
      >
        <div className="flex h-16 items-center justify-between px-6">
          <div className="flex items-center space-x-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-gradient-to-br from-blue-500 to-purple-600 text-white">
              <Sparkles size={20} />
            </div>
            <div>
              <h1 className="text-xl font-semibold">AI Trade Copilot</h1>
              <p className="text-sm text-muted-foreground">
                Your intelligent assistant for trade data
              </p>
            </div>
          </div>
          <div className="flex items-center space-x-2">
            <Badge variant="secondary" className="text-xs">
              <div className="mr-1 h-2 w-2 rounded-full bg-green-500"></div>
              Online
            </Badge>
            {messages.length > 0 && (
              <button
                onClick={clearChat}
                className="text-sm text-muted-foreground hover:text-foreground transition-colors"
              >
                Clear Chat
              </button>
            )}
          </div>
        </div>
      </motion.div>

      {/* Main Content */}
      <div className="flex flex-1 overflow-hidden">
        {/* Chat Interface */}
        <motion.div
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.5, delay: 0.2 }}
          className="flex flex-1 flex-col"
        >
          <ChatWindow
            messages={messages}
            isLoading={isLoading}
            isTyping={isTyping}
            onSendMessage={sendMessage}
            onRetryMessage={retryMessage}
            placeholder="Ask me about tariffs, trade agreements, or product classifications..."
            className="h-full"
          />
        </motion.div>

        {/* Sidebar - Hidden on mobile, shown on larger screens */}
        <motion.div
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.5, delay: 0.3 }}
          className="hidden w-80 border-l bg-muted/30 lg:flex lg:flex-col"
        >
          <div className="flex-1 overflow-y-auto p-6">
            {/* Conversation History */}
            <div className="mb-6">
              <ConversationHistory
                onSelectConversation={loadConversation}
                currentConversationId={conversationId}
                className="border-0 shadow-none bg-transparent"
              />
            </div>

            {/* Features */}
            <div className="mb-6">
              <h3 className="mb-4 text-sm font-medium text-muted-foreground uppercase tracking-wide">
                Capabilities
              </h3>
              <div className="space-y-4">
                {features.map((feature, index) => {
                  const Icon = feature.icon;
                  return (
                    <motion.div
                      key={feature.title}
                      initial={{ opacity: 0, y: 10 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ duration: 0.3, delay: 0.4 + index * 0.1 }}
                      className="flex items-start space-x-3"
                    >
                      <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/10 text-primary">
                        <Icon size={16} />
                      </div>
                      <div className="flex-1">
                        <h4 className="text-sm font-medium">{feature.title}</h4>
                        <p className="text-xs text-muted-foreground">
                          {feature.description}
                        </p>
                      </div>
                    </motion.div>
                  );
                })}
              </div>
            </div>

            {/* Example Queries */}
            <div>
              <h3 className="mb-4 text-sm font-medium text-muted-foreground uppercase tracking-wide">
                Try These Examples
              </h3>
              <div className="space-y-2">
                {exampleQueries.map((query, index) => (
                  <motion.button
                    key={index}
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.3, delay: 0.6 + index * 0.1 }}
                    onClick={() => handleExampleQuery(query)}
                    disabled={isLoading}
                    className="w-full rounded-lg border bg-background p-3 text-left text-sm transition-colors hover:bg-muted/50 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <div className="flex items-start space-x-2">
                      <MessageSquare size={14} className="mt-0.5 text-muted-foreground" />
                      <span className="flex-1">{query}</span>
                    </div>
                  </motion.button>
                ))}
              </div>
            </div>
          </div>

          {/* Footer */}
          <div className="border-t p-4">
            <div className="text-center text-xs text-muted-foreground">
              <p>Powered by AI • Real-time trade data</p>
              <p className="mt-1">
                Ask questions naturally and get instant insights
              </p>
            </div>
          </div>
        </motion.div>
      </div>

      {/* Mobile Example Queries - Shown only when no messages */}
      {messages.length === 0 && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.4 }}
          className="border-t bg-muted/30 p-4 lg:hidden"
        >
          <h3 className="mb-3 text-sm font-medium">Try asking:</h3>
          <div className="grid gap-2">
            {exampleQueries.slice(0, 2).map((query, index) => (
              <button
                key={index}
                onClick={() => handleExampleQuery(query)}
                disabled={isLoading}
                className="rounded-lg border bg-background p-2 text-left text-sm transition-colors hover:bg-muted/50 disabled:opacity-50"
              >
                {query}
              </button>
            ))}
          </div>
        </motion.div>
      )}
    </div>
  );
}