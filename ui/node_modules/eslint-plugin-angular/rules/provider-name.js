/**
 * require and specify a prefix for all provider names
 *
 * All your providers should have a name starting with the parameter you can define in your config object.
 * The second parameter can be a Regexp wrapped in quotes.
 * You can not prefix your providers by "$" (reserved keyword for AngularJS services) ("provider-name":  [2, "ng"])
 **
 * @styleguideReference {johnpapa} `y125` Naming - Factory and Service Names
 * @version 0.1.0
 * @category naming
 */
'use strict';


var utils = require('./utils/utils');


module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/provider-name.md'
        },
        schema: [{
            type: ['string', 'object']
        }, {
            type: 'object'
        }]
    },
    create: function(context) {
        return {

            CallExpression: function(node) {
                var prefix = context.options[0];
                var convertedPrefix; // convert string from JSON .eslintrc to regex
                var isProvider;

                if (prefix === undefined) {
                    return;
                }

                convertedPrefix = utils.convertPrefixToRegex(prefix);
                isProvider = utils.isAngularProviderDeclaration(node);

                if (isProvider) {
                    var name = node.arguments[0].value;

                    if (name !== undefined && name.indexOf('$') === 0) {
                        context.report(node, 'The {{provider}} provider should not start with "$". This is reserved for AngularJS services', {
                            provider: name
                        });
                    } else if (name !== undefined && !convertedPrefix.test(name)) {
                        if (typeof prefix === 'string' && !utils.isStringRegexp(prefix)) {
                            context.report(node, 'The {{provider}} provider should be prefixed by {{prefix}}', {
                                provider: name,
                                prefix: prefix
                            });
                        } else {
                            context.report(node, 'The {{provider}} provider should follow this pattern: {{prefix}}', {
                                provider: name,
                                prefix: prefix.toString()
                            });
                        }
                    }
                }
            }
        };
    }
};
