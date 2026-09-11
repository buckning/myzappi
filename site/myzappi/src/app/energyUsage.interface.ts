export type EnergyUsageResolution = 'minute' | 'hourly';

export interface EnergyUsageReading {
  timestamp: string;
  solarGenerationKWh: number;
  importedKWh: number;
  exportedKWh: number;
  consumedKWh: number;
  importedKW: number;
  exportedKW: number;
  consumedKW: number;
}

export interface EnergyUsageResponse {
  date: string;
  zoneId: string;
  resolution: EnergyUsageResolution;
  readings: EnergyUsageReading[];
}
