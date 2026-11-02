export const CITIES = ["Lucknow", "Patna", "Kanpur", "Delhi", "Varanasi"] as const;

export const CATEGORIES = [
  {
    value: "MAID",
    label: "Maid",
    description: "Cleaning, laundry, and everyday home help",
  },
  {
    value: "MECHANIC",
    label: "Mechanic",
    description: "Appliance repair and home maintenance",
  },
  {
    value: "WORKER",
    label: "Worker",
    description: "Assembly, shifting, and general household work",
  },
] as const;

export const TIME_SLOTS = [
  "Morning (9AM - 12PM)",
  "Afternoon (12PM - 4PM)",
  "Evening (4PM - 8PM)",
] as const;

export function categoryLabel(value?: string) {
  return CATEGORIES.find((item) => item.value === value)?.label || value || "Service";
}

export function cityFromText(value?: string) {
  if (!value) return "";
  const match = CITIES.find((city) => value.toLowerCase().includes(city.toLowerCase()));
  return match || "";
}
