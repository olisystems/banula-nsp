import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import path from "path";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  base: "/navigator/ui",
  define: {
    "process.env": {
      routerBasename: "/navigator/ui",
      nonOcpiPath:
        process.env.NSP_API_NON_OCPI_PREFIX || "/navigator/non-ocpi/nsp",
      serviceUrl: process.env.BASE_URL || "http://localhost:8085",
      tokenB:
        process.env.NSP_TOKEN_B || "tokenB-br-lon",
    },
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
});
