/**
 * disallow empty controllers
 *
 * If you have one empty controller, maybe you have linked it in your Router configuration or in one of your views.
 * You can remove this declaration because this controller is useless
 *
 * @version 0.1.0
 * @category bestPractice
 * @sinceAngularVersion 1.x
 */
'use strict';

var utils = require('./utils/utils');

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/empty-controller.md'
        },
        schema: []
    },
    create: function(context) {
        function report(node, name) {
            context.report(node, 'The {{ctrl}} controller is useless because empty. You can remove it from your Router configuration or in one of your view', {
                ctrl: name
            });
        }

        return {

            CallExpression: function(node) {
                if (utils.isAngularControllerDeclaration(node)) {
                    var name = node.arguments[0].value;

                    var fn = node.arguments[1];
                    if (utils.isArrayType(node.arguments[1])) {
                        fn = node.arguments[1].elements[node.arguments[1].elements.length - 1];
                    }
                    if (utils.isFunctionType(fn) && utils.isEmptyFunction(fn)) {
                        report(node, name);
                    }
                }
            }
        };
    }
};
