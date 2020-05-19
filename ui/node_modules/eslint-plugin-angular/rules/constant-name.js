/**
 * require and specify a prefix for all constant names
 *
 * All your constants should have a name starting with the parameter you can define in your config object.
 * The second parameter can be a Regexp wrapped in quotes.
 * You can not prefix your constants by "$" (reserved keyword for AngularJS services) ("constant-name":  [2, "ng"])
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
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/constant-name.md'
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
                var isConstant;

                if (prefix === undefined) {
                    return;
                }

                convertedPrefix = utils.convertPrefixToRegex(prefix);
                isConstant = utils.isAngularConstantDeclaration(node);

                if (isConstant) {
                    var name = node.arguments[0].value;

                    if (name !== undefined && name.indexOf('$') === 0) {
                        context.report(node, 'The {{constant}} constant should not start with "$". This is reserved for AngularJS services', {
                            constant: name
                        });
                    } else if (name !== undefined && !convertedPrefix.test(name)) {
                        if (typeof prefix === 'string' && !utils.isStringRegexp(prefix)) {
                            context.report(node, 'The {{constant}} constant should be prefixed by {{prefix}}', {
                                constant: name,
                                prefix: prefix
                            });
                        } else {
                            context.report(node, 'The {{constant}} constant should follow this pattern: {{prefix}}', {
                                constant: name,
                                prefix: prefix.toString()
                            });
                        }
                    }
                }
            }
        };
    }
};
