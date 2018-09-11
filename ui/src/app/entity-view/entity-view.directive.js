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
/* eslint-disable import/no-unresolved, import/default */

import entityViewFieldsetTemplate from './entity-view-fieldset.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EntityViewDirective($compile, $templateCache, $filter, toast, $translate, $mdConstant,
                                            types, clipboardService, entityViewService, customerService) {
    var linker = function (scope, element) {
        var template = $templateCache.get(entityViewFieldsetTemplate);
        element.html(template);

        scope.types = types;
        scope.isAssignedToCustomer = false;
        scope.isPublic = false;
        scope.assignedCustomer = null;

        scope.allowedEntityTypes = [types.entityType.device, types.entityType.asset];

        var semicolon = 186;
        scope.separatorKeys = [$mdConstant.KEY_CODE.ENTER, $mdConstant.KEY_CODE.COMMA, semicolon];

        scope.$watch('entityView', function(newVal) {
            if (newVal) {
                if (scope.entityView.customerId && scope.entityView.customerId.id !== types.id.nullUid) {
                    scope.isAssignedToCustomer = true;
                    customerService.getShortCustomerInfo(scope.entityView.customerId.id).then(
                        function success(customer) {
                            scope.assignedCustomer = customer;
                            scope.isPublic = customer.isPublic;
                        }
                    );
                } else {
                    scope.isAssignedToCustomer = false;
                    scope.isPublic = false;
                    scope.assignedCustomer = null;
                }
                scope.startTs = new Date(scope.entityView.startTs);
                scope.endTs = new Date(scope.entityView.endTs);
                if (!scope.entityView.keys) {
                    scope.entityView.keys = {};
                    scope.entityView.keys.timeseries = [];
                    scope.entityView.keys.attributes = {};
                    scope.entityView.keys.attributes.ss = [];
                    scope.entityView.keys.attributes.cs = [];
                    scope.entityView.keys.attributes.sh = [];
                }
            }
        });


        scope.$watch('startTs', function (newDate) {
            if (newDate) {
                if (newDate.getTime() > scope.maxStartTs) {
                    scope.startTs = angular.copy(scope.maxStartTs);
                }
                updateMinMaxDates();
            }
        });

        scope.$watch('endTs', function (newDate) {
            if (newDate) {
                if (newDate.getTime() < scope.minEndTs) {
                    scope.endTs = angular.copy(scope.minEndTs);
                }
                updateMinMaxDates();
            }
        });

        function updateMinMaxDates() {
            if (scope.endTs) {
                scope.maxStartTs = angular.copy(new Date(scope.endTs.getTime() - 1000));
                scope.entityView.endTs = scope.endTs.getTime();
            }
            if (scope.startTs) {
                scope.minEndTs = angular.copy(new Date(scope.startTs.getTime() + 1000));
                scope.entityView.startTs = scope.startTs.getTime();
            }
        }

        scope.onEntityViewIdCopied = function() {
            toast.showSuccess($translate.instant('entity-view.idCopiedMessage'), 750, angular.element(element).parent().parent(), 'bottom left');
        };

        $compile(element.contents())(scope);
    }
    return {
        restrict: "E",
        link: linker,
        scope: {
            entityView: '=',
            isEdit: '=',
            entityViewScope: '=',
            theForm: '=',
            onAssignToCustomer: '&',
            onMakePublic: '&',
            onUnassignFromCustomer: '&',
            onDeleteEntityView: '&'
        }
    };
}
