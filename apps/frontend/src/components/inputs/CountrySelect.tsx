import React from 'react'

export type Country = {
  code: string // ISO2 code e.g. 'SG'
  name: string
  flag: string // emoji flag
}

const DEFAULT_COUNTRIES: Country[] = [
  { code: 'SG', name: 'Singapore', flag: '🇸🇬' },
  { code: 'US', name: 'United States', flag: '🇺🇸' },
  { code: 'CN', name: 'China', flag: '🇨🇳' },
  { code: 'DE', name: 'Germany', flag: '🇩🇪' },
  { code: 'IN', name: 'India', flag: '🇮🇳' },
  { code: 'GB', name: 'United Kingdom', flag: '🇬🇧' },
  { code: 'JP', name: 'Japan', flag: '🇯🇵' },
]

type Props = {
  value?: string
  onChange?: (code: string) => void
  countries?: Country[]
  placeholder?: string
  className?: string
}

/**
 * Simple accessible country select that shows emoji flags in the options.
 * Replace your current country dropdown with this or adapt to your UI library.
 */
export function CountrySelect({ value, onChange, countries = DEFAULT_COUNTRIES, placeholder = 'Select country', className }: Props) {
  return (
    <select
      className={`rounded-md border px-3 py-2 bg-white dark:bg-slate-900 ${className ?? ''}`}
      value={value ?? ''}
      onChange={(e) => onChange?.(e.target.value)}
      aria-label="Select country"
    >
      <option value="">{placeholder}</option>
      {countries.map((c) => (
        <option key={c.code} value={c.code}>
          {`${c.flag} ${c.name}`}
        </option>
      ))}
    </select>
  )
}
