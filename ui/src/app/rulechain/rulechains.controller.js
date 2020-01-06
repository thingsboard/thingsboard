/*
 * Copyright © 2016-2020 The Thingsboard Authors
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

import addRuleChainTemplate from './add-rulechain.tpl.html';
import ruleChainCard from './rulechain-card.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function RuleChainsController(ruleChainService, userService, importExport, $state,
                                             $stateParams, $filter, $translate, $mdDialog, types) {

    var ruleChainActionsList = [
        {
            onAction: function ($event, item) {
                vm.grid.openItem($event, item);
            },
            name: function() { return $translate.instant('rulechain.details') },
            details: function() { return $translate.instant('rulechain.rulechain-details') },
            icon: "edit"
        },
        {
            onAction: function ($event, item) {
                exportRuleChain($event, item);
            },
            name: function() { $translate.instant('action.export') },
            details: function() { return $translate.instant('rulechain.export') },
            icon: "file_download"
        },
        {
            onAction: function ($event, item) {
                setRootRuleChain($event, item);
            },
            name: function() { return $translate.instant('rulechain.set-root') },
            details: function() { return $translate.instant('rulechain.set-root') },
            icon: "flag",
            isEnabled: isNonRootRuleChain
        },
        {
            onAction: function ($event, item) {
                vm.grid.deleteItem($event, item);
            },
            name: function() { return $translate.instant('action.delete') },
            details: function() { return $translate.instant('rulechain.delete') },
            icon: "delete",
            isEnabled: isNonRootRuleChain
        }
    ];

    var ruleChainAddItemActionsList = [
        {
            onAction: function ($event) {
                vm.grid.addItem($event);
            },
            name: function() { return $translate.instant('action.create') },
            details: function() { return $translate.instant('rulechain.create-new-rulechain') },
            icon: "insert_drive_file"
        },
        {
            onAction: function ($event) {
                importExport.importRuleChain($event).then(
                    function(ruleChainImport) {
                        $state.go('home.ruleChains.importRuleChain', {ruleChainImport:ruleChainImport});
                    }
                );
            },
            name: function() { return $translate.instant('action.import') },
            details: function() { return $translate.instant('rulechain.import') },
            icon: "file_upload"
        }
    ];

    var vm = this;

    vm.types = types;

    vm.ruleChainGridConfig = {

        refreshParamsFunc: null,

        deleteItemTitleFunc: deleteRuleChainTitle,
        deleteItemContentFunc: deleteRuleChainText,
        deleteItemsTitleFunc: deleteRuleChainsTitle,
        deleteItemsActionTitleFunc: deleteRuleChainsActionTitle,
        deleteItemsContentFunc: deleteRuleChainsText,

        fetchItemsFunc: fetchRuleChains,
        saveItemFunc: saveRuleChain,
        clickItemFunc: openRuleChain,
        deleteItemFunc: deleteRuleChain,

        getItemTitleFunc: getRuleChainTitle,
        itemCardTemplateUrl: ruleChainCard,
        parentCtl: vm,

        actionsList: ruleChainActionsList,
        addItemActions: ruleChainAddItemActionsList,

        onGridInited: gridInited,

        addItemTemplateUrl: addRuleChainTemplate,

        addItemText: function() { return $translate.instant('rulechain.add-rulechain-text') },
        noItemsText: function() { return $translate.instant('rulechain.no-rulechains-text') },
        itemDetailsText: function() { return $translate.instant('rulechain.rulechain-details') },
        isSelectionEnabled: isNonRootRuleChain
    };

    if (angular.isDefined($stateParams.items) && $stateParams.items !== null) {
        vm.ruleChainGridConfig.items = $stateParams.items;
    }

    if (angular.isDefined($stateParams.topIndex) && $stateParams.topIndex > 0) {
        vm.ruleChainGridConfig.topIndex = $stateParams.topIndex;
    }

    vm.isRootRuleChain = isRootRuleChain;
    vm.isNonRootRuleChain = isNonRootRuleChain;

    vm.exportRuleChain = exportRuleChain;
    vm.setRootRuleChain = setRootRuleChain;

    function deleteRuleChainTitle(ruleChain) {
        return $translate.instant('rulechain.delete-rulechain-title', {ruleChainName: ruleChain.name});
    }

    function deleteRuleChainText() {
        return $translate.instant('rulechain.delete-rulechain-text');
    }

    function deleteRuleChainsTitle(selectedCount) {
        return $translate.instant('rulechain.delete-rulechains-title', {count: selectedCount}, 'messageformat');
    }

    function deleteRuleChainsActionTitle(selectedCount) {
        return $translate.instant('rulechain.delete-rulechains-action-title', {count: selectedCount}, 'messageformat');
    }

    function deleteRuleChainsText() {
        return $translate.instant('rulechain.delete-rulechains-text');
    }

    function gridInited(grid) {
        vm.grid = grid;
    }

    function fetchRuleChains(pageLink) {
        return ruleChainService.getRuleChains(pageLink);
    }

    function saveRuleChain(ruleChain) {
        return ruleChainService.saveRuleChain(ruleChain);
    }

    function openRuleChain($event, ruleChain) {
        if ($event) {
            $event.stopPropagation();
        }
        $state.go('home.ruleChains.ruleChain', {ruleChainId: ruleChain.id.id});
    }

    function deleteRuleChain(ruleChainId) {
        return ruleChainService.deleteRuleChain(ruleChainId);
    }

    function getRuleChainTitle(ruleChain) {
        return ruleChain ? ruleChain.name : '';
    }

    function isRootRuleChain(ruleChain) {
        return ruleChain && ruleChain.root;
    }

    function isNonRootRuleChain(ruleChain) {
        return ruleChain && !ruleChain.root;
    }

    function exportRuleChain($event, ruleChain) {
        $event.stopPropagation();
        importExport.exportRuleChain(ruleChain.id.id);
    }

    function setRootRuleChain($event, ruleChain) {
        $event.stopPropagation();
        var confirm = $mdDialog.confirm()
            .targetEvent($event)
            .title($translate.instant('rulechain.set-root-rulechain-title', {ruleChainName: ruleChain.name}))
            .htmlContent($translate.instant('rulechain.set-root-rulechain-text'))
            .ariaLabel($translate.instant('rulechain.set-root'))
            .cancel($translate.instant('action.no'))
            .ok($translate.instant('action.yes'));
        $mdDialog.show(confirm).then(function () {
            ruleChainService.setRootRuleChain(ruleChain.id.id).then(
                () => {
                    vm.grid.refreshList();
                }
            );
        });

    }
}
