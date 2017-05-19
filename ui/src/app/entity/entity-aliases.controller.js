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
import './entity-aliases.scss';

/*@ngInject*/
export default function EntityAliasesController(utils, entityService, toast, $scope, $mdDialog, $document, $q, $translate,
                                                  types, config) {

    var vm = this;

    vm.isSingleEntityAlias = config.isSingleEntityAlias;
    vm.singleEntityAlias = config.singleEntityAlias;
    vm.entityAliases = [];
    vm.title = config.customTitle ? config.customTitle : 'entity.aliases';
    vm.disableAdd = config.disableAdd;
    vm.aliasToWidgetsMap = {};
    vm.allowedEntityTypes = config.allowedEntityTypes;

    vm.onFilterEntityChanged = onFilterEntityChanged;
    vm.addAlias = addAlias;
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

        if (vm.isSingleEntityAlias) {
            checkEntityAlias(vm.singleEntityAlias);
        }

        for (aliasId in config.entityAliases) {
            var entityAlias = config.entityAliases[aliasId];
            var result = {id: aliasId, alias: entityAlias.alias, entityType: entityAlias.entityType, entityFilter: entityAlias.entityFilter, changed: true};
            checkEntityAlias(result);
            vm.entityAliases.push(result);
        }
    }

    function checkEntityAlias(entityAlias) {
        if (!entityAlias.entityType) {
            entityAlias.entityType = types.entityType.device;
        }
        if (!entityAlias.entityFilter || entityAlias.entityFilter == null) {
            entityAlias.entityFilter = {
                useFilter: false,
                entityNameFilter: '',
                entityList: [],
            };
        }
    }

    function onFilterEntityChanged(entity, entityAlias) {
        if (entityAlias) {
            if (!entityAlias.alias || entityAlias.alias.length == 0) {
                entityAlias.changed = false;
            }
            if (!entityAlias.changed && entity && entityAlias.entityType) {
                entityAlias.alias = entityService.entityName(entityAlias.entityType, entity);
            }
        }
    }

    function addAlias() {
        var aliasId = 0;
        for (var a in vm.entityAliases) {
            aliasId = Math.max(vm.entityAliases[a].id, aliasId);
        }
        aliasId++;
        var entityAlias = {id: aliasId, alias: '', entityType: types.entityType.device,
            entityFilter: {useFilter: false, entityNameFilter: '', entityList: []}, changed: false};
        vm.entityAliases.push(entityAlias);
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
                alert._options.skipHide = true;
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

    function cleanupEntityFilter(entityFilter) {
        if (entityFilter.useFilter) {
            entityFilter.entityList = [];
        } else {
            entityFilter.entityNameFilter = '';
        }
        return entityFilter;
    }

    function save() {

        var entityAliases = {};
        var uniqueAliasList = {};

        var valid = true;
        var aliasId, maxAliasId;
        var alias;
        var i;

        if (vm.isSingleEntityAlias) {
            maxAliasId = 0;
            vm.singleEntityAlias.entityFilter = cleanupEntityFilter(vm.singleEntityAlias.entityFilter);
            for (i = 0; i < vm.entityAliases.length; i ++) {
                aliasId = vm.entityAliases[i].id;
                alias = vm.entityAliases[i].alias;
                if (alias === vm.singleEntityAlias.alias) {
                    valid = false;
                    break;
                }
                maxAliasId = Math.max(aliasId, maxAliasId);
            }
            maxAliasId++;
            vm.singleEntityAlias.id = maxAliasId;
        } else {
            for (i = 0; i < vm.entityAliases.length; i++) {
                aliasId = vm.entityAliases[i].id;
                alias = vm.entityAliases[i].alias;
                if (!uniqueAliasList[alias]) {
                    uniqueAliasList[alias] = alias;
                    entityAliases[aliasId] = {alias: alias, entityType: vm.entityAliases[i].entityType, entityFilter: cleanupEntityFilter(vm.entityAliases[i].entityFilter)};
                } else {
                    valid = false;
                    break;
                }
            }
        }
        if (valid) {
            $scope.theForm.$setPristine();
            if (vm.isSingleEntityAlias) {
                $mdDialog.hide(vm.singleEntityAlias);
            } else {
                $mdDialog.hide(entityAliases);
            }
        } else {
            toast.showError($translate.instant('entity.duplicate-alias-error', {alias: alias}));
        }
    }

}
