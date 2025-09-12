import React from 'react'
import { motion } from 'framer-motion'
import { FileText } from 'lucide-react'

export function Reports() {
  return (
    <div className="flex-1 space-y-6 p-6">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
      >
        <h1 className="text-3xl font-bold tracking-tight flex items-center gap-3">
          <FileText className="w-8 h-8 text-brand-600" />
          Reports
        </h1>
        <p className="text-muted-foreground">
          Export and documentation
        </p>
      </motion.div>
      
      <div className="flex items-center justify-center h-96 text-muted-foreground">
        Reports page coming soon...
      </div>
    </div>
  )
}