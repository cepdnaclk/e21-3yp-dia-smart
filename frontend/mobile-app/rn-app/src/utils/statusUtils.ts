export function getRelativeTime(timestamp?: string | number | Date): string {
  if (!timestamp) return "No recent record";
  const date = new Date(timestamp);
  if (isNaN(date.getTime())) return "No recent record";

  const diffMs = Date.now() - date.getTime();
  if (diffMs < 0) return "Just now";

  const diffSecs = Math.floor(diffMs / 1000);
  const diffMins = Math.floor(diffSecs / 60);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);

  if (diffSecs < 60) return "Just now";
  if (diffMins < 60) return `${diffMins} min${diffMins > 1 ? "s" : ""} ago`;
  if (diffHours < 24) return `${diffHours} hr${diffHours > 1 ? "s" : ""} ago`;
  return `${diffDays} day${diffDays > 1 ? "s" : ""} ago`;
}

export interface DynamicStatus {
  text: string;
  color: string;
}

export function getGlucoseStatus(glucose: number, targetMin = 70, targetMax = 180): DynamicStatus {
  if (!glucose || glucose === 0) {
    return { text: "No Reading", color: "#64748b" };
  }
  if (glucose < targetMin) {
    return { text: `Low (${glucose} mg/dL)`, color: "#ef4444" };
  }
  if (glucose > targetMax) {
    return { text: `High (${glucose} mg/dL)`, color: "#ef4444" };
  }
  return { text: "In Range", color: "#10b981" };
}

export function getInventoryStatus(inventoryG: number, estimatedRemainingPercent?: number, inventoryStatus?: string): DynamicStatus {
  if (inventoryStatus === "REMOVED") {
    return { text: "Pen/Cartridge Removed", color: "#f59e0b" };
  }
  if (inventoryStatus === "EMPTY" || (inventoryG === 0 && estimatedRemainingPercent === undefined)) {
    return { text: "Cartridge Empty", color: "#ef4444" };
  }
  if (inventoryStatus === "CRITICAL") {
    return { text: "Critically Low", color: "#ef4444" };
  }
  if (estimatedRemainingPercent !== undefined && estimatedRemainingPercent !== null) {
    const pct = Math.round(estimatedRemainingPercent);
    if (pct <= 20) {
      return { text: `Low (${pct}% Left)`, color: "#f59e0b" };
    }
    return { text: `${pct}% Remaining`, color: "#10b981" };
  }
  if (inventoryStatus === "LOW") {
    return { text: "Low Supply", color: "#f59e0b" };
  }
  return { text: "Adequate Level", color: "#10b981" };
}

export function getTemperatureStatus(tempC: number, temperatureStatus?: string): DynamicStatus {
  if (tempC === 0 && !temperatureStatus) {
    return { text: "No Sensor Data", color: "#64748b" };
  }
  if (temperatureStatus === "LOW" || tempC < 2.0) {
    return { text: "Too Cold (< 2°C)", color: "#3ec1fa" };
  }
  if (temperatureStatus === "HIGH" || tempC > 8.0) {
    return { text: "Too Warm (> 8°C)", color: "#ef4444" };
  }
  return { text: `Normal (${tempC.toFixed(1)}°C)`, color: "#10b981" };
}

export function getLastDoseStatus(lastDoseUnits: number, injectedAt?: string): DynamicStatus {
  if (!injectedAt || lastDoseUnits === 0) {
    return { text: "No Recent Dose", color: "#64748b" };
  }
  return { text: getRelativeTime(injectedAt), color: "#64748b" };
}
