/**
 * require and specify a prefix for all component names
 *
 * All your components should have a name starting with the parameter you can define in your config object.
 * The second parameter can be a Regexp wrapped in quotes.
 * You can not prefix your components by "ng" (reserved keyword for AngularJS components) ("component-name":  [2, "ng"])
 *
 * @version 0.1.0
 * @category naming
 * @sinceAngularVersion 1.x
 */
'use strict';

var utils = require('./utils/utils');

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/component-name.md'
        },
        schema: [{
            type: ['string', 'object']
        }]
    },
    create: function(context) {
        if (context.settings.angular === 2) {
            return {};
        }

        return {

            CallExpression: function(node) {
                var prefix = context.options[0];
                var convertedPrefix; // convert string from JSON .eslintrc to regex

                if (prefix === undefined) {
                    return;
                }

                convertedPrefix = utils.convertPrefixToRegex(prefix);

                if (utils.isAngularComponentDeclaration(node)) {
                    var name = node.arguments[0].value;

                    if (name !== undefined && name.indexOf('ng') === 0) {
                        context.report(node, 'The {{component}} component should not start with "ng". This is reserved for AngularJS components', {
                            component: name
                        });
                    } else if (name !== undefined && !convertedPrefix.test(name)) {
                        if (typeof prefix === 'string' && !utils.isStringRegexp(prefix)) {
                            context.report(node, 'The {{component}} component should be prefixed by {{prefix}}', {
                                component: name,
                                prefix: prefix
                            });
                        } else {
                            context.report(node, 'The {{component}} component should follow this pattern: {{prefix}}', {
                                component: name,
                                prefix: prefix.toString()
                            });
                        }
                    }
                }
            }
        };
    }
};
