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
export default angular.module('thingsboard.api.ui_configuration', [])
  .factory('uiConfigurationService', UiConfiguration)
  .name;

/*@ngInject*/
function UiConfiguration($http, $q, $log) {
    var configs;

    var service = {
      getUiConfiguration : getUiConfiguration,
      isDepthSeriesEnabled: isDepthSeriesEnabled
    }

    getUiConfiguration();
    return service;

    function getUiConfiguration() {
        var deferred = $q.defer();
        if(configs){
            deferred.resolve(configs);
        } else {
            var url = '/api/ui/configurations';
            $http.get(url).then(function success (response) {
                configs = response.data;
                deferred.resolve(configs);
            }, function fail () {
                $log.error('UiConfiguration : getUiConfiguration : Failed')
                deferred.reject();
            });
        }
        return deferred.promise;
    }

    function isDepthSeriesEnabled() {
        if (configs.depthSeries === 'false') {
            return false;
        } else {
            return true;
        }
    }
}