export interface DashboardData {
  glucose: number;
  glucoseMeasuredAt?: string;

  temperature: number;
  temperatureStatus?: string;
  temperatureMeasuredAt?: string;

  inventory: number;
  inventoryStatus?: string;
  inventoryMeasuredAt?: string;
  estimatedRemainingPercent?: number;

  lastDose: number;
  lastDoseInjectedAt?: string;
}