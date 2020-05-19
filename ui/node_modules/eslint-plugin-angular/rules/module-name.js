/**
 * require and specify a prefix for all module names
 *
 * When you create a new module, its name should start with the parameter you can define in your config object.
 * The second parameter can be a Regexp wrapped in quotes.
 * You can not prefix your modules by "ng" (reserved keyword for AngularJS modules) ("module-name":  [2, "ng"])
 *
 * @styleguideReference {johnpapa} `y127` Naming - Modules
 * @version 0.1.0
 * @category naming
 * @sinceAngularVersion 1.x
 */
'use strict';

var utils = require('./utils/utils');

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/module-name.md'
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

                if (prefix === undefined) {
                    return;
                }

                convertedPrefix = utils.convertPrefixToRegex(prefix);

                if (utils.isAngularModuleDeclaration(node)) {
                    var name = node.arguments[0].value;

                    if (name !== undefined && name.indexOf('ng') === 0) {
                        context.report(node, 'The {{module}} module should not start with "ng". This is reserved for AngularJS modules', {
                            module: name
                        });
                    } else if (name !== undefined && !convertedPrefix.test(name)) {
                        if (typeof prefix === 'string' && !utils.isStringRegexp(prefix)) {
                            context.report(node, 'The {{module}} module should be prefixed by {{prefix}}', {
                                module: name,
                                prefix: prefix
                            });
                        } else {
                            context.report(node, 'The {{module}} module should follow this pattern: {{prefix}}', {
                                module: name,
                                prefix: prefix.toString()
                            });
                        }
                    }
                }
            }
        };
    }
};
