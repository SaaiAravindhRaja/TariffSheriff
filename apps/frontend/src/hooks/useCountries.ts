import { useMemo } from 'react';
import { 
  countries, 
  getCountryByCode, 
  getCountriesByRegion, 
  getActiveCountries,
  searchCountries,
  getRegions,
  getCurrencies,
  getCountryOptions,
  type Country 
} from '../data/countries';

export const useCountries = () => {
  const countryOptions = useMemo(() => getCountryOptions(), []);
  const regions = useMemo(() => getRegions(), []);
  const currencies = useMemo(() => getCurrencies(), []);
  const activeCountries = useMemo(() => getActiveCountries(), []);

  return {
    countries,
    countryOptions,
    regions,
    currencies,
    activeCountries,
    getCountryByCode,
    getCountriesByRegion,
    searchCountries,
  };
};

export const useCountry = (countryCode?: string): Country | undefined => {
  return useMemo(() => {
    return countryCode ? getCountryByCode(countryCode) : undefined;
  }, [countryCode]);
};

export default useCountries;