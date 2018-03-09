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
import thingsboardDatakeyConfig from './datakey-config.directive';

export default angular.module('thingsboard.dialogs.datakeyConfigDialog', [thingsboardDatakeyConfig])
    .controller('DatakeyConfigDialogController', DatakeyConfigDialogController)
    .name;

/*@ngInject*/
function DatakeyConfigDialogController($scope, $mdDialog, $q, entityService, dataKey, dataKeySettingsSchema, entityAlias, aliasController) {

    var vm = this;

    vm.dataKey = dataKey;
    vm.dataKeySettingsSchema = dataKeySettingsSchema;
    vm.entityAlias = entityAlias;
    vm.aliasController = aliasController;

    vm.hide = function () {
        $mdDialog.hide();
    };

    vm.cancel = function () {
        $mdDialog.cancel();
    };

    vm.fetchEntityKeys = function (entityAliasId, query, type) {
        var deferred = $q.defer();
        vm.aliasController.getAliasInfo(entityAliasId).then(
            function success(aliasInfo) {
                var entity = aliasInfo.currentEntity;
                if (entity) {
                    entityService.getEntityKeys(entity.entityType, entity.id, query, type, {ignoreLoading: true}).then(
                        function success(keys) {
                            deferred.resolve(keys);
                        },
                        function fail() {
                            deferred.resolve([]);
                        }
                    );
                } else {
                    deferred.resolve([]);
                }
            },
            function fail() {
                deferred.resolve([]);
            }
        );
        return deferred.promise;
    };

    vm.save = function () {
        $scope.$broadcast('form-submit');
        if ($scope.theForm.$valid) {
            $scope.theForm.$setPristine();
            $mdDialog.hide(vm.dataKey);
        }
    };
}