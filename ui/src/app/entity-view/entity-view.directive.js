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
                if (scope.entityView.startTimeMs > 0) {
                    scope.startTimeMs = new Date(scope.entityView.startTimeMs);
                }
                if (scope.entityView.endTimeTs > 0) {
                    scope.endTimeTs = new Date(scope.entityView.endTimeTs);
                }
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


        scope.$watch('startTimeMs', function (newDate) {
            if (newDate) {
                if (newDate.getTime() > scope.maxStartTimeMs) {
                    scope.startTimeMs = angular.copy(scope.maxStartTimeMs);
                }
                updateMinMaxDates();
            }
        });

        scope.$watch('endTimeTs', function (newDate) {
            if (newDate) {
                if (newDate.getTime() < scope.minEndTimeTs) {
                    scope.endTimeTs = angular.copy(scope.minEndTimeTs);
                }
                updateMinMaxDates();
            }
        });

        function updateMinMaxDates() {
            if (scope.endTimeTs) {
                scope.maxStartTimeMs = angular.copy(new Date(scope.endTimeTs.getTime()));
                scope.entityView.endTimeTs = scope.endTimeTs.getTime();
            }
            if (scope.startTimeMs) {
                scope.minEndTimeTs = angular.copy(new Date(scope.startTimeMs.getTime()));
                scope.entityView.startTimeMs = scope.startTimeMs.getTime();
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
