/**
 * unittest `inject` functions should only consist of assignments from injected values to describe block variables
 *
 * `inject` functions in unittests should only contain a sorted mapping of injected values to values in the `describe` block with matching names.
 * This way the dependency injection setup is separated from the other setup logic, improving readability of the test.
 *
 * @version 0.15.0
 * @category conventions
 * @sinceAngularVersion 1.x
 */
'use strict';

var angularRule = require('./utils/angular-rule');


module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/dumb-inject.md'
        },
        schema: []
    },
    create: angularRule(function(context) {
        function report(node, name) {
            context.report(node, 'inject functions may only consist of assignments in the form {{name}} = _{{name}}_', {
                name: name || 'myService'
            });
        }

        return {
            'angular?inject': function(callExpression, fn) {
                if (!fn) {
                    return;
                }
                var valid = [];
                // Report bad statement types
                fn.body.body.forEach(function(statement) {
                    if (statement.type !== 'ExpressionStatement') {
                        return report(statement);
                    }
                    if (statement.expression.type !== 'AssignmentExpression') {
                        return report(statement);
                    }
                    if (statement.expression.right.type !== 'Identifier') {
                        return report(statement);
                    }
                    // From this point there is more context on what to report.
                    var name = statement.expression.right.name.replace(/^_(.+)_$/, '$1');
                    if (statement.expression.left.type !== 'Identifier') {
                        return report(statement, name);
                    }
                    if (statement.expression.right.name !== '_' + name + '_') {
                        return report(statement, name);
                    }
                    if (statement.expression.left.name !== name) {
                        return report(statement, name);
                    }
                    // Register valid statements for sort order validation
                    valid.push(statement);
                });
                // Validate the sorting order
                var lastValid;
                valid.forEach(function(statement) {
                    if (!lastValid) {
                        lastValid = statement.expression.left.name;
                        return;
                    }
                    if (statement.expression.left.name.localeCompare(lastValid) !== -1) {
                        lastValid = statement.expression.left.name;
                        return;
                    }
                    context.report(statement, "'{{current}}' must be sorted before '{{previous}}'", {
                        current: statement.expression.left.name,
                        previous: lastValid
                    });
                });
            }
        };
    })
};
