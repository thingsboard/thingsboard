/**
 * enforce use of`angular.fromJson` and 'angular.toJson'
 *
 * You should use angular.fromJson or angular.toJson instead of JSON.parse and JSON.stringify
 *
 * @linkDescription use `angular.fromJson` and 'angular.toJson' instead of `JSON.parse` and `JSON.stringify`
 * @version 0.1.0
 * @category angularWrapper
 * @sinceAngularVersion 1.x
 */
'use strict';

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/json-functions.md'
        },
        schema: []
    },
    create: function(context) {
        return {

            MemberExpression: function(node) {
                if (node.object.name === 'JSON') {
                    if (node.property.name === 'stringify') {
                        context.report(node, 'You should use the angular.toJson method instead of JSON.stringify', {});
                    } else if (node.property.name === 'parse') {
                        context.report(node, 'You should use the angular.fromJson method instead of JSON.parse', {});
                    }
                }
            }
        };
    }
};
