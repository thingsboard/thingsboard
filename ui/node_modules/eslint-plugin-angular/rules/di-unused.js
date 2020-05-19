/**
 * disallow unused DI parameters
 *
 * Unused dependencies should not be injected.
 *
 * @version 0.8.0
 * @category bestPractice
 * @sinceAngularVersion 1.x
 */
'use strict';

var angularRule = require('./utils/angular-rule');


module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/di-unused.md'
        },
        schema: []
    },
    create: angularRule(function(context) {
        // Keeps track of visited scopes in the collectAngularScopes function to prevent infinite recursion on circular references.
        var visitedScopes = [];

        // This collects the variable scopes for the injectable functions which have been collected.
        function collectAngularScopes(scope) {
            if (visitedScopes.indexOf(scope) === -1) {
                visitedScopes.push(scope);
                scope.childScopes.forEach(function(child) {
                    collectAngularScopes(child);
                });
            }
        }

        function reportUnusedVariables(callee, fn) {
            if (!fn) {
                return;
            }
            visitedScopes.some(function(scope) {
                if (scope.block !== fn) {
                    return;
                }

                if (scope.type === 'function-expression-name') {
                    return;
                }

                scope.variables.forEach(function(variable) {
                    if (variable.name === 'arguments') {
                        return;
                    }
                    if (fn.params.indexOf(variable.identifiers[0]) === -1) {
                        return;
                    }
                    if (variable.references.length === 0) {
                        context.report(fn, 'Unused injected value {{name}}', variable);
                    }
                });
                return true;
            });
        }

        return {
            'angular?animation': reportUnusedVariables,
            'angular?config': reportUnusedVariables,
            'angular?controller': reportUnusedVariables,
            'angular?directive': reportUnusedVariables,
            'angular?factory': reportUnusedVariables,
            'angular?filter': reportUnusedVariables,
            'angular?inject': reportUnusedVariables,
            'angular?run': reportUnusedVariables,
            'angular?service': reportUnusedVariables,
            'angular?provider': function(callee, providerFn, $get) {
                reportUnusedVariables(null, providerFn);
                reportUnusedVariables(null, $get);
            },

            'Program:exit': function() {
                var globalScope = context.getScope();
                collectAngularScopes(globalScope);
            }
        };
    })
};
