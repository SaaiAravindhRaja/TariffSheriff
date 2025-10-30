import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react'
import { authApi } from '@/services/api'

// Types for our authentication system
export interface User {
  id: number
  name: string
  email: string
  aboutMe?: string
  role: 'USER' | 'ADMIN'
  isAdmin: boolean
}

export interface AuthContextType {
  user: User | null
  token: string | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (name: string, email: string, password: string, aboutMe?: string) => Promise<void>
  logout: () => void
}

// Create the context
const AuthContext = createContext<AuthContextType | null>(null)

// Custom hook to use auth context
export const useAuth = () => {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}

// Auth provider component
interface AuthProviderProps {
  children: ReactNode
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null)
  const [token, setToken] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const isAuthenticated = !!user && !!token

  // Check for existing token on app start
  useEffect(() => {
    const storedToken = localStorage.getItem('auth_token')
    const storedUser = localStorage.getItem('auth_user')
    
    if (storedToken && storedUser) {
      try {
        setToken(storedToken)
        setUser(JSON.parse(storedUser))
      } catch (error) {
        // Clear invalid stored data
        localStorage.removeItem('auth_token')
        localStorage.removeItem('auth_user')
      }
    }
    
    setIsLoading(false)
  }, [])

  // Login function
  const login = async (email: string, password: string): Promise<void> => {
    try {
      const { data } = await authApi.login({ email, password })

      const nextUser: User = {
        id: data.id,
        name: data.name,
        email: data.email,
        aboutMe: '',
        role: data.role,
        isAdmin: data.isAdmin,
      }

      localStorage.setItem('auth_token', data.token)
      localStorage.setItem('auth_user', JSON.stringify(nextUser))

      setToken(data.token)
      setUser(nextUser)
    } catch (err: any) {
      const message = err?.response?.data?.message || err?.message || 'Login failed'
      console.error('Login error:', message)
      throw new Error(message)
    }
  }

  // Register function
  const register = async (name: string, email: string, password: string, aboutMe?: string): Promise<void> => {
    try {
      const { data } = await authApi.register({ name, email, password, aboutMe })

      const nextUser: User = {
        id: data.id,
        name: data.name,
        email: data.email,
        aboutMe: '',
        role: data.role,
        isAdmin: data.isAdmin,
      }

      localStorage.setItem('auth_token', data.token)
      localStorage.setItem('auth_user', JSON.stringify(nextUser))

      setToken(data.token)
      setUser(nextUser)
    } catch (err: any) {
      const message = err?.response?.data?.message || err?.message || 'Registration failed'
      console.error('Registration error:', message)
      throw new Error(message)
    }
  }

  // Logout function
  const logout = () => {
    localStorage.removeItem('auth_token')
    localStorage.removeItem('auth_user')
    setToken(null)
    setUser(null)
  }

  const value: AuthContextType = {
    user,
    token,
    isAuthenticated,
    isLoading,
    login,
    register,
    logout,
  }

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}
