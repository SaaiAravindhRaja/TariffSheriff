import React from 'react'
import { motion } from 'framer-motion'
import { Globe } from 'lucide-react'

export function Routes() {
  return (
    <div className="flex-1 space-y-6 p-6">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
      >
        <h1 className="text-3xl font-bold tracking-tight flex items-center gap-3">
          <Globe className="w-8 h-8 text-brand-600" />
          Trade Routes
        </h1>
        <p className="text-muted-foreground">
          Optimize trade paths and routes
        </p>
      </motion.div>
      
      <div className="flex items-center justify-center h-96 text-muted-foreground">
        Trade routes page coming soon...
      </div>
    </div>
  )
}