import { describe, it, expect, beforeEach } from 'vitest'

// Minimal sanity test to keep CI healthy. Component tests are covered elsewhere.
describe.skip('sanity', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('true is truthy', () => {
    expect(true).toBeTruthy()
  })
})
