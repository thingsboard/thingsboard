/**
 * enforce to reference modules with the getter syntax
 *
 * When using a module, avoid using a variable and instead use chaining with the getter syntax
 *
 * @linkDescription disallow to reference modules with variables and require to use the getter syntax instead `angular.module('myModule')`
 * @styleguideReference {johnpapa} `y022` Module - Getters
 * @version 0.1.0
 * @category possibleError
 * @sinceAngularVersion 1.x
 */
'use strict';

var utils = require('./utils/utils');

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/module-getter.md'
        },
        schema: []
    },
    create: function(context) {
        return {

            ExpressionStatement: function(node) {
                if ((utils.isAngularControllerDeclaration(node.expression) ||
                    utils.isAngularFilterDeclaration(node.expression) ||
                    utils.isAngularServiceDeclaration(node.expression) ||
                    utils.isAngularFactoryDeclaration(node.expression) ||
                    utils.isAngularConstantDeclaration(node.expression) ||
                    utils.isAngularValueDeclaration(node.expression) ||
                    utils.isAngularDirectiveDeclaration(node.expression) ||
                    utils.isAngularRunSection(node.expression) ||
                    utils.isAngularConfigSection(node.expression)) &&

                    !utils.isAngularModuleDeclaration(node.expression)) {
                    var calleeObject = node.expression.callee.object;

                    while (calleeObject !== undefined && calleeObject.type === 'CallExpression' && !utils.isAngularModuleGetter(calleeObject)) {
                        calleeObject = calleeObject.callee.object;
                    }
                    if (!(calleeObject !== undefined && calleeObject.type === 'CallExpression' && utils.isAngularModuleGetter(calleeObject))) {
                        context.report(node, 'Avoid using a variable and instead use chaining with the getter syntax.');
                    }
                }
            }
        };
    }
};
