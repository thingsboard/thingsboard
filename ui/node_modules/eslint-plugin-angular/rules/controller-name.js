/**
 * require and specify a prefix for all controller names
 *
 * All your controllers should have a name starting with the parameter you can define in your config object.
 * The second parameter can be a Regexp wrapped in quotes.
 * ("controller-name":  [2, "ng"])
 *
 * @styleguideReference {johnpapa} `y123` Controller Names
 * @styleguideReference {johnpapa} `y124` Controller Name Suffix
 * @version 0.1.0
 * @category naming
 * @sinceAngularVersion 1.x
 */
'use strict';

var utils = require('./utils/utils');

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/controller-name.md'
        },
        schema: [
            {
                type: 'string'
            }
        ]
    },
    create: function(context) {
        return {

            CallExpression: function(node) {
                var prefix = context.options[0] || '/^[A-Z][a-zA-Z0-9]*Controller$/';
                var convertedPrefix; // convert string from JSON .eslintrc to regex

                convertedPrefix = utils.convertPrefixToRegex(prefix);

                var callee = node.callee;
                if (callee.type === 'MemberExpression' && callee.property.name === 'controller') {
                    /**
                     * Allow the usage of element.controller() and element.controller('directiveName') in unittests
                     */
                    if (node.arguments.length < 2) {
                        return;
                    }

                    var name = node.arguments[0].value;

                    if (name !== undefined && !convertedPrefix.test(name)) {
                        if (typeof prefix === 'string' && !utils.isStringRegexp(prefix)) {
                            context.report(node, 'The {{controller}} controller should be prefixed by {{prefix}}', {
                                controller: name,
                                prefix: prefix
                            });
                        } else {
                            context.report(node, 'The {{controller}} controller should follow this pattern: {{prefix}}', {
                                controller: name,
                                prefix: prefix.toString()
                            });
                        }
                    }
                }
            }
        };
    }
};
