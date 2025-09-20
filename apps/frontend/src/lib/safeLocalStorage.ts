// Lightweight, safe localStorage wrapper
// - SSR-safe (no window access unless available)
// - JSON parse/stringify safety
// - In-memory fallback when localStorage is unavailable or throws
// - Optional TTL support (ms)

export type StoredValue<T> = { v: T; exp?: number }

const inMemoryStore = new Map<string, string>()

function isBrowser(): boolean {
  return typeof window !== 'undefined' && !!window.localStorage
}

function nowMs(): number {
  return Date.now()
}

function _readRaw(key: string): string | null {
  try {
    if (isBrowser()) return window.localStorage.getItem(key)
    return inMemoryStore.get(key) ?? null
  } catch {
    return inMemoryStore.get(key) ?? null
  }
}

function _writeRaw(key: string, value: string) {
  try {
    if (isBrowser()) {
      window.localStorage.setItem(key, value)
      return
    }
    inMemoryStore.set(key, value)
  } catch {
    inMemoryStore.set(key, value)
  }
}

export const safeLocalStorage = {
  get<T = unknown>(key: string, fallback?: T): T | undefined {
    const raw = _readRaw(key)
    if (!raw) return fallback
    try {
      const parsed = JSON.parse(raw) as StoredValue<T> | T
      // If wrapped format
      if (parsed && typeof parsed === 'object' && 'v' in (parsed as any)) {
        const wrapped = parsed as StoredValue<T>
        if (wrapped.exp && wrapped.exp <= nowMs()) {
          this.remove(key)
          return fallback
        }
        return wrapped.v as T
      }
      // Older/raw format
      return parsed as T
    } catch {
      // corrupted -> remove and return fallback
      this.remove(key)
      return fallback
    }
  },

  set<T = unknown>(key: string, value: T, opts?: { ttlMs?: number }) {
    const payload: StoredValue<T> = { v: value }
    if (opts?.ttlMs) payload.exp = nowMs() + opts.ttlMs
    const str = JSON.stringify(payload)
    _writeRaw(key, str)
  },

  remove(key: string) {
    try {
      if (isBrowser()) {
        window.localStorage.removeItem(key)
        return
      }
      inMemoryStore.delete(key)
    } catch {
      inMemoryStore.delete(key)
    }
  },

  clearPrefix(prefix: string) {
    try {
      if (isBrowser()) {
        for (let i = window.localStorage.length - 1; i >= 0; i--) {
          const k = window.localStorage.key(i)
          if (k && k.startsWith(prefix)) window.localStorage.removeItem(k)
        }
        return
      }
      for (const k of Array.from(inMemoryStore.keys())) {
        if (k.startsWith(prefix)) inMemoryStore.delete(k)
      }
    } catch {
      for (const k of Array.from(inMemoryStore.keys())) {
        if (k.startsWith(prefix)) inMemoryStore.delete(k)
      }
    }
  }
}

export default safeLocalStorage
