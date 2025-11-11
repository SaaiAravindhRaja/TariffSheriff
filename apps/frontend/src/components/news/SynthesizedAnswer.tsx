import { useState } from 'react'
import { Sparkles, ChevronDown, ChevronUp, ExternalLink } from 'lucide-react'
import { Card, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'

interface Article {
  id?: number
  title: string
  url: string
  content: string
  queryContext?: string
  source?: 'db' | 'api'
  publishedAt?: string
}

interface SynthesizedAnswerProps {
  answer: string
  articles: Article[]
  source?: 'db' | 'api'
}

export function SynthesizedAnswer({ answer, articles, source }: SynthesizedAnswerProps) {
  const [isExpanded, setIsExpanded] = useState(false)

  if (!answer) return null

  return (
    <Card className="border-primary/20 bg-primary/5">
      <CardContent className="pt-6">
        <div className="flex items-start gap-3">
          <Sparkles className="w-6 h-6 text-primary flex-shrink-0 mt-1" />
          <div className="flex-1">
            <div className="flex items-center gap-2 mb-2">
              <h3 className="font-semibold text-lg">AI Summary</h3>
              {source && (
                <span className="text-xs px-2 py-1 rounded-full bg-primary/10 text-primary">
                  {source === 'db' ? 'From Cache' : 'Fresh Results'}
                </span>
              )}
            </div>
            
            {/* Answer Text */}
            <p className="text-gray-700 leading-relaxed whitespace-pre-wrap mb-3">
              {answer}
            </p>
            
            {/* Source Article Count */}
            <p className="text-xs text-gray-500 mb-3">
              Based on {articles.length} relevant article{articles.length !== 1 ? 's' : ''}
            </p>

            {/* Expandable Article References */}
            {articles.length > 0 && (
              <div className="mt-4">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => setIsExpanded(!isExpanded)}
                  className="flex items-center gap-2 text-primary hover:text-primary/80 p-0 h-auto"
                >
                  {isExpanded ? (
                    <>
                      <ChevronUp className="w-4 h-4" />
                      Hide article references
                    </>
                  ) : (
                    <>
                      <ChevronDown className="w-4 h-4" />
                      Show article references
                    </>
                  )}
                </Button>

                {isExpanded && (
                  <div className="mt-3 space-y-2">
                    {articles.map((article, index) => (
                      <div
                        key={article.id || index}
                        className="p-3 bg-white rounded-md border border-gray-200 hover:border-primary/30 transition-colors"
                      >
                        <div className="flex items-start justify-between gap-2">
                          <div className="flex-1">
                            <h4 className="font-medium text-sm mb-1 line-clamp-2">
                              {article.title}
                            </h4>
                            {article.queryContext && (
                              <p className="text-xs text-gray-600 line-clamp-2">
                                {article.queryContext}
                              </p>
                            )}
                          </div>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="flex-shrink-0 h-8 w-8"
                            onClick={() => window.open(article.url, '_blank', 'noopener,noreferrer')}
                          >
                            <ExternalLink className="w-4 h-4" />
                          </Button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  )
}
