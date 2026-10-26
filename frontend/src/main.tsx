import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import { AuthProvider } from './auth'
import ErrorBoundary from './ErrorBoundary'
import './styles.scss'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <ErrorBoundary><App /></ErrorBoundary>
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>,
)
