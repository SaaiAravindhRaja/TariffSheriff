import React from 'react';
import { getCountryByCode } from '../../data/countries';

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
  const country = getCountryByCode(countryCode);

  if (!country) {
    return <span className={`${sizeClasses[size]} ${className}`}>{countryCode}</span>;
  }

  return (
    <span 
      className={`inline-flex items-center gap-1 ${sizeClasses[size]} ${className}`}
      title={`${country.name} (${country.region})`}
    >
      <span className="flag-emoji">{country.emoji}</span>
      {showName && <span>{country.name}</span>}
    </span>
  );
};

export default CountryFlag;