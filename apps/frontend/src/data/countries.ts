import countriesData from './countries.json';

export interface Country {
  code: string;
  name: string;
  emoji: string;
  region: string;
  currency: string;
  active: boolean;
  svgUrl?: string; // Optional SVG flag URL
}

export const countries: Country[] = countriesData.countries;

// Utility functions for working with country data
export const getCountryByCode = (code: string): Country | undefined => {
  return countries.find(country => country.code === code);
};

export const getCountriesByRegion = (region: string): Country[] => {
  return countries.filter(country => country.region === region);
};

export const getActiveCountries = (): Country[] => {
  return countries.filter(country => country.active);
};

export const searchCountries = (query: string): Country[] => {
  const lowercaseQuery = query.toLowerCase();
  return countries.filter(country => 
    country.name.toLowerCase().includes(lowercaseQuery) ||
    country.code.toLowerCase().includes(lowercaseQuery)
  );
};

export const getCountryDisplay = (code: string): string => {
  const country = getCountryByCode(code);
  return country ? `${country.emoji} ${country.name}` : code;
};

export const getRegions = (): string[] => {
  return [...new Set(countries.map(country => country.region))].sort();
};

export const getCurrencies = (): string[] => {
  return [...new Set(countries.map(country => country.currency))].sort();
};

// For select components
export const getCountryOptions = () => {
  return getActiveCountries().map(country => ({
    value: country.code,
    label: `${country.emoji} ${country.name}`,
    country
  }));
};

export default countries;