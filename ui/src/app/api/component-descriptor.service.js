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
export default angular.module('thingsboard.api.componentDescriptor', [])
    .factory('componentDescriptorService', ComponentDescriptorService).name;

/*@ngInject*/
function ComponentDescriptorService($http, $q) {

    var componentsByType = {};
    var componentsByClazz = {};

    var service = {
        getComponentDescriptorsByTypes: getComponentDescriptorsByTypes
    }

    return service;

    function getComponentDescriptorsByTypes(componentTypes, type) {
        var deferred = $q.defer();
        var result = [];
        if (!componentsByType[type]) {
            componentsByType[type] = {};
        }
        for (var i=componentTypes.length-1;i>=0;i--) {
            var componentType = componentTypes[i];
            if (componentsByType[type][componentType]) {
                result = result.concat(componentsByType[type][componentType]);
                componentTypes.splice(i, 1);
            }
        }
        if (!componentTypes.length) {
            deferred.resolve(result);
        } else {
            var url = '/api/components?componentTypes=' + componentTypes.join(',') + '&ruleChainType=' + type;
            $http.get(url, null).then(function success(response) {
                var components = response.data;
                for (var i = 0; i < components.length; i++) {
                    var component = components[i];
                    var componentsList = componentsByType[type][component.type];
                    if (!componentsList) {
                        componentsList = [];
                        componentsByType[type][component.type] = componentsList;
                    }
                    componentsList.push(component);
                    componentsByClazz[component.clazz] = component;
                }
                result = result.concat(components);
                deferred.resolve(components);
            }, function fail() {
                deferred.reject();
            });
        }
        return deferred.promise;
    }
}
