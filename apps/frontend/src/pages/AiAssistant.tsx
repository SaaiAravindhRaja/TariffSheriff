import { useState, useRef, useEffect } from 'react'
import { Send, Bot, User, Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { Card } from '@/components/ui/card'
import { api } from '@/services/api'

interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
  toolsUsed?: string[]
  processingTimeMs?: number
}

interface ChatQueryRequest {
  query: string
  conversationId?: string
  userId?: string
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

export function AiAssistant() {
  const [messages, setMessages] = useState<Message[]>([])
  const [input, setInput] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [conversationId, setConversationId] = useState<string>()
  const [error, setError] = useState<string>()
  const messagesEndRef = useRef<HTMLDivElement>(null)

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!input.trim() || isLoading) return

    const userMessage: Message = {
      id: Date.now().toString(),
      role: 'user',
      content: input.trim(),
      timestamp: new Date(),
    }

    setMessages(prev => [...prev, userMessage])
    setInput('')
    setIsLoading(true)
    setError(undefined)

    try {
      const requestData: ChatQueryRequest = {
        query: userMessage.content,
        conversationId,
      }

      const response = await api.post<ChatQueryResponse>('/chatbot/query', requestData)
      
      const assistantMessage: Message = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: response.data.response,
        timestamp: new Date(response.data.timestamp),
        toolsUsed: response.data.toolsUsed,
        processingTimeMs: response.data.processingTimeMs,
      }

      setMessages(prev => [...prev, assistantMessage])
      
      if (response.data.conversationId) {
        setConversationId(response.data.conversationId)
      }
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
      
      const errorMsg: Message = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: errorMessage,
        timestamp: new Date(),
      }
      
      setMessages(prev => [...prev, errorMsg])
    } finally {
      setIsLoading(false)
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSubmit(e)
    }
  }

  return (
    <div className="container mx-auto p-6 max-w-5xl">
      <div className="mb-6">
        <h1 className="text-3xl font-bold mb-2">AI Trade Assistant</h1>
        <p className="text-gray-600">
          Ask questions about tariffs, trade agreements, HS codes, and more. The AI assistant can help you find information and calculate tariff rates.
        </p>
      </div>

      <Card className="flex flex-col h-[calc(100vh-250px)]">
        {/* Messages Area */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4">
          {messages.length === 0 && (
            <div className="text-center text-gray-500 mt-8">
              <Bot className="w-16 h-16 mx-auto mb-4 text-gray-400" />
              <p className="text-lg font-medium mb-2">Welcome to the AI Trade Assistant</p>
              <p className="text-sm">Start a conversation by asking a question about tariffs or trade.</p>
              <div className="mt-6 text-left max-w-md mx-auto space-y-2">
                <p className="text-sm font-medium">Example questions:</p>
                <ul className="text-sm text-gray-600 space-y-1">
                  <li>• What is the tariff rate for importing electronics from China to the US?</li>
                  <li>• Find HS codes for coffee beans</li>
                  <li>• What trade agreements does Canada have?</li>
                  <li>• Calculate tariff for HS code 8471.30 from Mexico to US</li>
                </ul>
              </div>
            </div>
          )}

          {messages.map((message) => (
            <div
              key={message.id}
              className={`flex gap-3 ${
                message.role === 'user' ? 'justify-end' : 'justify-start'
              }`}
            >
              {message.role === 'assistant' && (
                <div className="flex-shrink-0">
                  <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center">
                    <Bot className="w-5 h-5 text-primary" />
                  </div>
                </div>
              )}
              
              <div
                className={`max-w-[70%] rounded-lg p-4 ${
                  message.role === 'user'
                    ? 'bg-primary text-primary-foreground'
                    : 'bg-gray-100 text-gray-900'
                }`}
              >
                <p className="whitespace-pre-wrap break-words">{message.content}</p>
                
                {message.toolsUsed && message.toolsUsed.length > 0 && (
                  <div className="mt-2 pt-2 border-t border-gray-300 text-xs text-gray-600">
                    <span className="font-medium">Tools used:</span> {message.toolsUsed.join(', ')}
                  </div>
                )}
                
                {message.processingTimeMs && (
                  <div className="mt-1 text-xs text-gray-500">
                    {message.processingTimeMs}ms
                  </div>
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

          {isLoading && (
            <div className="flex gap-3 justify-start">
              <div className="flex-shrink-0">
                <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center">
                  <Bot className="w-5 h-5 text-primary" />
                </div>
              </div>
              <div className="bg-gray-100 rounded-lg p-4">
                <Loader2 className="w-5 h-5 animate-spin text-gray-600" />
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* Input Area */}
        <div className="border-t p-4">
          {error && (
            <div className="mb-3 p-3 bg-red-50 border border-red-200 rounded-md text-sm text-red-800">
              {error}
            </div>
          )}
          
          <form onSubmit={handleSubmit} className="flex gap-2">
            <Textarea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Ask a question about tariffs, trade agreements, or HS codes..."
              className="flex-1 min-h-[60px] max-h-[120px] resize-none"
              disabled={isLoading}
            />
            <Button
              type="submit"
              disabled={!input.trim() || isLoading}
              className="self-end"
            >
              {isLoading ? (
                <Loader2 className="w-5 h-5 animate-spin" />
              ) : (
                <Send className="w-5 h-5" />
              )}
            </Button>
          </form>
          
          <p className="text-xs text-gray-500 mt-2">
            Press Enter to send, Shift+Enter for new line
          </p>
        </div>
      </Card>
    </div>
  )
}

export default AiAssistant
