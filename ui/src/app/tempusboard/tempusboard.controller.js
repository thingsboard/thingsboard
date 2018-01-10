/*
 * Copyright © 2016-2017 Ganesh hegde - HashmapInc Authors
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

//import AliasController from '../api/alias-controller';

/*@ngInject*/
export function TempusboardController($scope, $log, $state, $stateParams, deviceService, types, attributeService, $q, dashboardService, applicationService) {
	var vm = this;
	vm.types = types;
    vm.deviceSelected = false;
    vm.dashboardCtx = {
        state: null,
        stateController: {
            openRightLayout: false
            }
    };

	vm.assetSelected = function(device){
        vm.widgetsSet = [];
        applicationService.getApplicationsByDeviceType(device.type.toLowerCase())
            .then(function success(applications) {
                applications.forEach(function(application){
                     dashboardService.getDashboard(application.miniDashboardId.id)
                        .then(function success(dashboard) {
                            vm.deviceSelected = true;
                          //  vm.dashboard = dashboardUtils.validateAndUpdateDashboard(dashboard);
                            vm.dashboardConfiguration = dashboard.configuration;
                            vm.dashboardCtx.dashboard = dashboard;
                           // vm.dashboardCtx.dashboardTimewindow = vm.dashboardConfiguration.timewindow;
                          //  vm.dashboardCtx.aliasController = new AliasController($scope, $q, $filter, utils,
                            //    types, entityService, null, vm.dashboardConfiguration.entityAliases);
                          vm.timeseriesWidgetTypes = dashboard.configuration.widgets;
                          var widgetObj = {'widget': dashboard.configuration.widgets, 'name': application.name, 'dashboard': 'dashboards/'+application.dashboardId.id};
                          vm.widgetsSet.push(widgetObj);
                        }, function fail() {
                            vm.configurationError = true;
                        });
                })                
               
            }, function fail() {
                vm.configurationError = true;
            });
	}
	var pageLink = {limit: 100};


    var subscriptionIdMap = {};
//	var entityAttributesSubscriptionMap = {};


    function success(attributes, update, apply) {
        vm.last_telemetry = attributes;
        vm.devices.forEach(function(device, index, theArray){
            if(device.subscriptionId === attributes.subscriptionId){
                theArray[index].data = attributes.data
                $log.log(theArray);
            }
        })

        if (!update) {
            $scope.selectedAttributes = [];
        }
        if (apply) {
            $scope.$digest();
        }
    }

	var getEntityAttributes = function(forceUpdate, reset) {
        if ($scope.attributesDeferred) {
            $scope.attributesDeferred.resolve();
        }
        if ($scope.entityId && $scope.entityType && $scope.attributeScope) {
            if (reset) {
                $scope.attributes = {
                    count: 0,
                    data: []
                };
            }
            $scope.checkSubscription();
            $scope.attributesDeferred = attributeService.getEntityAttributes($scope.entityType, $scope.entityId, $scope.attributeScope.value,
                $scope.query, function(attributes, update, apply) {
                    success(attributes, update || forceUpdate, apply);
                }
            );
        } else {
            var deferred = $q.defer();
            $scope.attributesDeferred = deferred;
            success({
                count: 0,
                data: []
            });
            deferred.resolve();
        }
    }
    $scope.checkSubscription = function() {
        var newSubscriptionId = null;
        if ($scope.entityId && $scope.entityType) {
            newSubscriptionId = attributeService.subscribeForEntityAttributes($scope.entityType, $scope.entityId, $scope.attributeScope.value);
        }
        if ($scope.subscriptionId && $scope.subscriptionId != newSubscriptionId) {
            //attributeService.unsubscribeForEntityAttributes($scope.subscriptionId);
        }
        $scope.subscriptionId = newSubscriptionId;
        subscriptionIdMap[$scope.entityId] = newSubscriptionId;
        $log.log(subscriptionIdMap);
    }

    var devices = deviceService.getTenantDevices(pageLink, true, null);
	devices.then(function (data) {
		$log.log(data);
		vm.devices = data.data;

		$scope.entityType = 'DEVICE';
		$scope.attributeScope = {value:'LATEST_TELEMETRY'};
		
		vm.devices.forEach(function(device, index, theArray){
			theArray[index].subscriptionId = $scope.entityType + device.id.id + $scope.attributeScope.value;
			$scope.query = {limit:5, order:"key", page:1, search:null};
			$scope.entityId = device.id.id;
			if(device.id.id == 'fdaa4a90-f0a7-11e7-a908-81edfb2c80e3'){
				getEntityAttributes(true, false);
			}


		})

		vm.assetSelected(vm.devices[0]);
		
	}, function() {
		$log.log('Failed: ');
	})

	


    $scope.$on('$destroy', function() {

            if ($scope.subscriptionId) {
                attributeService.unsubscribeForEntityAttributes($scope.subscriptionId);
            }
        });

}
