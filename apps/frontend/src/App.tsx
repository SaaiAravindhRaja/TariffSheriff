
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { SettingsProvider } from '@/contexts/SettingsContext'
import { AuthProvider, useAuth } from '@/contexts/AuthContext'
import { ProtectedRoute } from '@/components/auth/ProtectedRoute'
import { AuthPage } from '@/pages/AuthPage'
import { Header } from '@/components/layout/Header'
import { Sidebar } from '@/components/layout/Sidebar'
import { Dashboard } from '@/pages/Dashboard'
import { Calculator } from '@/pages/Calculator'
import { Database } from '@/pages/Database'
 
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
  const { isAuthenticated, isLoading } = useAuth()

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

  // Show auth page if not authenticated
  if (!isAuthenticated) {
    return (
      <Routes>
        <Route path="/*" element={<AuthPage />} />
      </Routes>
    )
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
          <ProtectedRoute>
            <Routes>
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/calculator" element={<Calculator />} />
              <Route path="/database" element={<Database />} />
              {/* Simplified navigation: dashboard, calculator, database, settings/profile */}
              <Route path="/profile" element={<Profile />} />
              <Route path="/settings" element={<Settings />} />
              <Route path="/about" element={<About />} />
              <Route path="/privacy" element={<Privacy />} />
              <Route path="/team" element={<Team />} />
              <Route path="/contact" element={<Contact />} />
              
            </Routes>
          </ProtectedRoute>
        </main>
      </div>
      <Footer />
    </div>
  )
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <SettingsProvider>
          <Router>
            <AppContent />
          </Router>
        </SettingsProvider>
      </AuthProvider>
    </QueryClientProvider>
  )
}

export default App
