import React from 'react'
import { tariffApi } from '@/services/api'

interface HsCode {
  hsCode: string
  hsLabel: string
}

interface HsCodeSelectProps {
  value?: string
  onChange?: (code: string) => void
  placeholder?: string
  className?: string
  required?: boolean
  disabled?: boolean
}

const HsCodeSelect: React.FC<HsCodeSelectProps> = ({
  value = '',
  onChange,
  placeholder = 'Search HS by code or name',
  className = '',
  required = false,
  disabled = false,
}) => {
  const [query, setQuery] = React.useState('')
  const [open, setOpen] = React.useState(false)
  const [highlight, setHighlight] = React.useState(0)
  const [results, setResults] = React.useState<HsCode[]>([])
  const [loading, setLoading] = React.useState(false)
  const [error, setError] = React.useState<string | null>(null)
  const ref = React.useRef<HTMLDivElement | null>(null)

  React.useEffect(() => {
    if (!value) return
    // When value is controlled externally, keep input in sync (format with dot groups optionally later)
    setQuery(value)
  }, [value])

  React.useEffect(() => {
    function onDoc(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('click', onDoc)
    return () => document.removeEventListener('click', onDoc)
  }, [])

  // Debounced search
  React.useEffect(() => {
    let cancelled = false
    const q = query.trim()
    if (q.length < 2) {
      setResults([])
      setError(null)
      return
    }
    setLoading(true)
    setError(null)
    const t = setTimeout(async () => {
      try {
        const res = await tariffApi.searchHsProducts({ q, limit: 10 })
        if (cancelled) return
        setResults(res.data || [])
      } catch (e: any) {
        if (cancelled) return
        setResults([])
        setError(e?.response?.data?.message || e?.message || 'Search failed')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }, 200)
    return () => {
      cancelled = true
      clearTimeout(t)
    }
  }, [query])

  const select = (item: HsCode) => {
    onChange?.(item.hsCode)
    setQuery(item.hsCode)
    setOpen(false)
  }

  const handleKeyDown: React.KeyboardEventHandler<HTMLInputElement> = (e) => {
    if (!open && (e.key === 'ArrowDown' || e.key === 'ArrowUp')) {
      setOpen(true)
      e.preventDefault()
      return
    }
    if (e.key === 'ArrowDown') {
      setHighlight((h) => Math.min(h + 1, Math.max(0, results.length - 1)))
      e.preventDefault()
    } else if (e.key === 'ArrowUp') {
      setHighlight((h) => Math.max(0, h - 1))
      e.preventDefault()
    } else if (e.key === 'Enter') {
      const sel = results[highlight]
      if (sel) select(sel)
      e.preventDefault()
    } else if (e.key === 'Escape') {
      setOpen(false)
    }
  }

  return (
    <div ref={ref} className={`relative ${className}`}>
      <div className="flex flex-col">
        <label className="sr-only">Choose HS code</label>
        <input
          type="text"
          role="combobox"
          aria-expanded={open}
          aria-autocomplete="list"
          placeholder={placeholder}
          value={query}
          disabled={disabled}
          onChange={(e) => { setQuery(e.target.value); setOpen(true); setHighlight(0) }}
          onFocus={() => setOpen(true)}
          onKeyDown={handleKeyDown}
          required={required}
          className="rounded-md border px-3 py-2 bg-white dark:bg-slate-900 w-full disabled:opacity-50 disabled:cursor-not-allowed"
        />
      </div>

      {open && (
        <ul role="listbox" className="absolute z-50 mt-1 max-h-64 w-full overflow-auto rounded-md border bg-white dark:bg-slate-900 shadow-lg">
          {loading ? (
            <li className="px-3 py-2 text-sm text-muted-foreground">Searching…</li>
          ) : error ? (
            <li className="px-3 py-2 text-sm text-red-600">{error}</li>
          ) : results.length === 0 ? (
            <li className="px-3 py-2 text-sm text-muted-foreground">No matches</li>
          ) : (
            results.map((r, idx) => (
              <li
                key={`${r.hsCode}-${idx}`}
                role="option"
                aria-selected={value === r.hsCode}
                onMouseDown={(e) => { e.preventDefault(); select(r) }}
                onMouseEnter={() => setHighlight(idx)}
                className={`px-3 py-2 cursor-pointer text-sm ${idx === highlight ? 'bg-brand-50 dark:bg-brand-800/40' : ''}`}
              >
                <div className="flex items-center justify-between gap-3">
                  <span className="font-medium">{r.hsCode}</span>
                  <span className="text-xs text-muted-foreground flex-1 text-right">{r.hsLabel}</span>
                </div>
              </li>
            ))
          )}
        </ul>
      )}
    </div>
  )
}

export default HsCodeSelect

