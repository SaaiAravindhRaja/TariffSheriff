import React, { useState, useEffect } from 'react';
import { cn } from '@/lib/utils';
import { Loader2, Clock, Zap } from 'lucide-react';

interface ProgressIndicatorProps {
  isVisible: boolean;
  className?: string;
}

const PROGRESS_MESSAGES = [
  { message: "Analyzing your query...", icon: Loader2, delay: 0 },
  { message: "Searching trade data...", icon: Clock, delay: 3000 },
  { message: "Processing results...", icon: Zap, delay: 8000 },
  { message: "Almost done...", icon: Loader2, delay: 15000 },
];

export const ProgressIndicator: React.FC<ProgressIndicatorProps> = ({
  isVisible,
  className,
}) => {
  const [currentStep, setCurrentStep] = useState(0);
  const [startTime, setStartTime] = useState<number | null>(null);

  useEffect(() => {
    if (isVisible) {
      setStartTime(Date.now());
      setCurrentStep(0);
    } else {
      setStartTime(null);
      setCurrentStep(0);
    }
  }, [isVisible]);

  useEffect(() => {
    if (!isVisible || startTime === null) return;

    const timers: NodeJS.Timeout[] = [];

    PROGRESS_MESSAGES.forEach((step, index) => {
      if (index === 0) return; // First message shows immediately

      const timer = setTimeout(() => {
        setCurrentStep(index);
      }, step.delay);

      timers.push(timer);
    });

    return () => {
      timers.forEach(timer => clearTimeout(timer));
    };
  }, [isVisible, startTime]);

  if (!isVisible) return null;

  const currentMessage = PROGRESS_MESSAGES[currentStep];
  const Icon = currentMessage.icon;

  return (
    <div className={cn('flex items-center gap-3 p-4', className)}>
      <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-primary-foreground">
        <Icon size={16} className="animate-spin" />
      </div>
      <div className="flex flex-col">
        <div className="flex items-center space-x-2 rounded-lg bg-muted px-4 py-3 text-sm text-muted-foreground">
          <span>{currentMessage.message}</span>
          <div className="flex space-x-1">
            <div className="h-1 w-1 animate-pulse rounded-full bg-current"></div>
            <div className="h-1 w-1 animate-pulse rounded-full bg-current delay-75"></div>
            <div className="h-1 w-1 animate-pulse rounded-full bg-current delay-150"></div>
          </div>
        </div>
        {startTime && (
          <div className="mt-1 text-xs text-muted-foreground/70">
            {Math.floor((Date.now() - startTime) / 1000)}s elapsed
          </div>
        )}
      </div>
    </div>
  );
};

ProgressIndicator.displayName = 'ProgressIndicator';