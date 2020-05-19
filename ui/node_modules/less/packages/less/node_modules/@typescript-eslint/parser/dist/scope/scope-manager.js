"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const experimental_utils_1 = require("@typescript-eslint/experimental-utils");
const scopes_1 = require("./scopes");
/**
 * based on eslint-scope
 */
class ScopeManager extends experimental_utils_1.TSESLintScope.ScopeManager {
    constructor(options) {
        super(options);
    }
    /** @internal */
    __nestEnumScope(node) {
        return this.__nestScope(new scopes_1.EnumScope(this, this.__currentScope, node));
    }
    /** @internal */
    __nestEmptyFunctionScope(node) {
        return this.__nestScope(new scopes_1.EmptyFunctionScope(this, this.__currentScope, node));
    }
}
exports.ScopeManager = ScopeManager;
//# sourceMappingURL=scope-manager.js.map