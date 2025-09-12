import React from 'react'
import { motion } from 'framer-motion'
import { Settings as SettingsIcon } from 'lucide-react'

export function Settings() {
  return (
    <div className="flex-1 space-y-6 p-6">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
      >
        <h1 className="text-3xl font-bold tracking-tight flex items-center gap-3">
          <SettingsIcon className="w-8 h-8 text-brand-600" />
          Settings
        </h1>
        <p className="text-muted-foreground">
          Preferences and configuration
        </p>
      </motion.div>
      
      <div className="flex items-center justify-center h-96 text-muted-foreground">
        Settings page coming soon...
      </div>
    </div>
  )
}