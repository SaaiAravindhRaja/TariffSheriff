import React from 'react';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Lightbulb, TrendingUp, Calculator, FileText, Globe, AlertTriangle } from 'lucide-react';

interface Suggestion {
  id: string;
  text: string;
  category: 'tariff' | 'compliance' | 'market' | 'calculation' | 'general' | 'risk';
  icon?: React.ReactNode;
  description?: string;
}

interface ChatSuggestionsProps {
  suggestions: Suggestion[];
  onSuggestionClick: (suggestion: Suggestion) => void;
  className?: string;
  visible?: boolean;
}

const categoryIcons = {
  tariff: <Calculator size={14} />,
  compliance: <FileText size={14} />,
  market: <TrendingUp size={14} />,
  calculation: <Calculator size={14} />,
  general: <Globe size={14} />,
  risk: <AlertTriangle size={14} />,
};

const categoryColors = {
  tariff: 'border-blue-200 bg-blue-50 text-blue-700 hover:bg-blue-100',
  compliance: 'border-green-200 bg-green-50 text-green-700 hover:bg-green-100',
  market: 'border-purple-200 bg-purple-50 text-purple-700 hover:bg-purple-100',
  calculation: 'border-orange-200 bg-orange-50 text-orange-700 hover:bg-orange-100',
  general: 'border-gray-200 bg-gray-50 text-gray-700 hover:bg-gray-100',
  risk: 'border-red-200 bg-red-50 text-red-700 hover:bg-red-100',
};

export const ChatSuggestions: React.FC<ChatSuggestionsProps> = ({
  suggestions,
  onSuggestionClick,
  className,
  visible = true,
}) => {
  if (!visible || suggestions.length === 0) {
    return null;
  }

  return (
    <Card className={cn('p-3 mb-4', className)}>
      <div className="flex items-center gap-2 mb-3">
        <Lightbulb size={16} className="text-muted-foreground" />
        <span className="text-sm font-medium text-muted-foreground">
          Suggested queries
        </span>
      </div>
      
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
        {suggestions.map((suggestion) => (
          <Button
            key={suggestion.id}
            variant="ghost"
            className={cn(
              'h-auto p-3 justify-start text-left border',
              categoryColors[suggestion.category]
            )}
            onClick={() => onSuggestionClick(suggestion)}
          >
            <div className="flex items-start gap-2 w-full">
              <div className="flex-shrink-0 mt-0.5">
                {suggestion.icon || categoryIcons[suggestion.category]}
              </div>
              <div className="flex-1 min-w-0">
                <div className="text-sm font-medium truncate">
                  {suggestion.text}
                </div>
                {suggestion.description && (
                  <div className="text-xs opacity-75 mt-1 line-clamp-2">
                    {suggestion.description}
                  </div>
                )}
              </div>
            </div>
          </Button>
        ))}
      </div>
    </Card>
  );
};

// Default suggestions based on context
export const getContextualSuggestions = (
  conversationHistory: any[],
  userPreferences?: any
): Suggestion[] => {
  const baseSuggestions: Suggestion[] = [
    {
      id: 'tariff-lookup',
      text: 'What are the tariff rates for importing electronics from China to the US?',
      category: 'tariff',
      description: 'Get current tariff rates and trade agreement benefits'
    },
    {
      id: 'hs-code-finder',
      text: 'Help me find the HS code for wireless headphones',
      category: 'compliance',
      description: 'Product classification and regulatory requirements'
    },
    {
      id: 'market-analysis',
      text: 'Show me trade trends between Germany and Japan',
      category: 'market',
      description: 'Market intelligence and trade volume analysis'
    },
    {
      id: 'cost-calculation',
      text: 'Calculate total landed cost for 1000 units from Vietnam',
      category: 'calculation',
      description: 'Comprehensive cost analysis including duties and shipping'
    },
    {
      id: 'compliance-check',
      text: 'What documentation do I need to import medical devices?',
      category: 'compliance',
      description: 'Regulatory compliance and required certifications'
    },
    {
      id: 'risk-assessment',
      text: 'Assess trade risks for sourcing from Eastern Europe',
      category: 'risk',
      description: 'Political, economic, and supply chain risk analysis'
    }
  ];

  // Filter suggestions based on conversation history
  if (conversationHistory.length > 0) {
    const lastMessage = conversationHistory[conversationHistory.length - 1];
    const content = lastMessage?.content?.toLowerCase() || '';
    
    // If user asked about tariffs, suggest related queries
    if (content.includes('tariff') || content.includes('duty')) {
      return [
        {
          id: 'compare-routes',
          text: 'Compare tariff rates across different trade routes',
          category: 'calculation',
          description: 'Find the most cost-effective import route'
        },
        {
          id: 'seasonal-rates',
          text: 'Are there seasonal variations in these tariff rates?',
          category: 'market',
          description: 'Analyze timing for optimal cost savings'
        },
        ...baseSuggestions.slice(0, 4)
      ];
    }
    
    // If user asked about compliance, suggest related queries
    if (content.includes('compliance') || content.includes('regulation')) {
      return [
        {
          id: 'documentation-checklist',
          text: 'Create a documentation checklist for this import',
          category: 'compliance',
          description: 'Step-by-step compliance requirements'
        },
        {
          id: 'timeline-analysis',
          text: 'What are the typical processing times?',
          category: 'compliance',
          description: 'Import timeline and potential delays'
        },
        ...baseSuggestions.slice(0, 4)
      ];
    }
  }

  return baseSuggestions;
};

ChatSuggestions.displayName = 'ChatSuggestions';