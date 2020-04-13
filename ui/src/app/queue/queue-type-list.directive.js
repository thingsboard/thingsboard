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

        var comparator = function(actual, expected) {
            if (angular.isUndefined(actual)) {
                return false;
            }
            if ((actual === null) || (expected === null)) {
                return actual === expected;
            }
            return actual.startsWith(expected);
        };

        scope.fetchQueues = function(searchText) {
            var deferred = $q.defer();
            loadQueues().then(
                function success(queues) {
                    var result = $filter('filter')(queues, {'$': searchText}, comparator);
                    if (result && result.length) {
                        if (searchText && searchText.length && result.indexOf(searchText) === -1) {
                            result.push(searchText);
                        }
                        result.sort();
                        deferred.resolve(result);
                    }
                    else {
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

        function loadQueues() {
            var deferred = $q.defer();
            if (!scope.queues) {
                var queuePromise;
                switch (scope.queueType) {
                    case 'TB_RULE_ENGINE':
                        queuePromise = queueService.getTenantQueuesByServiceType(scope.queueType);
                        break;
                    case 'TB_CORE':
                        queuePromise = queueService.getTenantQueuesByServiceType(scope.queueType);
                        break;
                    case 'TB_TRANSPORT':
                        queuePromise = queueService.getTenantQueuesByServiceType(scope.queueType);
                        break;
                    case 'JS_EXECUTOR':
                        queuePromise = queueService.getTenantQueuesByServiceType(scope.queueType);
                        break;
                }

                if (queuePromise) {
                    queuePromise.then(
                        function success(queueArr) {
                            scope.queues = [];
                            queueArr.data.forEach(function (queue) {
                                scope.queues.push(queue);
                            });
                            deferred.resolve(scope.queues);
                        },
                        function fail() {
                            deferred.reject();
                        }
                    );
                } else {
                    deferred.reject();
                }
            } else {
                deferred.resolve(scope.queues);
            }
            return deferred.promise;
        }
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