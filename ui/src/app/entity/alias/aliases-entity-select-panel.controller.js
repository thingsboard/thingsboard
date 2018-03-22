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
/*@ngInject*/
export default function AliasesEntitySelectPanelController(mdPanelRef, $scope, $filter, types, aliasController, onEntityAliasesUpdate) {

    var vm = this;
    vm._mdPanelRef = mdPanelRef;
    vm.aliasController = aliasController;
    vm.onEntityAliasesUpdate = onEntityAliasesUpdate;
    vm.entityAliases = {};
    vm.entityAliasesInfo = {};

    vm.currentAliasEntityChanged = currentAliasEntityChanged;

    var allEntityAliases = vm.aliasController.getEntityAliases();
    for (var aliasId in allEntityAliases) {
        var aliasInfo = vm.aliasController.getInstantAliasInfo(aliasId);
        if (aliasInfo && !aliasInfo.resolveMultiple && aliasInfo.currentEntity
            && aliasInfo.resolvedEntities.length > 1) {
            vm.entityAliasesInfo[aliasId] = angular.copy(aliasInfo);
            vm.entityAliasesInfo[aliasId].selectedId = aliasInfo.currentEntity.id;
        }
    }

    function currentAliasEntityChanged(aliasId, selectedId) {
        var resolvedEntities = vm.entityAliasesInfo[aliasId].resolvedEntities;
        var selected = $filter('filter')(resolvedEntities, {id: selectedId});
        if (selected && selected.length) {
            vm.aliasController.updateCurrentAliasEntity(aliasId, selected[0]);
            if (onEntityAliasesUpdate) {
                onEntityAliasesUpdate();
            }
        }
    }

}
