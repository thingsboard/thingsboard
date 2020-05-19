import { TSESTree, TSESLintScope } from '@typescript-eslint/experimental-utils';
/**
 * based on eslint-scope
 */
export declare class ScopeManager extends TSESLintScope.ScopeManager {
    scopes: TSESLintScope.Scope[];
    globalScope: TSESLintScope.Scope;
    constructor(options: TSESLintScope.ScopeManagerOptions);
    /** @internal */
    __nestEnumScope(node: TSESTree.TSEnumDeclaration): TSESLintScope.Scope;
    /** @internal */
    __nestEmptyFunctionScope(node: TSESTree.TSDeclareFunction): TSESLintScope.Scope;
}
//# sourceMappingURL=scope-manager.d.ts.map