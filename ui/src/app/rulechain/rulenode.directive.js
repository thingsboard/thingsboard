/*
 * Copyright © 2016-2018 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import './rulenode.scss';

/* eslint-disable import/no-unresolved, import/default */

import ruleNodeFieldsetTemplate from './rulenode-fieldset.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function RuleNodeDirective($compile, $templateCache, ruleChainService, types) {
    var linker = function (scope, element) {
        var template = $templateCache.get(ruleNodeFieldsetTemplate);
        element.html(template);

        scope.types = types;

        scope.params = {
            targetRuleChainId: null
        };

        scope.$watch('ruleNode', function() {
            if (scope.ruleNode && scope.ruleNode.component.type == types.ruleNodeType.RULE_CHAIN.value) {
                scope.params.targetRuleChainId = scope.ruleNode.targetRuleChainId;
                watchTargetRuleChain();
            } else {
                if (scope.targetRuleChainWatch) {
                    scope.targetRuleChainWatch();
                    scope.targetRuleChainWatch = null;
                }
            }
        });

        function watchTargetRuleChain() {
            scope.targetRuleChainWatch = scope.$watch('params.targetRuleChainId',
                function(targetRuleChainId) {
                    if (scope.ruleNode.targetRuleChainId != targetRuleChainId) {
                        scope.ruleNode.targetRuleChainId = targetRuleChainId;
                        if (targetRuleChainId) {
                            ruleChainService.getRuleChain(targetRuleChainId).then(
                                (ruleChain) => {
                                    scope.ruleNode.name = ruleChain.name;
                                }
                            );
                        } else {
                            scope.ruleNode.name = "";
                        }
                    }
                }
            );
        }
        $compile(element.contents())(scope);
    }
    return {
        restrict: "E",
        link: linker,
        scope: {
            ruleChainId: '=',
            ruleNode: '=',
            isEdit: '=',
            isReadOnly: '=',
            theForm: '=',
            onDeleteRuleNode: '&'
        }
    };
}
