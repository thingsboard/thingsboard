"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const experimental_utils_1 = require("@typescript-eslint/experimental-utils");
/** The scope class for enum. */
class EnumScope extends experimental_utils_1.TSESLintScope.Scope {
    constructor(scopeManager, upperScope, block) {
        super(scopeManager, 'enum', upperScope, block, false);
    }
}
exports.EnumScope = EnumScope;
/** The scope class for empty functions. */
class EmptyFunctionScope extends experimental_utils_1.TSESLintScope.Scope {
    constructor(scopeManager, upperScope, block) {
        super(scopeManager, 'empty-function', upperScope, block, false);
    }
}
exports.EmptyFunctionScope = EmptyFunctionScope;
//# sourceMappingURL=scopes.js.map