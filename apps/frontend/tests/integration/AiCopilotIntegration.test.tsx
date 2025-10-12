import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import AiAssistantPage from '../../src/pages/AiAssistantPage'
import { AuthProvider } from '../../src/contexts/AuthContext'
import { SettingsProvider } from '../../src/contexts/SettingsContext'

// Mock API responses
const mockApiResponses = {
  complexQuery: {
    response: "Based on my analysis, importing electric vehicles from Germany would cost approximately $45,000 per unit including tariffs (2.5%), shipping ($2,000), and compliance costs ($500). From Japan, the total would be $43,500 per unit with similar tariff rates but lower shipping costs ($1,500).",
    metadata: {
      toolsUsed: ['TariffLookupTool', 'ComplianceAnalysisTool', 'MarketIntelligenceTool'],
      executionTime: 8500,
      confidence: 0.92
    }
  },
  contextualQuery: {
    response: "For the Germany option we discussed, you'll need: 1) EPA compliance certificate, 2) DOT safety certification, 3) Customs Form 3461, 4) Commercial invoice, 5) Bill of lading. The compliance process typically takes 4-6 weeks.",
    metadata: {
      contextResolved: true,
      toolsUsed: ['ComplianceAnalysisTool'],
      executionTime: 3200
    }
  },
  riskAssessment: {
    response: "Risk assessment for importing semiconductors from Taiwan shows: Political risk: MEDIUM (cross-strait tensions), Economic risk: LOW (stable economy), Supply chain risk: HIGH (concentration risk), Overall risk score: 6.5/10",
    metadata: {
      riskScore: 6.5,
      riskFactors: ['political_tension', 'supply_concentration', 'geopolitical'],
      toolsUsed: ['RiskAssessmentTool']
    }
  }
}

// Mock fetch
global.fetch = vi.fn()

const renderWithProviders = (component: React.ReactElement) => {
  return render(
    <BrowserRouter>
      <AuthProvider>
        <SettingsProvider>
          {component}
        </SettingsProvider>
      </AuthProvider>
    </BrowserRouter>
  )
}

describe('AI Copilot Integration Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Mock successful authentication
    localStorage.setItem('token', 'mock-token')
    localStorage.setItem('user', JSON.stringify({ id: 'test-user', email: 'test@example.com' }))
  })

  afterEach(() => {
    localStorage.clear()
  })

  it('should handle complex multi-tool queries with orchestration', async () => {
    // Mock API response for complex query
    ;(global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => mockApiResponses.complexQuery
    })

    renderWithProviders(<AiAssistantPage />)

    // Wait for component to load
    await waitFor(() => {
      expect(screen.getByPlaceholderText(/ask about trade/i)).toBeInTheDocument()
    })

    const input = screen.getByPlaceholderText(/ask about trade/i)
    const sendButton = screen.getByRole('button', { name: /send/i })

    // Type complex query
    fireEvent.change(input, {
      target: { value: 'Compare the total cost of importing 10,000 units of electric vehicles from Germany vs Japan to the US' }
    })

    fireEvent.click(sendButton)

    // Wait for response
    await waitFor(() => {
      expect(screen.getByText(/Based on my analysis/)).toBeInTheDocument()
    }, { timeout: 10000 })

    // Verify response content
    expect(screen.getByText(/Germany would cost approximately/)).toBeInTheDocument()
    expect(screen.getByText(/Japan.*\$43,500/)).toBeInTheDocument()

    // Verify metadata display
    expect(screen.getByText(/Tools used:/)).toBeInTheDocument()
    expect(screen.getByText(/TariffLookupTool/)).toBeInTheDocument()
    expect(screen.getByText(/ComplianceAnalysisTool/)).toBeInTheDocument()
  })

  it('should maintain context across follow-up queries', async () => {
    // First query
    ;(global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => mockApiResponses.complexQuery
    })

    // Follow-up query
    ;(global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => mockApiResponses.contextualQuery
    })

    renderWithProviders(<AiAssistantPage />)

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/ask about trade/i)).toBeInTheDocument()
    })

    const input = screen.getByPlaceholderText(/ask about trade/i)
    const sendButton = screen.getByRole('button', { name: /send/i })

    // First query
    fireEvent.change(input, {
      target: { value: 'Compare importing electric vehicles from Germany vs Japan' }
    })
    fireEvent.click(sendButton)

    await waitFor(() => {
      expect(screen.getByText(/Based on my analysis/)).toBeInTheDocument()
    })

    // Clear input and ask follow-up
    fireEvent.change(input, {
      target: { value: 'What about the compliance requirements for the Germany option?' }
    })
    fireEvent.click(sendButton)

    await waitFor(() => {
      expect(screen.getByText(/For the Germany option we discussed/)).toBeInTheDocument()
    })

    // Verify contextual understanding
    expect(screen.getByText(/EPA compliance certificate/)).toBeInTheDocument()
    expect(screen.getByText(/4-6 weeks/)).toBeInTheDocument()
  })

  it('should display risk assessment with visual indicators', async () => {
    ;(global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => mockApiResponses.riskAssessment
    })

    renderWithProviders(<AiAssistantPage />)

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/ask about trade/i)).toBeInTheDocument()
    })

    const input = screen.getByPlaceholderText(/ask about trade/i)
    const sendButton = screen.getByRole('button', { name: /send/i })

    fireEvent.change(input, {
      target: { value: 'Assess the risks of importing semiconductors from Taiwan' }
    })
    fireEvent.click(sendButton)

    await waitFor(() => {
      expect(screen.getByText(/Risk assessment for importing semiconductors/)).toBeInTheDocument()
    })

    // Verify risk indicators
    expect(screen.getByText(/Political risk: MEDIUM/)).toBeInTheDocument()
    expect(screen.getByText(/Economic risk: LOW/)).toBeInTheDocument()
    expect(screen.getByText(/Supply chain risk: HIGH/)).toBeInTheDocument()
    expect(screen.getByText(/Overall risk score: 6.5\/10/)).toBeInTheDocument()
  })

  it('should handle visualization rendering for complex data', async () => {
    const visualizationResponse = {
      response: "Here's a comparison of your three scenarios:",
      metadata: {
        visualization: {
          type: 'comparison_table',
          data: {
            scenarios: [
              { country: 'Vietnam', cost: 15000, tariff: '0%', timeline: '18 days' },
              { country: 'Bangladesh', cost: 14500, tariff: '0% (GSP)', timeline: '22 days' },
              { country: 'Mexico', cost: 16000, tariff: '0% (USMCA)', timeline: '12 days' }
            ]
          }
        }
      }
    }

    ;(global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => visualizationResponse
    })

    renderWithProviders(<AiAssistantPage />)

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/ask about trade/i)).toBeInTheDocument()
    })

    const input = screen.getByPlaceholderText(/ask about trade/i)
    const sendButton = screen.getByRole('button', { name: /send/i })

    fireEvent.change(input, {
      target: { value: 'Compare importing textiles from Vietnam, Bangladesh, and Mexico' }
    })
    fireEvent.click(sendButton)

    await waitFor(() => {
      expect(screen.getByText(/Here's a comparison/)).toBeInTheDocument()
    })

    // Verify table rendering
    expect(screen.getByText('Vietnam')).toBeInTheDocument()
    expect(screen.getByText('Bangladesh')).toBeInTheDocument()
    expect(screen.getByText('Mexico')).toBeInTheDocument()
    expect(screen.getByText('GSP')).toBeInTheDocument()
    expect(screen.getByText('USMCA')).toBeInTheDocument()
  })

  it('should handle export functionality', async () => {
    ;(global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => mockApiResponses.complexQuery
    })

    renderWithProviders(<AiAssistantPage />)

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/ask about trade/i)).toBeInTheDocument()
    })

    const input = screen.getByPlaceholderText(/ask about trade/i)
    const sendButton = screen.getByRole('button', { name: /send/i })

    fireEvent.change(input, {
      target: { value: 'Compare importing costs from Germany vs Japan' }
    })
    fireEvent.click(sendButton)

    await waitFor(() => {
      expect(screen.getByText(/Based on my analysis/)).toBeInTheDocument()
    })

    // Look for export button
    const exportButton = screen.getByRole('button', { name: /export/i })
    expect(exportButton).toBeInTheDocument()

    fireEvent.click(exportButton)

    // Verify export dialog opens
    await waitFor(() => {
      expect(screen.getByText(/Export Options/)).toBeInTheDocument()
    })

    expect(screen.getByText(/PDF/)).toBeInTheDocument()
    expect(screen.getByText(/Word/)).toBeInTheDocument()
    expect(screen.getByText(/HTML/)).toBeInTheDocument()
  })

  it('should handle voice input functionality', async () => {
    // Mock speech recognition
    const mockSpeechRecognition = {
      start: vi.fn(),
      stop: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn()
    }

    ;(global as any).SpeechRecognition = vi.fn(() => mockSpeechRecognition)
    ;(global as any).webkitSpeechRecognition = vi.fn(() => mockSpeechRecognition)

    renderWithProviders(<AiAssistantPage />)

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/ask about trade/i)).toBeInTheDocument()
    })

    // Look for voice input button
    const voiceButton = screen.getByRole('button', { name: /voice input/i })
    expect(voiceButton).toBeInTheDocument()

    fireEvent.click(voiceButton)

    expect(mockSpeechRecognition.start).toHaveBeenCalled()
  })

  it('should handle error states gracefully', async () => {
    // Mock API error
    ;(global.fetch as any).mockRejectedValueOnce(new Error('Network error'))

    renderWithProviders(<AiAssistantPage />)

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/ask about trade/i)).toBeInTheDocument()
    })

    const input = screen.getByPlaceholderText(/ask about trade/i)
    const sendButton = screen.getByRole('button', { name: /send/i })

    fireEvent.change(input, {
      target: { value: 'Test query' }
    })
    fireEvent.click(sendButton)

    await waitFor(() => {
      expect(screen.getByText(/error occurred/i)).toBeInTheDocument()
    })

    // Verify retry option is available
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument()
  })

  it('should handle loading states during complex queries', async () => {
    // Mock delayed response
    ;(global.fetch as any).mockImplementationOnce(() => 
      new Promise(resolve => 
        setTimeout(() => resolve({
          ok: true,
          json: async () => mockApiResponses.complexQuery
        }), 2000)
      )
    )

    renderWithProviders(<AiAssistantPage />)

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/ask about trade/i)).toBeInTheDocument()
    })

    const input = screen.getByPlaceholderText(/ask about trade/i)
    const sendButton = screen.getByRole('button', { name: /send/i })

    fireEvent.change(input, {
      target: { value: 'Complex analysis query' }
    })
    fireEvent.click(sendButton)

    // Verify loading indicator
    await waitFor(() => {
      expect(screen.getByText(/analyzing/i)).toBeInTheDocument()
    })

    // Verify progress indicator
    expect(screen.getByRole('progressbar')).toBeInTheDocument()
  })
})