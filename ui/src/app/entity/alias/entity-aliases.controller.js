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
import './entity-aliases.scss';

/* eslint-disable import/no-unresolved, import/default */

import entityAliasDialogTemplate from './entity-alias-dialog.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EntityAliasesController(utils, entityService, toast, $scope, $mdDialog, $document, $q, $translate,
                                                  types, config) {

    var vm = this;

    vm.types = types;
    vm.entityAliases = [];
    vm.title = config.customTitle ? config.customTitle : 'entity.aliases';
    vm.disableAdd = config.disableAdd;
    vm.aliasToWidgetsMap = {};
    vm.allowedEntityTypes = config.allowedEntityTypes;

    vm.addAlias = addAlias;
    vm.editAlias = editAlias;
    vm.removeAlias = removeAlias;

    vm.cancel = cancel;
    vm.save = save;

    initController();

    function initController() {
        var aliasId;
        if (config.widgets) {
            var widgetsTitleList, widget;
            if (config.isSingleWidget && config.widgets.length == 1) {
                widget = config.widgets[0];
                widgetsTitleList = [widget.config.title];
                for (aliasId in config.entityAliases) {
                    vm.aliasToWidgetsMap[aliasId] = widgetsTitleList;
                }
            } else {
                for (var w in config.widgets) {
                    widget = config.widgets[w];
                    if (widget.type === types.widgetType.rpc.value) {
                        if (widget.config.targetDeviceAliasIds && widget.config.targetDeviceAliasIds.length > 0) {
                            var targetDeviceAliasId = widget.config.targetDeviceAliasIds[0];
                            widgetsTitleList = vm.aliasToWidgetsMap[targetDeviceAliasId];
                            if (!widgetsTitleList) {
                                widgetsTitleList = [];
                                vm.aliasToWidgetsMap[targetDeviceAliasId] = widgetsTitleList;
                            }
                            widgetsTitleList.push(widget.config.title);
                        }
                    } else {
                        var datasources = utils.validateDatasources(widget.config.datasources);
                        for (var i=0;i<datasources.length;i++) {
                            var datasource = datasources[i];
                            if (datasource.type === types.datasourceType.entity && datasource.entityAliasId) {
                                widgetsTitleList = vm.aliasToWidgetsMap[datasource.entityAliasId];
                                if (!widgetsTitleList) {
                                    widgetsTitleList = [];
                                    vm.aliasToWidgetsMap[datasource.entityAliasId] = widgetsTitleList;
                                }
                                widgetsTitleList.push(widget.config.title);
                            }
                        }
                    }
                }
            }
        }

        for (aliasId in config.entityAliases) {
            var entityAlias = config.entityAliases[aliasId];
            var filter = entityAlias.filter;
            if (!filter) {
                filter = {
                    resolveMultiple: false
                };
            }
            if (!filter.resolveMultiple) {
                filter.resolveMultiple = false;
            }
            var result = {id: aliasId, alias: entityAlias.alias, filter: filter};
            vm.entityAliases.push(result);
        }
    }

    function addAlias($event) {
        openAliasDialog($event);
    }

    function editAlias($event, entityAlias) {
        openAliasDialog($event, entityAlias);
    }

    function openAliasDialog($event, entityAlias) {
        var isAdd = entityAlias ? false : true;
        var aliasIndex;
        if (!isAdd) {
            aliasIndex = vm.entityAliases.indexOf(entityAlias);
        }
        $mdDialog.show({
            controller: 'EntityAliasDialogController',
            controllerAs: 'vm',
            templateUrl: entityAliasDialogTemplate,
            locals: {
                isAdd: isAdd,
                allowedEntityTypes: vm.allowedEntityTypes,
                entityAliases: vm.entityAliases,
                alias: isAdd ? null : angular.copy(entityAlias)
            },
            parent: angular.element($document[0].body),
            fullscreen: true,
            multiple: true,
            targetEvent: $event
        }).then(function (alias) {
            if (isAdd) {
                vm.entityAliases.push(alias);
            } else {
                vm.entityAliases[aliasIndex] = alias;
            }
            if ($scope.theForm) {
                $scope.theForm.$setDirty();
            }
        }, function () {
        });
    }

    function removeAlias($event, entityAlias) {
        var index = vm.entityAliases.indexOf(entityAlias);
        if (index > -1) {
            var widgetsTitleList = vm.aliasToWidgetsMap[entityAlias.id];
            if (widgetsTitleList) {
                var widgetsListHtml = '';
                for (var t in widgetsTitleList) {
                    widgetsListHtml += '<br/>\'' + widgetsTitleList[t] + '\'';
                }
                var alert = $mdDialog.alert()
                    .parent(angular.element($document[0].body))
                    .clickOutsideToClose(true)
                    .title($translate.instant('entity.unable-delete-entity-alias-title'))
                    .htmlContent($translate.instant('entity.unable-delete-entity-alias-text', {entityAlias: entityAlias.alias, widgetsList: widgetsListHtml}))
                    .ariaLabel($translate.instant('entity.unable-delete-entity-alias-title'))
                    .ok($translate.instant('action.close'))
                    .targetEvent($event);
                alert._options.multiple = true;
                alert._options.fullscreen = true;

                $mdDialog.show(alert);
            } else {
                vm.entityAliases.splice(index, 1);
                if ($scope.theForm) {
                    $scope.theForm.$setDirty();
                }
            }
        }
    }

    function cancel() {
        $mdDialog.cancel();
    }

    function save() {

        var entityAliases = {};
        var uniqueAliasList = {};

        var valid = true;
        var message, aliasId, alias, filter;

        for (var i = 0; i < vm.entityAliases.length; i++) {
            aliasId = vm.entityAliases[i].id;
            alias = vm.entityAliases[i].alias;
            filter = vm.entityAliases[i].filter;
            if (uniqueAliasList[alias]) {
                valid = false;
                message = $translate.instant('entity.duplicate-alias-error', {alias: alias});
                break;
            } else if (!filter || !filter.type) {
                valid = false;
                message = $translate.instant('entity.missing-entity-filter-error', {alias: alias});
                break;
            } else {
                uniqueAliasList[alias] = alias;
                entityAliases[aliasId] = {id: aliasId, alias: alias, filter: filter};
            }
        }
        if (valid) {
            $scope.theForm.$setPristine();
            $mdDialog.hide(entityAliases);
        } else {
            toast.showError(message);
        }
    }

}
