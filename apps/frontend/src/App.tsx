
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { SettingsProvider } from '@/contexts/SettingsContext'
import { AuthProvider } from '@/contexts/AuthContext'
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
import { About } from '@/pages/About'
import { Privacy } from '@/pages/Privacy'
import { Team } from '@/pages/Team'
import { Contact } from '@/pages/Contact'
import { CountryDashboard } from '@/pages/CountryDashboard'
import { Unauthorized } from '@/pages/Unauthorized'
import { VerifyEmail } from '@/pages/VerifyEmail'
import { ProtectedRoute } from '@/components/auth/ProtectedRoute'
import { PublicRoute } from '@/components/auth/PublicRoute'
import { LoginForm } from '@/components/auth/LoginForm'
import { RegisterForm } from '@/components/auth/RegisterForm'
import { ForgotPasswordForm } from '@/components/auth/ForgotPasswordForm'
import { ResetPasswordForm } from '@/components/auth/ResetPasswordForm'
import { EmailVerificationPage } from '@/components/auth/EmailVerificationPage'
import { UserRole } from '@/types/auth'
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

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <SettingsProvider>
        <AuthProvider>
          <Router>
            <Routes>
              {/* Public authentication routes */}
              <Route path="/login" element={
                <PublicRoute>
                  <div className="min-h-screen flex items-center justify-center bg-background">
                    <LoginForm />
                  </div>
                </PublicRoute>
              } />
              <Route path="/register" element={
                <PublicRoute>
                  <div className="min-h-screen flex items-center justify-center bg-background">
                    <RegisterForm />
                  </div>
                </PublicRoute>
              } />
              <Route path="/forgot-password" element={
                <PublicRoute>
                  <div className="min-h-screen flex items-center justify-center bg-background">
                    <ForgotPasswordForm />
                  </div>
                </PublicRoute>
              } />
              <Route path="/reset-password" element={
                <PublicRoute>
                  <div className="min-h-screen flex items-center justify-center bg-background">
                    <ResetPasswordForm />
                  </div>
                </PublicRoute>
              } />
              <Route path="/verify-email" element={
                <PublicRoute redirectIfAuthenticated={false}>
                  <div className="min-h-screen flex items-center justify-center bg-background">
                    <EmailVerificationPage />
                  </div>
                </PublicRoute>
              } />
              <Route path="/unauthorized" element={
                <div className="min-h-screen flex items-center justify-center bg-background">
                  <Unauthorized />
                </div>
              } />

              {/* Public pages accessible to everyone */}
              <Route path="/about" element={
                <PublicRoute redirectIfAuthenticated={false}>
                  <div className="min-h-screen bg-background font-sans antialiased">
                    <a href="#main-content" className="sr-only focus:not-sr-only skip-link">Skip to content</a>
                    <Header />
                    <div className="flex">
                      <Sidebar />
                      <main id="main-content" className="flex-1 ml-64 transition-all duration-300">
                        <About />
                      </main>
                    </div>
                    <Footer />
                  </div>
                </PublicRoute>
              } />
              <Route path="/privacy" element={
                <PublicRoute redirectIfAuthenticated={false}>
                  <div className="min-h-screen bg-background font-sans antialiased">
                    <a href="#main-content" className="sr-only focus:not-sr-only skip-link">Skip to content</a>
                    <Header />
                    <div className="flex">
                      <Sidebar />
                      <main id="main-content" className="flex-1 ml-64 transition-all duration-300">
                        <Privacy />
                      </main>
                    </div>
                    <Footer />
                  </div>
                </PublicRoute>
              } />
              <Route path="/team" element={
                <PublicRoute redirectIfAuthenticated={false}>
                  <div className="min-h-screen bg-background font-sans antialiased">
                    <a href="#main-content" className="sr-only focus:not-sr-only skip-link">Skip to content</a>
                    <Header />
                    <div className="flex">
                      <Sidebar />
                      <main id="main-content" className="flex-1 ml-64 transition-all duration-300">
                        <Team />
                      </main>
                    </div>
                    <Footer />
                  </div>
                </PublicRoute>
              } />
              <Route path="/contact" element={
                <PublicRoute redirectIfAuthenticated={false}>
                  <div className="min-h-screen bg-background font-sans antialiased">
                    <a href="#main-content" className="sr-only focus:not-sr-only skip-link">Skip to content</a>
                    <Header />
                    <div className="flex">
                      <Sidebar />
                      <main id="main-content" className="flex-1 ml-64 transition-all duration-300">
                        <Contact />
                      </main>
                    </div>
                    <Footer />
                  </div>
                </PublicRoute>
              } />

              {/* Protected application routes */}
              <Route path="/*" element={
                <ProtectedRoute>
                  <div className="min-h-screen bg-background font-sans antialiased">
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
                          <Route path="/analytics" element={
                            <ProtectedRoute requiredRoles={[UserRole.ANALYST, UserRole.ADMIN]}>
                              <Analytics />
                            </ProtectedRoute>
                          } />
                          <Route path="/simulator" element={
                            <ProtectedRoute requiredRoles={[UserRole.ANALYST, UserRole.ADMIN]}>
                              <Simulator />
                            </ProtectedRoute>
                          } />
                          <Route path="/reports" element={<Reports />} />
                          <Route path="/profile" element={<Profile />} />
                          <Route path="/settings" element={<Settings />} />
                          <Route path="/country/:countryCode" element={<CountryDashboard />} />
                        </Routes>
                      </main>
                    </div>
                    <Footer />
                  </div>
                </ProtectedRoute>
              } />
            </Routes>
          </Router>
        </AuthProvider>
      </SettingsProvider>
    </QueryClientProvider>
  )
}

export default App