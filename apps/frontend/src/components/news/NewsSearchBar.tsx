import { useState } from 'react'
import { Search, Loader2, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'

interface NewsSearchBarProps {
  onSearch: (query: string) => void
  loading: boolean
  onClear?: () => void
}

export function NewsSearchBar({ onSearch, loading, onClear }: NewsSearchBarProps) {
  const [query, setQuery] = useState('')

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    
    // Validate query is not empty
    if (!query.trim() || loading) return
    
    onSearch(query.trim())
  }

  const handleClear = () => {
    setQuery('')
    if (onClear) {
      onClear()
    }
  }

  // Handle Enter key submission
  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && !loading && query.trim()) {
      handleSubmit(e as any)
    }
  }

  return (
    <Card>
      <CardContent className="pt-6">
        <form onSubmit={handleSubmit} className="flex gap-2">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
            <Input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Search for tariff news, trade policies, or specific topics..."
              className="pl-10"
              disabled={loading}
            />
            {query && (
              <button
                type="button"
                onClick={handleClear}
                className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors"
                disabled={loading}
              >
                <X className="w-4 h-4" />
              </button>
            )}
          </div>
          <Button
            type="submit"
            disabled={!query.trim() || loading}
          >
            {loading ? (
              <Loader2 className="w-5 h-5 animate-spin" />
            ) : (
              'Search'
            )}
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}
