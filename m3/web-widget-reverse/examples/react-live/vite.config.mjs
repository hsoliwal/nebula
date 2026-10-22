// SPDX-License-Identifier: Apache-2.0
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// The static bundle is presentation only. SWT/Nebula owns all logical state.
export default defineConfig({
  base: "./",
  plugins: [react()],
  server: {
    host: "127.0.0.1",
    port: 5173,
    strictPort: true,
  },
  build: {
    target: "es2020",
  },
});
