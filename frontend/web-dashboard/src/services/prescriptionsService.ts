export const prescriptionsService = {
  async getPrescriptions() {
    return [
      {
        id: 1,
        medication: "Insulin Glargine",
        dosage: "10 Units",
        frequency: "Daily",
        status: "Active",
      },
      {
        id: 2,
        medication: "Rapid Insulin",
        dosage: "5 Units",
        frequency: "Before Meals",
        status: "Active",
      },
    ];
  },
};