import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export interface CartItem {
  id: string // unique ID for cart item
  hsCode: string
  hsLabel?: string
  originIso3: string
  originName?: string
  importerIso3: string
  importerName?: string
  quantity: number
  unitValue: number // value per unit in USD
  totalValue: number // quantity * unitValue
  
  // Tariff calculation results
  mfnRate?: number
  preferentialRate?: number
  selectedBasis?: 'MFN' | 'PREF'
  selectedRate?: number
  tariffAmount?: number
  totalWithTariff?: number
  
  // Agreement info
  agreementId?: number
  agreementName?: string
  rvcThreshold?: number
  rvcPercentage?: number
}

interface CartStore {
  items: CartItem[]
  
  // Actions
  addItem: (item: Omit<CartItem, 'id' | 'totalValue'>) => void
  updateItem: (id: string, updates: Partial<CartItem>) => void
  removeItem: (id: string) => void
  clearCart: () => void
  
  // Computed values
  getTotalValue: () => number
  getTotalTariff: () => number
  getTotalWithTariff: () => number
  getItemCount: () => number
}

export const useCartStore = create<CartStore>()(
  persist(
    (set, get) => ({
      items: [],

      addItem: (item) => {
        const newItem: CartItem = {
          ...item,
          id: `cart-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
          totalValue: item.quantity * item.unitValue,
        }
        
        set((state) => ({
          items: [...state.items, newItem],
        }))
      },

      updateItem: (id, updates) => {
        set((state) => ({
          items: state.items.map((item) => {
            if (item.id === id) {
              const updated = { ...item, ...updates }
              // Recalculate totalValue if quantity or unitValue changed
              if (updates.quantity !== undefined || updates.unitValue !== undefined) {
                updated.totalValue = updated.quantity * updated.unitValue
              }
              // Recalculate tariffAmount if rate or value changed
              if (updated.selectedRate !== undefined && updated.totalValue !== undefined) {
                updated.tariffAmount = (updated.totalValue * updated.selectedRate) / 100
                updated.totalWithTariff = updated.totalValue + updated.tariffAmount
              }
              return updated
            }
            return item
          }),
        }))
      },

      removeItem: (id) => {
        set((state) => ({
          items: state.items.filter((item) => item.id !== id),
        }))
      },

      clearCart: () => {
        set({ items: [] })
      },

      getTotalValue: () => {
        return get().items.reduce((sum, item) => sum + item.totalValue, 0)
      },

      getTotalTariff: () => {
        return get().items.reduce((sum, item) => sum + (item.tariffAmount || 0), 0)
      },

      getTotalWithTariff: () => {
        return get().items.reduce((sum, item) => sum + (item.totalWithTariff || item.totalValue), 0)
      },

      getItemCount: () => {
        return get().items.length
      },
    }),
    {
      name: 'tariff-cart-storage',
    }
  )
)
