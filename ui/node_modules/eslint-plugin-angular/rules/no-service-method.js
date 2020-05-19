/**
 * use `factory()` instead of `service()`
 *
 * You should prefer the factory() method instead of service()
 *
 * @styleguideReference {johnpapa} `y040` Services - Singletons
 * @version 0.1.0
 * @category conventions
 * @sinceAngularVersion 1.x
 */
'use strict';

var utils = require('./utils/utils');

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/no-service-method.md'
        },
        schema: []
    },
    create: function(context) {
        return {

            CallExpression: function(node) {
                if (utils.isAngularComponent(node) && node.callee.property && node.callee.property.name === 'service') {
                    context.report(node, 'You should prefer the factory() method instead of service()', {});
                }
            }
        };
    }
};
