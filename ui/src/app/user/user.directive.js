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
import './user-fieldset.scss';

/* eslint-disable import/no-unresolved, import/default */

import userFieldsetTemplate from './user-fieldset.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function UserDirective($compile, $templateCache, userService) {
    var linker = function (scope, element) {
        var template = $templateCache.get(userFieldsetTemplate);
        element.html(template);

        scope.isTenantAdmin = function() {
            return scope.user && scope.user.authority === 'TENANT_ADMIN';
        };

        scope.isCustomerUser = function() {
            return scope.user && scope.user.authority === 'CUSTOMER_USER';
        };

        scope.loginAsUserEnabled = userService.isUserTokenAccessEnabled();

        $compile(element.contents())(scope);
    }
    return {
        restrict: "E",
        link: linker,
        scope: {
            user: '=',
            isEdit: '=',
            theForm: '=',
            onDisplayActivationLink: '&',
            onResendActivation: '&',
            onLoginAsUser: '&',
            onDeleteUser: '&'
        }
    };
}
