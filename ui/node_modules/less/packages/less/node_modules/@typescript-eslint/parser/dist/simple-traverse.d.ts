import { TSESTree } from '@typescript-eslint/typescript-estree';
interface SimpleTraverseOptions {
    enter: (node: TSESTree.Node, parent: TSESTree.Node | undefined) => void;
}
export declare function simpleTraverse(startingNode: TSESTree.Node, options: SimpleTraverseOptions): void;
export {};
//# sourceMappingURL=simple-traverse.d.ts.map