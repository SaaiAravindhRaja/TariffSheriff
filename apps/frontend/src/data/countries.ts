// Deprecated hardcoded countries module; retained only for types shared by components.
// All country data now comes from the backend via useDbCountries.

export interface Country {
  code: string;
  name: string;
  emoji?: string;
  region?: string;
  currency?: string;
  active?: boolean;
  svgUrl?: string;
}

export default {} as Record<string, never>;