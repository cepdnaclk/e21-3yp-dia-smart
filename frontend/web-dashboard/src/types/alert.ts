export interface Alert {
  id: number;
  title: string;
  description: string;
  severity: "error" | "warning" | "info" | "success";
}