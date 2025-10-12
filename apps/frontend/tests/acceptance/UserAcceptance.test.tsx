import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import AiAssistantPage from '../../src/pages/AiAssistantPage'
import { AuthProvider } from '../../src/contexts/AuthContext'
import { SettingsProvider } from '../../src/contexts/SettingsContext'

// Mock realistic trade expert responses
const mockTradeExpertResponses = {
  customsBroker: {
    response: "For importing industrial machinery from Germany, you'll need: 1) Proper HS classification (likely 8459-8466 series), 2) Commercial invoice with detailed specifications, 3) Bill of lading, 4) Certificate of origin, 5) Machinery safety certificates. Tariff rates range from 0-4.4% depending on specific classification. Total landed cost including duties, fees, and shipping typically adds 15-20% to FOB value.",
    metadata: {
      toolsUsed: ['TariffLookupTool', 'ComplianceAnalysisTool', 'HsCodeFinderTool'],
      confidence: 0.94,
      executionTime: 6500
    }
  },
  complianceOfficer: {
    response: "FDA compliance for pharmaceutical APIs requires: 1) Drug Master File (DMF) registration, 2) Current Good Manufacturing Practice (cGMP) certification, 3) FDA facility registration, 4) Prior notice filing. India facilities face enhanced scrutiny with potential for unannounced inspections. Switzerland has mutual recognition agreement providing streamlined process. Key difference: India requires additional documentation and longer approval timelines (6-12 months vs 3-6 months for Switzerland).",
    metadata: {
      toolsUsed: ['ComplianceAnalysisTool', 'RiskAssessmentTool'],
      confidence: 0.96,
      executionTime: 8200
    }
  },
  supplyChainManager: {
    response: "Global sourcing analysis: Mexico (USMCA benefits: 0% tariffs, 12-day transit, medium political risk), China (MFN rates: 7.5-25% tariffs, 18-day transit, high trade war risk), Germany (MFN rates: 2.5-4.4% tariffs, 14-day transit, low risk). Recommendation: Diversify with 40% Mexico, 35% Germany, 25% China to optimize cost-risk balance. Consider nearshoring to Mexico for critical components.",
    metadata: {
      toolsUsed: ['TariffLookupTool', 'RiskAssessmentTool', 'MarketIntelligenceTool', 'AgreementTool'],
      confidence: 0.92,
      executionTime: 12000
    }
  },
  tradeLawyer: {
    response: "Anti-dumping investigation process: 1) ITC preliminary injury determination (45 days), 2) Commerce preliminary dumping determination (140 days), 3) ITC final injury determination (280 days). Defense strategies: challenge injury causation, argue no dumping margin, seek exclusions for specialized products. Immediate actions: suspend new orders, consider bonding, prepare questionnaire responses. Potential outcomes: 0-200%+ duties if affirmative determination.",
    metadata: {
      toolsUsed: ['ComplianceAnalysisTool', 'RiskAssessmentTool'],
      confidence: 0.89,
      executionTime: 9500
    }
  }
}

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

describe('User Acceptance Tests - Trade Expert Scenarios', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.setItem('token', 'mock-token')
    localStorage.setItem('user', JSON.stringify({ id: 'test-user', email: 'test@example.com' }))
  })

  afterEach(() => {
    localStorage.clear()
  })

  it('should provide professional-grade guidance for customs brokers', async () => {
    ;(global.fetch as any).mockResolvedValue({
      ok: true,
      json: async () => mockTradeExpertResponses.customsBroker
    })

    renderWithProviders(<AiAssistantPage />)

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/ask about trade/i)).toBeInTheDocument()
    })

    const input = screen.getByPlaceholderText(/ask about trade/i)
    const sendButton = screen.getByRole('button', { name: /send/i })

    fireEvent.change(input, {
      target: { value: 'My client wants to import 500 units of industrial machinery from Germany. What is the complete process and cost breakdown?' }
    })

    fireEvent.click(sendButton)

    await waitFor(() => {
      expect(screen.getByText(/HS classification/)).toBeInTheDocument()
    }, { timeout: 10000 })

    // Verify professional-grade content
    expect(screen.getByText(/8459-8466 series/)).toBeInTheDocument()
    expect(screen.getByText(/Commercial invoice/)).toBeInTheDocument()
    expect(screen.getByText(/Certificate of origin/)).toBeInTheDocument()
    expect(screen.getByText(/15-20%/)).toBeInTheDocument()

    // Verify metadata display for professionals
    expect(screen.getByText(/Tools used:/)).toBeInTheDocument()
    expect(screen.getByText(/Confidence: 94%/)).toBeInTheDocument()
    expect(screen.getByText(/TariffLookupTool/)).toBeInTheDocument()
    expect(screen.getByText(/ComplianceAnalysisTool/)).toBeInTheDocument()
  })

  it('should handle complex compliance scenarios for trade officers', async () => {
    ;(global.fetch as any).mockResolvedValue({
      ok: true,
      json: async () => mockTradeExpertResponses.complianceOfficer
    })

    renderWithProviders(<AiAssistantPage />)

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/ask about trade/i)).toBeInTheDocument()
    })

    const input = screen.getByPlaceholderText(/ask about trade/i)
    const sendButton = screen.getByRole('button', { name: /send/i })

    fireEvent.change(input, {
      target: { value: 'We are importing pharmaceutical APIs from India and Switzerland. What are the FDA compliance requirements and key differences?' }
    })

    fireEvent.click(sendButton)

    await waitFor(() => {
      expect(screen.getByText(/Drug Master File/)).toBeInTheDocument()
    })

    // Verify comprehensive compliance guidance
    expect(screen.getByText(/cGMP/)).toBeInTheDocument()
    expect(screen.getByText(/FDA facility registration/)).toBeInTheDocument()
    expect(screen.getByText(/mutual recognition agreement/)).toBeInTheDocument()
    expect(screen.getByText(/6-12 months vs 3-6 months/)).toBeInTheDocument()

    // Verify comparative analysis
    expect(screen.getByText(/India/)).toBeInTheDocument()
    expect(screen.getByText(/Switzerland/)).toBeInTheDocument()
    expect(screen.getByText(/enhanced scrutiny/)).toBeInTheDocument()
  })

  it('should provide strategic analysis for supply chain managers', async () => {
    ;(global.fetch as any).mockResolvedValue({
      ok: true,
      json: async () => mockTradeExpertResponses.supplyChainManager
    })

    renderWithProviders(<AiAssistantPage />)

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/ask about trade/i)).toBeInTheDocument()
    })

    const input = screen.getByPlaceholderText(/ask about trade/i)
    const sendButton = screen.getByRole('button', { name: /send/i })

    fireEvent.change(input, {
      target: { value: 'Analyze our global sourcing strategy for automotive components from Mexico, China, and Germany. Include costs, risks, and recommendations.' }
    })

    fireEvent.click(sendButton)

    await waitFor(() => {
      expect(screen.getByText(/Global sourcing analysis/)).toBeInTheDocument()
    })

    // Verify strategic recommendations
    expect(screen.getByText(/USMCA benefits/)).toBeInTheDocument()
    expect(screen.getByText(/40% Mexico, 35% Germany, 25% China/)).toBeInTheDocument()
    expect(screen.getByText(/nearshoring/)).toBeInTheDocument()
    expect(screen.getByText(/cost-risk balance/)).toBeInTheDocument()

    // Verify detailed analysis
    expect(screen.getByText(/0% tariffs/)).toBeInTheDocument()
    expect(screen.getByText(/trade war risk/)).toBeInTheDocument()
    expect(screen.getByText(/12-day transit/)).toBeInTheDocument()
  })

  it('should provide legal expertise for trade lawyers', async () => {
    ;(global.fetch as any).mockResolvedValue({
      ok: true,
      json: async () => mockTradeExpertResponses.tradeLawyer
    })

    renderWithProviders(<AiAssistantPage />)

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/ask about trade/i)).toBeInTheDocument()
    })

    const input = screen.getByPlaceholderText(/ask about trade/i)
    const sendButton = screen.getByRole('button', { name: /send/i })

    fireEvent.change(input, {
      target: { value: 'My client faces an anti-dumping investigation on steel imports. What are the legal procedures and defense strategies?' }
    })

    fireEvent.click(sendButton)

    await waitFor(() => {
      expect(screen.getByText(/Anti-dumping investigation process/)).toBeInTheDocument()
    })

    // Verify legal procedural guidance
    expect(screen.getByText(/ITC preliminary injury/)).toBeInTheDocument()
    expect(screen.getByText(/Commerce preliminary dumping/)).toBeInTheDocument()
    expect(screen.getByText(/45 days/)).toBeInTheDocument()
    expect(screen.getByText(/140 days/)).toBeInTheDocument()
    expect(screen.getByText(/280 days/)).toBeInTheDocument()

    // Verify defense strategies
    expect(screen.getByText(/challenge injury causation/)).toBeInTheDocument()
    expect(screen.getByText(/no dumping margin/)).toBeInTheDocument()
    expect(screen.getByText(/0-200%\+ duties/)).toBeInTheDocument()
  })

  it('should maintain context across professional conversations', async () => {
    // First query
    ;(global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => mockTradeExpertResponses.customsBroker
    })

    // Follow-up query
    ;(global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        response: "For the German machinery import we discussed, specific HS codes are: CNC machines (8459.10), Lathes (8458.11), Milling machines (8459.61). Current tariff rates: CNC machines 4.4%, Lathes 4.4%, Milling machines 2.4%. These rates apply under MFN status. No current anti-dumping duties on German machinery.",
        metadata: {
          contextResolved: true,
          toolsUsed: ['HsCodeFinderTool', 'TariffLookupTool'],
          confidence: 0.97
        }
      })
    })

    renderWithProviders(<AiAssistantPage />)

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/ask about trade/i)).toBeInTheDocument()
    })

    const input = screen.getByPlaceholderText(/ask about trade/i)
    const sendButton = screen.getByRole('button', { name: /send/i })

    // First query
    fireEvent.change(input, {
      target: { value: 'Industrial machinery import from Germany - need complete process' }
    })
    fireEvent.click(sendButton)

    await waitFor(() => {
      expect(screen.getByText(/HS classification/)).toBeInTheDocument()
    })

    // Follow-up query
    fireEvent.change(input, {
      target: { value: 'What are the specific HS codes and tariff rates for the machinery we just discussed?' }
    })
    fireEvent.click(sendButton)

    await waitFor(() => {
      expect(screen.getByText(/German machinery import we discussed/)).toBeInTheDocument()
    })

    // Verify context was maintained
    expect(screen.getByText(/8459.10/)).toBeInTheDocument()
    expect(screen.getByText(/8458.11/)).toBeInTheDocument()
    expect(screen.getByText(/8459.61/)).toBeInTheDocument()
    expect(screen.getByText(/4.4%/)).toBeInTheDocument()
    expect(screen.getByText(/2.4%/)).toBeInTheDocument()
  })

  it('should provide accessible interface for all user types', async () => {
    ;(global.fetch as any).mockResolvedValue({
      ok: true,
      json: async () => ({
        response: "Simple tariff lookup: Cars from Germany to US have a 2.5% tariff rate under HS code 8703. This is the Most Favored Nation (MFN) rate.",
        metadata: { toolsUsed: ['TariffLookupTool'] }
      })
    })

    renderWithProviders(<AiAssistantPage />)

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/ask about trade/i)).toBeInTheDocument()
    })

    // Test keyboard navigation
    const input = screen.getByPlaceholderText(/ask about trade/i)
    input.focus()
    expect(document.activeElement).toBe(input)

    // Test with simple query
    fireEvent.change(input, {
      target: { value: 'What is the tariff rate for cars from Germany?' }
    })

    // Test Enter key submission
    fireEvent.keyPress(input, { key: 'Enter', code: 'Enter', charCode: 13 })

    await waitFor(() => {
      expect(screen.getByText(/2.5% tariff rate/)).toBeInTheDocument()
    })

    // Verify accessibility features
    const responseElement = screen.getByText(/2.5% tariff rate/)
    expect(responseElement).toBeInTheDocument()

    // Test that complex information is presented clearly
    expect(screen.getByText(/HS code 8703/)).toBeInTheDocument()
    expect(screen.getByText(/Most Favored Nation/)).toBeInTheDocument()
  })

  it('should handle error states gracefully for professionals', async () => {
    // Mock API error
    ;(global.fetch as any).mockRejectedValue(new Error('Service temporarily unavailable'))

    renderWithProviders(<AiAssistantPage />)

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/ask about trade/i)).toBeInTheDocument()
    })

    const input = screen.getByPlaceholderText(/ask about trade/i)
    const sendButton = screen.getByRole('button', { name: /send/i })

    fireEvent.change(input, {
      target: { value: 'Complex trade analysis query' }
    })
    fireEvent.click(sendButton)

    await waitFor(() => {
      expect(screen.getByText(/temporarily unavailable/i)).toBeInTheDocument()
    })

    // Should provide professional error handling
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument()
    expect(screen.getByText(/contact support/i)).toBeInTheDocument()

    // Should not expose technical error details
    expect(screen.queryByText(/stack trace/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/internal server error/i)).not.toBeInTheDocument()
  })

  it('should provide export and sharing capabilities for professionals', async () => {
    ;(global.fetch as any).mockResolvedValue({
      ok: true,
      json: async () => mockTradeExpertResponses.customsBroker
    })

    renderWithProviders(<AiAssistantPage />)

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/ask about trade/i)).toBeInTheDocument()
    })

    const input = screen.getByPlaceholderText(/ask about trade/i)
    const sendButton = screen.getByRole('button', { name: /send/i })

    fireEvent.change(input, {
      target: { value: 'Professional analysis for client presentation' }
    })
    fireEvent.click(sendButton)

    await waitFor(() => {
      expect(screen.getByText(/HS classification/)).toBeInTheDocument()
    })

    // Test export functionality
    const exportButton = screen.getByRole('button', { name: /export/i })
    fireEvent.click(exportButton)

    await waitFor(() => {
      expect(screen.getByText(/Export Options/)).toBeInTheDocument()
    })

    // Should offer professional formats
    expect(screen.getByText(/PDF Report/)).toBeInTheDocument()
    expect(screen.getByText(/Word Document/)).toBeInTheDocument()
    expect(screen.getByText(/Excel Spreadsheet/)).toBeInTheDocument()

    // Test sharing functionality
    const shareButton = screen.getByRole('button', { name: /share/i })
    fireEvent.click(shareButton)

    await waitFor(() => {
      expect(screen.getByText(/Share Analysis/)).toBeInTheDocument()
    })

    expect(screen.getByText(/Email/)).toBeInTheDocument()
    expect(screen.getByText(/Copy Link/)).toBeInTheDocument()
    expect(screen.getByText(/Generate Report/)).toBeInTheDocument()
  })

  it('should provide performance metrics for professional validation', async () => {
    ;(global.fetch as any).mockResolvedValue({
      ok: true,
      json: async () => ({
        ...mockTradeExpertResponses.supplyChainManager,
        metadata: {
          ...mockTradeExpertResponses.supplyChainManager.metadata,
          performanceMetrics: {
            responseTime: 8500,
            dataFreshness: '2024-01-15T10:30:00Z',
            sourcesConsulted: 12,
            confidenceBreakdown: {
              tariffData: 0.98,
              riskAssessment: 0.89,
              marketIntelligence: 0.94
            }
          }
        }
      })
    })

    renderWithProviders(<AiAssistantPage />)

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/ask about trade/i)).toBeInTheDocument()
    })

    const input = screen.getByPlaceholderText(/ask about trade/i)
    const sendButton = screen.getByRole('button', { name: /send/i })

    fireEvent.change(input, {
      target: { value: 'Detailed supply chain analysis with metrics' }
    })
    fireEvent.click(sendButton)

    await waitFor(() => {
      expect(screen.getByText(/Global sourcing analysis/)).toBeInTheDocument()
    })

    // Verify performance metrics are displayed
    expect(screen.getByText(/Response time: 8.5s/)).toBeInTheDocument()
    expect(screen.getByText(/Sources consulted: 12/)).toBeInTheDocument()
    expect(screen.getByText(/Data freshness:/)).toBeInTheDocument()

    // Verify confidence breakdown for professionals
    expect(screen.getByText(/Tariff data: 98%/)).toBeInTheDocument()
    expect(screen.getByText(/Risk assessment: 89%/)).toBeInTheDocument()
    expect(screen.getByText(/Market intelligence: 94%/)).toBeInTheDocument()
  })
})