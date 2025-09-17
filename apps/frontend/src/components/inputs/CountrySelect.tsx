import React from 'react'

export type Country = {
  code: string // ISO2 code e.g. 'SG'
  name: string
  flag: string // emoji flag
}

// Expanded default list for reasonable suggestions in calculators
const DEFAULT_COUNTRIES: Country[] = [
  { code: 'SA', name: 'Saudi Arabia', flag: '🇸🇦' },
  { code: 'SG', name: 'Singapore', flag: '🇸🇬' },
  { code: 'ES', name: 'Spain', flag: '🇪🇸' },
  { code: 'US', name: 'United States', flag: '🇺🇸' },
  { code: 'CN', name: 'China', flag: '🇨🇳' },
  { code: 'DE', name: 'Germany', flag: '🇩🇪' },
  { code: 'IN', name: 'India', flag: '🇮🇳' },
  { code: 'GB', name: 'United Kingdom', flag: '🇬🇧' },
  { code: 'JP', name: 'Japan', flag: '🇯🇵' },
  { code: 'MX', name: 'Mexico', flag: '🇲🇽' },
  { code: 'CA', name: 'Canada', flag: '🇨🇦' },
  { code: 'BR', name: 'Brazil', flag: '🇧🇷' },
  { code: 'FR', name: 'France', flag: '🇫🇷' },
  { code: 'IT', name: 'Italy', flag: '🇮🇹' },
  { code: 'AU', name: 'Australia', flag: '🇦🇺' },
]

type Props = {
  // single-select: value is string | undefined
  // multi-select: value is comma-separated codes OR JSON array string (for backward compatibility)
  value?: string
  onChange?: (codeOrCodes: string | string[]) => void
  countries?: Country[]
  placeholder?: string
  className?: string
  multi?: boolean
}

/**
 * Searchable CountrySelect
 * - type to filter countries
 * - keyboard navigation (arrows + enter)
 * - single-select by default (multi optional)
 */
export const CountrySelect: React.FC<Props> = ({ value, onChange, countries = DEFAULT_COUNTRIES, placeholder = 'Select country', className = '', multi = false }) => {
  const [query, setQuery] = React.useState('')
  const [open, setOpen] = React.useState(false)
  const [highlight, setHighlight] = React.useState(0)
  const ref = React.useRef<HTMLDivElement | null>(null)
  // internal selected codes for multi-mode
  const [selectedCodes, setSelectedCodes] = React.useState<string[]>([])

  // If a value (code) is provided, keep input text in sync
  React.useEffect(() => {
    if (multi) {
      if (!value) {
        setSelectedCodes([])
      } else {
        try {
          // try parse JSON array
          const parsed = JSON.parse(value)
          if (Array.isArray(parsed)) setSelectedCodes(parsed)
          else setSelectedCodes(String(value).split(',').filter(Boolean))
        } catch (e) {
          setSelectedCodes(String(value).split(',').filter(Boolean))
        }
      }
      setQuery('')
    } else {
      if (value) {
        const found = countries.find(c => c.code === value)
        if (found) setQuery(found.name)
      } else {
        setQuery('')
      }
    }
  }, [value, countries, multi])

  // Close on outside click
  React.useEffect(() => {
    function onDoc(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('click', onDoc)
    return () => document.removeEventListener('click', onDoc)
  }, [])

  const normalized = (s: string) => s.trim().toLowerCase()
  const q = normalized(query)

  // Filter and prioritize startsWith matches
  const filtered = countries
    .map(c => ({
      c,
      name: normalized(c.name)
    }))
    .filter(({ name }) => q === '' ? true : name.includes(q))
    .sort((a, b) => {
      const aStarts = a.name.startsWith(q) ? 0 : 1
      const bStarts = b.name.startsWith(q) ? 0 : 1
      if (aStarts !== bStarts) return aStarts - bStarts
      return a.c.name.localeCompare(b.c.name)
    })
    .map(x => x.c)

  const select = (c: Country) => {
    if (multi) {
      setSelectedCodes(prev => {
        if (prev.includes(c.code)) return prev
        const next = [...prev, c.code]
        // emit array to consumer
        onChange?.(next)
        return next
      })
      setQuery('')
      setOpen(true)
    } else {
      onChange?.(c.code)
      setQuery(c.name)
      setOpen(false)
    }
  }

  const removeCode = (code: string) => {
    setSelectedCodes(prev => {
      const next = prev.filter(c => c !== code)
      onChange?.(next)
      return next
    })
  }

  const handleKeyDown: React.KeyboardEventHandler<HTMLInputElement> = (e) => {
    if (!open && (e.key === 'ArrowDown' || e.key === 'ArrowUp')) {
      setOpen(true)
      e.preventDefault()
      return
    }
    if (e.key === 'ArrowDown') {
      setHighlight(h => Math.min(h + 1, Math.max(0, filtered.length - 1)))
      e.preventDefault()
    } else if (e.key === 'ArrowUp') {
      setHighlight(h => Math.max(0, h - 1))
      e.preventDefault()
    } else if (e.key === 'Enter') {
      const sel = filtered[highlight]
      if (sel) select(sel)
      e.preventDefault()
    } else if (e.key === 'Escape') {
      setOpen(false)
    }
  }

  return (
    <div ref={ref} className={`relative ${className}`}>
      <div className="flex flex-col">
        <div className="flex flex-wrap gap-2 items-center">
          {multi && selectedCodes.length > 0 && (
            <div className="flex flex-wrap gap-2">
              {selectedCodes.map(code => {
                const c = countries.find(x => x.code === code)
                return (
                  <button key={code} type="button" onClick={() => removeCode(code)} className="inline-flex items-center gap-2 px-2 py-1 rounded-full bg-muted/20 text-sm">
                    <span>{c?.flag ?? '🏳️'}</span>
                    <span>{c?.name ?? code}</span>
                    <span className="ml-1 text-xs">✕</span>
                  </button>
                )
              })}
            </div>
          )}
          <input
            type="text"
            role="combobox"
            aria-expanded={open}
            aria-autocomplete="list"
            placeholder={placeholder}
            value={query}
            onChange={(e) => { setQuery(e.target.value); setOpen(true); setHighlight(0) }}
            onFocus={() => setOpen(true)}
            onKeyDown={handleKeyDown}
            className="rounded-md border px-3 py-2 bg-white dark:bg-slate-900 w-full"
          />
        </div>
      </div>

      {open && (
        <ul role="listbox" className="absolute z-50 mt-1 max-h-56 w-full overflow-auto rounded-md border bg-white dark:bg-slate-900 shadow-lg">
          {filtered.length === 0 ? (
            <li className="px-3 py-2 text-sm text-muted-foreground">No results</li>
          ) : (
            filtered.map((c, idx) => (
              <li
                key={c.code}
                role="option"
                aria-selected={multi ? selectedCodes.includes(c.code) : value === c.code}
                onMouseDown={(e) => { e.preventDefault(); select(c) }}
                onMouseEnter={() => setHighlight(idx)}
                className={`flex items-center gap-2 px-3 py-2 cursor-pointer text-sm ${idx === highlight ? 'bg-brand-50 dark:bg-brand-800/40' : ''}`}
              >
                <span className="text-lg">{c.flag}</span>
                <span className="flex-1">{c.name}</span>
                <span className="text-xs text-muted-foreground">{c.code}</span>
              </li>
            ))
          )}
        </ul>
      )}
    </div>
  )
}

export default CountrySelect
