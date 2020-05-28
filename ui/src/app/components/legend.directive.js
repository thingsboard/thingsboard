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
import './legend.scss';

/* eslint-disable import/no-unresolved, import/default */

import legendTemplate from './legend.tpl.html';

/* eslint-enable import/no-unresolved, import/default */


export default angular.module('thingsboard.directives.legend', [])
    .directive('tbLegend', Legend)
    .name;

/*@ngInject*/
function Legend($compile, $templateCache, types) {

    var linker = function (scope, element) {
        var template = $templateCache.get(legendTemplate);
        element.html(template);

        scope.displayHeader = function() {
            return scope.legendConfig.showMin === true ||
                   scope.legendConfig.showMax === true ||
                   scope.legendConfig.showAvg === true ||
                   scope.legendConfig.showTotal === true;
        }

        scope.isHorizontal = scope.legendConfig.position === types.position.bottom.value ||
            scope.legendConfig.position === types.position.top.value;

        scope.isRowDirection = scope.legendConfig.direction === types.direction.row.value;

        scope.toggleHideData = function(index) {
            if (!scope.legendData.keys[index].dataKey.settings.disableDataHiding) {
                scope.legendData.keys[index].dataKey.hidden = !scope.legendData.keys[index].dataKey.hidden;
            }
        }

        $compile(element.contents())(scope);

    }

    /*    scope.legendData = {
     keys: [],
     data: []

     key: {
       dataKey: dataKey,
       dataIndex: 0
     }
     data: {
       min: null,
       max: null,
       avg: null,
       total: null
     }
     };*/

    return {
        restrict: "E",
        link: linker,
        scope: {
            legendConfig: '=',
            legendData: '='
        }
    };
}
