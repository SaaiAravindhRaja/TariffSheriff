
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Header } from '@/components/layout/Header'
import { Sidebar } from '@/components/layout/Sidebar'
import { Dashboard } from '@/pages/Dashboard'
import { Calculator } from '@/pages/Calculator'
import { Database } from '@/pages/Database'
import { Analytics } from '@/pages/Analytics'
import { Simulator } from '@/pages/Simulator'
import { Routes as TradeRoutes } from '@/pages/Routes'
import { Reports } from '@/pages/Reports'
import { Settings } from '@/pages/Settings'
import Profile from '@/pages/Profile'
import '@/styles/globals.css'

// Create a client
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5, // 5 minutes
      gcTime: 1000 * 60 * 10, // 10 minutes
    },
  },
})

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <Router>
        <div className="min-h-screen bg-background font-sans antialiased">
          {/* Skip link for keyboard users */}
          <a href="#main-content" className="sr-only focus:not-sr-only skip-link">Skip to content</a>
          <Header />
          <div className="flex">
            <Sidebar />
            <main id="main-content" className="flex-1 ml-64 transition-all duration-300">
              <Routes>
                <Route path="/" element={<Dashboard />} />
                <Route path="/calculator" element={<Calculator />} />
                <Route path="/database" element={<Database />} />
                <Route path="/routes" element={<TradeRoutes />} />
                <Route path="/analytics" element={<Analytics />} />
                <Route path="/simulator" element={<Simulator />} />
                <Route path="/reports" element={<Reports />} />
                <Route path="/profile" element={<Profile />} />
                <Route path="/settings" element={<Settings />} />
              </Routes>
            </main>
          </div>
        </div>
      </Router>
    </QueryClientProvider>
  )
}

export default App