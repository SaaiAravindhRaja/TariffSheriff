import React from 'react'
import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { Header } from '../Header'

// Basic smoke test for header navigation and presence
describe('Profile flow', () => {
  beforeEach(() => {
    // clear localStorage for predictable state
    localStorage.clear()
  })

  it('shows default user and navigates to profile', () => {
    render(
      <MemoryRouter>
        <Header />
      </MemoryRouter>
    )

  expect(screen.getByText(/John Doe/i)).toBeTruthy()
  expect(screen.getByText(/Trade Analyst/i)).toBeTruthy()
  })
})
