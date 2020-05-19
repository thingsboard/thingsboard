/**
 * require and specify a prefix for all directive names
 *
 * All your directives should have a name starting with the parameter you can define in your config object.
 * The second parameter can be a Regexp wrapped in quotes.
 * You can not prefix your directives by "ng" (reserved keyword for AngularJS directives) ("directive-name":  [2, "ng"])
 *
 * @styleguideReference {johnpapa} `y073` Provide a Unique Directive Prefix
 * @styleguideReference {johnpapa} `y126` Directive Component Names
 * @version 0.1.0
 * @category naming
 * @sinceAngularVersion 1.x
 */
'use strict';

var utils = require('./utils/utils');

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/directive-name.md'
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

                if (utils.isAngularDirectiveDeclaration(node)) {
                    var name = node.arguments[0].value;

                    if (name !== undefined && name.indexOf('ng') === 0) {
                        context.report(node, 'The {{directive}} directive should not start with "ng". This is reserved for AngularJS directives', {
                            directive: name
                        });
                    } else if (name !== undefined && !convertedPrefix.test(name)) {
                        if (typeof prefix === 'string' && !utils.isStringRegexp(prefix)) {
                            context.report(node, 'The {{directive}} directive should be prefixed by {{prefix}}', {
                                directive: name,
                                prefix: prefix
                            });
                        } else {
                            context.report(node, 'The {{directive}} directive should follow this pattern: {{prefix}}', {
                                directive: name,
                                prefix: prefix.toString()
                            });
                        }
                    }
                }
            }
        };
    }
};
