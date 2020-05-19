/**
 * disallow different rest service and specify one of '$http', '$resource', 'Restangular'
 *
 * Check the service used to send request to your REST API.
 * This rule can have one parameter, with one of the following values: $http, $resource or Restangular ('rest-service': [0, '$http']).
 *
 * @version 0.5.0
 * @category conventions
 * @sinceAngularVersion 1.x
 */
'use strict';

const utils = require('./utils/utils');

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/rest-service.md'
        },
        schema: [{
            type: 'string'
        }]
    },
    create: function(context) {
        let angularObjectList = ['controller', 'filter', 'directive', 'service', 'factory', 'provider'];
        let services = ['$http', '$resource', 'Restangular'];
        let message = 'You should use the same service ({{method}}) for REST API calls';


        return {

            CallExpression: function(node) {
                function checkElement(element) {
                    if (element.type === 'Identifier' && services.indexOf(element.name) >= 0 && context.options[0] !== element.name) {
                        context.report(node, message, {
                            method: context.options[0]
                        });
                    } else if (element.type === 'Literal' && services.indexOf(element.value) >= 0 && context.options[0] !== element.value) {
                        context.report(node, message, {
                            method: context.options[0]
                        });
                    }
                }

                function checkAllElements(elements) {
                    elements.forEach(checkElement);
                }

                var callee = node.callee;

                if (utils.isAngularComponent(node) && callee.type === 'MemberExpression' && angularObjectList.indexOf(callee.property.name) >= 0) {
                    if (context.options[0] === node.arguments[0].value) {
                        return;
                    }

                    if (utils.isFunctionType(node.arguments[1])) {
                        checkAllElements(node.arguments[1].params);
                    }

                    if (utils.isArrayType(node.arguments[1])) {
                        checkAllElements(node.arguments[1].elements);
                    }
                }
            }
        };
    }
};
