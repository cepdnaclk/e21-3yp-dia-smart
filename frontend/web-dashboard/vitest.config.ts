import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    globals: true,
    server: {
      deps: {
        inline: [/@mui/, "react-transition-group"],
      },
    },
  },
});