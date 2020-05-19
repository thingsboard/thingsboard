"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * The goal of this ruleset is to update the eslint:recommended config to better
 * suit Typescript. There are two main reasons to change the configuration:
 * 1. The Typescript compiler natively checks some things that therefore don't
 *    need extra rules anymore.
 * 2. Typescript allows for more modern Javascript code that can thus be
 *    enabled.
 */
exports.default = {
    overrides: [
        {
            files: ['*.ts', '*.tsx'],
            rules: {
                /**
                 * 1. Disable things that are checked by Typescript
                 */
                //Checked by Typescript - ts(2378)
                'getter-return': 'off',
                // Checked by Typescript - ts(2300)
                'no-dupe-args': 'off',
                // Checked by Typescript - ts(1117)
                'no-dupe-keys': 'off',
                // Checked by Typescript - ts(7027)
                'no-unreachable': 'off',
                // Checked by Typescript - ts(2367)
                'valid-typeof': 'off',
                // Checked by Typescript - ts(2588)
                'no-const-assign': 'off',
                // Checked by Typescript - ts(2588)
                'no-new-symbol': 'off',
                // Checked by Typescript - ts(2376)
                'no-this-before-super': 'off',
                // This is checked by Typescript using the option `strictNullChecks`.
                'no-undef': 'off',
                // This is already checked by Typescript.
                'no-dupe-class-members': 'off',
                // This is already checked by Typescript.
                'no-redeclare': 'off',
                /**
                 * 2. Enable more ideomatic code
                 */
                // Typescript allows const and let instead of var.
                'no-var': 'error',
                'prefer-const': 'error',
                // The spread operator/rest parameters should be prefered in Typescript.
                'prefer-rest-params': 'error',
                'prefer-spread': 'error',
            },
        },
    ],
};
//# sourceMappingURL=eslint-recommended.js.map