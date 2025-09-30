import React from 'react'
import { Country as CountryDef } from '@/data/countries'

interface CountrySelectProps {
  value?: string
  onChange?: (codeOrCodes: string | string[]) => void
  countries?: CountryDef[]
  placeholder?: string
  className?: string
  multi?: boolean
  disabled?: boolean
  required?: boolean
  loading?: boolean
  error?: string | null
  'aria-label'?: string
  'aria-describedby'?: string
}

const CountrySelect: React.FC<CountrySelectProps> = ({
  value,
  onChange,
  countries = [],
  placeholder = 'Select country',
  className = '',
  multi = false,
  disabled = false,
  required = false,
  loading = false,
  error = null,
  'aria-label': ariaLabel,
  'aria-describedby': ariaDescribedBy
}) => {
  const [query, setQuery] = React.useState('')
  const [open, setOpen] = React.useState(false)
  const [highlight, setHighlight] = React.useState(0)
  const ref = React.useRef<HTMLDivElement | null>(null)
  const inputId = 'country-select-input'
  const listboxId = 'country-select-listbox'

  const [selectedCodes, setSelectedCodes] = React.useState<string[]>([])
  const allCountries = countries && countries.length ? countries : []

  React.useEffect(() => {
    if (multi) {
      if (!value) setSelectedCodes([])
      else {
        try {
          const parsed = JSON.parse(value)
          if (Array.isArray(parsed)) setSelectedCodes(parsed)
          else setSelectedCodes(String(value).split(',').filter(Boolean))
        } catch {
          setSelectedCodes(String(value).split(',').filter(Boolean))
        }
      }
      setQuery('')
    } else {
      if (value) {
        const found = allCountries.find((c) => c.code === value)
        if (found) setQuery(found.name)
      } else setQuery('')
    }
  }, [value, countries, multi])

  React.useEffect(() => {
    function onDoc(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('click', onDoc)
    return () => document.removeEventListener('click', onDoc)
  }, [])

  const normalized = (s: string) => s.trim().toLowerCase()
  const q = normalized(query)

  const filtered = allCountries
    .map((c) => ({ c, name: normalized(c.name) }))
    .filter(({ name }) => (q === '' ? true : name.includes(q)))
    .sort((a, b) => {
      const aStarts = a.name.startsWith(q) ? 0 : 1
      const bStarts = b.name.startsWith(q) ? 0 : 1
      if (aStarts !== bStarts) return aStarts - bStarts
      return a.c.name.localeCompare(b.c.name)
    })
    .map((x) => x.c)

  const select = (c: CountryDef) => {
    if (multi) {
      setSelectedCodes((prev) => {
        if (prev.includes(c.code)) return prev
        const next = [...prev, c.code]
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
    setSelectedCodes((prev) => {
      const next = prev.filter((c) => c !== code)
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
      setHighlight((h) => Math.min(h + 1, Math.max(0, filtered.length - 1)))
      e.preventDefault()
    } else if (e.key === 'ArrowUp') {
      setHighlight((h) => Math.max(0, h - 1))
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
        <label id="country-select-label" className="sr-only">Choose country</label>
        <div className="flex flex-wrap gap-2 items-center">
          {multi && selectedCodes.length > 0 && (
            <div className="flex flex-wrap gap-2">
              {selectedCodes.map((code) => {
                const c = allCountries.find((x) => x.code === code)
                return (
                  <button key={code} type="button" onClick={() => removeCode(code)} aria-label={`Remove ${c?.name ?? code}`} className="inline-flex items-center gap-2 px-2 py-1 rounded-full bg-brand-50 dark:bg-brand-800/40 text-sm border border-brand-100 dark:border-brand-700">
                    <span className="text-lg">{c?.emoji ?? '🏳️'}</span>
                    <span className="font-medium text-sm">{c?.name ?? code}</span>
                    <span className="ml-2 text-xs text-muted-foreground">✕</span>
                  </button>
                )
              })}
            </div>
          )}
          <input
            id={inputId}
            aria-labelledby="country-select-label"
            aria-controls={listboxId}
            type="text"
            role="combobox"
            aria-expanded={open}
            aria-autocomplete="list"
            aria-label={ariaLabel || `${multi ? 'Select countries' : 'Select country'}`}
            aria-describedby={ariaDescribedBy}
            aria-required={required}
            placeholder={placeholder}
            value={query}
            disabled={disabled || loading}
            onChange={(e) => { setQuery(e.target.value); setOpen(true); setHighlight(0) }}
            onFocus={() => setOpen(true)}
            onKeyDown={handleKeyDown}
            className="rounded-md border px-3 py-2 bg-white dark:bg-slate-900 w-full disabled:opacity-50 disabled:cursor-not-allowed"
          />
        </div>
      </div>

      {open && (
        <ul id={listboxId} role="listbox" aria-labelledby="country-select-label" className="absolute z-50 mt-1 max-h-56 w-full overflow-auto rounded-md border bg-white dark:bg-slate-900 shadow-lg">
          {loading ? (
            <li className="px-3 py-2 text-sm text-muted-foreground">Loading...</li>
          ) : error ? (
            <li className="px-3 py-2 text-sm text-red-600">{error}</li>
          ) : filtered.length === 0 ? (
            <li className="px-3 py-2 text-sm text-muted-foreground">No results</li>
          ) : (
            filtered.map((c, idx) => (
              <li
                key={c.code}
                role="option"
                id={`country-option-${c.code}`}
                aria-selected={multi ? selectedCodes.includes(c.code) : value === c.code}
                onMouseDown={(e) => { e.preventDefault(); select(c) }}
                onMouseEnter={() => setHighlight(idx)}
                className={`flex items-center gap-2 px-3 py-2 cursor-pointer text-sm ${idx === highlight ? 'bg-brand-50 dark:bg-brand-800/40' : ''}`}
              >
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
