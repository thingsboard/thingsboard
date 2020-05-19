/**
 * disallow the deprecated directive replace property
 *
 * This rule disallows the replace attribute in a directive definition object.
 * The replace property of a directive definition object is deprecated since angular 1.3 ([latest angular docs](https://docs.angularjs.org/api/ng/service/$compile).
 *
 * The option `ignoreReplaceFalse` let you ignore directive definitions with replace set to false.
 *
 * @version 0.15.0
 * @category deprecatedAngularFeature
 * @sinceAngularVersion 1.x
 */
'use strict';

var angularRule = require('./utils/angular-rule');

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/no-directive-replace.md'
        },
        schema: [{
            type: 'object',
            properties: {
                ignoreReplaceFalse: {
                    type: 'boolean'
                }
            }
        }]
    },
    create: angularRule(function(context) {
        var options = context.options[0] || {};
        var ignoreReplaceFalse = !!options.ignoreReplaceFalse;

        var potentialReplaceNodes = {};

        function addPotentialReplaceNode(variableName, node) {
            var nodeList = potentialReplaceNodes[variableName] || [];

            nodeList.push({
                name: variableName,
                node: node,
                block: context.getScope().block.body
            });

            potentialReplaceNodes[variableName] = nodeList;
        }

        return {
            'angular?directive': function(callExpressionNode, fnNode) {
                if (!fnNode || !fnNode.body) {
                    return;
                }

                var body = fnNode.body.body ? fnNode.body.body : [fnNode.body];

                body.forEach(function(statement) {
                    if (statement.type === 'ReturnStatement' || statement.type === 'ObjectExpression') {
                        // get potential replace node by argument name of empty string for object expressions
                        var potentialNodes = potentialReplaceNodes[''];
                        if (statement.argument) {
                            potentialNodes = potentialReplaceNodes[statement.argument.name || ''];
                        }

                        if (!potentialNodes) {
                            return;
                        }
                        potentialNodes.forEach(function(report) {
                            var block = statement.parent.type === 'ArrowFunctionExpression' ? statement : statement.parent;

                            // only reports nodes that belong to the same expression
                            if (report.block === block) {
                                context.report(report.node, 'Directive definition property replace is deprecated.');
                            }
                        });
                    }
                });
            },
            AssignmentExpression: function(node) {
                // Only check for literal member property assignments.
                if (node.left.type !== 'MemberExpression') {
                    return;
                }
                // Only check setting properties named 'replace'.
                if (node.left.property.name !== 'replace') {
                    return;
                }
                if (ignoreReplaceFalse && node.right.value === false) {
                    return;
                }
                addPotentialReplaceNode(node.left.object.name, node);
            },
            Property: function(node) {
                // This only checks for objects which have defined a literal restrict property.
                if (node.key.name !== 'replace') {
                    return;
                }
                if (ignoreReplaceFalse === true && node.value.value === false) {
                    return;
                }

                // assumption: Property always belongs to a ObjectExpression
                var objectExpressionParent = node.parent.parent;

                // add to potential replace nodes if the object is defined in a variable
                if (objectExpressionParent.type === 'VariableDeclarator') {
                    addPotentialReplaceNode(objectExpressionParent.id.name, node);
                }

                // report directly if object is part of a return statement and inside a directive body
                if (objectExpressionParent.type === 'ReturnStatement') {
                    addPotentialReplaceNode('', node);
                }

                // report directly if object is part of a arrow function and inside a directive body
                if (objectExpressionParent.type === 'ArrowFunctionExpression') {
                    addPotentialReplaceNode('', node);
                }
            }
        };
    })
};
