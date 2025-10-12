import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import AiAssistantPage from '../../src/pages/AiAssistantPage'
import { AuthProvider } from '../../src/contexts/AuthContext'
import { SettingsProvider } from '../../src/contexts/SettingsContext'

// Performance monitoring utilities
class PerformanceMonitor {
  private metrics: Array<{
    operation: string
    duration: number
    timestamp: number
  }> = []

  startTiming(operation: string): () => void {
    const start = performance.now()
    return () => {
      const duration = performance.now() - start
      this.metrics.push({
        operation,
        duration,
        timestamp: Date.now()
      })
    }
  }

  getAverageTime(operation: string): number {
    const operationMetrics = this.metrics.filter(m => m.operation === operation)
    if (operationMetrics.length === 0) return 0
    return operationMetrics.reduce((sum, m) => sum + m.duration, 0) / operationMetrics.length
  }

  getMaxTime(operation: string): number {
    const operationMetrics = this.metrics.filter(m => m.operation === operation)
    if (operationMetrics.length === 0) return 0
    return Math.max(...operationMetrics.map(m => m.duration))
  }

  reset(): void {
    this.metrics = []
  }

  getMetrics(): typeof this.metrics {
    return [...this.metrics]
  }
}

// Mock API with realistic delays
const createMockApiWithDelay = (delay: number) => {
  return vi.fn().mockImplementation(() => 
    new Promise(resolve => 
      setTimeout(() => resolve({
        ok: true,
        json: async () => ({
          response: "Mock response for performance testing",
          metadata: {
            toolsUsed: ['TariffLookupTool'],
            executionTime: delay,
            confidence: 0.95
          }
        })
      }), delay)
    )
  )
}

global.fetch = createMockApiWithDelay(1000)

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

describe('AI Copilot Performance Tests', () => {
  let performanceMonitor: PerformanceMonitor

  beforeEach(() => {
    vi.clearAllMocks()
    performanceMonitor = new PerformanceMonitor()
    localStorage.setItem('token', 'mock-token')
    localStorage.setItem('user', JSON.stringify({ id: 'test-user', email: 'test@example.com' }))
  })

  afterEach(() => {
    localStorage.clear()
    console.log('Performance Metrics:', performanceMonitor.getMetrics())
  })

  it('should render chat interface within performance threshold', async () => {
    const endTiming = performanceMonitor.startTiming('initial-render')
    
    renderWithProviders(<AiAssistantPage />)
    
    await waitFor(() => {
      expect(screen.getByPlaceholderText(/ask about trade/i)).toBeInTheDocument()
    })
    
    endTiming()
    
    const renderTime = performanceMonitor.getAverageTime('initial-render')
    expect(renderTime).toBeLessThan(2000) // Should render within 2 seconds
  })

  it('should handle multiple rapid queries efficiently', async () => {
    global.fetch = createMockApiWithDelay(500)
    
    renderWithProviders(<AiAssistantPage />)
    
    await waitFor(() => {
      expect(screen.getByPlaceholderText(/ask about trade/i)).toBeInTheDocument()
    })

    const input = screen.getByPlaceholderText(/ask about trade/i)
    const sendButton = screen.getByRole('button', { name: /send/i })

    const queries = [
      'What is the tariff rate for cars?',
      'Compare Germany vs Japan imports',
      'Show compliance requirements',
      'Analyze trade risks',
      'Calculate shipping costs'
    ]

    // Send multiple queries rapidly
    for (let i = 0; i < queries.length; i++) {
      const endTiming = performanceMonitor.startTiming(`query-${i}`)
      
      fireEvent.change(input, { target: { value: queries[i] } })
      fireEvent.click(sendButton)
      
      await waitFor(() => {
        expect(screen.getByText(/Mock response/)).toBeInTheDocument()
      }, { timeout: 10000 })
      
      endTiming()
    }

    // Verify performance metrics
    const averageQueryTime = performanceMonitor.getAverageTime('query-0')
    const maxQueryTime = performanceMonitor.getMaxTime('query-0')
    
    expect(averageQueryTime).toBeLessThan(3000) // Average under 3 seconds
    expect(maxQueryTime).toBeLessThan(5000) // Max under 5 seconds
  })

  it('should handle complex visualization rendering efficiently', async () => {
    const visualizationResponse = {
      response: "Here's your analysis with charts and tables",
      metadata: {
        visualization: {
          type: 'complex_dashboard',
          data: {
            charts: Array.from({ length: 5 }, (_, i) => ({
              id: `chart-${i}`,
              type: 'line',
              data: Array.from({ length: 100 }, (_, j) => ({ x: j, y: Math.random() * 100 }))
            })),
            tables: Array.from({ length: 3 }, (_, i) => ({
              id: `table-${i}`,
              rows: Array.from({ length: 50 }, (_, j) => ({
                country: `Country ${j}`,
                tariff: Math.random() * 10,
                volume: Math.random() * 1000
              }))
            }))
          }
        }
      }
    }

    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => visualizationResponse
    })

    renderWithProviders(<AiAssistantPage />)

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/ask about trade/i)).toBeInTheDocument()
    })

    const input = screen.getByPlaceholderText(/ask about trade/i)
    const sendButton = screen.getByRole('button', { name: /send/i })

    const endTiming = performanceMonitor.startTiming('complex-visualization')

    fireEvent.change(input, { target: { value: 'Show complex analysis with charts' } })
    fireEvent.click(sendButton)

    await waitFor(() => {
      expect(screen.getByText(/Here's your analysis/)).toBeInTheDocument()
    }, { timeout: 15000 })

    endTiming()

    const visualizationTime = performanceMonitor.getAverageTime('complex-visualization')
    expect(visualizationTime).toBeLessThan(10000) // Complex visualizations under 10 seconds
  })

  it('should maintain performance with large conversation history', async () => {
    global.fetch = createMockApiWithDelay(300)
    
    renderWithProviders(<AiAssistantPage />)

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/ask about trade/i)).toBeInTheDocument()
    })

    const input = screen.getByPlaceholderText(/ask about trade/i)
    const sendButton = screen.getByRole('button', { name: /send/i })

    // Simulate a long conversation (20 exchanges)
    for (let i = 0; i < 20; i++) {
      const endTiming = performanceMonitor.startTiming(`conversation-${i}`)
      
      fireEvent.change(input, { target: { value: `Query number ${i + 1}` } })
      fireEvent.click(sendButton)
      
      await waitFor(() => {
        expect(screen.getByText(/Mock response/)).toBeInTheDocument()
      })
      
      endTiming()
    }

    // Performance should not degrade significantly with conversation length
    const firstQueryTime = performanceMonitor.getAverageTime('conversation-0')
    const lastQueryTime = performanceMonitor.getAverageTime('conversation-19')
    
    // Last query should not be more than 50% slower than first
    expect(lastQueryTime).toBeLessThan(firstQueryTime * 1.5)
  })

  it('should handle memory efficiently during extended usage', async () => {
    global.fetch = createMockApiWithDelay(200)
    
    renderWithProviders(<AiAssistantPage />)

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/ask about trade/i)).toBeInTheDocument()
    })

    const input = screen.getByPlaceholderText(/ask about trade/i)
    const sendButton = screen.getByRole('button', { name: /send/i })

    // Check initial memory usage (if available)
    const initialMemory = (performance as any).memory?.usedJSHeapSize || 0

    // Simulate extended usage
    for (let i = 0; i < 50; i++) {
      fireEvent.change(input, { target: { value: `Memory test query ${i}` } })
      fireEvent.click(sendButton)
      
      await waitFor(() => {
        expect(screen.getByText(/Mock response/)).toBeInTheDocument()
      })

      // Trigger garbage collection if available
      if ((window as any).gc) {
        (window as any).gc()
      }
    }

    const finalMemory = (performance as any).memory?.usedJSHeapSize || 0
    
    if (initialMemory > 0 && finalMemory > 0) {
      const memoryIncrease = finalMemory - initialMemory
      const memoryIncreasePercent = (memoryIncrease / initialMemory) * 100
      
      console.log(`Memory increase: ${memoryIncrease} bytes (${memoryIncreasePercent.toFixed(2)}%)`)
      
      // Memory increase should be reasonable (less than 100% increase)
      expect(memoryIncreasePercent).toBeLessThan(100)
    }
  })

  it('should handle concurrent operations efficiently', async () => {
    global.fetch = createMockApiWithDelay(800)
    
    renderWithProviders(<AiAssistantPage />)

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/ask about trade/i)).toBeInTheDocument()
    })

    const input = screen.getByPlaceholderText(/ask about trade/i)
    const sendButton = screen.getByRole('button', { name: /send/i })

    // Simulate concurrent operations
    const operations = [
      () => {
        fireEvent.change(input, { target: { value: 'Concurrent query 1' } })
        fireEvent.click(sendButton)
      },
      () => {
        // Simulate export operation
        const exportButton = screen.queryByRole('button', { name: /export/i })
        if (exportButton) fireEvent.click(exportButton)
      },
      () => {
        // Simulate voice input
        const voiceButton = screen.queryByRole('button', { name: /voice/i })
        if (voiceButton) fireEvent.click(voiceButton)
      }
    ]

    const endTiming = performanceMonitor.startTiming('concurrent-operations')

    // Execute operations concurrently
    operations.forEach(op => op())

    await waitFor(() => {
      expect(screen.getByText(/Mock response/)).toBeInTheDocument()
    }, { timeout: 10000 })

    endTiming()

    const concurrentTime = performanceMonitor.getAverageTime('concurrent-operations')
    expect(concurrentTime).toBeLessThan(5000) // Concurrent operations under 5 seconds
  })

  it('should optimize re-renders during typing', async () => {
    renderWithProviders(<AiAssistantPage />)

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/ask about trade/i)).toBeInTheDocument()
    })

    const input = screen.getByPlaceholderText(/ask about trade/i)
    const longQuery = "This is a very long query that simulates a user typing a complex question about international trade regulations and tariff calculations"

    const endTiming = performanceMonitor.startTiming('typing-performance')

    // Simulate rapid typing
    for (let i = 0; i < longQuery.length; i++) {
      fireEvent.change(input, { target: { value: longQuery.substring(0, i + 1) } })
    }

    endTiming()

    const typingTime = performanceMonitor.getAverageTime('typing-performance')
    expect(typingTime).toBeLessThan(1000) // Typing should be responsive (under 1 second)
  })

  it('should handle error states without performance degradation', async () => {
    // Mock API errors
    global.fetch = vi.fn()
      .mockRejectedValueOnce(new Error('Network error'))
      .mockRejectedValueOnce(new Error('Server error'))
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          response: "Recovery successful",
          metadata: { toolsUsed: ['TariffLookupTool'] }
        })
      })

    renderWithProviders(<AiAssistantPage />)

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/ask about trade/i)).toBeInTheDocument()
    })

    const input = screen.getByPlaceholderText(/ask about trade/i)
    const sendButton = screen.getByRole('button', { name: /send/i })

    // First error
    const endTiming1 = performanceMonitor.startTiming('error-handling-1')
    fireEvent.change(input, { target: { value: 'Error test 1' } })
    fireEvent.click(sendButton)
    
    await waitFor(() => {
      expect(screen.getByText(/error/i)).toBeInTheDocument()
    })
    endTiming1()

    // Second error
    const endTiming2 = performanceMonitor.startTiming('error-handling-2')
    fireEvent.change(input, { target: { value: 'Error test 2' } })
    fireEvent.click(sendButton)
    
    await waitFor(() => {
      expect(screen.getByText(/error/i)).toBeInTheDocument()
    })
    endTiming2()

    // Recovery
    const endTiming3 = performanceMonitor.startTiming('error-recovery')
    fireEvent.change(input, { target: { value: 'Recovery test' } })
    fireEvent.click(sendButton)
    
    await waitFor(() => {
      expect(screen.getByText(/Recovery successful/)).toBeInTheDocument()
    })
    endTiming3()

    // Error handling should not significantly impact performance
    const errorTime1 = performanceMonitor.getAverageTime('error-handling-1')
    const errorTime2 = performanceMonitor.getAverageTime('error-handling-2')
    const recoveryTime = performanceMonitor.getAverageTime('error-recovery')

    expect(errorTime1).toBeLessThan(3000)
    expect(errorTime2).toBeLessThan(3000)
    expect(recoveryTime).toBeLessThan(3000)
    
    // Recovery should not be significantly slower than error handling
    expect(recoveryTime).toBeLessThan(errorTime1 * 1.5)
  })
})