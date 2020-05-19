/**
 * require and specify a capture variable for `this` in controllers
 *
 * You should use a capture variable for 'this' when using the controllerAs syntax.
 * The second parameter specifies the capture variable you want to use in your application.
 * The third parameter can be a Regexp for identifying controller functions (when using something like Browserify)
 *
 * ### Options
 *
 * - The name that should be used for the view model.
 *
 * @styleguideReference {johnpapa} `y032` controllerAs with vm
 * @version 0.1.0
 * @category bestPractice
 * @sinceAngularVersion 1.x
 */
'use strict';

var utils = require('./utils/utils');

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/controller-as-vm.md'
        },
        schema: [{
            type: 'string'
        }, {
            type: 'string'
        }]
    },
    create: function(context) {
        var badStatements = [];
        var badCaptureStatements = [];
        var controllerFunctions = [];

        var viewModelName = context.options[0] || 'vm';
        // If your Angular code is written so that controller functions are in
        // separate files from your .controller() calls, you can specify a regex for your controller function names
        var controllerNameMatcher = context.options[1];
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
                badCaptureStatements.forEach(function(item) {
                    item.parents.filter(isControllerFunction).forEach(function() {
                        context.report(item.stmt, 'You should assign "this" to a consistent variable across your project: {{capture}}',
                            {
                                capture: viewModelName
                            }
                        );
                    });
                });
                badStatements.forEach(function(item) {
                    item.parents.filter(isControllerFunction).forEach(function() {
                        context.report(item.stmt, 'You should not use "this" directly. Instead, assign it to a variable called "{{capture}}"',
                            {
                                capture: viewModelName
                            }
                        );
                    });
                });
            }
        }

        function isClassDeclaration(ancestors) {
            return ancestors.findIndex(function(ancestor) {
                return ancestor.type === 'ClassDeclaration';
            }) > -1;
        }

        return {
            // Looking for .controller() calls here and getting the associated controller function
            'CallExpression:exit': function(node) {
                if (utils.isAngularControllerDeclaration(node)) {
                    controllerFunctions.push(utils.getControllerDefinition(context, node));
                }
            },
            // statements are checked here for bad uses of $scope
            ThisExpression: function(stmt) {
                var parents = context.getAncestors();
                if (!isClassDeclaration(parents)) {
                    if (stmt.parent.type === 'VariableDeclarator') {
                        if (!stmt.parent.id || stmt.parent.id.name !== viewModelName) {
                            badCaptureStatements.push({parents: parents, stmt: stmt});
                        }
                    } else {
                        badStatements.push({parents: parents, stmt: stmt});
                    }
                }
            },
            'Program:exit': function() {
                reportBadUses();
            }
        };
    }
};
