/**
 * disallow assignments to `$scope` in controllers
 *
 * You should not set properties on $scope in controllers.
 * Use controllerAs syntax and add data to 'this'.
 * The second parameter can be a Regexp for identifying controller functions (when using something like Browserify)
 *
 * @styleguideReference {johnpapa} `y031` controllerAs Controller Syntax
 * @version 0.1.0
 * @category bestPractice
 * @sinceAngularVersion 1.x
 */
'use strict';

var utils = require('./utils/utils');

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/controller-as.md'
        },
        schema: [{
            type: ['object', 'string']
        }]
    },
    create: function(context) {
        var badStatements = [];
        var controllerFunctions = [];

        // If your Angular code is written so that controller functions are in
        // separate files from your .controller() calls, you can specify a regex for your controller function names
        var controllerNameMatcher = context.options[0];
        if (controllerNameMatcher && utils.isStringRegexp(controllerNameMatcher)) {
            controllerNameMatcher = utils.convertStringToRegex(controllerNameMatcher);
        }

        // check node against known controller functions or pattern if specified
        function isControllerFunction(node) {
            return controllerFunctions.indexOf(node) >= 0 ||
                (controllerNameMatcher && (node.type === 'FunctionExpression' || node.type === 'FunctionDeclaration') &&
                    node.id && controllerNameMatcher.test(node.id.name));
        }

        // for each of the bad uses, find any parent nodes that are controller functions
        function reportBadUses() {
            if (controllerFunctions.length > 0 || controllerNameMatcher) {
                badStatements.forEach(function(item) {
                    item.parents.forEach(function(parent) {
                        if (isControllerFunction(parent)) {
                            context.report(item.stmt, 'You should not set properties on $scope in controllers. Use controllerAs syntax and add data to "this"');
                        }
                    });
                });
            }
        }

        return {
            // Looking for .controller() calls here and getting the associated controller function
            'CallExpression:exit': function(node) {
                if (utils.isAngularControllerDeclaration(node)) {
                    controllerFunctions.push(utils.getControllerDefinition(context, node));
                }
            },
            // statements are checked here for bad uses of $scope
            ExpressionStatement: function(stmt) {
                if (stmt.expression.type === 'AssignmentExpression' &&
                    stmt.expression.left.object &&
                    stmt.expression.left.object.name === '$scope' &&
                    utils.scopeProperties.indexOf(stmt.expression.left.property.name) < 0) {
                    badStatements.push({parents: context.getAncestors(), stmt: stmt});
                } else if (stmt.expression.type === 'CallExpression' &&
                    stmt.expression.callee.object &&
                    stmt.expression.callee.object.name === '$scope' &&
                    utils.scopeProperties.indexOf(stmt.expression.callee.property.name) < 0) {
                    badStatements.push({parents: context.getAncestors(), stmt: stmt});
                }
            },
            'Program:exit': function() {
                reportBadUses();
            }
        };
    }
};
