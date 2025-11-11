import React, { createContext, useContext, useState, useCallback } from 'react'
import { CheckCircle, XCircle, AlertCircle, X } from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'

type ToastType = 'success' | 'error' | 'info'

interface Toast {
  id: string
  message: string
  type: ToastType
}

interface ToastContextType {
  showToast: (message: string, type?: ToastType) => void
}

const ToastContext = createContext<ToastContextType | undefined>(undefined)

export const useToast = () => {
  const context = useContext(ToastContext)
  if (!context) {
    throw new Error('useToast must be used within ToastProvider')
  }
  return context
}

export const ToastProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [toasts, setToasts] = useState<Toast[]>([])

  const showToast = useCallback((message: string, type: ToastType = 'success') => {
    const id = Math.random().toString(36).substring(7)
    setToasts(prev => [...prev, { id, message, type }])

    // Auto remove after 3 seconds
    setTimeout(() => {
      setToasts(prev => prev.filter(toast => toast.id !== id))
    }, 3000)
  }, [])

  const removeToast = (id: string) => {
    setToasts(prev => prev.filter(toast => toast.id !== id))
  }

  const getIcon = (type: ToastType) => {
    switch (type) {
      case 'success':
        return <CheckCircle className="w-5 h-5" />
      case 'error':
        return <XCircle className="w-5 h-5" />
      case 'info':
        return <AlertCircle className="w-5 h-5" />
    }
  }

  const getBackground = (type: ToastType) => {
    switch (type) {
      case 'success':
        return 'bg-white dark:bg-gray-800 border-green-500 dark:border-green-600'
      case 'error':
        return 'bg-white dark:bg-gray-800 border-red-500 dark:border-red-600'
      case 'info':
        return 'bg-white dark:bg-gray-800 border-blue-500 dark:border-blue-600'
    }
  }

  const getTextColor = (type: ToastType) => {
    switch (type) {
      case 'success':
        return 'text-green-700 dark:text-green-400'
      case 'error':
        return 'text-red-700 dark:text-red-400'
      case 'info':
        return 'text-blue-700 dark:text-blue-400'
    }
  }

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <div className="fixed top-20 right-4 z-50 flex flex-col gap-2 pointer-events-none">
        <AnimatePresence>
          {toasts.map(toast => (
            <motion.div
              key={toast.id}
              initial={{ opacity: 0, y: -20, x: 100 }}
              animate={{ opacity: 1, y: 0, x: 0 }}
              exit={{ opacity: 0, x: 100 }}
              transition={{ duration: 0.3 }}
              className={`flex items-center gap-3 px-4 py-3 rounded-lg border-2 shadow-lg pointer-events-auto ${getBackground(toast.type)}`}
            >
              <div className={getTextColor(toast.type)}>
                {getIcon(toast.type)}
              </div>
              <span className={`text-sm font-medium ${getTextColor(toast.type)}`}>
                {toast.message}
              </span>
              <button
                onClick={() => removeToast(toast.id)}
                className="ml-2 text-gray-400 hover:text-gray-600 dark:text-gray-500 dark:hover:text-gray-300 transition-colors"
              >
                <X className="w-4 h-4" />
              </button>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
    </ToastContext.Provider>
  )
}
