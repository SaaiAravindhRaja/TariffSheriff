import React from 'react';
import { HelpCircle } from 'lucide-react';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip';

interface FieldLabelProps {
  label: string;
  required?: boolean;
  tooltip?: string;
  className?: string;
}

export function FieldLabel({ label, required = false, tooltip, className = '' }: FieldLabelProps) {
  return (
    <TooltipProvider>
      <div className={`flex items-center gap-1 text-sm font-medium ${className}`}>
        <span>
          {label}
          {required && <span className="text-red-500 ml-1">*</span>}
        </span>
        {tooltip && (
          <Tooltip>
            <TooltipTrigger asChild>
              <HelpCircle className="w-4 h-4 text-muted-foreground hover:text-foreground cursor-help" />
            </TooltipTrigger>
            <TooltipContent side="top" className="max-w-xs">
              <p className="text-xs leading-relaxed">{tooltip}</p>
            </TooltipContent>
          </Tooltip>
        )}
      </div>
    </TooltipProvider>
  );
}