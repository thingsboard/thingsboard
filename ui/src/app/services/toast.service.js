/*
 * Copyright © 2016 The Thingsboard Authors
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

import successToast from './success-toast.tpl.html';
import errorToast from './error-toast.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function Toast($mdToast, $document) {

    var showing = false;

    var service = {
        showSuccess: showSuccess,
        showError: showError,
        hide: hide
    }

    return service;

    function showSuccess(successMessage, delay, toastParent, position) {
        if (!toastParent) {
            toastParent = angular.element($document[0].getElementById('toast-parent'));
        }
        if (!position) {
            position = 'top left';
        }
        $mdToast.show({
            hideDelay: delay || 0,
            position: position,
            controller: 'ToastController',
            controllerAs: 'vm',
            templateUrl: successToast,
            locals: {message: successMessage},
            parent: toastParent
        });
    }

    function showError(errorMessage, toastParent, position) {
        if (!showing) {
            if (!toastParent) {
                toastParent = angular.element($document[0].getElementById('toast-parent'));
            }
            if (!position) {
                position = 'top left';
            }
            showing = true;
            $mdToast.show({
                hideDelay: 0,
                position: position,
                controller: 'ToastController',
                controllerAs: 'vm',
                templateUrl: errorToast,
                locals: {message: errorMessage},
                parent: toastParent
            }).then(function hide() {
                showing = false;
            }, function cancel() {
                showing = false;
            });
        }
    }

    function hide() {
        if (showing) {
            $mdToast.hide();
        }
    }

}