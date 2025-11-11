import { useState, useEffect } from 'react'
import { Newspaper, Calendar, ExternalLink, X, Loader2, Search, AlertCircle, Sparkles } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { LoadingSpinner } from '@/components/ui/loading'
import { api } from '@/services/api'
import { useAuth } from '@/hooks/useAuth'
import { cn } from '@/lib/utils'

interface Article {
  id?: number
  title: string
  url: string
  content: string
  queryContext?: string
  source?: 'db' | 'api'
  publishedAt?: string
}

interface NewsQueryResponse {
  synthesizedAnswer: string
  source: 'db' | 'api'
  articles: Article[]
  conversationId?: number
}

export function News() {
  const [articles, setArticles] = useState<Article[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [selectedArticle, setSelectedArticle] = useState<Article | null>(null)
  const [searchQuery, setSearchQuery] = useState('')
  const [synthesizedAnswer, setSynthesizedAnswer] = useState<string | null>(null)
  const [conversationId, setConversationId] = useState<number | null>(null)
  const [searchLoading, setSearchLoading] = useState(false)
  const [answerSource, setAnswerSource] = useState<'db' | 'api' | null>(null)
  const { user } = useAuth()

  // Fetch all articles on mount
  useEffect(() => {
    fetchArticles()
  }, [])

  const fetchArticles = async () => {
    try {
      setLoading(true)
      setError(null)
      const response = await api.get<Article[]>('/news/articles')
      setArticles(response.data)
    } catch (err: any) {
      console.error('Error fetching articles:', err)
      setError('Failed to load articles. Please try again later.')
    } finally {
      setLoading(false)
    }
  }

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!searchQuery.trim() || searchLoading) return

    try {
      setSearchLoading(true)
      setError(null)
      
      const requestData: any = {
        query: searchQuery.trim(),
      }
      
      if (user?.email) {
        requestData.username = user.email
      }
      
      if (conversationId) {
        requestData.conversationId = conversationId
      }

      const response = await api.post<NewsQueryResponse>('/news/query', null, {
        params: requestData
      })
      
      setArticles(response.data.articles)
      setSynthesizedAnswer(response.data.synthesizedAnswer)
      setAnswerSource(response.data.source)
      
      if (response.data.conversationId) {
        setConversationId(response.data.conversationId)
      }
    } catch (err: any) {
      console.error('Error searching articles:', err)
      
      let errorMessage = 'Failed to search articles. Please try again.'
      
      if (err.response?.status === 400) {
        errorMessage = 'Invalid search query. Please try a different search.'
      } else if (err.response?.status === 500) {
        errorMessage = 'Server error occurred. Please try again later.'
      } else if (err.response?.data?.message) {
        errorMessage = err.response.data.message
      }
      
      setError(errorMessage)
    } finally {
      setSearchLoading(false)
    }
  }

  const handleReadMore = (article: Article) => {
    setSelectedArticle(article)
    document.body.style.overflow = 'hidden'
  }

  const handleCloseModal = () => {
    setSelectedArticle(null)
    document.body.style.overflow = 'unset'
  }

  const handleRetry = () => {
    if (searchQuery.trim()) {
      handleSearch({ preventDefault: () => {} } as React.FormEvent)
    } else {
      fetchArticles()
    }
  }

  // Handle Escape key to close modal
  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && selectedArticle) {
        handleCloseModal()
      }
    }

    document.addEventListener('keydown', handleEscape)
    return () => document.removeEventListener('keydown', handleEscape)
  }, [selectedArticle])

  return (
    <div className="container mx-auto p-6 max-w-7xl">
      {/* Page Header */}
      <div className="mb-6">
        <div className="flex items-center gap-3 mb-2">
          <Newspaper className="w-8 h-8 text-primary" />
          <h1 className="text-3xl font-bold">Tariff News</h1>
        </div>
        <p className="text-gray-600">
          Stay informed about the latest tariff-related news, trade policies, and international commerce developments
        </p>
      </div>

      {/* Search Bar */}
      <Card className="mb-6">
        <CardContent className="pt-6">
          <form onSubmit={handleSearch} className="flex gap-2">
            <div className="flex-1 relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
              <Input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search for tariff news, trade policies, or specific topics..."
                className="pl-10"
                disabled={searchLoading}
              />
            </div>
            <Button
              type="submit"
              disabled={!searchQuery.trim() || searchLoading}
            >
              {searchLoading ? (
                <Loader2 className="w-5 h-5 animate-spin" />
              ) : (
                'Search'
              )}
            </Button>
            {searchQuery && (
              <Button
                type="button"
                variant="outline"
                onClick={() => {
                  setSearchQuery('')
                  setSynthesizedAnswer(null)
                  setAnswerSource(null)
                  fetchArticles()
                }}
              >
                Clear
              </Button>
            )}
          </form>
        </CardContent>
      </Card>

      {/* Synthesized Answer */}
      {synthesizedAnswer && (
        <Card className="mb-6 border-primary/20 bg-primary/5">
          <CardContent className="pt-6">
            <div className="flex items-start gap-3">
              <Sparkles className="w-6 h-6 text-primary flex-shrink-0 mt-1" />
              <div className="flex-1">
                <div className="flex items-center gap-2 mb-2">
                  <h3 className="font-semibold text-lg">AI Summary</h3>
                  <span className="text-xs px-2 py-1 rounded-full bg-primary/10 text-primary">
                    {answerSource === 'db' ? 'From Cache' : 'Fresh Results'}
                  </span>
                </div>
                <p className="text-gray-700 leading-relaxed whitespace-pre-wrap">
                  {synthesizedAnswer}
                </p>
                <p className="text-xs text-gray-500 mt-3">
                  Based on {articles.length} relevant article{articles.length !== 1 ? 's' : ''}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Error Message */}
      {error && (
        <Card className="mb-6 border-red-200 bg-red-50">
          <CardContent className="pt-6">
            <div className="flex items-start gap-3">
              <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
              <div className="flex-1">
                <p className="text-red-800">{error}</p>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleRetry}
                  className="mt-3"
                >
                  Try Again
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Loading State */}
      {loading && (
        <div className="flex flex-col items-center justify-center py-16">
          <LoadingSpinner size="lg" className="mb-4" />
          <p className="text-gray-600">Loading articles...</p>
        </div>
      )}

      {/* Empty State */}
      {!loading && articles.length === 0 && !error && (
        <Card>
          <CardContent className="py-16">
            <div className="text-center">
              <Newspaper className="w-16 h-16 mx-auto mb-4 text-gray-400" />
              <h3 className="text-xl font-semibold mb-2">No articles found</h3>
              <p className="text-gray-600 mb-4">
                {searchQuery
                  ? 'Try adjusting your search query or search for different topics.'
                  : 'No articles are currently available. Check back later for updates.'}
              </p>
              {searchQuery && (
                <Button
                  variant="outline"
                  onClick={() => {
                    setSearchQuery('')
                    setSynthesizedAnswer(null)
                    fetchArticles()
                  }}
                >
                  Clear Search
                </Button>
              )}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Articles Grid */}
      {!loading && articles.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {articles.map((article, index) => (
            <ArticleCard
              key={article.id || index}
              article={article}
              onReadMore={handleReadMore}
            />
          ))}
        </div>
      )}

      {/* Article Modal */}
      {selectedArticle && (
        <ArticleModal
          article={selectedArticle}
          isOpen={!!selectedArticle}
          onClose={handleCloseModal}
        />
      )}
    </div>
  )
}

// Article Card Component
interface ArticleCardProps {
  article: Article
  onReadMore: (article: Article) => void
}

function ArticleCard({ article, onReadMore }: ArticleCardProps) {
  const formatDate = (dateString?: string) => {
    if (!dateString) return 'Date unknown'
    try {
      return new Date(dateString).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
      })
    } catch {
      return 'Date unknown'
    }
  }

  return (
    <Card className="flex flex-col h-full hover:shadow-lg transition-shadow duration-200">
      <CardContent className="pt-6 flex flex-col flex-1">
        <h3 className="font-semibold text-lg mb-2 line-clamp-2">
          {article.title}
        </h3>
        
        <div className="flex items-center gap-2 text-sm text-gray-500 mb-3">
          <Calendar className="w-4 h-4" />
          <span>{formatDate(article.publishedAt)}</span>
        </div>

        <p className="text-gray-600 text-sm mb-4 line-clamp-3 flex-1">
          {article.content}
        </p>

        {article.queryContext && (
          <div className="mb-4 p-3 bg-blue-50 rounded-md border border-blue-100">
            <p className="text-xs text-blue-800">
              <span className="font-semibold">Relevance: </span>
              {article.queryContext}
            </p>
          </div>
        )}

        <div className="flex gap-2 mt-auto">
          <Button
            variant="outline"
            size="sm"
            onClick={() => onReadMore(article)}
            className="flex-1"
          >
            Read More
          </Button>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => window.open(article.url, '_blank', 'noopener,noreferrer')}
            className="flex items-center gap-1"
          >
            <ExternalLink className="w-4 h-4" />
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}

// Article Modal Component
interface ArticleModalProps {
  article: Article | null
  isOpen: boolean
  onClose: () => void
}

function ArticleModal({ article, isOpen, onClose }: ArticleModalProps) {
  if (!article || !isOpen) return null

  const formatDate = (dateString?: string) => {
    if (!dateString) return 'Date unknown'
    try {
      return new Date(dateString).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      })
    } catch {
      return 'Date unknown'
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-lg shadow-xl max-w-3xl w-full max-h-[90vh] overflow-hidden flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Modal Header */}
        <div className="flex items-start justify-between p-6 border-b">
          <div className="flex-1 pr-4">
            <h2 className="text-2xl font-bold mb-2">{article.title}</h2>
            <div className="flex items-center gap-2 text-sm text-gray-500">
              <Calendar className="w-4 h-4" />
              <span>{formatDate(article.publishedAt)}</span>
            </div>
          </div>
          <Button
            variant="ghost"
            size="icon"
            onClick={onClose}
            className="flex-shrink-0"
          >
            <X className="w-5 h-5" />
          </Button>
        </div>

        {/* Modal Content */}
        <div className="flex-1 overflow-y-auto p-6">
          <div className="prose max-w-none">
            <p className="text-gray-700 leading-relaxed whitespace-pre-wrap">
              {article.content}
            </p>
          </div>

          {article.queryContext && (
            <div className="mt-6 p-4 bg-blue-50 rounded-md border border-blue-100">
              <p className="text-sm text-blue-800">
                <span className="font-semibold">Why this article is relevant: </span>
                {article.queryContext}
              </p>
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="flex items-center justify-between p-6 border-t bg-gray-50">
          <Button
            variant="outline"
            onClick={onClose}
          >
            Close
          </Button>
          <Button
            onClick={() => window.open(article.url, '_blank', 'noopener,noreferrer')}
            className="flex items-center gap-2"
          >
            <ExternalLink className="w-4 h-4" />
            Read Full Article
          </Button>
        </div>
      </div>
    </div>
  )
}

export default News
