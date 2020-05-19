"use strict";
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (Object.hasOwnProperty.call(mod, k)) result[k] = mod[k];
    result["default"] = mod;
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
const tsutils = __importStar(require("tsutils"));
const ts = __importStar(require("typescript"));
const util = __importStar(require("../util"));
exports.default = util.createRule({
    name: 'no-floating-promises',
    meta: {
        docs: {
            description: 'Requires Promise-like values to be handled appropriately.',
            category: 'Best Practices',
            recommended: false,
        },
        messages: {
            floating: 'Promises must be handled appropriately',
        },
        schema: [],
        type: 'problem',
    },
    defaultOptions: [],
    create(context) {
        const parserServices = util.getParserServices(context);
        const checker = parserServices.program.getTypeChecker();
        return {
            ExpressionStatement(node) {
                const { expression } = parserServices.esTreeNodeToTSNodeMap.get(node);
                if (isUnhandledPromise(checker, expression)) {
                    context.report({
                        messageId: 'floating',
                        node,
                    });
                }
            },
        };
    },
});
function isUnhandledPromise(checker, node) {
    // First, check expressions whose resulting types may not be promise-like
    if (ts.isBinaryExpression(node) &&
        node.operatorToken.kind === ts.SyntaxKind.CommaToken) {
        // Any child in a comma expression could return a potentially unhandled
        // promise, so we check them all regardless of whether the final returned
        // value is promise-like.
        return (isUnhandledPromise(checker, node.left) ||
            isUnhandledPromise(checker, node.right));
    }
    else if (ts.isVoidExpression(node)) {
        // Similarly, a `void` expression always returns undefined, so we need to
        // see what's inside it without checking the type of the overall expression.
        return isUnhandledPromise(checker, node.expression);
    }
    // Check the type. At this point it can't be unhandled if it isn't a promise
    if (!isPromiseLike(checker, node)) {
        return false;
    }
    if (ts.isCallExpression(node)) {
        // If the outer expression is a call, it must be either a `.then()` or
        // `.catch()` that handles the promise.
        return (!isPromiseCatchCallWithHandler(node) &&
            !isPromiseThenCallWithRejectionHandler(node));
    }
    else if (ts.isConditionalExpression(node)) {
        // We must be getting the promise-like value from one of the branches of the
        // ternary. Check them directly.
        return (isUnhandledPromise(checker, node.whenFalse) ||
            isUnhandledPromise(checker, node.whenTrue));
    }
    else if (ts.isPropertyAccessExpression(node) ||
        ts.isIdentifier(node) ||
        ts.isNewExpression(node)) {
        // If it is just a property access chain or a `new` call (e.g. `foo.bar` or
        // `new Promise()`), the promise is not handled because it doesn't have the
        // necessary then/catch call at the end of the chain.
        return true;
    }
    // We conservatively return false for all other types of expressions because
    // we don't want to accidentally fail if the promise is handled internally but
    // we just can't tell.
    return false;
}
// Modified from tsutils.isThenable() to only consider thenables which can be
// rejected/caught via a second parameter. Original source (MIT licensed):
//
//   https://github.com/ajafff/tsutils/blob/49d0d31050b44b81e918eae4fbaf1dfe7b7286af/util/type.ts#L95-L125
function isPromiseLike(checker, node) {
    const type = checker.getTypeAtLocation(node);
    for (const ty of tsutils.unionTypeParts(checker.getApparentType(type))) {
        const then = ty.getProperty('then');
        if (then === undefined) {
            continue;
        }
        const thenType = checker.getTypeOfSymbolAtLocation(then, node);
        if (hasMatchingSignature(thenType, signature => signature.parameters.length >= 2 &&
            isFunctionParam(checker, signature.parameters[0], node) &&
            isFunctionParam(checker, signature.parameters[1], node))) {
            return true;
        }
    }
    return false;
}
function hasMatchingSignature(type, matcher) {
    for (const t of tsutils.unionTypeParts(type)) {
        if (t.getCallSignatures().some(matcher)) {
            return true;
        }
    }
    return false;
}
function isFunctionParam(checker, param, node) {
    const type = checker.getApparentType(checker.getTypeOfSymbolAtLocation(param, node));
    for (const t of tsutils.unionTypeParts(type)) {
        if (t.getCallSignatures().length !== 0) {
            return true;
        }
    }
    return false;
}
function isPromiseCatchCallWithHandler(expression) {
    return (tsutils.isPropertyAccessExpression(expression.expression) &&
        expression.expression.name.text === 'catch' &&
        expression.arguments.length >= 1);
}
function isPromiseThenCallWithRejectionHandler(expression) {
    return (tsutils.isPropertyAccessExpression(expression.expression) &&
        expression.expression.name.text === 'then' &&
        expression.arguments.length >= 2);
}
//# sourceMappingURL=no-floating-promises.js.map