import { useAuth0 } from '@auth0/auth0-react'
import { useEffect } from 'react'
import { setAuth0TokenGetter } from '../services/api'

export const useAuth = () => {
  const { 
    isAuthenticated, 
    isLoading, 
    user, 
    loginWithRedirect, 
    logout, 
    getAccessTokenSilently 
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
