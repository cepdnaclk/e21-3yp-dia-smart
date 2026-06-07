export const alertsService = {
  async getAlerts() {
    return [
      {
        id: 1,
        severity: "error",
        title: "Inventory Low",
        description:
          "Insulin inventory dropped below 20 units.",
      },
      {
        id: 2,
        severity: "warning",
        title: "Temperature Warning",
        description:
          "Refrigerator temperature exceeded 8°C.",
      },
      {
        id: 3,
        severity: "warning",
        title: "Missed Dose",
        description:
          "Patient missed scheduled insulin dose.",
      },
      {
        id: 4,
        severity: "info",
        title: "Glucose Reading Received",
        description:
          "New BLE glucose reading synced successfully.",
      },
    ];
  },
};