import { type ClassValue, clsx } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatCurrency(
  amount: number,
  currency: string = "USD",
  locale: string = "en-US"
): string {
  return new Intl.NumberFormat(locale, {
    style: "currency",
    currency,
  }).format(amount)
}

export function formatPercentage(value: number, decimals: number = 2): string {
  return `${(value * 100).toFixed(decimals)}%`
}

export function formatDate(date: Date | string, format: string = "short"): string {
  const dateObj = typeof date === "string" ? new Date(date) : date
  
  const options: Intl.DateTimeFormatOptions = {
    short: { month: "short", day: "numeric", year: "numeric" },
    long: { 
      weekday: "long", 
      year: "numeric", 
      month: "long", 
      day: "numeric" 
    },
    time: { 
      hour: "2-digit", 
      minute: "2-digit", 
      hour12: true 
    }
  }[format] || { month: "short", day: "numeric", year: "numeric" }
  
  return dateObj.toLocaleDateString("en-US", options)
}

export function debounce<T extends (...args: any[]) => any>(
  func: T,
  wait: number
): (...args: Parameters<T>) => void {
  let timeout: NodeJS.Timeout
  return (...args: Parameters<T>) => {
    clearTimeout(timeout)
    timeout = setTimeout(() => func(...args), wait)
  }
}

export function generateId(): string {
  return Math.random().toString(36).substr(2, 9)
}

export function calculateTariffTotal(
  baseValue: number,
  tariffRate: number,
  additionalFees: number = 0
): number {
  return baseValue * (1 + tariffRate) + additionalFees
}

export function getCountryFlag(countryCode: string): string {
  const codePoints = countryCode
    .toUpperCase()
    .split('')
    .map(char => 127397 + char.charCodeAt(0))
  return String.fromCodePoint(...codePoints)
}