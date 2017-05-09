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

import deviceFilterTemplate from './device-filter.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import './device-filter.scss';

export default angular.module('thingsboard.directives.deviceFilter', [])
    .directive('tbDeviceFilter', DeviceFilter)
    .name;

/*@ngInject*/
function DeviceFilter($compile, $templateCache, $q, deviceService) {

    var linker = function (scope, element, attrs, ngModelCtrl) {

        var template = $templateCache.get(deviceFilterTemplate);
        element.html(template);

        scope.ngModelCtrl = ngModelCtrl;

        scope.fetchDevices = function(searchText, limit) {
            var pageLink = {limit: limit, textSearch: searchText};

            var deferred = $q.defer();

            deviceService.getTenantDevices(pageLink, false).then(function success(result) {
                deferred.resolve(result.data);
            }, function fail() {
                deferred.reject();
            });

            return deferred.promise;
        }

        scope.updateValidity = function() {
            if (ngModelCtrl.$viewValue) {
                var value = ngModelCtrl.$viewValue;
                var valid;
                if (value.useFilter) {
                    ngModelCtrl.$setValidity('deviceList', true);
                    if (angular.isDefined(value.deviceNameFilter) && value.deviceNameFilter.length > 0) {
                        ngModelCtrl.$setValidity('deviceNameFilter', true);
                        valid = angular.isDefined(scope.model.matchingFilterDevice) && scope.model.matchingFilterDevice != null;
                        ngModelCtrl.$setValidity('deviceNameFilterDeviceMatch', valid);
                    } else {
                        ngModelCtrl.$setValidity('deviceNameFilter', false);
                    }
                } else {
                    ngModelCtrl.$setValidity('deviceNameFilter', true);
                    ngModelCtrl.$setValidity('deviceNameFilterDeviceMatch', true);
                    valid = angular.isDefined(value.deviceList) && value.deviceList.length > 0;
                    ngModelCtrl.$setValidity('deviceList', valid);
                }
            }
        }

        ngModelCtrl.$render = function () {
            destroyWatchers();
            scope.model = {
                useFilter: false,
                deviceList: [],
                deviceNameFilter: ''
            }
            if (ngModelCtrl.$viewValue) {
                var value = ngModelCtrl.$viewValue;
                var model = scope.model;
                model.useFilter = value.useFilter === true ? true: false;
                model.deviceList = [];
                model.deviceNameFilter = value.deviceNameFilter || '';
                processDeviceNameFilter(model.deviceNameFilter).then(
                    function(device) {
                        scope.model.matchingFilterDevice = device;
                        if (value.deviceList && value.deviceList.length > 0) {
                            deviceService.getDevices(value.deviceList).then(function (devices) {
                                model.deviceList = devices;
                                updateMatchingDevice();
                                initWatchers();
                            });
                        } else {
                            updateMatchingDevice();
                            initWatchers();
                        }
                    }
                )
            }
        }

        function updateMatchingDevice() {
            if (scope.model.useFilter) {
                scope.model.matchingDevice = scope.model.matchingFilterDevice;
            } else {
                if (scope.model.deviceList && scope.model.deviceList.length > 0) {
                    scope.model.matchingDevice = scope.model.deviceList[0];
                } else {
                    scope.model.matchingDevice = null;
                }
            }
        }

        function processDeviceNameFilter(deviceNameFilter) {
            var deferred = $q.defer();
            if (angular.isDefined(deviceNameFilter) && deviceNameFilter.length > 0) {
                scope.fetchDevices(deviceNameFilter, 1).then(function (devices) {
                    if (devices && devices.length > 0) {
                        deferred.resolve(devices[0]);
                    } else {
                        deferred.resolve(null);
                    }
                });
            } else {
                deferred.resolve(null);
            }
            return deferred.promise;
        }

        function destroyWatchers() {
            if (scope.deviceListDeregistration) {
                scope.deviceListDeregistration();
                scope.deviceListDeregistration = null;
            }
            if (scope.useFilterDeregistration) {
                scope.useFilterDeregistration();
                scope.useFilterDeregistration = null;
            }
            if (scope.deviceNameFilterDeregistration) {
                scope.deviceNameFilterDeregistration();
                scope.deviceNameFilterDeregistration = null;
            }
            if (scope.matchingDeviceDeregistration) {
                scope.matchingDeviceDeregistration();
                scope.matchingDeviceDeregistration = null;
            }
        }

        function initWatchers() {
            scope.deviceListDeregistration = scope.$watch('model.deviceList', function () {
                if (ngModelCtrl.$viewValue) {
                    var value = ngModelCtrl.$viewValue;
                    value.deviceList = [];
                    if (scope.model.deviceList && scope.model.deviceList.length > 0) {
                        for (var i in scope.model.deviceList) {
                            value.deviceList.push(scope.model.deviceList[i].id.id);
                        }
                    }
                    updateMatchingDevice();
                    ngModelCtrl.$setViewValue(value);
                    scope.updateValidity();
                }
            }, true);
            scope.useFilterDeregistration = scope.$watch('model.useFilter', function () {
                if (ngModelCtrl.$viewValue) {
                    var value = ngModelCtrl.$viewValue;
                    value.useFilter = scope.model.useFilter;
                    updateMatchingDevice();
                    ngModelCtrl.$setViewValue(value);
                    scope.updateValidity();
                }
            });
            scope.deviceNameFilterDeregistration = scope.$watch('model.deviceNameFilter', function (newNameFilter, prevNameFilter) {
                if (ngModelCtrl.$viewValue) {
                    if (!angular.equals(newNameFilter, prevNameFilter)) {
                        var value = ngModelCtrl.$viewValue;
                        value.deviceNameFilter = scope.model.deviceNameFilter;
                        processDeviceNameFilter(value.deviceNameFilter).then(
                            function(device) {
                                scope.model.matchingFilterDevice = device;
                                updateMatchingDevice();
                                ngModelCtrl.$setViewValue(value);
                                scope.updateValidity();
                            }
                        );
                    }
                }
            });

            scope.matchingDeviceDeregistration = scope.$watch('model.matchingDevice', function (newMatchingDevice, prevMatchingDevice) {
                if (!angular.equals(newMatchingDevice, prevMatchingDevice)) {
                    if (scope.onMatchingDeviceChange) {
                        scope.onMatchingDeviceChange({device: newMatchingDevice});
                    }
                }
            });
        }

        $compile(element.contents())(scope);

    }

    return {
        restrict: "E",
        require: "^ngModel",
        link: linker,
        scope: {
            isEdit: '=',
            onMatchingDeviceChange: '&'
        }
    };

}
