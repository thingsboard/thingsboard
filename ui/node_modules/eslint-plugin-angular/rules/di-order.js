/**
 * require DI parameters to be sorted alphabetically
 *
 * Injected dependencies should be sorted alphabetically.
 * If the second parameter is set to false, values which start and end with an underscore those underscores are stripped.
 * This means for example that `_$httpBackend_` goes before `_$http_`.
 *
 * @version 0.6.0
 * @category conventions
 * @sinceAngularVersion 1.x
 */
'use strict';

var angularRule = require('./utils/angular-rule');
var caseSensitive = 'case_sensitive';

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/di-order.md'
        },
        schema: [{
            type: 'boolean'
        }, {
            type: 'string'
        }]
    },
    create: angularRule(function(context) {
        var stripUnderscores = context.options[0] !== false;
        var caseSensitiveOpt = (context.options[1] || caseSensitive) === caseSensitive;

        function checkOrder(callee, fn) {
            if (!fn || !fn.params) {
                return;
            }
            var args = fn.params.map(function(arg) {
                var formattedArg = arg.name;
                if (stripUnderscores) {
                    formattedArg = formattedArg.replace(/^_(.+)_$/, '$1');
                }
                return caseSensitiveOpt ? formattedArg : formattedArg.toLowerCase();
            });
            var sortedArgs = args.slice().sort();
            sortedArgs.some(function(value, index) {
                if (args.indexOf(value) !== index) {
                    context.report(fn, 'Injected values should be sorted alphabetically');
                    return true;
                }
            });
        }

        return {
            'angular?animation': checkOrder,
            'angular?config': checkOrder,
            'angular?controller': checkOrder,
            'angular?directive': checkOrder,
            'angular?factory': checkOrder,
            'angular?filter': checkOrder,
            'angular?inject': checkOrder,
            'angular?run': checkOrder,
            'angular?service': checkOrder,
            'angular?provider': function(callee, providerFn, $get) {
                checkOrder(null, providerFn);
                checkOrder(null, $get);
            }
        };
    })
};
