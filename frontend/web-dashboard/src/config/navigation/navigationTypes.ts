import type { ElementType } from "react";

export interface NavigationItem {
  id: string;
  label: string;
  icon: ElementType;
  route: string;
}
