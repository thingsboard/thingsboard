/// <reference types="vitest" />
import { defineConfig } from 'vitest/config';
import angular from '@analogjs/vite-plugin-angular';
import { resolve } from 'node:path';

export default defineConfig({
  logLevel: 'error',
  plugins: [
    angular({ tsconfig: resolve(__dirname, 'src/tsconfig.spec.json') }),
  ],
  test: {
    globals: true,
    environment: 'jsdom',
    include: ['src/**/*.spec.ts'],
    exclude: [
      'src/app/core/auth/auth.service.spec.ts',
      'src/app/core/services/generate-sanitize-report.spec.ts',
    ],
    setupFiles: ['src/test-setup.ts'],
    reporters: ['default'],
  },
  resolve: {
    alias: {
      '@app': resolve(__dirname, 'src/app'),
      '@env': resolve(__dirname, 'src/environments'),
      '@core': resolve(__dirname, 'src/app/core'),
      '@modules': resolve(__dirname, 'src/app/modules'),
      '@shared': resolve(__dirname, 'src/app/shared'),
      '@home': resolve(__dirname, 'src/app/modules/home'),
    },
  },
});
