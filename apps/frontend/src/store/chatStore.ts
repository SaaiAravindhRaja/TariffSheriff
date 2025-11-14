import { create } from 'zustand'
import type { ChatConversationSummary } from '@/services/api'

export type ChatMessage = {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
  toolsUsed?: string[]
  processingTimeMs?: number
}

interface ChatState {
  messages: ChatMessage[]
  conversations: ChatConversationSummary[]
  activeConversationId?: string
  isConversationsLoading: boolean
  isProcessing: boolean
  error?: string
  input: string
  setInput: (value: string) => void
  setError: (value?: string) => void
  setProcessing: (value: boolean) => void
  setMessages: (updater: (prev: ChatMessage[]) => ChatMessage[]) => void
  setConversations: (list: ChatConversationSummary[]) => void
  setConversationsLoading: (value: boolean) => void
  setActiveConversation: (id?: string) => void
  resetConversation: () => void
}

export const useChatStore = create<ChatState>((set) => ({
  messages: [],
  conversations: [],
  isConversationsLoading: true,
  isProcessing: false,
  input: '',
  setInput: (value) => set({ input: value }),
  setError: (value) => set({ error: value }),
  setProcessing: (value) => set({ isProcessing: value }),
  setMessages: (updater) => set((state) => ({ messages: updater(state.messages) })),
  setConversations: (list) => set({ conversations: list }),
  setConversationsLoading: (value) => set({ isConversationsLoading: value }),
  setActiveConversation: (id) => set({ activeConversationId: id }),
  resetConversation: () => set({ messages: [], activeConversationId: undefined }),
}))
