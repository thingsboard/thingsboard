/*
 * Copyright © 2016-2019 The Thingsboard Authors
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

import edgeFieldsetTemplate from './edge-fieldset.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function EdgeDirective($compile, $templateCache, $translate, $mdDialog, $document, toast, types) {
    var linker = function (scope, element) {
        var template = $templateCache.get(edgeFieldsetTemplate);
        element.html(template);

        scope.types = types;

        scope.onEdgeIdCopied = function() {
            toast.showSuccess($translate.instant('edge.idCopiedMessage'), 750, angular.element(element).parent().parent(), 'bottom left');
        };

        $compile(element.contents())(scope);

    };
    return {
        restrict: "E",
        link: linker,
        scope: {
            edge: '=',
            isEdit: '=',
            theForm: '=',
            isCreate: '<',
            onExportEdge: '&',
            onDeleteEdge: '&'
        }
    };
}
