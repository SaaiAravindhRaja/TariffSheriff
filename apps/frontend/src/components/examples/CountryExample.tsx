import React, { useState } from 'react';
import { CountrySelect } from '../ui/CountrySelect';
import { CountryFlag } from '../ui/CountryFlag';
import { useCountries, useCountry } from '../../hooks/useCountries';
import type { Country } from '../../data/countries';

export const CountryExample: React.FC = () => {
  const [selectedOrigin, setSelectedOrigin] = useState<string>('');
  const [selectedDestination, setSelectedDestination] = useState<string>('');
  
  const { regions, currencies } = useCountries();
  const originCountry = useCountry(selectedOrigin);
  const destinationCountry = useCountry(selectedDestination);

  const handleOriginChange = (code: string, country?: Country) => {
    setSelectedOrigin(code);
    console.log('Origin selected:', country);
  };

  const handleDestinationChange = (code: string, country?: Country) => {
    setSelectedDestination(code);
    console.log('Destination selected:', country);
  };

  return (
    <div className="max-w-2xl mx-auto p-6 space-y-6">
      <h2 className="text-2xl font-bold text-gray-900">Country Data Example</h2>
      
      {/* Country Selection */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Origin Country
          </label>
          <CountrySelect
            value={selectedOrigin}
            onChange={handleOriginChange}
            placeholder="Select origin country..."
          />
        </div>
        
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Destination Country
          </label>
          <CountrySelect
            value={selectedDestination}
            onChange={handleDestinationChange}
            placeholder="Select destination country..."
          />
        </div>
      </div>

      {/* Selected Countries Display */}
      {(selectedOrigin || selectedDestination) && (
        <div className="bg-gray-50 p-4 rounded-lg">
          <h3 className="text-lg font-semibold mb-3">Selected Countries</h3>
          <div className="space-y-2">
            {selectedOrigin && (
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-600">Origin:</span>
                <div className="flex items-center gap-2">
                  <CountryFlag countryCode={selectedOrigin} showName />
                  {originCountry && (
                    <span className="text-xs text-gray-500">
                      ({originCountry.currency})
                    </span>
                  )}
                </div>
              </div>
            )}
            
            {selectedDestination && (
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-600">Destination:</span>
                <div className="flex items-center gap-2">
                  <CountryFlag countryCode={selectedDestination} showName />
                  {destinationCountry && (
                    <span className="text-xs text-gray-500">
                      ({destinationCountry.currency})
                    </span>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Stats */}
      <div className="grid grid-cols-2 gap-4">
        <div className="bg-blue-50 p-4 rounded-lg">
          <h4 className="font-semibold text-blue-900">Regions</h4>
          <p className="text-2xl font-bold text-blue-600">{regions.length}</p>
        </div>
        
        <div className="bg-green-50 p-4 rounded-lg">
          <h4 className="font-semibold text-green-900">Currencies</h4>
          <p className="text-2xl font-bold text-green-600">{currencies.length}</p>
        </div>
      </div>

      {/* Flag Examples */}
      <div className="space-y-3">
        <h3 className="text-lg font-semibold">Flag Size Examples</h3>
        <div className="flex items-center gap-4">
          <CountryFlag countryCode="US" size="sm" showName />
          <CountryFlag countryCode="DE" size="md" showName />
          <CountryFlag countryCode="JP" size="lg" showName />
        </div>
      </div>
    </div>
  );
};

export default CountryExample;