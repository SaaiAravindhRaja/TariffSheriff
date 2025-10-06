import React, { useState } from 'react'
import { Login } from '@/components/auth/Login'
import { Register } from '@/components/auth/Register'

type AuthMode = 'login' | 'register'

export const AuthPage: React.FC = () => {
  const [authMode, setAuthMode] = useState<AuthMode>('login')

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-cyan-50 flex items-center justify-center p-4">
      <div className="w-full max-w-6xl grid lg:grid-cols-2 gap-8 items-center">
        {/* Left side - Branding */}
        <div className="hidden lg:block space-y-6">
          <div className="space-y-4">
            <h1 className="text-6xl font-bold text-gray-900">
              Tariff<span className="text-primary">Sheriff</span>
            </h1>
            <p className="text-xl text-gray-600 leading-relaxed">
              Your comprehensive platform for international trade analysis, 
              tariff calculation, and market intelligence.
            </p>
          </div>
          
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-2 h-2 bg-primary rounded-full"></div>
              <span className="text-gray-700">Real-time tariff calculations</span>
            </div>
            <div className="flex items-center gap-3">
              <div className="w-2 h-2 bg-primary rounded-full"></div>
              <span className="text-gray-700">Comprehensive trade analytics</span>
            </div>
            <div className="flex items-center gap-3">
              <div className="w-2 h-2 bg-primary rounded-full"></div>
              <span className="text-gray-700">Market intelligence dashboard</span>
            </div>
            <div className="flex items-center gap-3">
              <div className="w-2 h-2 bg-primary rounded-full"></div>
              <span className="text-gray-700">Export route optimization</span>
            </div>
          </div>

          <div className="pt-8">
            <div className="bg-white/50 backdrop-blur-sm rounded-lg p-6 border border-gray-200">
              <p className="text-sm text-gray-600 italic">
                "TariffSheriff has revolutionized how we approach international trade. 
                The insights and calculations are invaluable for our business decisions."
              </p>
              <div className="mt-3 flex items-center gap-2">
                <div className="w-8 h-8 bg-primary rounded-full flex items-center justify-center text-white text-sm font-semibold">
                  J
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-900">John Smith</p>
                  <p className="text-xs text-gray-600">Trade Director, Global Corp</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Right side - Auth Forms */}
        <div className="w-full">
          <div className="bg-white rounded-2xl shadow-xl p-8 lg:p-12">
            {authMode === 'login' ? (
              <Login onSwitchToRegister={() => setAuthMode('register')} />
            ) : (
              <Register onSwitchToLogin={() => setAuthMode('login')} />
            )}
          </div>
        </div>
      </div>

      {/* Mobile branding - shown only on small screens */}
      <div className="lg:hidden absolute top-8 left-1/2 transform -translate-x-1/2">
        <h1 className="text-3xl font-bold text-gray-900 text-center">
          Tariff<span className="text-primary">Sheriff</span>
        </h1>
      </div>
    </div>
  )
}