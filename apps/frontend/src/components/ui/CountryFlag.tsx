import React from 'react';

interface CountryFlagProps {
  countryCode: string;
  size?: 'sm' | 'md' | 'lg';
  showName?: boolean;
  className?: string;
}

const sizeClasses = {
  sm: 'text-sm',
  md: 'text-base',
  lg: 'text-lg'
};

export const CountryFlag: React.FC<CountryFlagProps> = ({
  countryCode,
  size = 'md',
  showName = false,
  className = ''
}) => {
  // Hardcoded country data removed. Fallback to showing the ISO code only.
  const country = { code: countryCode, name: countryCode } as const;

  return (
    <span 
      className={`inline-flex items-center gap-1 ${sizeClasses[size]} ${className}`}
      title={country.name}
    >
      {showName && <span>{country.name}</span>}
    </span>
  );
};

export default CountryFlag;