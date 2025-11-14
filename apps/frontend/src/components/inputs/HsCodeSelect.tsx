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
  importerIso3?: string
  filterCodes?: string[] // Optional list of allowed HS codes
}

const HsCodeSelect: React.FC<HsCodeSelectProps> = ({
  value = '',
  onChange,
  placeholder = 'Search HS by code or name',
  className = '',
  required = false,
  disabled = false,
  importerIso3,
  filterCodes,
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

  // Debounced search, including initial fetch when importer is selected and query is empty
  React.useEffect(() => {
    let cancelled = false
    const q = query.trim()
    const allowEmptyForImporter = open && importerIso3 && importerIso3.trim().length > 0 && q.length === 0
    const isDigitsOnly = /^[0-9]*$/.test(q) // allow empty as digits-only
    const tooShort = (!allowEmptyForImporter) && (isDigitsOnly ? q.length < 1 : q.length < 2)
    if (tooShort) {
      setResults([])
      setError(null)
      return
    }
    setLoading(true)
    setError(null)
    
    const t = setTimeout(async () => {
      try {
        // If filterCodes exist and query is short, show those first
        if (filterCodes && filterCodes.length > 0 && q.length < 2) {
          // Fetch details for filtered codes
          const codePromises = filterCodes.slice(0, 10).map(async (code) => {
            try {
              const res = await tariffApi.searchHsProducts({ q: code, limit: 1 })
              return res.data?.[0] || { hsCode: code, hsLabel: 'Product description' }
            } catch {
              return { hsCode: code, hsLabel: 'Product description' }
            }
          })
          const codeDetails = await Promise.all(codePromises)
          if (cancelled) return
          setResults(codeDetails)
          setLoading(false)
          return
        }
        
        // For queries with at least 1 character, do normal search
        if (q.length < 1) {
          // Show some popular/common codes as examples
          const res = await tariffApi.searchHsProducts({ q: '87', limit: 10 })
          if (cancelled) return
          setResults(res.data || [])
          setLoading(false)
          return
        }
        
        const res = await tariffApi.searchHsProducts({ q, limit: 200, importerIso3 })
        if (cancelled) return
        
        // If filterCodes is provided, prioritize them but don't exclude others
        let filteredData = res.data || []
        if (filterCodes && filterCodes.length > 0) {
          // Sort: matching filterCodes first, then others
          filteredData = filteredData.sort((a, b) => {
            const aMatch = filterCodes.includes(a.hsCode) ? 0 : 1
            const bMatch = filterCodes.includes(b.hsCode) ? 0 : 1
            return aMatch - bMatch
          })
        }
        
        setResults(filteredData)
      } catch (e: any) {
        if (cancelled) return
        setResults([])
        setError(e?.response?.data?.message || e?.message || 'Search failed')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }, q.length === 0 ? 0 : 200) // No delay for empty query, 200ms for others
    
    return () => {
      cancelled = true
      clearTimeout(t)
    }
  }, [query, filterCodes, importerIso3, open])

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
            results.map((r, idx) => {
              const isAvailable = filterCodes && filterCodes.length > 0 ? filterCodes.includes(r.hsCode) : true
              return (
                <li
                  key={`${r.hsCode}-${idx}`}
                  role="option"
                  aria-selected={value === r.hsCode}
                  onMouseDown={(e) => { e.preventDefault(); select(r) }}
                  onMouseEnter={() => setHighlight(idx)}
                  className={`px-3 py-2 cursor-pointer text-sm ${idx === highlight ? 'bg-brand-50 dark:bg-brand-800/40' : ''}`}
                >
                  <div className="flex items-center justify-between gap-3">
                    <div className="flex items-center gap-2">
                      {filterCodes && filterCodes.length > 0 && (
                        <span className={`w-2 h-2 rounded-full ${isAvailable ? 'bg-green-500' : 'bg-gray-300'}`} title={isAvailable ? 'Data available for this route' : 'Limited data for this route'} />
                      )}
                      <span className="font-medium">{r.hsCode}</span>
                    </div>
                    <span className="text-xs text-muted-foreground flex-1 text-right">{r.hsLabel}</span>
                  </div>
                </li>
              )
            })
          )}
        </ul>
      )}
    </div>
  )
}

export default HsCodeSelect

