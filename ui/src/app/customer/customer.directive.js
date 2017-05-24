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

import customerFieldsetTemplate from './customer-fieldset.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function CustomerDirective($compile, $templateCache, $translate, toast) {
    var linker = function (scope, element) {
        var template = $templateCache.get(customerFieldsetTemplate);
        element.html(template);

        scope.isPublic = false;

        scope.onCustomerIdCopied = function() {
            toast.showSuccess($translate.instant('customer.idCopiedMessage'), 750, angular.element(element).parent().parent(), 'bottom left');
        };

        scope.$watch('customer', function(newVal) {
            if (newVal) {
                if (scope.customer.additionalInfo) {
                    scope.isPublic = scope.customer.additionalInfo.isPublic;
                } else {
                    scope.isPublic = false;
                }
            }
        });

        $compile(element.contents())(scope);

    }
    return {
        restrict: "E",
        link: linker,
        scope: {
            customer: '=',
            isEdit: '=',
            theForm: '=',
            onManageUsers: '&',
            onManageAssets: '&',
            onManageDevices: '&',
            onManageDashboards: '&',
            onDeleteCustomer: '&'
        }
    };
}
