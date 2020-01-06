/*
 * Copyright © 2016-2019 The Thingsboard Authors
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
/**
 * Created by igor on 6/20/17.
 */
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

import './manage-widget-actions.scss';

import thingsboardMaterialIconSelect from '../../material-icon-select.directive';

import WidgetActionDialogController from './widget-action-dialog.controller';
import CustomActionPrettyEditor from './custom-action-pretty-editor.directive';

/* eslint-disable import/no-unresolved, import/default */

import manageWidgetActionsTemplate from './manage-widget-actions.tpl.html';
import widgetActionDialogTemplate from './widget-action-dialog.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.directives.widgetActions', [thingsboardMaterialIconSelect, CustomActionPrettyEditor])
    .controller('WidgetActionDialogController', WidgetActionDialogController)
    .directive('tbManageWidgetActions', ManageWidgetActions)
    .name;

/*@ngInject*/
function ManageWidgetActions() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            actionSources: '=',
            widgetActions: '=',
            fetchDashboardStates: '&',
        },
        controller: ManageWidgetActionsController,
        controllerAs: 'vm',
        templateUrl: manageWidgetActionsTemplate
    };
}

/* eslint-disable angular/angularelement */


/*@ngInject*/
function ManageWidgetActionsController($rootScope, $scope, $document, $mdDialog, $q, $filter,
                              $translate, $timeout, utils, types) {

    let vm = this;

    vm.allActions = [];

    vm.actions = [];
    vm.actionsCount = 0;

    vm.query = {
        order: 'actionSourceName',
        limit: 10,
        page: 1,
        search: null
    };

    vm.enterFilterMode = enterFilterMode;
    vm.exitFilterMode = exitFilterMode;
    vm.onReorder = onReorder;
    vm.onPaginate = onPaginate;
    vm.addAction = addAction;
    vm.editAction = editAction;
    vm.deleteAction = deleteAction;

    $timeout(function(){
        $scope.manageWidgetActionsForm.querySearchInput.$pristine = false;
    });

    $scope.$watch('vm.widgetActions', function() {
        if (vm.widgetActions) {
            reloadActions();
        }
    });

    $scope.$watch("vm.query.search", function(newVal, prevVal) {
        if (!angular.equals(newVal, prevVal) && vm.query.search != null) {
            updateActions();
        }
    });

    function enterFilterMode (event) {
        let $button = angular.element(event.currentTarget);
        let $toolbarsContainer = $button.closest('.toolbarsContainer');

        vm.query.search = '';

        $timeout(()=>{
            $toolbarsContainer.find('.searchInput').focus();
        })
    }

    function exitFilterMode () {
        vm.query.search = null;
        updateActions();
    }

    function onReorder () {
        updateActions();
    }

    function onPaginate () {
        updateActions();
    }

    function addAction($event) {
        if ($event) {
            $event.stopPropagation();
        }
        openWidgetActionDialog($event, null, true);
    }

    function editAction ($event, action) {
        if ($event) {
            $event.stopPropagation();
        }
        openWidgetActionDialog($event, action, false);
    }

    function deleteAction($event, action) {
        if ($event) {
            $event.stopPropagation();
        }
        if (action) {
            var title = $translate.instant('widget-config.delete-action-title');
            var content = $translate.instant('widget-config.delete-action-text', {actionName: action.name});
            var confirm = $mdDialog.confirm()
                .targetEvent($event)
                .title(title)
                .htmlContent(content)
                .ariaLabel(title)
                .cancel($translate.instant('action.no'))
                .ok($translate.instant('action.yes'));

            confirm._options.multiple = true;
            confirm._options.fullscreen = true;

            $mdDialog.show(confirm).then(function () {
                var index = getActionIndex(action.id, vm.allActions);
                if (index > -1) {
                    vm.allActions.splice(index, 1);
                }
                var targetActions = vm.widgetActions[action.actionSourceId];
                index = getActionIndex(action.id, targetActions);
                if (index > -1) {
                    targetActions.splice(index, 1);
                }
                $scope.manageWidgetActionsForm.$setDirty();
                updateActions();
            });
        }
    }

    function openWidgetActionDialog($event, action, isAdd) {
        var prevActionId = null;
        if (!isAdd) {
            prevActionId = action.id;
        }
        var availableActionSources = {};
        for (var id in vm.actionSources) {
            var actionSource = vm.actionSources[id];
            if (actionSource.multiple) {
                availableActionSources[id] = actionSource;
            } else {
                if (!isAdd && action.actionSourceId == id) {
                    availableActionSources[id] = actionSource;
                } else {
                    var result = $filter('filter')(vm.allActions, {actionSourceId: id});
                    if (!result || !result.length) {
                        availableActionSources[id] = actionSource;
                    }
                }
            }
        }
        $mdDialog.show({
            controller: 'WidgetActionDialogController',
            controllerAs: 'vm',
            templateUrl: widgetActionDialogTemplate,
            parent: angular.element($document[0].body),
            locals: {isAdd: isAdd, fetchDashboardStates: vm.fetchDashboardStates,
                actionSources: availableActionSources, widgetActions: vm.widgetActions,
                action: angular.copy(action)},
            multiple: true,
            fullscreen: true,
            targetEvent: $event
        }).then(function (action) {
            saveAction(action, prevActionId);
            updateActions();
        });
    }

    function getActionIndex(id, actions) {
        var result = $filter('filter')(actions, {id: id}, true);
        if (result && result.length) {
            return actions.indexOf(result[0]);
        }
        return -1;
    }

    function saveAction(action, prevActionId) {
        var actionSourceName = vm.actionSources[action.actionSourceId].name;
        action.actionSourceName = utils.customTranslation(actionSourceName, actionSourceName);
        action.typeName = $translate.instant(types.widgetActionTypes[action.type].name);
        var actionSourceId = action.actionSourceId;
        var widgetAction = angular.copy(action);
        delete widgetAction.actionSourceId;
        delete widgetAction.actionSourceName;
        delete widgetAction.typeName;
        var targetActions = vm.widgetActions[actionSourceId];
        if (!targetActions) {
            targetActions = [];
            vm.widgetActions[actionSourceId] = targetActions;
        }
        if (prevActionId) {
            const indexInTarget = getActionIndex(prevActionId, targetActions);
            const indexInAllActions = getActionIndex(prevActionId, vm.allActions);
            if (indexInTarget > -1) {
                targetActions[indexInTarget] = widgetAction;
            } else if (indexInAllActions > -1) {
                const prevActionSourceId = vm.allActions[indexInAllActions].actionSourceId;
                const index = getActionIndex(prevActionId,vm.widgetActions[prevActionSourceId]);
                vm.widgetActions[prevActionSourceId].splice(index,1);
                targetActions.push(widgetAction);
            }
            if (indexInAllActions > -1) {
                vm.allActions[indexInAllActions] = action;
            }
        } else {
            vm.allActions.push(action);
            targetActions.push(widgetAction);
        }
        $scope.manageWidgetActionsForm.$setDirty();
    }

    function reloadActions() {
        vm.allActions = [];
        vm.actions = [];
        vm.actionsCount = 0;

        for (var actionSourceId in vm.widgetActions) {
            var actionSource = vm.actionSources[actionSourceId];
            var actionSourceActions = vm.widgetActions[actionSourceId];
            for (var i=0;i<actionSourceActions.length;i++) {
                var actionSourceAction = actionSourceActions[i];
                var action = angular.copy(actionSourceAction);
                action.actionSourceId = actionSourceId;
                action.actionSourceName = utils.customTranslation(actionSource.name, actionSource.name);
                action.typeName = $translate.instant(types.widgetActionTypes[actionSourceAction.type].name);
                vm.allActions.push(action);
            }
        }

        updateActions ();
    }

    function updateActions () {
        var result = $filter('orderBy')(vm.allActions, vm.query.order);
        if (vm.query.search != null) {
            result = $filter('filter')(result, {$: vm.query.search});
        }
        vm.actionsCount = result.length;
        var startIndex = vm.query.limit * (vm.query.page - 1);
        vm.actions = result.slice(startIndex, startIndex + vm.query.limit);
    }
}