import { useEffect, useState } from 'react'
import { tariffApi } from '@/services/api'
import type { Country as CountryDef } from '@/data/countries'

export type DbCountry = { iso2: string; iso3: string; name: string }

export function useDbCountries() {
  const [countries, setCountries] = useState<CountryDef[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    async function load() {
      try {
        const res = await tariffApi.getCountries({ page: 0, size: 500 })
        const list: DbCountry[] = Array.isArray(res.data) ? res.data : (Array.isArray(res.data?.content) ? res.data.content : [])
        if (!cancelled) {
          const mapped: CountryDef[] = list.map((c) => ({
            code: c.iso2,
            name: c.name,
            emoji: '',
            region: '',
            currency: '',
            active: true,
          }))
          setCountries(mapped)
        }
      } catch (e: any) {
        console.error('Failed to load countries:', e?.response?.status, e?.response?.data || e?.message)
        if (!cancelled) {
          const status = e?.response?.status
          const msg = e?.response?.data?.message || e?.message || 'Unknown error'
          setError(`Failed to load countries${status ? ` (${status})` : ''}: ${msg}`)
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => { cancelled = true }
  }, [])

  return { countries, loading, error }
}

export default useDbCountries


