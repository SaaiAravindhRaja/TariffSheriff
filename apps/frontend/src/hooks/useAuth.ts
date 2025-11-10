import { useAuth0 } from '@auth0/auth0-react'
import { useEffect } from 'react'
import { setAuth0TokenGetter } from '../services/api'

export const useAuth = () => {
  const domain = import.meta.env.VITE_AUTH0_DOMAIN
  const clientId = import.meta.env.VITE_AUTH0_CLIENT_ID
  const audience = import.meta.env.VITE_AUTH0_AUDIENCE
  const redirectUri = import.meta.env.VITE_AUTH0_REDIRECT_URI
  const authDisabled = String(import.meta.env.VITE_AUTH_DISABLED || '').toLowerCase() === 'true'

  // Dev fallback: if Auth0 not configured or explicitly disabled, treat as authenticated
  const missingConfig = !domain || !clientId || !audience || !redirectUri
  if (authDisabled || missingConfig) {
    return {
      isAuthenticated: true,
      isLoading: false,
      user: null as any,
      login: () => {},
      logout: () => {},
    }
  }

  const {
    isAuthenticated,
    isLoading,
    user,
    loginWithRedirect,
    logout,
    getAccessTokenSilently,
  } = useAuth0()

  // Set up the token getter for axios interceptors
  useEffect(() => {
    if (isAuthenticated) {
      setAuth0TokenGetter(getAccessTokenSilently)
    }
  }, [isAuthenticated, getAccessTokenSilently])

  return {
    isAuthenticated,
    isLoading,
    user,
    login: () => loginWithRedirect(),
    logout: () => logout({ logoutParams: { returnTo: window.location.origin } }),
  }
}
