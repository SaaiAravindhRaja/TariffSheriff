import React, { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, Globe, TrendingUp } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { useCountries } from '@/hooks/useCountries';
import { CountryFlag } from '@/components/ui/CountryFlag';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';

interface CountrySearchProps {
  className?: string;
  placeholder?: string;
  onCountrySelect?: (countryCode: string) => void;
}

export const CountrySearch: React.FC<CountrySearchProps> = ({
  className,
  placeholder = "Search countries...",
  onCountrySelect
}) => {
  const [query, setQuery] = useState('');
  const [isOpen, setIsOpen] = useState(false);
  const [highlightedIndex, setHighlightedIndex] = useState(0);
  const navigate = useNavigate();
  const { searchCountries, activeCountries } = useCountries();
  const inputRef = useRef<HTMLInputElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  // Filter countries based on search query
  const filteredCountries = query.trim() 
    ? searchCountries(query).slice(0, 8) // Limit to 8 results
    : activeCountries.slice(0, 6); // Show top 6 when no query

  // Handle country selection
  const handleCountrySelect = (countryCode: string) => {
    setQuery('');
    setIsOpen(false);
    setHighlightedIndex(0);
    
    if (onCountrySelect) {
      onCountrySelect(countryCode);
    } else {
      navigate(`/country/${countryCode.toLowerCase()}`);
    }
  };

  // Keyboard navigation
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (!isOpen) {
      if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
        setIsOpen(true);
        e.preventDefault();
      }
      return;
    }

    switch (e.key) {
      case 'ArrowDown':
        setHighlightedIndex(prev => 
          prev < filteredCountries.length - 1 ? prev + 1 : 0
        );
        e.preventDefault();
        break;
      case 'ArrowUp':
        setHighlightedIndex(prev => 
          prev > 0 ? prev - 1 : filteredCountries.length - 1
        );
        e.preventDefault();
        break;
      case 'Enter':
        if (filteredCountries[highlightedIndex]) {
          handleCountrySelect(filteredCountries[highlightedIndex].code);
        }
        e.preventDefault();
        break;
      case 'Escape':
        setIsOpen(false);
        setHighlightedIndex(0);
        inputRef.current?.blur();
        break;
    }
  };

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Reset highlighted index when query changes
  useEffect(() => {
    setHighlightedIndex(0);
  }, [query]);

  return (
    <div ref={containerRef} className={cn("relative", className)}>
      <div className="relative">
        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground w-4 h-4" />
        <input
          ref={inputRef}
          type="text"
          placeholder={placeholder}
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setIsOpen(true);
          }}
          onFocus={() => setIsOpen(true)}
          onKeyDown={handleKeyDown}
          className={cn(
            "w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-600",
            "rounded-lg bg-white dark:bg-gray-800",
            "focus:ring-2 focus:ring-brand-500 focus:border-brand-500",
            "placeholder:text-muted-foreground",
            "transition-all duration-200"
          )}
        />
      </div>

      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, y: -10, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -10, scale: 0.95 }}
            transition={{ duration: 0.2 }}
            className={cn(
              "absolute z-50 w-full mt-2",
              "bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700",
              "rounded-lg shadow-lg max-h-80 overflow-y-auto"
            )}
          >
            {filteredCountries.length === 0 ? (
              <div className="p-4 text-center text-muted-foreground">
                <Globe className="w-8 h-8 mx-auto mb-2 opacity-50" />
                <p>No countries found</p>
                <p className="text-xs">Try a different search term</p>
              </div>
            ) : (
              <div className="py-2">
                {!query && (
                  <div className="px-4 py-2 text-xs font-medium text-muted-foreground border-b border-gray-100 dark:border-gray-700">
                    Popular Countries
                  </div>
                )}
                {filteredCountries.map((country, index) => (
                  <motion.button
                    key={country.code}
                    initial={{ opacity: 0, x: -10 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ duration: 0.2, delay: index * 0.05 }}
                    onClick={() => handleCountrySelect(country.code)}
                    onMouseEnter={() => setHighlightedIndex(index)}
                    className={cn(
                      "w-full px-4 py-3 text-left hover:bg-gray-50 dark:hover:bg-gray-700",
                      "flex items-center justify-between group transition-colors",
                      highlightedIndex === index && "bg-brand-50 dark:bg-brand-900/20"
                    )}
                  >
                    <div className="flex items-center space-x-3">
                      <CountryFlag countryCode={country.code} size="md" />
                      <div>
                        <div className="font-medium text-gray-900 dark:text-gray-100">
                          {country.name}
                        </div>
                        <div className="text-sm text-muted-foreground">
                          {country.region} • {country.currency}
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center space-x-2">
                      <Badge variant="secondary" className="text-xs">
                        {country.code}
                      </Badge>
                      <TrendingUp className="w-4 h-4 text-muted-foreground opacity-0 group-hover:opacity-100 transition-opacity" />
                    </div>
                  </motion.button>
                ))}
              </div>
            )}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default CountrySearch;