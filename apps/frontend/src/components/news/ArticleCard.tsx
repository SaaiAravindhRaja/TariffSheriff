import { Calendar, ExternalLink } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'

interface Article {
  id?: number
  title: string
  url: string
  content: string
  queryContext?: string
  source?: 'db' | 'api'
  publishedAt?: string
}

interface ArticleCardProps {
  article: Article
  onReadMore: (article: Article) => void
}

export function ArticleCard({ article, onReadMore }: ArticleCardProps) {
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
