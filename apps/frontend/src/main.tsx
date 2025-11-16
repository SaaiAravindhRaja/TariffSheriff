import React from 'react'
import ReactDOM from 'react-dom/client'
import { Auth0Provider } from '@auth0/auth0-react'
import App from './App'

// Set initial theme before React mounts to avoid FOUC
const rootElement = document.documentElement
try {
  const stored = localStorage.getItem('theme')
  const initial = stored === 'light' || stored === 'dark' ? stored : 'light' // default to light now
  rootElement.classList.remove('light', 'dark')
  rootElement.classList.add(initial)
} catch {
  rootElement.classList.add('light')
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <Auth0Provider
      domain={import.meta.env.VITE_AUTH0_DOMAIN}
      clientId={import.meta.env.VITE_AUTH0_CLIENT_ID}
      authorizationParams={{
        redirect_uri: import.meta.env.VITE_AUTH0_REDIRECT_URI,
        audience: import.meta.env.VITE_AUTH0_AUDIENCE,
        scope: "openid profile email write:tariff_calculations",
      }}
      useRefreshTokens={true}
      cacheLocation="localstorage"
    >
      <App />
    </Auth0Provider>
  </React.StrictMode>,
)
