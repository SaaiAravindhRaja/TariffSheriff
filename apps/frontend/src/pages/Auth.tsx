import React from 'react'
import { motion } from 'framer-motion'
import { Shield, TrendingUp, Database, Calculator, ArrowRight, Lock, Globe, BarChart3, Mail, KeyRound, Sun, Moon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import BackgroundFX from '@/components/visual/BackgroundFX'
import { useTheme } from '@/contexts/ThemeContext'

interface AuthPageProps {
  onLogin: () => void
}

export function AuthPage({ onLogin }: AuthPageProps) {
  const [isLoggingIn, setIsLoggingIn] = React.useState(false)
  const { theme, toggleTheme } = useTheme()

  const handleLogin = () => {
    setIsLoggingIn(true)
    onLogin()
  }

  const features = [
    {
      icon: Calculator,
      title: "Smart Tariff Calculator",
      description: "Calculate import/export tariffs with real-time data"
    },
    {
      icon: Database,
      title: "Comprehensive Database",
      description: "Access global trade data and tariff information"
    },
    {
      icon: BarChart3,
      title: "Advanced Analytics",
      description: "Visualize trade patterns and trends"
    },
    {
      icon: Globe,
      title: "Multi-Country Support",
      description: "Trade intelligence for 100+ countries"
    }
  ]

  return (
    <div className="relative min-h-screen flex items-center justify-center p-6">
      <BackgroundFX variant="hero" />
      {/* Mini theme toggle (hero only) */}
      <div className="absolute top-4 right-4 z-10">
        <button
          onClick={toggleTheme}
          className="h-9 w-9 inline-flex items-center justify-center rounded-full border border-black/10 bg-white/80 text-gray-800 shadow-sm backdrop-blur dark:border-white/10 dark:bg-gray-900/60 dark:text-white"
          aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}
          title={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}
        >
          {theme === 'dark' ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
        </button>
      </div>
      {/* Loading Overlay */}
      {isLoggingIn && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="fixed inset-0 bg-black/30 dark:bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center"
        >
          <div className="bg-white/85 text-gray-900 border border-black/10 dark:bg-gray-900/70 dark:text-white dark:border-white/10 backdrop-blur-2xl rounded-2xl p-8 shadow-2xl text-center space-y-4">
            <div className="w-16 h-16 border-4 border-brand-500 dark:border-brand-400 border-t-transparent rounded-full animate-spin mx-auto"></div>
            <p className="text-lg font-semibold">Redirecting to secure login...</p>
            <p className="text-sm text-muted-foreground">You'll be back in a moment</p>
          </div>
        </motion.div>
      )}

      <div className="w-full max-w-6xl grid lg:grid-cols-2 gap-8 items-center">
        
        {/* Left Side - Branding & Features */}
        <motion.div
          initial={{ opacity: 0, x: -50 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.6 }}
          className="space-y-8"
        >
          {/* Logo & Title */}
          <div className="space-y-4">
            <motion.div 
              className="flex items-center space-x-3"
              whileHover={{ scale: 1.02 }}
            >
              <div className="w-14 h-14 rounded-xl bg-gradient-to-br from-indigo-600 to-purple-600 flex items-center justify-center shadow-lg shadow-indigo-500/20">
                <Shield className="w-8 h-8 text-white" />
              </div>
              <div>
                <h1 className={`text-5xl font-extrabold tracking-tight bg-clip-text text-transparent ${
                  theme === 'dark'
                    ? 'bg-gradient-to-r from-indigo-300 via-purple-300 to-indigo-400'
                    : 'bg-gradient-to-r from-indigo-800 via-purple-800 to-indigo-900'
                }`}>
                  TariffSheriff
                </h1>
                <p className="text-sm text-muted-foreground">Trade Intelligence Platform</p>
              </div>
            </motion.div>

            <p className="text-lg text-muted-foreground leading-relaxed max-w-xl">
              Simplify your international trade calculations with real-time tariff data,
              comprehensive analytics, and intelligent insights.
            </p>
          </div>

          {/* Features Grid */}
          <div className="grid sm:grid-cols-2 gap-4">
            {features.map((feature, index) => (
              <motion.div
                key={index}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.2 + index * 0.1 }}
                className="p-4 rounded-lg border bg-white/30 border-white/40 ring-1 ring-black/5 backdrop-blur-md hover:border-indigo-300 transition-colors dark:bg-gray-900/50 dark:border-white/10 dark:hover:border-indigo-400/30"
              >
                <feature.icon className="w-8 h-8 text-indigo-600 dark:text-indigo-300 mb-2" />
                <h3 className="font-semibold mb-1">{feature.title}</h3>
                <p className="text-sm text-muted-foreground">{feature.description}</p>
              </motion.div>
            ))}
          </div>

          {/* Trust Indicators */}
          <div className="flex items-center space-x-6 pt-4">
            <div className="flex items-center space-x-2">
              <Lock className="w-5 h-5 text-green-600" />
              <span className="text-sm text-gray-600">Secure Auth0</span>
            </div>
            <div className="flex items-center space-x-2">
              <TrendingUp className="w-5 h-5 text-blue-600" />
              <span className="text-sm text-gray-600">Real-time Data</span>
            </div>
          </div>
        </motion.div>

        {/* Right Side - Login Card */}
        <motion.div
          initial={{ opacity: 0, x: 50 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.6, delay: 0.2 }}
          className="lg:ml-auto w-full max-w-md"
        >
          <div className="bg-white/10 backdrop-blur-2xl rounded-2xl shadow-2xl p-8 space-y-6 border border-white/40 ring-1 ring-black/5 dark:bg-gray-900/60 dark:border-white/10">
            {/* Welcome Text */}
            <div className="text-center space-y-2">
              <h2 className="text-3xl font-bold">Welcome Back</h2>
              <p className="text-muted-foreground">
                Sign in to access your trade intelligence dashboard
              </p>
            </div>

            {/* Login Form Mockup */}
            <div className="space-y-4">
              {/* Email Login - Primary Option */}
              <motion.div whileHover={{ scale: 1.01 }} whileTap={{ scale: 0.99 }}>
                <button
                  onClick={handleLogin}
                  className="w-full h-12 bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 text-white font-semibold rounded-lg shadow-lg shadow-indigo-500/20 transition-all duration-200 flex items-center justify-center space-x-2"
                >
                  <Mail className="w-5 h-5" />
                  <span>Sign in with Auth0</span>
                  <ArrowRight className="w-5 h-5" />
                </button>
              </motion.div>

              {/* Hidden for now - Google/GitHub authentication not yet configured */}
              {/* Uncomment when social auth is properly set up in Auth0 */}
              {/* 
              <div className="flex items-center gap-3 text-sm">
                <div className="h-px flex-1 bg-gray-200 dark:bg-white/10" />
                <span className="text-muted-foreground">or</span>
                <div className="h-px flex-1 bg-gray-200 dark:bg-white/10" />
              </div>

              <div className="space-y-3">
                <motion.div whileHover={{ scale: 1.01 }} whileTap={{ scale: 0.99 }}>
                  <button
                    onClick={handleLogin}
                    className="w-full h-12 flex items-center justify-center space-x-3 border border-gray-300 rounded-lg bg-white/60 hover:border-indigo-400/40 hover:bg-white/80 transition-all group dark:bg-transparent dark:border-white/10 dark:hover:bg-white/5"
                  >
                    <svg className="w-5 h-5" viewBox="0 0 24 24">
                      <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                      <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                      <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                      <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                    </svg>
                    <span className="font-medium group-hover:text-indigo-700 dark:group-hover:text-indigo-300">Continue with Google</span>
                  </button>
                </motion.div>

                <motion.div whileHover={{ scale: 1.01 }} whileTap={{ scale: 0.99 }}>
                  <button
                    onClick={handleLogin}
                    className="w-full h-12 flex items-center justify-center space-x-3 border border-gray-300 rounded-lg bg-white/60 hover:border-purple-400/40 hover:bg-white/80 transition-all group dark:bg-transparent dark:border-white/10 dark:hover:bg-white/5"
                  >
                    <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                      <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z"/>
                    </svg>
                    <span className="font-medium group-hover:text-purple-700 dark:group-hover:text-purple-300">Continue with GitHub</span>
                  </button>
                </motion.div>
              </div>
              */}
            </div>

            {/* Info Text */}
            <p className="text-xs text-center text-muted-foreground">
              By continuing, you agree to our Terms of Service and Privacy Policy. 
              Your data is protected with enterprise-grade security.
            </p>

            {/* Benefits */}
            <div className="space-y-3 pt-4 border-t border-gray-200 dark:border-gray-100/10">
              <p className="text-sm font-semibold">What you'll get:</p>
              <ul className="space-y-2">
                {[
                  "Real-time tariff calculations",
                  "Comprehensive trade database",
                  "Advanced analytics dashboard",
                  "Multi-country insights"
                ].map((benefit, idx) => (
                  <li key={idx} className="flex items-center space-x-2 text-sm text-muted-foreground">
                    <div className="w-1.5 h-1.5 rounded-full bg-indigo-400"></div>
                    <span>{benefit}</span>
                  </li>
                ))}
              </ul>
            </div>
          </div>

          {/* Footer Note */}
          <p className="text-center text-sm text-muted-foreground mt-6">
            Need help? <a href="/contact" className="text-indigo-300 hover:text-indigo-200 font-medium">Contact our team</a>
          </p>
        </motion.div>
      </div>
    </div>
  )
}
