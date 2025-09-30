import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react'

// Types for our authentication system
export interface User {
  id: number
  name: string
  email: string
  aboutMe?: string
  role: 'USER' | 'ADMIN'
  admin: boolean
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
      const response = await fetch('http://localhost:8081/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, password }),
      })

      if (!response.ok) {
        const errorData = await response.json()
        throw new Error(errorData.message || 'Login failed')
      }

      const data = await response.json()
      
      // Create user object from response data
      const user = {
        id: data.id,
        name: data.name,
        email: data.email,
        aboutMe: '', // Not returned in login response
        role: data.role,
        admin: data.isAdmin
      }
      
      // Store token and user data
      localStorage.setItem('auth_token', data.token)
      localStorage.setItem('auth_user', JSON.stringify(user))
      
      setToken(data.token)
      setUser(user)
    } catch (error) {
      console.error('Login error:', error)
      throw error
    }
  }

  // Register function
  const register = async (name: string, email: string, password: string, aboutMe?: string): Promise<void> => {
    try {
      const response = await fetch('http://localhost:8081/api/auth/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ name, email, password, aboutMe }),
      })

      if (!response.ok) {
        const errorData = await response.json()
        throw new Error(errorData.message || 'Registration failed')
      }

      const data = await response.json()
      
      // Create user object from response data
      const user = {
        id: data.id,
        name: data.name,
        email: data.email,
        aboutMe: '', // Not returned in register response
        role: data.role,
        admin: data.isAdmin
      }
      
      // Store token and user data
      localStorage.setItem('auth_token', data.token)
      localStorage.setItem('auth_user', JSON.stringify(user))
      
      setToken(data.token)
      setUser(user)
    } catch (error) {
      console.error('Registration error:', error)
      throw error
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