/**
 * require and specify a prefix for all filter names
 *
 * All your filters should have a name starting with the parameter you can define in your config object.
 * The second parameter can be a Regexp wrapped in quotes.
 * ("filter-name":  [2, "ng"])
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
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/filter-name.md'
        },
        schema: [{
            type: ['string', 'object']
        }]
    },
    create: function(context) {
        return {

            CallExpression: function(node) {
                var prefix = context.options[0];
                var convertedPrefix; // convert string from JSON .eslintrc to regex;
                if (prefix === undefined) {
                    return;
                }

                convertedPrefix = utils.convertPrefixToRegex(prefix);

                if (utils.isAngularFilterDeclaration(node)) {
                    var name = node.arguments[0].value;

                    if (name !== undefined && !convertedPrefix.test(name)) {
                        if (typeof prefix === 'string' && !utils.isStringRegexp(prefix)) {
                            context.report(node, 'The {{filter}} filter should be prefixed by {{prefix}}', {
                                filter: name,
                                prefix: prefix
                            });
                        } else {
                            context.report(node, 'The {{filter}} filter should follow this pattern: {{prefix}}', {
                                filter: name,
                                prefix: prefix.toString()
                            });
                        }
                    }
                }
            }
        };
    }
};

