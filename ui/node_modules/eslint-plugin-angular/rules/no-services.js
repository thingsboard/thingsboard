/**
 * disallow DI of specified services
 *
 * Some services should be used only in a specific AngularJS service (Ajax-based service for example), in order to follow the separation of concerns paradigm.
 * The second parameter specifies the services.
 * The third parameter can be a list of angular objects (controller, factory, etc.).
 * Or second parameter can be an object, where keys are angular object names and value is a list of services (like {controller: ['$http'], factory: ['$q']})
 *
 * @linkDescription disallow DI of specified services for other angular components (`$http` for controllers, filters and directives)
 * @version 0.1.0
 * @category bestPractice
 * @sinceAngularVersion 1.x
 */
'use strict';

const utils = require('./utils/utils');

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/no-services.md'
        },
        schema: [{
            type: ['array', 'object']
        }, {
            type: 'array'
        }]
    },
    create: function(context) {
        let angularObjectList = ['controller', 'filter', 'directive'];
        let badServices = [];
        let map;
        let message = 'REST API calls should be implemented in a specific service';

        function isArray(item) {
            return Object.prototype.toString.call(item) === '[object Array]';
        }

        function isObject(item) {
            return Object.prototype.toString.call(item) === '[object Object]';
        }

        if (context.options[0] === undefined) {
            badServices = [/\$http/, /\$resource/, /Restangular/, /\$q/, /\$filter/];
        }

        if (isArray(context.options[0])) {
            badServices = context.options[0];
        }

        if (isArray(context.options[1])) {
            angularObjectList = context.options[1];
        }

        if (isObject(context.options[0])) {
            map = context.options[0];

            let result = [];
            let prop;

            for (prop in map) {
                if (map.hasOwnProperty(prop)) {
                    result.push(prop);
                }
            }

            angularObjectList = result;
        }

        function isSetBedService(serviceName, angularObjectName) {
            if (map) {
                return map[angularObjectName].find(object => utils.convertPrefixToRegex(object).test(serviceName));
            }
            return badServices.find(object => utils.convertPrefixToRegex(object).test(serviceName));
        }

        return {

            CallExpression: function(node) {
                let callee = node.callee;

                if (utils.isAngularComponent(node) && callee.type === 'MemberExpression' && angularObjectList.indexOf(callee.property.name) >= 0) {
                    if (utils.isFunctionType(node.arguments[1])) {
                        node.arguments[1].params.forEach(function(service) {
                            if (service.type === 'Identifier' && isSetBedService(service.name, callee.property.name)) {
                                context.report(node, message + ' (' + service.name + ' in ' + callee.property.name + ')', {});
                            }
                        });
                    }

                    if (utils.isArrayType(node.arguments[1])) {
                        node.arguments[1].elements.forEach(function(service) {
                            if (service.type === 'Literal' && isSetBedService(service.value, callee.property.name)) {
                                context.report(node, message + ' (' + service.value + ' in ' + callee.property.name + ')', {});
                            }
                        });
                    }
                }
            }
        };
    }
};
