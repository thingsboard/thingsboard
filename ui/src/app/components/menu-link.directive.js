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
import './menu-link.scss';

import thingsboardMenu from '../services/menu.service';

/* eslint-disable import/no-unresolved, import/default */

import menulinkTemplate from './menu-link.tpl.html';
import menutoggleTemplate from './menu-toggle.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.directives.menuLink', [thingsboardMenu])
    .directive('tbMenuLink', MenuLink)
    .filter('nospace', NoSpace)
    .name;

/*@ngInject*/
function MenuLink($compile, $templateCache, menu) {

    var linker = function (scope, element) {
        var template;

        if (scope.section.type === 'link') {
            template = $templateCache.get(menulinkTemplate);
        } else {
            template = $templateCache.get(menutoggleTemplate);

            var parentNode = element[0].parentNode.parentNode.parentNode;
            if (parentNode.classList.contains('parent-list-item')) {
                var heading = parentNode.querySelector('h2');
                element[0].firstChild.setAttribute('aria-describedby', heading.id);
            }

            scope.sectionActive = function () {
                return menu.sectionActive(scope.section);
            };

            scope.sectionHeight = function () {
                return menu.sectionHeight(scope.section);
            };
        }

        element.html(template);

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        link: linker,
        scope: {
            section: '='
        }
    };
}

function NoSpace() {
    return function (value) {
        return (!value) ? '' : value.replace(/ /g, '');
    }
}
