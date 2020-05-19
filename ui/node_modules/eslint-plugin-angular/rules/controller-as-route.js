/**
 * require the use of controllerAs in routes or states
 *
 * You should use Angular's controllerAs syntax when defining routes or states.
 *
 * @styleguideReference {johnpapa} `y031` controllerAs Controller Syntax
 * @version 0.1.0
 * @category bestPractice
 * @sinceAngularVersion 1.x
 */
'use strict';

var utils = require('./utils/utils');

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/controller-as-route.md'
        },
        schema: []
    },
    create: function(context) {
        return {
            CallExpression: function(node) {
                var routeObject = null;
                var stateObject = null;
                var hasControllerAs = false;
                var controllerProp = null;
                var stateName = null;

                if (utils.isRouteDefinition(node)) {
                    // second argument in $routeProvider.when('route', {...})
                    routeObject = node.arguments[1];

                    if (routeObject.properties) {
                        routeObject.properties.forEach(function(prop) {
                            if (prop.key.name === 'controller') {
                                controllerProp = prop;

                                if (new RegExp('\\sas\\s').test(prop.value.value)) {
                                    hasControllerAs = true;
                                }
                            }

                            if (prop.key.name === 'controllerAs') {
                                if (hasControllerAs) {
                                    context.report(node, 'The controllerAs syntax is defined twice for the route "{{route}}"', {
                                        route: node.arguments[0].value
                                    });
                                }

                                hasControllerAs = true;
                            }
                        });

                        // if it's a route without a controller, we shouldn't warn about controllerAs
                        if (controllerProp && !hasControllerAs) {
                            context.report(node, 'Route "{{route}}" should use controllerAs syntax', {
                                route: node.arguments[0].value
                            });
                        }
                    }
                } else if (utils.isUIRouterStateDefinition(node)) {
                    // state can be defined like .state({...}) or .state('name', {...})
                    var isObjectState = node.arguments.length === 1;
                    stateObject = isObjectState ? node.arguments[0] : node.arguments[1];

                    if (stateObject && stateObject.properties) {
                        stateObject.properties.forEach(function(prop) {
                            if (prop.key.name === 'controller') {
                                controllerProp = prop;
                            }
                            if (prop.key.name === 'controllerAs') {
                                hasControllerAs = true;
                            }
                            // grab the name from the object for when they aren't using .state('name',...)
                            if (prop.key.name === 'name') {
                                stateName = prop.value.value;
                            }
                        });

                        if (!hasControllerAs && controllerProp) {
                            // if the controller is a string, controllerAs can be set like 'controller as vm'
                            if (controllerProp.value.type !== 'Literal' || controllerProp.value.value.indexOf(' as ') < 0) {
                                context.report(node, 'State "{{state}}" should use controllerAs syntax', {
                                    state: isObjectState ? stateName : node.arguments[0].value
                                });
                            }
                        }
                    }
                }
            }
        };
    }
};
