import eslint from '@eslint/js';
import tseslint from 'typescript-eslint';
import eslintPluginAstro from 'eslint-plugin-astro';

export default [
  eslint.configs.recommended,
  ...tseslint.configs.strict,
  ...eslintPluginAstro.configs.recommended,
  {
    ignores: [
      'dist/',
      '.astro/',
      'node_modules/',
      'coverage/',
      'playwright-report/',
    ],
  },
  {
    rules: {
      '@typescript-eslint/no-explicit-any': 'error',
      '@typescript-eslint/no-non-null-assertion': 'error',
      '@typescript-eslint/consistent-type-imports': 'error',
    },
  },
  {
    files: ['public/scripts/*.js'],
    languageOptions: {
      globals: {
        document: 'readonly',
        Element: 'readonly',
        HTMLElement: 'readonly',
        HTMLSelectElement: 'readonly',
        navigator: 'readonly',
        window: 'readonly',
        localStorage: 'readonly',
      },
    },
  },
];
