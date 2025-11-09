import { type ClassValue, clsx } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

import { appConfig } from "@/config/app"

export function formatCurrency(
  amount: number,
  currency: string = appConfig.i18n.defaultCurrency,
  locale: string = appConfig.i18n.defaultLocale
): string {
  return new Intl.NumberFormat(locale, {
    style: "currency",
    currency,
  }).format(amount)
}

export function formatPercentage(value: number, decimals: number = 2): string {
  return `${(value * 100).toFixed(decimals)}%`
}

export function formatDate(
  date: Date | string, 
  format: string = "short",
  locale: string = appConfig.i18n.defaultLocale
): string {
  const dateObj = typeof date === "string" ? new Date(date) : date

  const shortOptions: Intl.DateTimeFormatOptions = { month: "short", day: "numeric", year: "numeric" }
  const longOptions: Intl.DateTimeFormatOptions = { weekday: "long", year: "numeric", month: "long", day: "numeric" }
  const timeOptions: Intl.DateTimeFormatOptions = { hour: "2-digit", minute: "2-digit", hour12: true }

  let options: Intl.DateTimeFormatOptions
  switch (format) {
    case "long":
      options = longOptions
      break
    case "time":
      options = timeOptions
      break
    case "short":
    default:
      options = shortOptions
  }

  // If only a time format is requested, use toLocaleTimeString
  if (format === "time") {
    return dateObj.toLocaleTimeString(locale, options)
  }

  return dateObj.toLocaleDateString(locale, options)
}

export function debounce<T extends (...args: any[]) => any>(
  func: T,
  wait: number
): (...args: Parameters<T>) => void {
  let timeout = 0 as number
  return (...args: Parameters<T>) => {
    clearTimeout(timeout)
    timeout = window.setTimeout(() => func(...args), wait)
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
  if (!countryCode) {
    return '🏳️'
  }

  const trimmed = countryCode.trim().toUpperCase()
  if (trimmed.length < 2) {
    return '🏳️'
  }

  const normalized = trimmed.length === 2 ? trimmed : trimmed.slice(0, 2)
  const codePoints = normalized
    .split('')
    .map((char) => 127397 + char.charCodeAt(0))

  return String.fromCodePoint(...codePoints)
}
