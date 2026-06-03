import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    coverage: {
      enabled: true,
      provider: 'v8',
      thresholds: {
        statements: 70,
        lines: 70,
        functions: 70,
        branches: 70,
      },
    },
  },
});
