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
import './datasource.scss';

import thingsboardTypes from '../common/types.constant';
import thingsboardDatasourceFunc from './datasource-func.directive'
import thingsboardDatasourceEntity from './datasource-entity.directive';

/* eslint-disable import/no-unresolved, import/default */

import datasourceTemplate from './datasource.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.directives.datasource', [thingsboardTypes, thingsboardDatasourceFunc, thingsboardDatasourceEntity])
    .directive('tbDatasource', Datasource)
    .name;

/*@ngInject*/
function Datasource($compile, $templateCache, utils, types) {

    var linker = function (scope, element, attrs, ngModelCtrl) {

        var template = $templateCache.get(datasourceTemplate);
        element.html(template);

        scope.types = types;

        if (scope.functionsOnly) {
            scope.datasourceTypes = [types.datasourceType.function];
        } else{
            scope.datasourceTypes = [types.datasourceType.entity, types.datasourceType.function];
        }

        scope.updateView = function () {
            if (!scope.model.dataKeys) {
                scope.model.dataKeys = [];
            }
            ngModelCtrl.$setViewValue(scope.model);
        }

        scope.$watch('model.type', function (newType, prevType) {
            if (newType && prevType && newType != prevType) {
                if (scope.widgetType == types.widgetType.alarm.value) {
                    scope.model.dataKeys = utils.getDefaultAlarmDataKeys();
                } else {
                    scope.model.dataKeys = [];
                }
            }
        });

        scope.$watch('model', function () {
            scope.updateView();
        }, true);

        ngModelCtrl.$render = function () {
            if (ngModelCtrl.$viewValue) {
                scope.model = ngModelCtrl.$viewValue;
            } else {
                scope.model = {};
            }
        };

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        require: "^ngModel",
        scope: {
            aliasController: '=',
            maxDataKeys: '=',
            optDataKeys: '=',
            widgetType: '=',
            functionsOnly: '=',
            datakeySettingsSchema: '=',
            generateDataKey: '&',
            fetchEntityKeys: '&',
            onCreateEntityAlias: '&'
        },
        link: linker
    };
}
