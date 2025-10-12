import React, { useState, useEffect } from 'react';
import { 
  Bot, 
  Sparkles, 
  MessageSquare, 
  Zap, 
  History, 
  Search, 
  Filter,
  Settings,
  TrendingUp,
  BarChart3,
  Globe,
  Clock,
  Star,
  Share,
  Download,
  Bookmark,
  ChevronLeft,
  ChevronRight,
  Eye,
  EyeOff
} from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
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
  },
  {
    icon: BarChart3,
    title: 'Visual Analytics',
    description: 'Get charts, tables, and interactive visualizations with your responses.'
  },
  {
    icon: Globe,
    title: 'Global Coverage',
    description: 'Access trade data from 200+ countries and territories worldwide.'
  },
  {
    icon: TrendingUp,
    title: 'Market Insights',
    description: 'Receive proactive insights and trend analysis for better decision making.'
  }
];

const quickActions = [
  {
    icon: BarChart3,
    label: 'Tariff Analysis',
    query: 'Analyze tariff rates for importing electronics from China to the US',
    category: 'analysis'
  },
  {
    icon: Globe,
    label: 'Trade Routes',
    query: 'Show me the best trade routes for importing textiles from India',
    category: 'logistics'
  },
  {
    icon: MessageSquare,
    label: 'HS Code Lookup',
    query: 'Find the HS code for wireless bluetooth headphones',
    category: 'classification'
  },
  {
    icon: TrendingUp,
    label: 'Market Trends',
    query: 'What are the current trade trends between US and EU?',
    category: 'intelligence'
  }
];

const exampleQueries = [
  "What's the tariff for importing avocados from Mexico to the USA?",
  "Find the HS code for electric skateboards",
  "What trade agreements does Japan have?",
  "Compare tariff rates for steel imports from China vs Germany",
  "Calculate total landed cost for 1000 units from Vietnam",
  "Show me compliance requirements for medical device imports"
];

// Mock conversation history data
const mockConversations = [
  {
    id: '1',
    title: 'Tariff Analysis - Electronics',
    timestamp: new Date(Date.now() - 1000 * 60 * 30), // 30 minutes ago
    messageCount: 8,
    category: 'analysis'
  },
  {
    id: '2',
    title: 'HS Code Classification',
    timestamp: new Date(Date.now() - 1000 * 60 * 60 * 2), // 2 hours ago
    messageCount: 5,
    category: 'classification'
  },
  {
    id: '3',
    title: 'Trade Route Optimization',
    timestamp: new Date(Date.now() - 1000 * 60 * 60 * 24), // 1 day ago
    messageCount: 12,
    category: 'logistics'
  }
];

// Mock user insights
const userInsights = [
  {
    icon: TrendingUp,
    title: 'Frequent Queries',
    description: 'You often ask about electronics tariffs',
    action: 'Set up alerts'
  },
  {
    icon: Globe,
    title: 'Top Countries',
    description: 'China, Germany, Japan are your focus',
    action: 'View analysis'
  },
  {
    icon: Clock,
    title: 'Best Time',
    description: 'You\'re most active in the morning',
    action: 'Schedule reports'
  }
];

export function AiAssistantPage() {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [activeTab, setActiveTab] = useState<'chat' | 'history' | 'insights'>('chat');
  const [searchTerm, setSearchTerm] = useState('');
  const [showTypingIndicator, setShowTypingIndicator] = useState(false);
  const [bookmarkedMessages, setBookmarkedMessages] = useState<Set<string>>(new Set());
  const [isMobileViewport, setIsMobileViewport] = useState(false);
  
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

  // Simulate typing indicator
  useEffect(() => {
    if (isLoading) {
      setShowTypingIndicator(true);
      const timer = setTimeout(() => setShowTypingIndicator(false), 3000);
      return () => clearTimeout(timer);
    }
  }, [isLoading]);

  useEffect(() => {
    if (typeof window === 'undefined') {
      return;
    }

    const updateViewportFlag = () => setIsMobileViewport(window.innerWidth < 1024);
    updateViewportFlag();

    window.addEventListener('resize', updateViewportFlag);
    return () => window.removeEventListener('resize', updateViewportFlag);
  }, []);

  const handleExampleQuery = (query: string) => {
    sendMessage(query);
  };

  const handleShareMessage = (messageId: string) => {
    // Implement sharing functionality
    console.log('Sharing message:', messageId);
  };

  const handleBookmarkMessage = (messageId: string) => {
    setBookmarkedMessages(prev => {
      const newSet = new Set(prev);
      if (newSet.has(messageId)) {
        newSet.delete(messageId);
      } else {
        newSet.add(messageId);
      }
      return newSet;
    });
  };

  const handleExportMessage = (messageId: string, format: 'pdf' | 'html' | 'json') => {
    // Implement export functionality
    console.log('Exporting message:', messageId, 'as', format);
  };

  const handleConversationSelect = (conversationId: string) => {
    loadConversation(conversationId);
    setActiveTab('chat');
  };

  const filteredConversations = mockConversations.filter(conv =>
    conv.title.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="flex h-full flex-col">
      {/* Enhanced Header */}
      <div className="border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="flex h-16 items-center justify-between px-6">
          <div className="flex items-center space-x-3">
            <Button
              variant="ghost"
              size="icon"
              onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
              className="lg:hidden"
            >
              {sidebarCollapsed ? <ChevronRight size={20} /> : <ChevronLeft size={20} />}
            </Button>
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-gradient-to-br from-blue-500 to-purple-600 text-white">
              <Sparkles size={20} />
            </div>
            <div>
              <h1 className="text-xl font-semibold">AI Trade Copilot</h1>
              <p className="text-sm text-muted-foreground">
                Enhanced with real-time insights
              </p>
            </div>
          </div>
          <div className="flex items-center space-x-2">
            {showTypingIndicator && (
              <Badge variant="secondary" className="text-xs animate-pulse">
                <Bot size={12} className="mr-1" />
                AI is thinking...
              </Badge>
            )}
            <Badge variant="secondary" className="text-xs">
              <div className="mr-1 h-2 w-2 rounded-full bg-green-500"></div>
              Online
            </Badge>
            {messages.length > 0 && (
              <Button
                variant="ghost"
                size="sm"
                onClick={clearChat}
                className="text-xs"
              >
                Clear Chat
              </Button>
            )}
            <Button
              variant="ghost"
              size="icon"
              onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
              className="hidden lg:flex"
            >
              {sidebarCollapsed ? <ChevronRight size={16} /> : <ChevronLeft size={16} />}
            </Button>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="flex flex-1 overflow-hidden">
        {/* Chat Interface */}
        <div className="flex flex-1 flex-col">
          <ChatWindow
            messages={messages}
            isLoading={isLoading}
            isTyping={isTyping}
            onSendMessage={sendMessage}
            onRetryMessage={retryMessage}
            onShareMessage={handleShareMessage}
            onBookmarkMessage={handleBookmarkMessage}
            onExportMessage={handleExportMessage}
            placeholder="Ask me about tariffs, trade agreements, or product classifications..."
            className="h-full"
            showSuggestions={messages.length === 0}
            voiceEnabled={true}
          />
        </div>

        {/* Enhanced Sidebar */}
        {!sidebarCollapsed && (
          <div className="w-80 border-l bg-muted/30 flex flex-col">
            {/* Sidebar Tabs */}
            <div className="border-b p-4">
              <div className="flex space-x-1 bg-muted rounded-lg p-1">
                <Button
                  variant={activeTab === 'chat' ? 'default' : 'ghost'}
                  size="sm"
                  onClick={() => setActiveTab('chat')}
                  className="flex-1 text-xs"
                >
                  <MessageSquare size={14} className="mr-1" />
                  Chat
                </Button>
                <Button
                  variant={activeTab === 'history' ? 'default' : 'ghost'}
                  size="sm"
                  onClick={() => setActiveTab('history')}
                  className="flex-1 text-xs"
                >
                  <History size={14} className="mr-1" />
                  History
                </Button>
                <Button
                  variant={activeTab === 'insights' ? 'default' : 'ghost'}
                  size="sm"
                  onClick={() => setActiveTab('insights')}
                  className="flex-1 text-xs"
                >
                  <TrendingUp size={14} className="mr-1" />
                  Insights
                </Button>
              </div>
            </div>

            <div className="flex-1 overflow-y-auto p-4">
              {/* Chat Tab Content */}
              {activeTab === 'chat' && (
                <div className="space-y-6">
                  {/* Quick Actions */}
                  <div>
                    <h3 className="mb-3 text-sm font-medium text-muted-foreground uppercase tracking-wide">
                      Quick Actions
                    </h3>
                    <div className="grid grid-cols-2 gap-2">
                      {quickActions.map((action) => {
                        const Icon = action.icon;
                        return (
                          <Button
                            key={action.label}
                            variant="outline"
                            size="sm"
                            onClick={() => handleExampleQuery(action.query)}
                            disabled={isLoading}
                            className="h-auto p-3 flex flex-col items-center text-center"
                          >
                            <Icon size={16} className="mb-1" />
                            <span className="text-xs">{action.label}</span>
                          </Button>
                        );
                      })}
                    </div>
                  </div>

                  {/* Features */}
                  <div>
                    <h3 className="mb-3 text-sm font-medium text-muted-foreground uppercase tracking-wide">
                      Capabilities
                    </h3>
                    <div className="space-y-3">
                      {features.map((feature) => {
                        const Icon = feature.icon;
                        return (
                          <div
                            key={feature.title}
                            className="flex items-start space-x-3"
                          >
                            <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-primary/10 text-primary">
                              <Icon size={14} />
                            </div>
                            <div className="flex-1">
                              <h4 className="text-xs font-medium">{feature.title}</h4>
                              <p className="text-xs text-muted-foreground">
                                {feature.description}
                              </p>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </div>

                  {/* Example Queries */}
                  <div>
                    <h3 className="mb-3 text-sm font-medium text-muted-foreground uppercase tracking-wide">
                      Try These Examples
                    </h3>
                    <div className="space-y-2">
                      {exampleQueries.slice(0, 4).map((query, index) => (
                        <Button
                          key={index}
                          variant="ghost"
                          onClick={() => handleExampleQuery(query)}
                          disabled={isLoading}
                          className="w-full h-auto p-2 text-left text-xs justify-start"
                        >
                          <MessageSquare size={12} className="mr-2 flex-shrink-0" />
                          <span className="truncate">{query}</span>
                        </Button>
                      ))}
                    </div>
                  </div>
                </div>
              )}

              {/* History Tab Content */}
              {activeTab === 'history' && (
                <div className="space-y-4">
                  {/* Search */}
                  <div className="relative">
                    <Search size={14} className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground" />
                    <Input
                      placeholder="Search conversations..."
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      className="pl-9 text-sm"
                    />
                  </div>

                  {/* Conversation List */}
                  <div className="space-y-2">
                    {filteredConversations.map((conversation) => (
                      <Card
                        key={conversation.id}
                        className="cursor-pointer hover:bg-muted/50 transition-colors"
                        onClick={() => handleConversationSelect(conversation.id)}
                      >
                        <CardContent className="p-3">
                          <div className="flex items-start justify-between">
                            <div className="flex-1 min-w-0">
                              <h4 className="text-sm font-medium truncate">
                                {conversation.title}
                              </h4>
                              <p className="text-xs text-muted-foreground">
                                {conversation.messageCount} messages
                              </p>
                            </div>
                            <div className="text-xs text-muted-foreground">
                              {conversation.timestamp.toLocaleDateString()}
                            </div>
                          </div>
                          <Badge variant="secondary" className="mt-2 text-xs">
                            {conversation.category}
                          </Badge>
                        </CardContent>
                      </Card>
                    ))}
                  </div>
                </div>
              )}

              {/* Insights Tab Content */}
              {activeTab === 'insights' && (
                <div className="space-y-4">
                  <div>
                    <h3 className="mb-3 text-sm font-medium">Personalized Insights</h3>
                    <div className="space-y-3">
                      {userInsights.map((insight, index) => {
                        const Icon = insight.icon;
                        return (
                          <Card key={index}>
                            <CardContent className="p-3">
                              <div className="flex items-start space-x-3">
                                <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/10 text-primary">
                                  <Icon size={14} />
                                </div>
                                <div className="flex-1">
                                  <h4 className="text-sm font-medium">{insight.title}</h4>
                                  <p className="text-xs text-muted-foreground mb-2">
                                    {insight.description}
                                  </p>
                                  <Button variant="outline" size="sm" className="text-xs">
                                    {insight.action}
                                  </Button>
                                </div>
                              </div>
                            </CardContent>
                          </Card>
                        );
                      })}
                    </div>
                  </div>

                  {/* Usage Stats */}
                  <Card>
                    <CardHeader className="pb-3">
                      <CardTitle className="text-sm">Usage Statistics</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-3">
                      <div className="flex justify-between text-sm">
                        <span className="text-muted-foreground">Queries this week</span>
                        <span className="font-medium">47</span>
                      </div>
                      <div className="flex justify-between text-sm">
                        <span className="text-muted-foreground">Avg. response time</span>
                        <span className="font-medium">2.3s</span>
                      </div>
                      <div className="flex justify-between text-sm">
                        <span className="text-muted-foreground">Bookmarked</span>
                        <span className="font-medium">{bookmarkedMessages.size}</span>
                      </div>
                    </CardContent>
                  </Card>
                </div>
              )}
            </div>

            {/* Sidebar Footer */}
            <div className="border-t p-3">
              <div className="text-center text-xs text-muted-foreground">
                <p>Enhanced AI • Real-time data</p>
                <p className="mt-1">
                  {messages.length} messages in this session
                </p>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Mobile Quick Actions - Shown only when no messages and sidebar is collapsed */}
  {messages.length === 0 && (sidebarCollapsed || isMobileViewport) && (
        <div className="border-t bg-muted/30 p-4">
          <h3 className="mb-3 text-sm font-medium">Quick Actions:</h3>
          <div className="grid grid-cols-2 gap-2">
            {quickActions.slice(0, 4).map((action) => {
              const Icon = action.icon;
              return (
                <Button
                  key={action.label}
                  variant="outline"
                  size="sm"
                  onClick={() => handleExampleQuery(action.query)}
                  disabled={isLoading}
                  className="h-auto p-2 flex flex-col items-center text-center"
                >
                  <Icon size={14} className="mb-1" />
                  <span className="text-xs">{action.label}</span>
                </Button>
              );
            })}
          </div>
        </div>
      )}

      {/* Error Display */}
      {error && (
        <div className="border-t bg-destructive/10 p-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <div className="h-2 w-2 rounded-full bg-destructive"></div>
              <span className="text-sm text-destructive">Connection error</span>
            </div>
            <Button
              variant="ghost"
              size="sm"
              onClick={clearError}
              className="text-xs"
            >
              Dismiss
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}