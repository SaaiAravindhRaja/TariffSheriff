import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'

const rootElement = document.documentElement
rootElement.classList.remove('dark')
rootElement.classList.add('light')

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
