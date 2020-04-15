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
import './queue-type-list.scss';

/* eslint-disable import/no-unresolved, import/default */

import queueTypeListTemplate from './queue-type-list.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function QueueTypeList($compile, $templateCache, $q, $filter, queueService) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(queueTypeListTemplate);
        element.html(template);

        scope.queues = null;
        scope.queue = null;
        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;
        scope.queueSearchText = '';

        scope.fetchQueues = function(searchText) {
            var deferred = $q.defer();
            loadQueues().then(
                function success(queueArr) {
                    let result = $filter('filter')(queueArr, {'$': searchText});
                    if (result && result.length) {
                        if (searchText && searchText.length && result.indexOf(searchText) === -1) {
                            result.push(searchText);
                        }
                        result.sort();
                        deferred.resolve(result);
                    } else {
                        deferred.resolve([searchText]);
                    }
                },
                function fail() {
                    deferred.reject();
                }
            );

            return deferred.promise;
        };

        scope.updateView = function () {
            if (!scope.disabled) {
                ngModelCtrl.$setViewValue(scope.queue);
            }
        };

        function loadQueues() {
            var deferred = $q.defer();
            if (!scope.queues) {
                queueService.getTenantQueuesByServiceType(scope.queueType).then(
                function success(queueArr) {
                    scope.queues = queueArr.data;
                    deferred.resolve(scope.queues);
                },
                function fail() {
                    deferred.reject();
                }
                );
            } else {
                deferred.resolve(scope.queues);
            }
            return deferred.promise;
        }

        ngModelCtrl.$render = function () {
            scope.queue = ngModelCtrl.$viewValue;
        };

        scope.$watch('queue', function (newValue, prevValue) {
            if (!angular.equals(newValue, prevValue)) {
                scope.updateView();
            }
        });

        scope.$watch('disabled', function () {
            scope.updateView();
        });

        $compile(element.contents())(scope);
    };

    return {
        restrict: "E",
        require: "^ngModel",
        link: linker,
        scope: {
            theForm: '=?',
            tbRequired: '=?',
            disabled:'=ngDisabled',
            queueType: '=?'
        }
    };
}