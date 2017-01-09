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
import './plugin-select.scss';

import thingsboardApiPlugin from '../api/plugin.service';

/* eslint-disable import/no-unresolved, import/default */

import pluginSelectTemplate from './plugin-select.tpl.html';

/* eslint-enable import/no-unresolved, import/default */


export default angular.module('thingsboard.directives.pluginSelect', [thingsboardApiPlugin])
    .directive('tbPluginSelect', PluginSelect)
    .name;

/*@ngInject*/
function PluginSelect($compile, $templateCache, $q, pluginService, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(pluginSelectTemplate);
        element.html(template);

        scope.tbRequired = angular.isDefined(scope.tbRequired) ? scope.tbRequired : false;
        scope.plugin = null;
        scope.pluginSearchText = '';
        scope.searchTextChanged = false;

        scope.pluginFetchFunction = pluginService.getAllPlugins;
        if (angular.isDefined(scope.pluginsScope)) {
            if (scope.pluginsScope === 'action') {
                scope.pluginFetchFunction = pluginService.getAllActionPlugins;
            } else if (scope.pluginsScope === 'system') {
                scope.pluginFetchFunction = pluginService.getSystemPlugins;
            } else if (scope.pluginsScope === 'tenant') {
                scope.pluginFetchFunction = pluginService.getTenantPlugins;
            }
        }

        scope.fetchPlugins = function(searchText) {
            var pageLink = {limit: 10, textSearch: searchText};

            var deferred = $q.defer();

            scope.pluginFetchFunction(pageLink).then(function success(result) {
                deferred.resolve(result.data);
            }, function fail() {
                deferred.reject();
            });

            return deferred.promise;
        }

        scope.pluginSearchTextChanged = function() {
            scope.searchTextChanged = true;
        }

        scope.isSystem = function(item) {
            return item && item.tenantId.id === types.id.nullUid;
        }

        scope.updateView = function () {
            ngModelCtrl.$setViewValue(scope.plugin);
        }

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                scope.plugin = ngModelCtrl.$viewValue;
            }
        }

        scope.$watch('plugin', function () {
            scope.updateView();
        })

        if (scope.selectFirstPlugin) {
            var pageLink = {limit: 1, textSearch: ''};
            scope.pluginFetchFunction(pageLink).then(function success(result) {
                var plugins = result.data;
                if (plugins.length > 0) {
                    scope.plugin = plugins[0];
                }
            }, function fail() {
            });
        }

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        require: "^ngModel",
        link: linker,
        scope: {
            pluginsScope: '@',
            theForm: '=?',
            tbRequired: '=?',
            selectFirstPlugin: '='
        }
    };
}
