/**
 * require and specify a prefix for all service names
 *
 * All your services should have a name starting with the parameter you can define in your config object.
 * The second parameter can be a Regexp wrapped in quotes.
 * You can not prefix your services by "$" (reserved keyword for AngularJS services) ("service-name":  [2, "ng"])
 *
 * If the oldBehavior is true (default value), this rule will check the name of all services defined with the different methods
 * provided by the framework : provider, service, factory, ... If this parameter is false, only services defined with the
 * service method will be checked.
 *
 * @styleguideReference {johnpapa} `y125` Naming - Factory and Service Names
 * @version 0.1.0
 * @category naming
 * @sinceAngularVersion 1.x
 */
'use strict';


var utils = require('./utils/utils');

/**
 * @param {Array.<*>} options
 * @returns {?string}
 */
function getPrefixFromOptions(options) {
    return options.find(function(option) {
        return ['String', 'RegExp', 'Null', 'Undefined'].indexOf(utils.getToStringTagType(option)) !== -1;
    });
}

/**
 * @param {Array.<*>} options
 * @returns {Object}
 */
function getConfig(options) {
    var config = options.find(function(option) {
        return utils.getToStringTagType(option) === 'Object';
    });

    config = config || {};
    if (typeof config.oldBehavior !== 'boolean') {
        config = Object.assign({
            oldBehavior: true
        });
    }

    return config;
}

/**
 * Used only by `ForDeprecatedBehavior()` for making sure it was run only one time
 * @type {boolean}
 */
var didWarnForDeprecatedBehavior = false;

/**
 * Warn if API is deprecated
 * @param {Array.<*>} options
 */
function warnForDeprecatedBehavior(options) {
    if (didWarnForDeprecatedBehavior) {
        return;
    }
    didWarnForDeprecatedBehavior = true;

    var config = getConfig(options);

    /* istanbul ignore if  */
    if (config.oldBehavior) {
        // eslint-disable-next-line
        console.warn('The rule `angular/service-name` will be split up to different rules in the next version. Please read the docs for more information');
    }
}

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/service-name.md'
        },
        schema: [{
            type: ['string', 'object']
        }, {
            type: 'object'
        }]
    },
    create: function(context) {
        warnForDeprecatedBehavior(context.options);

        return {

            CallExpression: function(node) {
                var config = getConfig(context.options);
                var prefix = getPrefixFromOptions(context.options);
                var convertedPrefix; // convert string from JSON .eslintrc to regex
                var isService;

                if (prefix === undefined) {
                    return;
                }

                convertedPrefix = utils.convertPrefixToRegex(prefix);

                if (config.oldBehavior) {
                    isService = utils.isAngularServiceDeclarationDeprecated(node);
                } else {
                    isService = utils.isAngularServiceDeclaration(node);
                }

                if (isService) {
                    var name = node.arguments[0].value;

                    if (name !== undefined && name.indexOf('$') === 0) {
                        context.report(node, 'The {{service}} service should not start with "$". This is reserved for AngularJS services', {
                            service: name
                        });
                    } else if (name !== undefined && !convertedPrefix.test(name)) {
                        if (typeof prefix === 'string' && !utils.isStringRegexp(prefix)) {
                            context.report(node, 'The {{service}} service should be prefixed by {{prefix}}', {
                                service: name,
                                prefix: prefix
                            });
                        } else {
                            context.report(node, 'The {{service}} service should follow this pattern: {{prefix}}', {
                                service: name,
                                prefix: prefix.toString()
                            });
                        }
                    }
                }
            }
        };
    }
};
