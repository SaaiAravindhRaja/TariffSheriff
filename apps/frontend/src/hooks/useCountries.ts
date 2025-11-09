import { useCallback, useMemo } from 'react'
import type { Country } from '@/data/countries'
import { useDbCountries } from './useDbCountries'

const toSortedUniqueList = (values: Array<string | undefined | null>) => {
  return Array.from(
    new Set(
      values
        .map((value) => value?.trim())
        .filter((value): value is string => Boolean(value)),
    ),
  ).sort((a, b) => a.localeCompare(b))
}

export const useCountries = () => {
  const { countries: rawCountries, loading, error } = useDbCountries()

  const activeCountries = useMemo(
    () => rawCountries.filter((country) => country.active !== false),
    [rawCountries],
  )

  const regions = useMemo(
    () => toSortedUniqueList(activeCountries.map((country) => country.region)),
    [activeCountries],
  )

  const currencies = useMemo(
    () => toSortedUniqueList(activeCountries.map((country) => country.currency)),
    [activeCountries],
  )

  const countryOptions = useMemo(
    () => activeCountries.map((country) => `${country.name} (${country.code})`),
    [activeCountries],
  )

  const getCountryByCode = useCallback(
    (code: string): Country | undefined =>
      activeCountries.find(
        (country) => country.code.toLowerCase() === code.toLowerCase(),
      ),
    [activeCountries],
  )

  const getCountriesByRegion = useCallback(
    (region: string) => {
      const normalized = region.trim().toLowerCase()
      if (!normalized) return activeCountries
      return activeCountries.filter(
        (country) => country.region?.trim().toLowerCase() === normalized,
      )
    },
    [activeCountries],
  )

  const searchCountries = useCallback(
    (query: string) => {
      const normalized = query.trim().toLowerCase()
      if (!normalized) return activeCountries
      return activeCountries.filter((country) => {
        const codeMatch = country.code.toLowerCase().includes(normalized)
        const nameMatch = country.name.toLowerCase().includes(normalized)
        return codeMatch || nameMatch
      })
    },
    [activeCountries],
  )

  return {
    countries: rawCountries,
    activeCountries,
    countryOptions,
    regions,
    currencies,
    getCountryByCode,
    getCountriesByRegion,
    searchCountries,
    loading,
    error,
  }
}

export default useCountries
