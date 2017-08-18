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

import './widget.scss';

import thingsboardLegend from '../legend.directive';
import thingsboardTypes from '../../common/types.constant';
import thingsboardApiDatasource from '../../api/datasource.service';

import WidgetController from './widget.controller';

export default angular.module('thingsboard.directives.widget', [thingsboardLegend, thingsboardTypes, thingsboardApiDatasource])
    .controller('WidgetController', WidgetController)
    .directive('tbWidget', Widget)
    .name;

/*@ngInject*/
function Widget($controller, widgetService) {
    return {
        scope: true,
        link: function (scope, elem, attrs) {

            var locals = scope.$eval(attrs.locals);
            var widget = locals.widget;

            var widgetController;
            var gridsterItem;

            //TODO: widgets visibility
            /*scope.$on('visibleRectChanged', function (event, newVisibleRect) {
                locals.visibleRect = newVisibleRect;
                if (widgetController) {
                    widgetController.visibleRectChanged(newVisibleRect);
                }
            });*/

            scope.$on('gridster-item-initialized', function (event, item) {
                gridsterItem = item;
                if (widgetController) {
                    widgetController.gridsterItemInitialized(gridsterItem);
                }
            });

            //TODO:
            //elem.html('<div id="progress-cover" flex layout="column" layout-align="center center" style="height: 100%;">' +
            //          '     <md-progress-circular md-mode="indeterminate" class="md-accent md-hue-2" md-diameter="120"></md-progress-circular>' +
            //          '</div>');

            //var progressElement = angular.element(elem[0].querySelector('#progress-cover'));
            //var progressScope = scope.$new();
            //$compile(elem.contents())(progressScope);

            widgetService.getWidgetInfo(widget.bundleAlias, widget.typeAlias, widget.isSystemType).then(
                function(widgetInfo) {
                    loadFromWidgetInfo(widgetInfo);
                }
            );

            function loadFromWidgetInfo(widgetInfo) {

                scope.loadingData = true;

                elem.addClass("tb-widget");

                var widgetNamespace = "widget-type-" + (widget.isSystemType ? 'sys-' : '')
                    + widget.bundleAlias + '-'
                    + widget.typeAlias;

                elem.addClass(widgetNamespace);

                var widgetType = widgetService.getWidgetTypeFunction(widget.bundleAlias, widget.typeAlias, widget.isSystemType);

                angular.extend(locals, {$scope: scope, $element: elem, widgetInfo: widgetInfo, widgetType: widgetType});

                widgetController = $controller('WidgetController', locals);

                if (gridsterItem) {
                    widgetController.gridsterItemInitialized(gridsterItem);
                }
            }
        }
    };
}
