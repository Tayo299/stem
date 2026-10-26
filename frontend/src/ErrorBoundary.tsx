import { Component } from 'react'
import type { ErrorInfo, ReactNode } from 'react'
import { Link } from 'react-router-dom'

type Props = { children: ReactNode }
type State = { failed: boolean }

export default class ErrorBoundary extends Component<Props, State> {
  state: State = { failed: false }

  static getDerivedStateFromError(): State {
    return { failed: true }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('HSSC interface error', error, info.componentStack)
  }

  render() {
    if (this.state.failed) {
      return <main className="page-message" role="alert"><span className="message-icon">!</span><h2>Something went wrong.</h2><p>This page could not be displayed. Please try again, or return to the HSSC home page.</p><Link className="button button-dark" to="/">Return home</Link></main>
    }
    return this.props.children
  }
}
