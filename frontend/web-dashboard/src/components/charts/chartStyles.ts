import type { CSSProperties } from "react";

// Shared Recharts presentation tokens so every chart reads as one system.
export const chartTooltipContentStyle: CSSProperties = {
  backgroundColor: "#12233b",
  borderRadius: 10,
  border: "none",
  color: "#ffffff",
  boxShadow: "0 10px 28px rgba(15, 23, 42, 0.22)",
  padding: "8px 12px",
  fontSize: 12,
};

export const chartTooltipLabelStyle: CSSProperties = {
  fontWeight: 700,
  color: "#3ec1fa",
  marginBottom: 2,
};

export const chartTooltipItemStyle: CSSProperties = {
  color: "#e2e8f0",
  fontWeight: 600,
  padding: 0,
};

export const chartAxisProps = {
  stroke: "#94a3b8",
  fontSize: 11,
  tickLine: false,
  axisLine: false,
} as const;

export const chartGridProps = {
  strokeDasharray: "4 4",
  stroke: "#e2e8f0",
  vertical: false,
} as const;

export const chartCursor = { stroke: "#94a3b8", strokeDasharray: "4 4", strokeWidth: 1 };
