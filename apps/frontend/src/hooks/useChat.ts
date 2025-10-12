import { useState, useCallback, useRef, useEffect } from 'react';
import { chatApi } from '@/services/api';
import { ChatMessage } from '@/components/chat/ChatMessage';
import safeLocalStorage from '@/lib/safeLocalStorage';

interface ChatQueryRequest {
  query: string;
  conversationId?: string;
}

interface ChatQueryResponse {
  response: string;
  conversationId: string;
  toolsUsed?: string[];
  processingTimeMs?: number;
  cached?: boolean;
}

interface ChatErrorResponse {
  error: string;
  message: string;
  suggestion?: string;
  timestamp: number;
}

interface UseChatOptions {
  maxMessages?: number;
  retryAttempts?: number;
  retryDelay?: number;
  conversationId?: string;
  persistConversation?: boolean;
}

interface UseChatReturn {
  messages: ChatMessage[];
  isLoading: boolean;
  isTyping: boolean;
  error: string | null;
  conversationId: string | null;
  sendMessage: (content: string) => Promise<void>;
  retryMessage: (messageId: string) => Promise<void>;
  clearChat: () => void;
  clearError: () => void;
  loadConversation: (conversationId: string) => Promise<void>;
}

export const useChat = (options: UseChatOptions = {}): UseChatReturn => {
  const {
    maxMessages = 100,
    retryAttempts = 3,
    retryDelay = 1000,
    conversationId: initialConversationId,
    persistConversation = true,
  } = options;

  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isTyping, setIsTyping] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [conversationId, setConversationId] = useState<string | null>(initialConversationId || null);
  
  const abortControllerRef = useRef<AbortController | null>(null);
  const messageIdCounter = useRef(0);
  const STORAGE_KEY = 'chat-conversation';

  // Load conversation from localStorage on mount
  useEffect(() => {
    if (persistConversation && !initialConversationId) {
      const saved = safeLocalStorage.get<{
        conversationId: string;
        messages: Array<Omit<ChatMessage, 'timestamp'> & { timestamp: string | number | Date }>;
      }>(STORAGE_KEY);

      if (saved && saved.messages && saved.messages.length > 0) {
        const normalizedMessages: ChatMessage[] = saved.messages.map((message) => {
          if (message.timestamp instanceof Date) {
            return message as ChatMessage;
          }

          const parsedTimestamp = new Date(message.timestamp);
          const fallbackTimestamp = Number.isNaN(parsedTimestamp.getTime()) ? new Date() : parsedTimestamp;

          return {
            ...message,
            timestamp: fallbackTimestamp,
          } as ChatMessage;
        });

        setMessages(normalizedMessages);
        setConversationId(saved.conversationId);
      }
    }
  }, [persistConversation, initialConversationId]);

  // Save conversation to localStorage when messages change
  useEffect(() => {
    if (persistConversation && conversationId && messages.length > 0) {
      const serializableMessages = messages.slice(-maxMessages).map((message) => ({
        ...message,
        timestamp: message.timestamp instanceof Date ? message.timestamp.toISOString() : message.timestamp,
      }));

      safeLocalStorage.set(STORAGE_KEY, {
        conversationId,
        messages: serializableMessages,
      });
    }
  }, [messages, conversationId, persistConversation, maxMessages]);

  const generateMessageId = () => {
    messageIdCounter.current += 1;
    return `msg-${Date.now()}-${messageIdCounter.current}`;
  };

  const addMessage = useCallback((message: Omit<ChatMessage, 'id'>) => {
    const newMessage: ChatMessage = {
      ...message,
      id: generateMessageId(),
    };

    setMessages(prev => {
      const updated = [...prev, newMessage];
      // Limit message history
      if (updated.length > maxMessages) {
        return updated.slice(-maxMessages);
      }
      return updated;
    });

    return newMessage.id;
  }, [maxMessages]);

  const updateMessage = useCallback((messageId: string, updates: Partial<ChatMessage>) => {
    setMessages(prev =>
      prev.map(msg =>
        msg.id === messageId ? { ...msg, ...updates } : msg
      )
    );
  }, []);

  const callChatApi = async (query: string, signal?: AbortSignal): Promise<ChatQueryResponse> => {
    try {
      const requestData: ChatQueryRequest = { 
        query,
        conversationId: conversationId || undefined
      };
      
      const response = await chatApi.postChatQuery(requestData, { signal, timeout: 30000 });
      return response.data;
    } catch (error: any) {
      if (error.name === 'AbortError') {
        throw new Error('Request was cancelled');
      }
      
      if (error.response?.data) {
        const errorData = error.response.data as ChatErrorResponse;
        throw new Error(errorData.message || errorData.error || 'Failed to get response');
      }
      
      throw new Error(error.message || 'Network error occurred');
    }
  };

  const sendMessageWithRetry = async (
    query: string,
    userMessageId: string,
    attempt: number = 1
  ): Promise<void> => {
    try {
      setIsTyping(true);
      setError(null);

      // Cancel any existing request
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }

      // Create new abort controller
      abortControllerRef.current = new AbortController();
      
      const startTime = Date.now();
      const response = await callChatApi(query, abortControllerRef.current.signal);
      const processingTime = Date.now() - startTime;

      // Update conversation ID if this is a new conversation
      if (response.conversationId && response.conversationId !== conversationId) {
        setConversationId(response.conversationId);
      }

      // Update user message status to sent
      updateMessage(userMessageId, { status: 'sent' });

      // Add assistant response
      addMessage({
        role: 'assistant',
        content: response.response,
        timestamp: new Date(),
        status: 'sent',
        metadata: {
          toolsUsed: response.toolsUsed,
          processingTime: response.processingTimeMs || processingTime,
          cached: response.cached,
        },
      });

    } catch (error: any) {
      console.error('Chat API error:', error);

      if (error.message === 'Request was cancelled') {
        return; // Don't show error for cancelled requests
      }

      // Retry logic
      if (attempt < retryAttempts && !error.message.includes('cancelled')) {
        console.log(`Retrying request (attempt ${attempt + 1}/${retryAttempts})`);
        setTimeout(() => {
          sendMessageWithRetry(query, userMessageId, attempt + 1);
        }, retryDelay * attempt);
        return;
      }

      // Update user message status to error
      updateMessage(userMessageId, { status: 'error' });

      // Set error state
      setError(error.message);

      // Add error message
      addMessage({
        role: 'assistant',
        content: `I apologize, but I encountered an error: ${error.message}. Please try again or rephrase your question.`,
        timestamp: new Date(),
        status: 'error',
      });
    } finally {
      setIsLoading(false);
      setIsTyping(false);
      abortControllerRef.current = null;
    }
  };

  const sendMessage = useCallback(async (content: string) => {
    if (!content.trim() || isLoading) {
      return;
    }

    setIsLoading(true);
    setError(null);

    // Add user message
    const userMessageId = addMessage({
      role: 'user',
      content: content.trim(),
      timestamp: new Date(),
      status: 'sending',
    });

    // Send to API
    await sendMessageWithRetry(content.trim(), userMessageId);
  }, [isLoading, addMessage]);

  const retryMessage = useCallback(async (messageId: string) => {
    const message = messages.find(msg => msg.id === messageId);
    if (!message || message.role !== 'user') {
      return;
    }

    // Update message status to sending
    updateMessage(messageId, { status: 'sending' });

    // Remove any error messages that came after this message
    setMessages(prev => {
      const messageIndex = prev.findIndex(msg => msg.id === messageId);
      if (messageIndex === -1) return prev;
      
      // Keep messages up to and including the retry message
      return prev.slice(0, messageIndex + 1);
    });

    setIsLoading(true);
    await sendMessageWithRetry(message.content, messageId);
  }, [messages, updateMessage]);

  const loadConversation = useCallback(async (newConversationId: string) => {
    try {
      // This would typically load from the backend API
      // For now, we'll just set the conversation ID
      setConversationId(newConversationId);
      setMessages([]);
      setError(null);
    } catch (error) {
      console.error('Error loading conversation:', error);
      setError('Failed to load conversation');
    }
  }, []);

  const clearChat = useCallback(() => {
    // Cancel any ongoing request
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    
    setMessages([]);
    setIsLoading(false);
    setIsTyping(false);
    setError(null);
    setConversationId(null);
    
    // Clear from localStorage
    if (persistConversation) {
      safeLocalStorage.remove(STORAGE_KEY);
    }
  }, [persistConversation]);

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  return {
    messages,
    isLoading,
    isTyping,
    error,
    conversationId,
    sendMessage,
    retryMessage,
    clearChat,
    clearError,
    loadConversation,
  };
};