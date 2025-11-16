import { useState, useEffect } from 'react'
import { Newspaper, AlertCircle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { LoadingSpinner } from '@/components/ui/loading'
import { newsApi, type Article } from '@/services/api'
import { useAuth } from '@/hooks/useAuth'
import { ArticleCard, ArticleModal, NewsSearchBar } from '@/components/news'

export function News() {
  const [articles, setArticles] = useState<Article[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [selectedArticle, setSelectedArticle] = useState<Article | null>(null)
  const [searchLoading, setSearchLoading] = useState(false)
  const [currentPage, setCurrentPage] = useState(0)
  const [hasMore, setHasMore] = useState(true)
  const [isSearchActive, setIsSearchActive] = useState(false)
  const articlesPerPage = 3
  const { user } = useAuth()

  // Fetch all articles on mount
  useEffect(() => {
    fetchArticles(0)
  }, [])

  const fetchArticles = async (page: number) => {
    try {
      setLoading(true)
      setError(null)
      const response = await newsApi.getAllArticles(page, articlesPerPage)
      setArticles(response.data)
      setCurrentPage(page)
      setHasMore(response.data.length === articlesPerPage)
      setIsSearchActive(false)
    } catch (err: any) {
      console.error('Error fetching articles:', err)
      setError(err.message || 'Failed to load articles. Please try again later.')
    } finally {
      setLoading(false)
    }
  }

  const handleSearch = async (query: string) => {
    if (!query.trim() || searchLoading) return

    try {
      setSearchLoading(true)
      setError(null)
      const payload: { query: string; username?: string } = { query: query.trim() }
      if (user?.email) {
        payload.username = user.email
      }
      const response = await newsApi.queryNews(payload)
      setArticles(response.data.articles ?? [])
      setIsSearchActive(true)
      setHasMore(false)
      setCurrentPage(0)
    } catch (err: any) {
      console.error('Error searching articles:', err)
      let errorMessage = 'Failed to search articles. Please try again.'
      
      if (err.message) {
        errorMessage = err.message
      } else if (err.errorCode) {
        errorMessage = `Error: ${err.errorCode}`
      }
      
      setError(errorMessage)
      setIsSearchActive(false)
    } finally {
      setSearchLoading(false)
    }
  }

  const handleReadMore = (article: Article) => {
    setSelectedArticle(article)
  }

  const handleCloseModal = () => {
    setSelectedArticle(null)
  }

  const handleClearSearch = () => {
    setCurrentPage(0)
    setIsSearchActive(false)
    fetchArticles(0)
  }

  const handleRetry = () => {
    fetchArticles(currentPage)
  }

  const handlePreviousPage = () => {
    if (currentPage > 0) {
      fetchArticles(currentPage - 1)
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
  }

  const handleNextPage = () => {
    if (hasMore) {
      fetchArticles(currentPage + 1)
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
  }

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
      <div className="mb-6">
        <NewsSearchBar
          onSearch={handleSearch}
          loading={searchLoading}
          onClear={handleClearSearch}
        />
      </div>

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
                {isSearchActive
                  ? 'Try adjusting your search query or search for different topics.'
                  : 'No articles are currently available. Check back later for updates.'}
              </p>
              {isSearchActive && (
                <Button
                  variant="outline"
                  onClick={handleClearSearch}
                >
                  Clear Search
                </Button>
              )}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Articles Grid - 3 per page in a single row on large screens */}
      {!loading && articles.length > 0 && (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {articles.map((article, index) => (
              <ArticleCard
                key={article.id || index}
                article={article}
                onReadMore={handleReadMore}
              />
            ))}
          </div>

          {/* Pagination Controls */}
          {!isSearchActive && (
            <div className="flex items-center justify-center gap-4 mt-8">
              <Button
                variant="outline"
                onClick={handlePreviousPage}
                disabled={currentPage === 0}
                className="flex items-center gap-2"
              >
                <span>←</span>
                Previous
              </Button>
              
              <span className="text-sm text-gray-600">
                Page {currentPage + 1}
              </span>
              
              <Button
                variant="outline"
                onClick={handleNextPage}
                disabled={!hasMore}
                className="flex items-center gap-2"
              >
                Next
                <span>→</span>
              </Button>
            </div>
          )}
        </>
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

export default News
