declare module 'geolib' {
  export function getDistance(
    start: { latitude: number; longitude: number },
    end: { latitude: number; longitude: number }
  ): number
}

declare module 'world-countries' {
  const countries: Array<any>
  export default countries
}
*** End Patch```} />

