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
import thingsboardTypes from '../common/types.constant';
import thingsboardApiDatasource from '../api/datasource.service';

import WidgetController from './widget.controller';

export default angular.module('thingsboard.directives.widget', [thingsboardTypes, thingsboardApiDatasource])
    .controller('WidgetController', WidgetController)
    .directive('tbWidget', Widget)
    .name;

/*@ngInject*/
function Widget($controller, $compile, widgetService) {
    return {
        scope: true,
        link: function (scope, elem, attrs) {

            var widgetController;
            var locals = scope.$eval(attrs.locals);
            var widget = locals.widget;
            var gridsterItem;

            scope.$on('gridster-item-initialized', function (event, item) {
                gridsterItem = item;
                if (widgetController) {
                    widgetController.gridsterItemInitialized(gridsterItem);
                }
            })

            elem.html('<div flex layout="column" layout-align="center center" style="height: 100%;">' +
                      '     <md-progress-circular md-mode="indeterminate" class="md-accent md-hue-2" md-diameter="120"></md-progress-circular>' +
                      '</div>');
            $compile(elem.contents())(scope);

            widgetService.getWidgetInfo(widget.bundleAlias, widget.typeAlias, widget.isSystemType).then(
                function(widgetInfo) {
                    loadFromWidgetInfo(widgetInfo);
                }
            );

            function loadFromWidgetInfo(widgetInfo) {

                elem.addClass("tb-widget");

                var widgetNamespace = "widget-type-" + (widget.isSystemType ? 'sys-' : '')
                    + widget.bundleAlias + '-'
                    + widget.typeAlias;

                elem.addClass(widgetNamespace);
                elem.html('<div id="container">' + widgetInfo.templateHtml + '</div>');

                $compile(elem.contents())(scope);

                angular.extend(locals, {$scope: scope, $element: elem});

                var controllerFunctionBody = 'var fns = { init: null, redraw: null, destroy: null };';
                controllerFunctionBody += widgetInfo.controllerScript;
                controllerFunctionBody += '' +
                    'angular.extend(this, $controller(\'WidgetController\',' +
                    '{' +
                    '$scope: $scope,' +
                    '$timeout: $timeout,' +
                    '$window: $window,' +
                    '$element: $element,' +
                    '$log: $log,' +
                    'types: types,' +
                    'visibleRect: visibleRect,' +
                    'datasourceService: datasourceService,' +
                    'deviceService: deviceService,' +
                    'isPreview: isPreview,' +
                    'widget: widget,' +
                    'deviceAliasList: deviceAliasList,' +
                    'fns: fns' +
                    '}));' +
                    '';

                var controllerFunction = new Function("$scope",
                    "$timeout",
                    "$window",
                    "$element",
                    "$log",
                    'types',
                    "visibleRect",
                    "datasourceService",
                    "deviceService",
                    "$controller",
                    "isPreview",
                    "widget",
                    "deviceAliasList",
                    controllerFunctionBody);

                controllerFunction.$inject = ["$scope",
                    "$timeout",
                    "$window",
                    "$element",
                    "$log",
                    'types',
                    "visibleRect",
                    "datasourceService",
                    "deviceService",
                    "$controller",
                    "isPreview",
                    "widget",
                    "deviceAliasList"];

                widgetController = $controller(controllerFunction, locals);
                if (gridsterItem) {
                    widgetController.gridsterItemInitialized(gridsterItem);
                }
            }
        }
    };
}
