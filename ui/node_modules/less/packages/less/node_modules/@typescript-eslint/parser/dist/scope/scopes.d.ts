import { TSESTree, TSESLintScope } from '@typescript-eslint/experimental-utils';
import { ScopeManager } from './scope-manager';
/** The scope class for enum. */
export declare class EnumScope extends TSESLintScope.Scope {
    constructor(scopeManager: ScopeManager, upperScope: TSESLintScope.Scope, block: TSESTree.TSEnumDeclaration | null);
}
/** The scope class for empty functions. */
export declare class EmptyFunctionScope extends TSESLintScope.Scope {
    constructor(scopeManager: ScopeManager, upperScope: TSESLintScope.Scope, block: TSESTree.TSDeclareFunction | null);
}
//# sourceMappingURL=scopes.d.ts.map