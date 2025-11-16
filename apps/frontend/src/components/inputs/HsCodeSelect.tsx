import React from 'react'

export interface HsCodeOption {
  code: string
  description: string
}

interface HsCodeSelectProps {
  value: string
  onChange: (code: string) => void
  placeholder?: string
  className?: string
  required?: boolean
  disabled?: boolean
  loading?: boolean
  options: HsCodeOption[]
}

const HsCodeSelect: React.FC<HsCodeSelectProps> = ({
  value,
  onChange,
  placeholder = 'Search HS by code or name',
  className = '',
  required = false,
  disabled = false,
  loading = false,
  options,
}) => {
  const [query, setQuery] = React.useState(value)
  const [open, setOpen] = React.useState(false)
  const [highlight, setHighlight] = React.useState(0)
  const containerRef = React.useRef<HTMLDivElement | null>(null)

  React.useEffect(() => {
    setQuery(value)
  }, [value])

  React.useEffect(() => {
    const handleClick = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setOpen(false)
      }
    }
    if (open) {
      document.addEventListener('mousedown', handleClick)
      return () => document.removeEventListener('mousedown', handleClick)
    }
  }, [open])

  const normalizedQuery = query.trim().toLowerCase()
  const filtered = normalizedQuery
    ? options.filter(
        (option) =>
          option.code.toLowerCase().includes(normalizedQuery) ||
          option.description.toLowerCase().includes(normalizedQuery),
      )
    : options

  React.useEffect(() => {
    setHighlight(0)
  }, [normalizedQuery])

  const selectOption = (option: HsCodeOption) => {
    onChange?.(option.code)
    setQuery(option.code)
    setOpen(false)
  }

  const handleKeyDown: React.KeyboardEventHandler<HTMLInputElement> = (event) => {
    if (!open && (event.key === 'ArrowDown' || event.key === 'ArrowUp')) {
      setOpen(true)
      event.preventDefault()
      return
    }
    if (event.key === 'ArrowDown') {
      setHighlight((prev) => Math.min(prev + 1, Math.max(filtered.length - 1, 0)))
      event.preventDefault()
    } else if (event.key === 'ArrowUp') {
      setHighlight((prev) => Math.max(prev - 1, 0))
      event.preventDefault()
    } else if (event.key === 'Enter') {
      const option = filtered[highlight]
      if (option) {
        selectOption(option)
      }
      event.preventDefault()
    } else if (event.key === 'Escape') {
      setOpen(false)
    }
  }

  const showDropdown = open && !disabled

  return (
    <div ref={containerRef} className={`relative ${className}`}>
      <div className="flex flex-col">
        <label className="sr-only">Choose HS code</label>
        <input
          type="text"
          role="combobox"
          aria-expanded={open}
          aria-controls="hs-code-options"
          aria-autocomplete="list"
          placeholder={placeholder}
          value={query}
          disabled={disabled}
          onChange={(event) => {
            setQuery(event.target.value)
            setOpen(true)
            setHighlight(0)
          }}
          onFocus={() => setOpen(true)}
          onKeyDown={handleKeyDown}
          required={required}
          className="rounded-md border px-3 py-2 bg-white dark:bg-slate-900 w-full disabled:opacity-50 disabled:cursor-not-allowed"
        />
      </div>

      {showDropdown && (
        <ul
          id="hs-code-options"
          role="listbox"
          className="absolute z-50 mt-1 max-h-64 w-full overflow-auto rounded-md border bg-white dark:bg-slate-900 shadow-lg"
        >
          {loading ? (
            <li className="px-3 py-2 text-sm text-muted-foreground">Loading…</li>
          ) : filtered.length === 0 ? (
            <li className="px-3 py-2 text-sm text-muted-foreground">No matches</li>
          ) : (
            filtered.map((option, index) => (
              <li
                key={option.code}
                role="option"
                aria-selected={value === option.code}
                onMouseDown={(event) => {
                  event.preventDefault()
                  selectOption(option)
                }}
                onMouseEnter={() => setHighlight(index)}
                className={`px-3 py-2 cursor-pointer text-sm ${
                  index === highlight ? 'bg-brand-50 dark:bg-brand-800/40' : ''
                }`}
              >
                <div className="flex flex-col">
                  <span className="font-medium">{option.code}</span>
                  <span className="text-xs text-muted-foreground line-clamp-2">
                    {option.description}
                  </span>
                </div>
              </li>
            ))
          )}
        </ul>
      )}

      {!loading && !disabled && options.length === 0 && (
        <p className="mt-1 text-xs text-muted-foreground">
          No HS codes available for this route yet.
        </p>
      )}

      {disabled && (
        <p className="mt-1 text-xs text-muted-foreground">
          Select importer and exporter first to load HS codes.
        </p>
      )}
    </div>
  )
}

export default HsCodeSelect
