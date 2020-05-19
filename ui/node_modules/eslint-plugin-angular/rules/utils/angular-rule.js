'use strict';

module.exports = angularRule;


/**
 * Method names from an AngularJS module which can be chained.
 */
var angularChainableNames = [
    'animation',
    'component',
    'config',
    'constant',
    'controller',
    'directive',
    'factory',
    'filter',
    'provider',
    'run',
    'service',
    'value'
];


/**
 * An angularRule defines a simplified interface for AngularJS component based rules.
 *
 * A full rule definition containing rules for all supported rules looks like this:
 * ```js
 * module.exports = angularRule(function(context) {
 *   return {
 *     'angular?animation': function(configCallee, configFn) {},
 *     'angular?component': function(componentCallee, componentObj) {},
 *     'angular?config': function(configCallee, configFn) {},
 *     'angular?controller': function(controllerCallee, controllerFn) {},
 *     'angular?directive': function(directiveCallee, directiveFn) {},
 *     'angular?factory': function(factoryCallee, factoryFn) {},
 *     'angular?filter': function(filterCallee, filterFn) {},
 *     'angular?inject': function(injectCallee, injectFn) {},  // inject() calls from angular-mocks
 *     'angular?run': function(runCallee, runFn) {},
 *     'angular?service': function(serviceCallee, serviceFn) {},
 *     'angular?provider': function(providerCallee, providerFn, provider$getFn) {}
 *   };
 * })
 * ```
 */
function angularRule(ruleDefinition) {
    var angularComponents;
    var angularModuleCalls;
    var angularModuleIdentifiers;
    var angularChainables;
    var injectCalls;

    return wrapper;

    function reset() {
        angularComponents = [];
        angularModuleCalls = [];
        angularModuleIdentifiers = [];
        angularChainables = [];
        injectCalls = [];
    }

    /**
     * A wrapper around the rule definition.
     */
    function wrapper(context) {
        reset();
        var ruleObject = ruleDefinition(context);
        injectCall(ruleObject, context, 'CallExpression:exit', checkCallee);
        injectCall(ruleObject, context, 'Program:exit', callAngularRules);
        return ruleObject;
    }

    /**
     * Makes sure an extra function gets called after custom defined rule has run.
     */
    function injectCall(ruleObject, context, propName, toCallAlso) {
        var original = ruleObject[propName];
        ruleObject[propName] = callBoth;

        function callBoth(node) {
            if (original) {
                original.call(ruleObject, node);
            }
            toCallAlso(ruleObject, context, node);
        }
    }

    /**
     * Collect expressions from an entire Angular module call chain expression statement and inject calls.
     *
     * This collects the following nodes:
     * ```js
     * angular.module()
     *         ^^^^^^
     * .animation('', function() {})
     *  ^^^^^^^^^     ^^^^^^^^^^
     * .component('', {})
     *  ^^^^^^^^^
     * .config(function() {})
     *  ^^^^^^ ^^^^^^^^^^
     * .constant()
     *  ^^^^^^^^
     * .controller('', function() {})
     *  ^^^^^^^^^^     ^^^^^^^^^^
     * .directive('', function() {})
     *  ^^^^^^^^^     ^^^^^^^^^^
     * .factory('', function() {})
     *  ^^^^^^^     ^^^^^^^^^^
     * .filter('', function() {})
     *  ^^^^^^     ^^^^^^^^^^
     * .provider('', function() {})
     *  ^^^^^^^^     ^^^^^^^^^^
     * .run('', function() {})
     *  ^^^     ^^^^^^^^^^
     * .service('', function() {})
     *  ^^^^^^^     ^^^^^^^^^^
     * .value();
     *  ^^^^^
     *
     * inject(function() {})
     * ^^^^^^ ^^^^^^^^^^
     * ```
     */
    function checkCallee(ruleObject, context, callExpressionNode) {
        var callee = callExpressionNode.callee;
        if (callee.type === 'Identifier') {
            if (callee.name === 'inject') {
                // inject()
                // ^^^^^^
                injectCalls.push({
                    callExpression: callExpressionNode,
                    fn: findFunctionByNode(callExpressionNode, context.getScope())
                });
            }
            return;
        }
        if (callee.type === 'MemberExpression') {
            if (callee.object.name === 'angular' && callee.property.name === 'module') {
                // angular.module()
                //         ^^^^^^
                angularModuleCalls.push(callExpressionNode);
            } else if (angularChainableNames.indexOf(callee.property.name !== -1) && (angularModuleCalls.indexOf(callee.object) !== -1 || angularChainables.indexOf(callee.object) !== -1)) {
                // angular.module().factory().controller()
                //                  ^^^^^^^   ^^^^^^^^^^
                angularChainables.push(callExpressionNode);
                angularComponents.push({
                    callExpression: callExpressionNode,
                    fn: findFunctionByNode(callExpressionNode, context.getScope())
                });
            } else if (callee.object.type === 'Identifier') {
                // var app = angular.module(); app.factory()
                //                                 ^^^^^^^
                var scope = context.getScope();
                var isAngularModule = scope.variables.some(function(variable) {
                    if (callee.object.name !== variable.name) {
                        return false;
                    }
                    return variable.identifiers.some(function(id) {
                        return angularModuleIdentifiers.indexOf(id) !== -1;
                    });
                });
                if (isAngularModule) {
                    angularChainables.push(callExpressionNode);
                    angularComponents.push({
                        callExpression: callExpressionNode,
                        fn: findFunctionByNode(callExpressionNode, context.getScope())
                    });
                } else {
                    return;
                }
            } else {
                return;
            }
            if (callExpressionNode.parent.type === 'VariableDeclarator') {
                // var app = angular.module()
                //     ^^^
                angularModuleIdentifiers.push(callExpressionNode.parent.id);
            }
        }
    }

    /**
     * Find the function expression or function declaration by an Angular component callee.
     */
    function findFunctionByNode(callExpressionNode, scope) {
        var node;
        if (callExpressionNode.callee.type === 'Identifier') {
            node = callExpressionNode.arguments[0];
        } else if (callExpressionNode.callee.property.name === 'run' || callExpressionNode.callee.property.name === 'config') {
            node = callExpressionNode.arguments[0];
        } else {
            node = callExpressionNode.arguments[1];
        }
        if (!node) {
            return;
        }
        if (node.type === 'ArrayExpression') {
            node = node.elements[node.elements.length - 1] || {};
        }
        if (node.type === 'FunctionExpression' || node.type === 'ArrowFunctionExpression' || node.type === 'FunctionDeclaration') {
            return node;
        }
        if (node.type !== 'Identifier') {
            return;
        }

        var func;
        scope.variables.some(function(variable) {
            if (variable.name === node.name) {
                variable.defs.forEach(function(def) {
                    if (def.node.type === 'FunctionDeclaration') {
                        func = def.node;
                        return true;
                    }
                });
                return true;
            }
        });
        return func;
    }

    /**
     * Call the Angular specific rules defined by the rule definition.
     */
    function callAngularRules(ruleObject, context) {
        angularComponents.forEach(function(component) {
            var name = component.callExpression.callee.property.name;
            var fn = ruleObject['angular?' + name];
            if (!fn) {
                return;
            }
            fn.apply(ruleObject, assembleArguments(component, context));
        });
        var injectRule = ruleObject['angular?inject'];
        if (injectRule) {
            injectCalls.forEach(function(node) {
                injectRule.call(ruleObject, node.CallExpression, node.fn);
            });
        }
    }

    /**
     * Assemble the arguments for an Angular callee check.
     */
    function assembleArguments(node) {
        switch (node.callExpression.callee.property.name) {
            case 'animation':
            case 'component':
            case 'config':
            case 'controller':
            case 'directive':
            case 'factory':
            case 'filter':
            case 'run':
            case 'service':
                return [node.callExpression.callee, node.fn];
            case 'provider':
                return assembleProviderArguments(node);
        }
    }

    /**
     * Assemble arguments for a provider rule.
     *
     * On top of a regular Angular component rule, the provider rule gets called with the $get function as its 3rd argument.
     */
    function assembleProviderArguments(node) {
        return [node.callExpression, node.fn, findProviderGet(node.fn)];
    }

    /**
     * Find the $get function of a provider based on the provider function body.
     */
    function findProviderGet(providerFn) {
        if (!providerFn) {
            return;
        }
        var getFn;
        providerFn.body.body.some(function(statement) {
            var expression = statement.expression;
            if (!expression || expression.type !== 'AssignmentExpression') {
                return;
            }
            if (expression.left.type === 'MemberExpression' && expression.left.property.name === '$get') {
                getFn = expression.right;
                return true;
            }
        });
        if (!getFn) {
            return;
        }
        if (getFn.type === 'ArrayExpression') {
            return getFn.elements[getFn.elements.length - 1];
        }
        return getFn;
    }
}
