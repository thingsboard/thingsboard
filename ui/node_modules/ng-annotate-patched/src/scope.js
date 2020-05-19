// scope.js
// MIT licensed, see LICENSE file
// Copyright (c) 2013-2016 Olov Lassus <olov.lassus@gmail.com>

"use strict";

const assert = require("assert");

module.exports = class Scope {
    constructor(args) {
        assert(["hoist", "block", "catch-block"].includes(args.kind));
        assert(args.node !== null && typeof args.node === "object");
        assert(args.parent === null || typeof args.parent === "object");

        // kind === "hoist": function scopes, program scope, injected globals
        // kind === "block": ES6 block scopes
        // kind === "catch-block": catch block scopes
        this.kind = args.kind;

        // the AST node the block corresponds to
        this.node = args.node;

        // parent scope
        this.parent = args.parent;

        // children scopes for easier traversal (populated internally)
        this.children = [];

        // scope declarations. decls[variable_name] = {
        //     kind: "fun" for functions,
        //           "param" for function parameters,
        //           "caught" for catch parameter
        //           "var",
        //           "const",
        //           "let"
        //     node: the AST node the declaration corresponds to
        //     from: source code index from which it is visible at earliest
        //           (only stored for "const", "let" [and "var"] nodes)
        // }
        this.decls = new Map();

        // names of all variables declared outside this hoist scope but
        // referenced in this scope (immediately or in child).
        // only stored on hoist scopes for efficiency
        // (because we currently generate lots of empty block scopes)
        this.propagates = (this.kind === "hoist" ? new Set() : null);

        // scopes register themselves with their parents for easier traversal
        if (this.parent) {
            this.parent.children.push(this);
        }
    }

    print(indent) {
        indent = indent || 0;
        const scope = this;
        const names = this.decls.keys().map(name => {
            return `${name} [${scope.decls.get(name).kind}]`;
        }).join(", ");
        const propagates = this.propagates ? this.propagates.items().join(", ") : "";
        console.log(`${fmt.repeat(" ", indent)}${this.node.type}: ${names}. propagates: ${propagates}`);
        this.children.forEach(c => {
            c.print(indent + 2);
        });
    }

    add(name, kind, node, referableFromPos) {
        assert(["fun", "param", "var", "caught", "const", "let"].includes(kind));

        // function isConstLet(kind) {
        //     return ["const", "let"].includes(kind);
        // }

        let scope = this;

        // search nearest hoist-scope for fun, param and var's
        // const, let and caught variables go directly in the scope (which may be hoist, block or catch-block)
        if (["fun", "param", "var"].includes(kind)) {
            while (scope.kind !== "hoist") {
                // if (scope.decls.has(name) && isConstLet(scope.decls.get(name).kind)) { // could be caught
                //     return error(getline(node), "{0} is already declared", name);
                // }
                scope = scope.parent;
            }
        }
        // name exists in scope and either new or existing kind is const|let => error
        // if (scope.decls.has(name) && (isConstLet(scope.decls.get(name).kind) || isConstLet(kind))) {
        //     return error(getline(node), "{0} is already declared", name);
        // }

        const declaration = {
            kind,
            node,
        };
        if (referableFromPos) {
            assert(["var", "const", "let"].includes(kind));
            declaration.from = referableFromPos;
        }
        scope.decls.set(name, declaration);
    }

    getKind(name) {
        assert(typeof name === "string");
        const decl = this.decls.get(name);
        return decl ? decl.kind : null;
    }

    getNode(name) {
        assert(typeof name === "string");
        const decl = this.decls.get(name);
        return decl ? decl.node : null;
    }

    getFromPos(name) {
        assert(typeof name === "string");
        const decl = this.decls.get(name);
        return decl ? decl.from : null;
    }

    hasOwn(name) {
        return this.decls.has(name);
    }

    remove(name) {
        return this.decls.remove(name);
    }

    doesPropagate(name) {
        return this.propagates.has(name);
    }

    markPropagates(name) {
        this.propagates.add(name);
    }

    closestHoistScope() {
        let scope = this;
        while (scope.kind !== "hoist") {
            scope = scope.parent;
        }
        return scope;
    }

    lookup(name) {
        for (let scope = this; scope; scope = scope.parent) {
            if (scope.decls.has(name)) {
                return scope;
            } else if (scope.kind === "hoist") {
                scope.propagates.add(name);
            }
        }
        return null;
    }
};
