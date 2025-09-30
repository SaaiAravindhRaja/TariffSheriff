// Deprecated: hardcoded countries are removed. Keep a minimal placeholder to avoid import crashes.
export const useCountries = () => ({
  countries: [],
  countryOptions: [],
  regions: [],
  currencies: [],
  activeCountries: [],
  getCountryByCode: (_code: string) => undefined,
  getCountriesByRegion: (_region: string) => [],
  searchCountries: (_q: string) => [],
});

export const useCountry = (_countryCode?: string) => undefined as undefined;

export default useCountries;