"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const visitor_keys_1 = require("./visitor-keys");
function isValidNode(x) {
    return x !== null && typeof x === 'object' && typeof x.type === 'string';
}
function getVisitorKeysForNode(allVisitorKeys, node) {
    const keys = allVisitorKeys[node.type];
    return keys || [];
}
class SimpleTraverser {
    constructor({ enter }) {
        this.allVisitorKeys = visitor_keys_1.visitorKeys;
        this.enter = enter;
    }
    traverse(node, parent) {
        if (!isValidNode(node)) {
            return;
        }
        this.enter(node, parent);
        const keys = getVisitorKeysForNode(this.allVisitorKeys, node);
        if (keys.length < 1) {
            return;
        }
        for (const key of keys) {
            const childOrChildren = node[key];
            if (Array.isArray(childOrChildren)) {
                for (const child of childOrChildren) {
                    this.traverse(child, node);
                }
            }
            else {
                this.traverse(childOrChildren, node);
            }
        }
    }
}
function simpleTraverse(startingNode, options) {
    new SimpleTraverser(options).traverse(startingNode, undefined);
}
exports.simpleTraverse = simpleTraverse;
//# sourceMappingURL=simple-traverse.js.map