import eslintJS from "@eslint/js";
import tsEslint from "typescript-eslint";
import angular from "angular-eslint";
import tailwind from "eslint-plugin-tailwindcss";

export default tsEslint.config(
  {
    files: ["**/*.ts"],
    languageOptions: {
      parserOptions: {
        project: true,
        tsconfigRootDir: import.meta.dirname
      },
    },
    extends: [
      eslintJS.configs.recommended,
      ...tsEslint.configs.recommended,
      ...tsEslint.configs.stylistic,
      ...angular.configs.tsRecommended,
    ],
    processor: angular.processInlineTemplates,
    rules: {
      "@typescript-eslint/explicit-member-accessibility": [
        "off",
        {
          accessibility: "explicit"
        }
      ],
      "arrow-parens": [
        "off",
        "always"
      ],
      "@angular-eslint/component-selector": [
        "error",
        {
          prefix: [
            "tb"
          ]
        }
      ],
      "id-blacklist": [
        "error",
        "any",
        "Number",
        "String",
        "string",
        "Boolean",
        "boolean",
        "Undefined",
        "undefined"
      ],
      "import/order": "off",
      "@typescript-eslint/member-ordering": "off",
      "no-underscore-dangle": "off",
      "@typescript-eslint/naming-convention": "off",
      "jsdoc/newline-after-description": 0,
      "@typescript-eslint/consistent-indexed-object-style": "off",
      "@typescript-eslint/array-type": "off",
      "no-extra-boolean-cast": "off",
      "@typescript-eslint/no-empty-function": "off",
      "@typescript-eslint/no-explicit-any": "off",
      "@typescript-eslint/no-inferrable-types": "off",
      "@typescript-eslint/no-unused-vars": "off",
      "@typescript-eslint/ban-ts-comment": "off",
      "no-case-declarations": "off",
      "no-prototype-builtins": "off",
      "@typescript-eslint/consistent-type-definitions": "off",
      "@angular-eslint/prefer-standalone": "off",
      "@angular-eslint/prefer-inject": "off"
    },
  },
  {
    files: ["**/*.html"],
    extends: [
      ...angular.configs.templateRecommended,
      ...angular.configs.templateAccessibility,
      ...tailwind.configs["flat/recommended"]
    ],
    rules: {
      "tailwindcss/no-custom-classname": "off",
      "tailwindcss/migration-from-tailwind-2": "off",
      "tailwindcss/enforces-negative-arbitrary-values": "off"
    }
  }
);
