import { useEffect, useRef, useState } from 'react'
import {
  Send,
  Bot,
  User,
  Loader2,
  Mic,
  MicOff,
  Trash2,
  PlusCircle,
  RefreshCcw,
} from 'lucide-react'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { Card } from '@/components/ui/card'
import { api, chatbotApi } from '@/services/api'
import { useToast } from '@/hooks/use-toast'
import { useChatStore, type ChatMessage } from '@/store/chatStore'

interface ChatQueryRequest {
  query: string
  conversationId?: string
}

interface ChatQueryResponse {
  response: string
  conversationId: string
  timestamp: string
  toolsUsed?: string[]
  processingTimeMs?: number
  success: boolean
  cached?: boolean
  degraded?: boolean
  confidence?: number
}

const formatDate = (value: string) =>
  new Date(value).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })

export function AiAssistant() {
  const {
    messages,
    input,
    setInput,
    isProcessing,
    setProcessing,
    error,
    setError,
    conversations,
    setConversations,
    isConversationsLoading,
    setConversationsLoading,
    activeConversationId,
    setActiveConversation,
    resetConversation,
    setMessages,
  } = useChatStore()

  const [conversationAction, setConversationAction] = useState<string | null>(null)
  const [isListening, setIsListening] = useState(false)
  const [isVoiceSupported, setIsVoiceSupported] = useState(false)
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const recognitionRef = useRef<SpeechRecognition | null>(null)
  const { toast } = useToast()

  const conversationId = activeConversationId

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  const loadConversations = async () => {
    try {
      setConversationsLoading(true)
      const res = await chatbotApi.listConversations()
      setConversations(res.data)
    } catch (e) {
      console.error('Failed to load conversations', e)
    } finally {
      setConversationsLoading(false)
    }
  }

  useEffect(() => {
    loadConversations()
  }, [])

  useEffect(() => {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
    setIsVoiceSupported(!!SpeechRecognition)
  }, [])

  const hydrateConversation = async (targetId: string) => {
    try {
      setConversationAction(targetId)
      const res = await chatbotApi.getConversation(targetId)
      const detail = res.data
      setActiveConversation(detail.conversationId)
      const hydrated: ChatMessage[] = detail.messages.map((msg, idx) => ({
        id: `${detail.conversationId}-${idx}`,
        role: msg.role === 'assistant' ? 'assistant' : 'user',
        content: msg.content,
        timestamp: new Date(msg.createdAt),
      }))
      setMessages(() => hydrated)
    } catch (e: any) {
      console.error('Failed to load conversation', e)
      setError(e?.response?.data?.message || e?.message || 'Unable to load conversation')
    } finally {
      setConversationAction(null)
    }
  }

  const handleDeleteConversation = async (targetId: string) => {
    try {
      setConversationAction(targetId)
      await chatbotApi.deleteConversation(targetId)
      if (conversationId === targetId) {
        resetConversation()
      }
      await loadConversations()
    } catch (e: any) {
      console.error('Failed to delete conversation', e)
      setError(e?.response?.data?.message || e?.message || 'Unable to delete conversation')
    } finally {
      setConversationAction(null)
    }
  }

  const startVoiceInput = () => {
    if (!isVoiceSupported) {
      toast({ description: 'Voice input is not supported in this browser.' })
      return
    }

    try {
      const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
      const recognition = new SpeechRecognition()
      recognition.continuous = false
      recognition.interimResults = true
      recognition.lang = 'en-US'
      recognition.maxAlternatives = 1

      recognition.onstart = () => setIsListening(true)
      recognition.onresult = (event: SpeechRecognitionEvent) => {
        let transcript = ''
        for (let i = 0; i < event.results.length; i++) {
          transcript += event.results[i][0].transcript
        }
        setInput(transcript)
      }
      recognition.onerror = (event: SpeechRecognitionErrorEvent) => {
        setIsListening(false)
        if (event.error === 'not-allowed') {
          toast({ variant: 'destructive', description: 'Microphone access denied.' })
        } else if (!['no-speech', 'aborted'].includes(event.error)) {
          toast({ variant: 'destructive', description: `Voice input error: ${event.error}` })
        }
      }
      recognition.onend = () => {
        setIsListening(false)
        recognitionRef.current = null
      }
      recognitionRef.current = recognition
      recognition.start()
    } catch (error) {
      console.error('Failed to start voice recognition:', error)
      setIsListening(false)
      toast({ variant: 'destructive', description: 'Failed to start voice input.' })
    }
  }

  const stopVoiceInput = () => {
    if (recognitionRef.current) {
      try {
        recognitionRef.current.stop()
      } catch (error) {
        console.error('Error stopping recognition:', error)
      }
      setIsListening(false)
    }
  }

  useEffect(() => () => stopVoiceInput(), [])

  const toggleVoiceInput = () => {
    if (isListening) stopVoiceInput()
    else startVoiceInput()
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!input.trim() || isProcessing) return

    const userMessage: ChatMessage = {
      id: Date.now().toString(),
      role: 'user',
      content: input.trim(),
      timestamp: new Date(),
    }

    setMessages((prev) => [...prev, userMessage])
    setInput('')
    setProcessing(true)
    setError(undefined)

    try {
      const requestData: ChatQueryRequest = {
        query: userMessage.content,
        conversationId,
      }
      const response = await api.post<ChatQueryResponse>('/chatbot/query', requestData)
      const assistantMessage: ChatMessage = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: response.data.response,
        timestamp: new Date(response.data.timestamp),
        toolsUsed: response.data.toolsUsed,
        processingTimeMs: response.data.processingTimeMs,
      }
      setMessages((prev) => [...prev, assistantMessage])
      if (response.data.conversationId) {
        setActiveConversation(response.data.conversationId)
      }
      loadConversations()
    } catch (err: any) {
      console.error('Error sending message:', err)
      let errorMessage = 'Failed to get response from AI assistant.'
      if (err.response?.status === 429) {
        errorMessage = 'Rate limit exceeded. Please wait a moment before trying again.'
      } else if (err.response?.status === 503) {
        errorMessage = 'AI service is currently unavailable. Please try again later.'
      } else if (err.response?.data?.message) {
        errorMessage = err.response.data.message
      }
      setError(errorMessage)
      const errorMsg: ChatMessage = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: errorMessage,
        timestamp: new Date(),
      }
      setMessages((prev) => [...prev, errorMsg])
    } finally {
      setProcessing(false)
    }
  }

  const handleNewConversation = () => {
    resetConversation()
    setInput('')
    setError(undefined)
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSubmit(e)
    }
  }

  return (
    <div className="space-y-6 p-6">
      <section className="rounded-3xl border bg-gradient-to-br from-primary/10 via-background to-background p-6 shadow-sm">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <p className="uppercase text-xs tracking-widest text-primary font-semibold">TariffSheriff Copilot</p>
            <h1 className="text-3xl font-bold mt-1 mb-2">AI Trade Assistant</h1>
            <p className="text-muted-foreground max-w-2xl">
              Chat with TariffSheriff to explore HS codes, compare MFN vs preferential rates, and walk through calculator inputs without leaving the dashboard.
            </p>
          </div>
          <div className="flex flex-wrap gap-3">
            <Button variant="outline" size="sm" onClick={handleNewConversation}>
              <PlusCircle className="mr-2 h-4 w-4" /> New conversation
            </Button>
            <Button variant="ghost" size="sm" onClick={loadConversations}>
              <RefreshCcw className="mr-2 h-4 w-4" /> Refresh history
            </Button>
          </div>
        </div>
      </section>

      <div className="grid gap-6 lg:grid-cols-[320px,1fr]">
        <Card className="h-fit rounded-2xl border bg-card">
          <div className="flex items-center justify-between border-b px-4 py-3">
            <div>
              <p className="text-sm font-semibold">Past conversations</p>
              <p className="text-xs text-muted-foreground">One history per chat thread</p>
            </div>
            <Button variant="ghost" size="icon" onClick={loadConversations}>
              <RefreshCcw className="w-4 h-4" />
            </Button>
          </div>
          <div className="max-h-[60vh] overflow-y-auto p-4 space-y-2">
            {isConversationsLoading ? (
              <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <Loader2 className="h-4 w-4 animate-spin" /> Loading conversations…
              </div>
            ) : conversations.length === 0 ? (
              <p className="text-sm text-muted-foreground">No saved chats yet.</p>
            ) : (
              conversations.map((conv) => {
                const isActive = conv.conversationId === conversationId
                return (
                  <div
                    key={conv.conversationId}
                    className={`rounded-xl border p-3 transition ${isActive ? 'border-primary bg-primary/5' : 'hover:border-primary/50'}`}
                  >
                    <button
                      className="text-left w-full"
                      onClick={() => hydrateConversation(conv.conversationId)}
                      disabled={conversationAction === conv.conversationId}
                    >
                      <p className="text-sm font-medium">{formatDate(conv.updatedAt)}</p>
                      <p className="text-xs text-muted-foreground">Started {new Date(conv.createdAt).toLocaleDateString()}</p>
                    </button>
                    <div className="flex items-center justify-between mt-2">
                      <span className="text-xs font-mono text-muted-foreground truncate">{conv.conversationId}</span>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => handleDeleteConversation(conv.conversationId)}
                        disabled={conversationAction === conv.conversationId}
                      >
                        {conversationAction === conv.conversationId ? (
                          <Loader2 className="w-4 h-4 animate-spin" />
                        ) : (
                          <Trash2 className="w-4 h-4" />
                        )}
                      </Button>
                    </div>
                  </div>
                )
              })
            )}
          </div>
        </Card>

        <Card className="flex flex-col rounded-2xl border bg-card h-[75vh]">
          <div className="flex items-center justify-between border-b px-6 py-4">
            <div>
              <p className="text-sm font-semibold">Live conversation</p>
              <p className="text-xs text-muted-foreground">
                {conversationId ? `Conversation ${conversationId}` : 'Draft conversation'}
              </p>
            </div>
          </div>

          <div className="flex-1 overflow-y-auto p-6 space-y-4">
            {messages.length === 0 && (
              <div className="text-center text-muted-foreground mt-12 space-y-4">
                <Bot className="w-16 h-16 mx-auto text-primary/40" />
                <div>
                  <p className="text-lg font-semibold">Ask anything about tariffs or HS codes</p>
                  <p className="text-sm">Try "What's the MFN vs preferential rate for 850760 from KOR to USA?"</p>
                </div>
              </div>
            )}

            {messages.map((message) => (
              <div key={message.id} className={`flex gap-3 ${message.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                {message.role === 'assistant' && (
                  <div className="flex-shrink-0">
                    <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center">
                      <Bot className="w-5 h-5 text-primary" />
                    </div>
                  </div>
                )}

                <div
                  className={`max-w-[70%] rounded-2xl p-4 text-sm leading-6 shadow-sm ${
                    message.role === 'user'
                      ? 'bg-primary text-primary-foreground'
                      : 'bg-muted text-muted-foreground'
                  }`}
                >
                  <ReactMarkdown remarkPlugins={[remarkGfm]}>{message.content ?? ''}</ReactMarkdown>
                  <div className="mt-3 flex items-center justify-between text-xs opacity-70">
                    <span>{message.timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                    {message.toolsUsed && message.toolsUsed.length > 0 && (
                      <span>Tools: {message.toolsUsed.join(', ')}</span>
                    )}
                  </div>
                  {message.processingTimeMs && (
                    <div className="text-[11px] opacity-60 mt-1">{message.processingTimeMs} ms</div>
                  )}
                </div>

                {message.role === 'user' && (
                  <div className="flex-shrink-0">
                    <div className="w-8 h-8 rounded-full bg-primary flex items-center justify-center">
                      <User className="w-5 h-5 text-primary-foreground" />
                    </div>
                  </div>
                )}
              </div>
            ))}

            {isProcessing && (
              <div className="flex gap-3 justify-start">
                <div className="flex-shrink-0">
                  <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center">
                    <Bot className="w-5 h-5 text-primary" />
                  </div>
                </div>
                <div className="bg-muted rounded-xl px-4 py-2">
                  <Loader2 className="w-5 h-5 animate-spin text-muted-foreground" />
                </div>
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>

          <div className="border-t px-6 py-4">
            {error && (
              <div className="mb-3 p-3 bg-red-50 border border-red-200 rounded-md text-sm text-red-800">
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit} className="flex gap-2">
              <div className="flex-1 relative">
                <Textarea
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  onKeyDown={handleKeyDown}
                  placeholder="Ask about tariffs, HS codes, agreements, or calculator inputs…"
                  className="min-h-[64px] max-h-[140px] resize-none pr-12"
                  disabled={isProcessing}
                />
                {isVoiceSupported && (
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    onClick={toggleVoiceInput}
                    disabled={isProcessing}
                    className={`absolute right-2 top-2 ${
                      isListening ? 'text-red-600 bg-red-50 animate-pulse' : 'text-muted-foreground'
                    }`}
                    title={isListening ? 'Stop voice input' : 'Start voice input'}
                  >
                    {isListening ? <Mic className="w-5 h-5" /> : <MicOff className="w-5 h-5" />}
                  </Button>
                )}
              </div>
              <Button type="submit" disabled={!input.trim() || isProcessing} className="self-end">
                {isProcessing ? <Loader2 className="w-5 h-5 animate-spin" /> : <Send className="w-5 h-5" />}
              </Button>
            </form>
            <p className="text-xs text-muted-foreground mt-2">
              Press Enter to send, Shift+Enter for a new line
              {isVoiceSupported && ' • Click the microphone to dictate'}
            </p>
          </div>
        </Card>
      </div>
    </div>
  )
}

export default AiAssistant
