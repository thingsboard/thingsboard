/**
 * require and specify a prefix for all value names
 *
 * All your values should have a name starting with the parameter you can define in your config object.
 * The second parameter can be a Regexp wrapped in quotes.
 * You can not prefix your values by "$" (reserved keyword for AngularJS services) ("value-name":  [2, "ng"])
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
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/value-name.md'
        },
        schema: [{
            type: ['string', 'object']
        }]
    },
    create: function(context) {
        return {

            CallExpression: function(node) {
                var prefix = context.options[0];
                var convertedPrefix; // convert string from JSON .eslintrc to regex
                var isValue;

                if (prefix === undefined) {
                    return;
                }

                convertedPrefix = utils.convertPrefixToRegex(prefix);
                isValue = utils.isAngularValueDeclaration(node);

                if (isValue) {
                    var name = node.arguments[0].value;

                    if (name !== undefined && name.indexOf('$') === 0) {
                        context.report(node, 'The {{value}} value should not start with "$". This is reserved for AngularJS services', {
                            value: name
                        });
                    } else if (name !== undefined && !convertedPrefix.test(name)) {
                        if (typeof prefix === 'string' && !utils.isStringRegexp(prefix)) {
                            context.report(node, 'The {{value}} value should be prefixed by {{prefix}}', {
                                value: name,
                                prefix: prefix
                            });
                        } else {
                            context.report(node, 'The {{value}} value should follow this pattern: {{prefix}}', {
                                value: name,
                                prefix: prefix.toString()
                            });
                        }
                    }
                }
            }
        };
    }
};
