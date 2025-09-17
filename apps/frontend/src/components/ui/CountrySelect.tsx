import React from 'react';
import { getCountryOptions, getCountryByCode, type Country } from '../../data/countries';

interface CountrySelectProps {
  value?: string;
  onChange: (countryCode: string, country?: Country) => void;
  placeholder?: string;
  className?: string;
  disabled?: boolean;
  required?: boolean;
}

export const CountrySelect: React.FC<CountrySelectProps> = ({
  value,
  onChange,
  placeholder = "Select a country...",
  className = "",
  disabled = false,
  required = false
}) => {
  const countryOptions = getCountryOptions();

  const handleChange = (event: React.ChangeEvent<HTMLSelectElement>) => {
    const selectedCode = event.target.value;
    const selectedCountry = getCountryByCode(selectedCode);
    onChange(selectedCode, selectedCountry);
  };

  return (
    <select
      value={value || ''}
      onChange={handleChange}
      disabled={disabled}
      required={required}
      className={`
        w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm
        focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500
        disabled:bg-gray-100 disabled:cursor-not-allowed
        ${className}
      `}
    >
      <option value="">{placeholder}</option>
      {countryOptions.map(({ value: code, label, country }) => (
        <option key={code} value={code} title={`${country.region} • ${country.currency}`}>
          {label}
        </option>
      ))}
    </select>
  );
};

export default CountrySelect;