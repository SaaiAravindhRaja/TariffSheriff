import React from 'react'
import { motion } from 'framer-motion'
import { Zap } from 'lucide-react'

export function Simulator() {
  return (
    <div className="flex-1 space-y-6 p-6">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
      >
        <h1 className="text-3xl font-bold tracking-tight flex items-center gap-3">
          <Zap className="w-8 h-8 text-brand-600" />
          Tariff Simulator
        </h1>
        <p className="text-muted-foreground">
          Model policy scenarios and their impact
        </p>
      </motion.div>
      
      <div className="flex items-center justify-center h-96 text-muted-foreground">
        Simulator page coming soon...
      </div>
    </div>
  )
}