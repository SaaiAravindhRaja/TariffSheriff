
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { SettingsProvider } from '@/contexts/SettingsContext'
import { useAuth } from '@/hooks/useAuth'
import { Header } from '@/components/layout/Header'
import { Sidebar } from '@/components/layout/Sidebar'
import { Dashboard } from '@/pages/Dashboard'
import { Calculator } from '@/pages/Calculator'
import { Database } from '@/pages/Database'
import { AuthPage } from '@/pages/Auth'
import { AiAssistant } from '@/pages/AiAssistant'
import { Settings } from '@/pages/Settings'
import Profile from '@/pages/Profile'
import { About } from '@/pages/About'
import { Privacy } from '@/pages/Privacy'
import { Team } from '@/pages/Team'
import { Contact } from '@/pages/Contact'
 
import '@/styles/globals.css'
import Footer from '@/components/layout/Footer'

// Create a client
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5, // 5 minutes
      gcTime: 1000 * 60 * 10, // 10 minutes
    },
  },
})

// Main App Content - only rendered when authenticated
function AppContent() {
  const { isAuthenticated, isLoading, login } = useAuth()

  // Show loading screen while checking authentication
  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="text-center">
          <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-primary mx-auto mb-4"></div>
          <p className="text-lg text-gray-600">Loading TariffSheriff...</p>
        </div>
      </div>
    )
  }

  // Show login button if not authenticated
  if (!isAuthenticated) {
    return <AuthPage onLogin={login} />
  }

  // Show main app if authenticated
  return (
    <div className="min-h-screen bg-background font-sans antialiased">
      {/* Skip link for keyboard users */}
      <a href="#main-content" className="sr-only focus:not-sr-only skip-link">Skip to content</a>
      <Header />
      <div className="flex">
        <Sidebar />
        <main id="main-content" className="flex-1 ml-64 transition-all duration-300">
          <Routes>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/calculator" element={<Calculator />} />
            <Route path="/database" element={<Database />} />
            <Route path="/ai-assistant" element={<AiAssistant />} />
            {/* Simplified navigation: dashboard, calculator, database, settings/profile */}
            <Route path="/profile" element={<Profile />} />
            <Route path="/settings" element={<Settings />} />
            <Route path="/about" element={<About />} />
            <Route path="/privacy" element={<Privacy />} />
            <Route path="/team" element={<Team />} />
            <Route path="/contact" element={<Contact />} />
          </Routes>
        </main>
      </div>
      <Footer />
    </div>
  )
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <SettingsProvider>
        <Router>
          <AppContent />
        </Router>
      </SettingsProvider>
    </QueryClientProvider>
  )
}

export default App
