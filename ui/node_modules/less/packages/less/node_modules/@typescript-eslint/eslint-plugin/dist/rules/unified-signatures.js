"use strict";
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (Object.hasOwnProperty.call(mod, k)) result[k] = mod[k];
    result["default"] = mod;
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
const experimental_utils_1 = require("@typescript-eslint/experimental-utils");
const util = __importStar(require("../util"));
exports.default = util.createRule({
    name: 'unified-signatures',
    meta: {
        docs: {
            description: 'Warns for any two overloads that could be unified into one by using a union or an optional/rest parameter',
            category: 'Variables',
            recommended: false,
        },
        type: 'suggestion',
        messages: {
            omittingRestParameter: '{{failureStringStart}} with a rest parameter.',
            omittingSingleParameter: '{{failureStringStart}} with an optional parameter.',
            singleParameterDifference: '{{failureStringStart}} taking `{{type1}} | {{type2}}`.',
        },
        schema: [],
    },
    defaultOptions: [],
    create(context) {
        const sourceCode = context.getSourceCode();
        //----------------------------------------------------------------------
        // Helpers
        //----------------------------------------------------------------------
        function failureStringStart(otherLine) {
            // For only 2 overloads we don't need to specify which is the other one.
            const overloads = otherLine === undefined
                ? 'These overloads'
                : `This overload and the one on line ${otherLine}`;
            return `${overloads} can be combined into one signature`;
        }
        function addFailures(failures) {
            for (const failure of failures) {
                const { unify, only2 } = failure;
                switch (unify.kind) {
                    case 'single-parameter-difference': {
                        const { p0, p1 } = unify;
                        const lineOfOtherOverload = only2 ? undefined : p0.loc.start.line;
                        const typeAnnotation0 = isTSParameterProperty(p0)
                            ? p0.parameter.typeAnnotation
                            : p0.typeAnnotation;
                        const typeAnnotation1 = isTSParameterProperty(p1)
                            ? p1.parameter.typeAnnotation
                            : p1.typeAnnotation;
                        context.report({
                            loc: p1.loc,
                            messageId: 'singleParameterDifference',
                            data: {
                                failureStringStart: failureStringStart(lineOfOtherOverload),
                                type1: sourceCode.getText(typeAnnotation0 && typeAnnotation0.typeAnnotation),
                                type2: sourceCode.getText(typeAnnotation1 && typeAnnotation1.typeAnnotation),
                            },
                            node: p1,
                        });
                        break;
                    }
                    case 'extra-parameter': {
                        const { extraParameter, otherSignature } = unify;
                        const lineOfOtherOverload = only2
                            ? undefined
                            : otherSignature.loc.start.line;
                        context.report({
                            loc: extraParameter.loc,
                            messageId: extraParameter.type === experimental_utils_1.AST_NODE_TYPES.RestElement
                                ? 'omittingRestParameter'
                                : 'omittingSingleParameter',
                            data: {
                                failureStringStart: failureStringStart(lineOfOtherOverload),
                            },
                            node: extraParameter,
                        });
                    }
                }
            }
        }
        function checkOverloads(signatures, typeParameters) {
            const result = [];
            const isTypeParameter = getIsTypeParameter(typeParameters);
            for (const overloads of signatures) {
                if (overloads.length === 2) {
                    const signature0 = overloads[0].value || overloads[0];
                    const signature1 = overloads[1].value || overloads[1];
                    const unify = compareSignatures(signature0, signature1, isTypeParameter);
                    if (unify !== undefined) {
                        result.push({ unify, only2: true });
                    }
                }
                else {
                    forEachPair(overloads, (a, b) => {
                        const signature0 = a.value || a;
                        const signature1 = b.value || b;
                        const unify = compareSignatures(signature0, signature1, isTypeParameter);
                        if (unify !== undefined) {
                            result.push({ unify, only2: false });
                        }
                    });
                }
            }
            return result;
        }
        function compareSignatures(a, b, isTypeParameter) {
            if (!signaturesCanBeUnified(a, b, isTypeParameter)) {
                return undefined;
            }
            return a.params.length === b.params.length
                ? signaturesDifferBySingleParameter(a.params, b.params)
                : signaturesDifferByOptionalOrRestParameter(a, b);
        }
        function signaturesCanBeUnified(a, b, isTypeParameter) {
            // Must return the same type.
            const aTypeParams = a.typeParameters !== undefined ? a.typeParameters.params : undefined;
            const bTypeParams = b.typeParameters !== undefined ? b.typeParameters.params : undefined;
            return (typesAreEqual(a.returnType, b.returnType) &&
                // Must take the same type parameters.
                // If one uses a type parameter (from outside) and the other doesn't, they shouldn't be joined.
                util.arraysAreEqual(aTypeParams, bTypeParams, typeParametersAreEqual) &&
                signatureUsesTypeParameter(a, isTypeParameter) ===
                    signatureUsesTypeParameter(b, isTypeParameter));
        }
        /** Detect `a(x: number, y: number, z: number)` and `a(x: number, y: string, z: number)`. */
        function signaturesDifferBySingleParameter(types1, types2) {
            const index = getIndexOfFirstDifference(types1, types2, parametersAreEqual);
            if (index === undefined) {
                return undefined;
            }
            // If remaining arrays are equal, the signatures differ by just one parameter type
            if (!util.arraysAreEqual(types1.slice(index + 1), types2.slice(index + 1), parametersAreEqual)) {
                return undefined;
            }
            const a = types1[index];
            const b = types2[index];
            // Can unify `a?: string` and `b?: number`. Can't unify `...args: string[]` and `...args: number[]`.
            // See https://github.com/Microsoft/TypeScript/issues/5077
            return parametersHaveEqualSigils(a, b) &&
                a.type !== experimental_utils_1.AST_NODE_TYPES.RestElement
                ? { kind: 'single-parameter-difference', p0: a, p1: b }
                : undefined;
        }
        /**
         * Detect `a(): void` and `a(x: number): void`.
         * Returns the parameter declaration (`x: number` in this example) that should be optional/rest, and overload it's a part of.
         */
        function signaturesDifferByOptionalOrRestParameter(a, b) {
            const sig1 = a.params;
            const sig2 = b.params;
            const minLength = Math.min(sig1.length, sig2.length);
            const longer = sig1.length < sig2.length ? sig2 : sig1;
            const shorter = sig1.length < sig2.length ? sig1 : sig2;
            const shorterSig = sig1.length < sig2.length ? a : b;
            // If one is has 2+ parameters more than the other, they must all be optional/rest.
            // Differ by optional parameters: f() and f(x), f() and f(x, ?y, ...z)
            // Not allowed: f() and f(x, y)
            for (let i = minLength + 1; i < longer.length; i++) {
                if (!parameterMayBeMissing(longer[i])) {
                    return undefined;
                }
            }
            for (let i = 0; i < minLength; i++) {
                const sig1i = sig1[i];
                const sig2i = sig2[i];
                const typeAnnotation1 = isTSParameterProperty(sig1i)
                    ? sig1i.parameter.typeAnnotation
                    : sig1i.typeAnnotation;
                const typeAnnotation2 = isTSParameterProperty(sig2i)
                    ? sig2i.parameter.typeAnnotation
                    : sig2i.typeAnnotation;
                if (!typesAreEqual(typeAnnotation1, typeAnnotation2)) {
                    return undefined;
                }
            }
            if (minLength > 0 &&
                shorter[minLength - 1].type === experimental_utils_1.AST_NODE_TYPES.RestElement) {
                return undefined;
            }
            return {
                extraParameter: longer[longer.length - 1],
                kind: 'extra-parameter',
                otherSignature: shorterSig,
            };
        }
        /** Given type parameters, returns a function to test whether a type is one of those parameters. */
        function getIsTypeParameter(typeParameters) {
            if (typeParameters === undefined) {
                return () => false;
            }
            const set = new Set();
            for (const t of typeParameters.params) {
                set.add(t.name.name);
            }
            return typeName => set.has(typeName);
        }
        /** True if any of the outer type parameters are used in a signature. */
        function signatureUsesTypeParameter(sig, isTypeParameter) {
            return sig.params.some((p) => typeContainsTypeParameter(isTSParameterProperty(p)
                ? p.parameter.typeAnnotation
                : p.typeAnnotation));
            function typeContainsTypeParameter(type) {
                if (!type) {
                    return false;
                }
                if (type.type === experimental_utils_1.AST_NODE_TYPES.TSTypeReference) {
                    const typeName = type.typeName;
                    if (isIdentifier(typeName) && isTypeParameter(typeName.name)) {
                        return true;
                    }
                }
                return typeContainsTypeParameter(type.typeAnnotation ||
                    type.elementType);
            }
        }
        function isTSParameterProperty(node) {
            return (node.type ===
                experimental_utils_1.AST_NODE_TYPES.TSParameterProperty);
        }
        function parametersAreEqual(a, b) {
            const typeAnnotationA = isTSParameterProperty(a)
                ? a.parameter.typeAnnotation
                : a.typeAnnotation;
            const typeAnnotationB = isTSParameterProperty(b)
                ? b.parameter.typeAnnotation
                : b.typeAnnotation;
            return (parametersHaveEqualSigils(a, b) &&
                typesAreEqual(typeAnnotationA, typeAnnotationB));
        }
        /** True for optional/rest parameters. */
        function parameterMayBeMissing(p) {
            const optional = isTSParameterProperty(p)
                ? p.parameter.optional
                : p.optional;
            return p.type === experimental_utils_1.AST_NODE_TYPES.RestElement || optional;
        }
        /** False if one is optional and the other isn't, or one is a rest parameter and the other isn't. */
        function parametersHaveEqualSigils(a, b) {
            const optionalA = isTSParameterProperty(a)
                ? a.parameter.optional
                : a.optional;
            const optionalB = isTSParameterProperty(b)
                ? b.parameter.optional
                : b.optional;
            return ((a.type === experimental_utils_1.AST_NODE_TYPES.RestElement) ===
                (b.type === experimental_utils_1.AST_NODE_TYPES.RestElement) &&
                (optionalA !== undefined) === (optionalB !== undefined));
        }
        function typeParametersAreEqual(a, b) {
            return (a.name.name === b.name.name &&
                constraintsAreEqual(a.constraint, b.constraint));
        }
        function typesAreEqual(a, b) {
            return (a === b ||
                (a !== undefined &&
                    b !== undefined &&
                    a.typeAnnotation.type === b.typeAnnotation.type));
        }
        function constraintsAreEqual(a, b) {
            return (a === b || (a !== undefined && b !== undefined && a.type === b.type));
        }
        /* Returns the first index where `a` and `b` differ. */
        function getIndexOfFirstDifference(a, b, equal) {
            for (let i = 0; i < a.length && i < b.length; i++) {
                if (!equal(a[i], b[i])) {
                    return i;
                }
            }
            return undefined;
        }
        /** Calls `action` for every pair of values in `values`. */
        function forEachPair(values, action) {
            for (let i = 0; i < values.length; i++) {
                for (let j = i + 1; j < values.length; j++) {
                    action(values[i], values[j]);
                }
            }
        }
        const scopes = [];
        let currentScope = {
            overloads: new Map(),
        };
        function createScope(parent, typeParameters) {
            currentScope && scopes.push(currentScope);
            currentScope = {
                overloads: new Map(),
                parent,
                typeParameters,
            };
        }
        function checkScope() {
            const failures = checkOverloads(Array.from(currentScope.overloads.values()), currentScope.typeParameters);
            addFailures(failures);
            currentScope = scopes.pop();
        }
        function addOverload(signature, key) {
            key = key || getOverloadKey(signature);
            if (currentScope && signature.parent === currentScope.parent && key) {
                const overloads = currentScope.overloads.get(key);
                if (overloads !== undefined) {
                    overloads.push(signature);
                }
                else {
                    currentScope.overloads.set(key, [signature]);
                }
            }
        }
        //----------------------------------------------------------------------
        // Public
        //----------------------------------------------------------------------
        return {
            Program: createScope,
            TSModuleBlock: createScope,
            TSInterfaceDeclaration(node) {
                createScope(node.body, node.typeParameters);
            },
            ClassDeclaration(node) {
                createScope(node.body, node.typeParameters);
            },
            TSTypeLiteral: createScope,
            // collect overloads
            TSDeclareFunction(node) {
                if (node.id && !node.body) {
                    addOverload(node, node.id.name);
                }
            },
            TSCallSignatureDeclaration: addOverload,
            TSConstructSignatureDeclaration: addOverload,
            TSMethodSignature: addOverload,
            TSAbstractMethodDefinition(node) {
                if (!node.value.body) {
                    addOverload(node);
                }
            },
            MethodDefinition(node) {
                if (!node.value.body) {
                    addOverload(node);
                }
            },
            // validate scopes
            'Program:exit': checkScope,
            'TSModuleBlock:exit': checkScope,
            'TSInterfaceDeclaration:exit': checkScope,
            'ClassDeclaration:exit': checkScope,
            'TSTypeLiteral:exit': checkScope,
        };
    },
});
function getOverloadKey(node) {
    const info = getOverloadInfo(node);
    return ((node.computed ? '0' : '1') +
        (node.static ? '0' : '1') +
        info);
}
function getOverloadInfo(node) {
    switch (node.type) {
        case experimental_utils_1.AST_NODE_TYPES.TSConstructSignatureDeclaration:
            return 'constructor';
        case experimental_utils_1.AST_NODE_TYPES.TSCallSignatureDeclaration:
            return '()';
        default: {
            const { key } = node;
            return isIdentifier(key) ? key.name : key.raw;
        }
    }
}
function isIdentifier(node) {
    return node.type === experimental_utils_1.AST_NODE_TYPES.Identifier;
}
//# sourceMappingURL=unified-signatures.js.map