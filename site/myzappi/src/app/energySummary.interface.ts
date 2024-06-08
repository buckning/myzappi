export interface DeviceEnergyUsage {
    deviceClass: string;
    serialNumber: string;
    usageRateKW: number;
}

export interface EnergySummary {
    solarGenerationKW: number;
    consumingKW: number;
    importingKW: number;
    exportingKW: number;
    zappiChargeRate: number;
    deviceUsage: DeviceEnergyUsage[];
}
