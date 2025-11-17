import { Trash2, Loader2 } from 'lucide-react'

type Nullable<T> = T | null | undefined

export interface CartItemCardProps {
  hsCode: string
  importerIso3: string
  originIso3?: Nullable<string>
  hsLabel?: Nullable<string>
  agreementName?: Nullable<string>
  quantity?: number
  unitValue?: number
  totalValue?: number
  mfnRate?: Nullable<number> // decimal (e.g., 0.03)
  prefRate?: Nullable<number> // decimal (e.g., 0.02)
  rvcThreshold?: Nullable<number>
  isLoading?: boolean
  onRemove?: () => void
}

export default function CartItemCard(props: CartItemCardProps) {
  const {
    hsCode,
    importerIso3,
    originIso3,
    hsLabel,
    agreementName,
    quantity = 0,
    unitValue = 0,
    totalValue = 0,
    mfnRate,
    prefRate,
    rvcThreshold,
    isLoading = false,
    onRemove,
  } = props

  const minRate = (prefRate ?? mfnRate) || 0
  const maxRate = (mfnRate ?? prefRate) || 0
  const minTotal = totalValue + totalValue * minRate
  const maxTotal = totalValue + totalValue * maxRate

  return (
    <div className="border rounded-lg p-4 bg-gray-50 dark:bg-gray-800/50">
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <div className="flex items-center gap-2 mb-2">
            <span className="font-mono text-sm font-semibold text-blue-600 dark:text-blue-400">
              {hsCode}
            </span>
            <span className="text-xs text-gray-500">
              {originIso3 || (isLoading ? '…' : '')} → {importerIso3}
            </span>
            {isLoading ? (
              <span className="text-[10px] px-2 py-0.5 rounded-full bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800">
                Finding optimal origin…
              </span>
            ) : (prefRate != null || mfnRate != null) ? (
              <span className="text-[10px] px-2 py-0.5 rounded-full bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300 border border-green-200 dark:border-green-800">
                Optimal origin selected
              </span>
            ) : null}
          </div>

          <p className="text-sm text-gray-700 dark:text-gray-300">
            {isLoading ? (
              <span className="inline-block h-4 w-64 bg-gray-200 dark:bg-gray-700 rounded animate-pulse" />
            ) : (
              hsLabel || 'Product description not available'
            )}
          </p>
          {!isLoading && agreementName && (
            <div className="mt-1 text-xs text-green-600 dark:text-green-400">
              Agreement: {agreementName}
            </div>
          )}

          <div className="mt-2 grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
            <div className="space-y-1">
              <div>
                <span className="text-gray-500">Quantity:</span>{' '}
                {isLoading ? (
                  <span className="inline-block h-3 w-10 bg-gray-200 dark:bg-gray-700 rounded animate-pulse" />
                ) : (
                  quantity
                )}
              </div>
              <div>
                <span className="text-gray-500">Unit Value:</span>{' '}
                {isLoading ? (
                  <span className="inline-block h-3 w-16 bg-gray-200 dark:bg-gray-700 rounded animate-pulse" />
                ) : (
                  `$${unitValue.toFixed(2)}`
                )}
              </div>
            </div>

            <div className="space-y-1">
              <div>
                <span className="text-gray-500">MFN Rate:</span>{' '}
                {isLoading ? (
                  <span className="inline-block h-3 w-12 bg-gray-200 dark:bg-gray-700 rounded animate-pulse" />
                ) : mfnRate != null ? (
                  `${(mfnRate * 100).toFixed(2)}%`
                ) : (
                  '-'
                )}
              </div>
              <div>
                <span className="text-gray-500">Best PREF:</span>{' '}
                {isLoading ? (
                  <span className="inline-block h-3 w-12 bg-gray-200 dark:bg-gray-700 rounded animate-pulse" />
                ) : prefRate != null ? (
                  `${(prefRate * 100).toFixed(2)}%`
                ) : (
                  '-'
                )}
              </div>
              {!isLoading && rvcThreshold != null && (
                <div className="text-xs text-green-600 dark:text-green-400">
                  RVC threshold: {Number(rvcThreshold)}%
                </div>
              )}
            </div>

            <div className="space-y-1">
              <div>
                <span className="text-gray-500">Total Value:</span>{' '}
                {isLoading ? (
                  <span className="inline-block h-3 w-20 bg-gray-200 dark:bg-gray-700 rounded animate-pulse" />
                ) : (
                  `$${totalValue.toFixed(2)}`
                )}
              </div>
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <span className="text-gray-500">Min Total:</span>{' '}
                  {isLoading ? (
                    <span className="inline-block h-3 w-20 bg-gray-200 dark:bg-gray-700 rounded animate-pulse" />
                  ) : (
                    `$${minTotal.toFixed(2)}`
                  )}
                </div>
                <div>
                  <span className="text-gray-500">Max Total:</span>{' '}
                  {isLoading ? (
                    <span className="inline-block h-3 w-20 bg-gray-200 dark:bg-gray-700 rounded animate-pulse" />
                  ) : (
                    `$${maxTotal.toFixed(2)}`
                  )}
                </div>
              </div>
            </div>
          </div>

          {isLoading && (
            <div className="mt-3 h-2 w-full rounded bg-gray-200 dark:bg-gray-700 overflow-hidden">
              <div className="h-2 w-1/3 bg-blue-500/70 animate-pulse rounded" />
            </div>
          )}
        </div>

        {isLoading ? (
          <Loader2 className="w-5 h-5 animate-spin mt-0.5 text-blue-600" />
        ) : onRemove ? (
          <button
            type="button"
            onClick={onRemove}
            className="text-red-500 hover:text-red-700 hover:bg-red-50 dark:hover:bg-red-900/20 rounded p-2"
            aria-label="Remove item"
          >
            <Trash2 className="w-4 h-4" />
          </button>
        ) : null}
      </div>
    </div>
  )
}


