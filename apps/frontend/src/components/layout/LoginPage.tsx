import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import safeLocalStorage from '@/lib/safeLocalStorage'

export default function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const navigate = useNavigate()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      })
      const data = await res.json()
      if (res.ok) {
        safeLocalStorage.set('token', data.token)
        safeLocalStorage.set('app_profile', JSON.stringify({ name: data.username, role: data.roles }))
        window.dispatchEvent(new Event('profile:updated'))
        navigate('/') // redirect after login
      } else {
        alert(data.message || 'Login failed')
      }
    } catch (err) {
      console.error(err)
    }
  }

  return (
    <div className="p-8 max-w-md mx-auto">
      <h1 className="text-2xl font-bold mb-4">Login</h1>
      <form onSubmit={handleSubmit} className="space-y-2">
        <input
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="block w-full p-2 border rounded"
          required
        />
        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          className="block w-full p-2 border rounded"
          required
        />
        <button type="submit" className="w-full p-2 bg-blue-600 text-white rounded">
          Sign In
        </button>
      </form>
    </div>
  )
}
