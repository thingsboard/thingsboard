/*
 * Copyright © 2016-2017 The Thingsboard Authors
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
/* eslint-disable import/no-unresolved, import/default */

import addRuleTemplate from './add-rule.tpl.html';
import ruleCard from './rule-card.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function RuleController(ruleService, userService, importExport, $state, $stateParams, $filter, $translate, types) {

    var ruleActionsList = [
        {
            onAction: function ($event, item) {
                exportRule($event, item);
            },
            name: function() { $translate.instant('action.export') },
            details: function() { return $translate.instant('rule.export') },
            icon: "file_download"
        },
        {
            onAction: function ($event, item) {
                activateRule($event, item);
            },
            name: function() { return $translate.instant('action.activate') },
            details: function() { return $translate.instant('rule.activate') },
            icon: "play_arrow",
            isEnabled: function(rule) {
                return isRuleEditable(rule) && rule && rule.state === 'SUSPENDED';
            }
        },
        {
            onAction: function ($event, item) {
                suspendRule($event, item);
            },
            name: function() { return $translate.instant('action.suspend') },
            details: function() { return $translate.instant('rule.suspend') },
            icon: "pause",
            isEnabled: function(rule) {
                return isRuleEditable(rule) && rule.state === 'ACTIVE';
            }
        },
        {
            onAction: function ($event, item) {
                vm.grid.deleteItem($event, item);
            },
            name: function() { return $translate.instant('action.delete') },
            details: function() { return $translate.instant('rule.delete') },
            icon: "delete",
            isEnabled: isRuleEditable
        }
    ];

    var ruleAddItemActionsList = [
        {
            onAction: function ($event) {
                vm.grid.addItem($event);
            },
            name: function() { return $translate.instant('action.create') },
            details: function() { return $translate.instant('rule.create-new-rule') },
            icon: "insert_drive_file"
        },
        {
            onAction: function ($event) {
                importExport.importRule($event).then(
                    function() {
                        vm.grid.refreshList();
                    }
                );
            },
            name: function() { return $translate.instant('action.import') },
            details: function() { return $translate.instant('rule.import') },
            icon: "file_upload"
        }
    ];

    var vm = this;

    vm.types = types;

    vm.ruleGridConfig = {

        refreshParamsFunc: null,

        deleteItemTitleFunc: deleteRuleTitle,
        deleteItemContentFunc: deleteRuleText,
        deleteItemsTitleFunc: deleteRulesTitle,
        deleteItemsActionTitleFunc: deleteRulesActionTitle,
        deleteItemsContentFunc: deleteRulesText,

        fetchItemsFunc: fetchRules,
        saveItemFunc: saveRule,
        deleteItemFunc: deleteRule,

        getItemTitleFunc: getRuleTitle,
        itemCardTemplateUrl: ruleCard,
        parentCtl: vm,

        actionsList: ruleActionsList,
        addItemActions: ruleAddItemActionsList,

        onGridInited: gridInited,

        addItemTemplateUrl: addRuleTemplate,

        addItemText: function() { return $translate.instant('rule.add-rule-text') },
        noItemsText: function() { return $translate.instant('rule.no-rules-text') },
        itemDetailsText: function() { return $translate.instant('rule.rule-details') },
        isSelectionEnabled: isRuleEditable,
        isDetailsReadOnly: function(rule) {
            return !isRuleEditable(rule);
        }
    };

    if (angular.isDefined($stateParams.items) && $stateParams.items !== null) {
        vm.ruleGridConfig.items = $stateParams.items;
    }

    if (angular.isDefined($stateParams.topIndex) && $stateParams.topIndex > 0) {
        vm.ruleGridConfig.topIndex = $stateParams.topIndex;
    }

    vm.isRuleEditable = isRuleEditable;

    vm.activateRule = activateRule;
    vm.suspendRule = suspendRule;
    vm.exportRule = exportRule;

    function deleteRuleTitle(rule) {
        return $translate.instant('rule.delete-rule-title', {ruleName: rule.name});
    }

    function deleteRuleText() {
        return $translate.instant('rule.delete-rule-text');
    }

    function deleteRulesTitle(selectedCount) {
        return $translate.instant('rule.delete-rules-title', {count: selectedCount}, 'messageformat');
    }

    function deleteRulesActionTitle(selectedCount) {
        return $translate.instant('rule.delete-rules-action-title', {count: selectedCount}, 'messageformat');
    }

    function deleteRulesText() {
        return $translate.instant('rule.delete-rules-text');
    }

    function gridInited(grid) {
        vm.grid = grid;
    }

    function fetchRules(pageLink) {
        return ruleService.getAllRules(pageLink);
    }

    function saveRule(rule) {
        return ruleService.saveRule(rule);
    }

    function deleteRule(ruleId) {
        return ruleService.deleteRule(ruleId);
    }

    function getRuleTitle(rule) {
        return rule ? rule.name : '';
    }

    function isRuleEditable(rule) {
        if (userService.getAuthority() === 'TENANT_ADMIN') {
            return rule && rule.tenantId.id != types.id.nullUid;
        } else {
            return userService.getAuthority() === 'SYS_ADMIN';
        }
    }

    function exportRule($event, rule) {
        $event.stopPropagation();
        importExport.exportRule(rule.id.id);
    }

    function activateRule(event, rule) {
        ruleService.activateRule(rule.id.id).then(function () {
            vm.grid.refreshList();
        }, function () {
        });
    }

    function suspendRule(event, rule) {
        ruleService.suspendRule(rule.id.id).then(function () {
            vm.grid.refreshList();
        }, function () {
        });
    }

}
