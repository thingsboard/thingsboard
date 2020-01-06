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
const SPECIAL_CHARS_REGEXP = /([:\-_]+(.))/g;
const MOZ_HACK_REGEXP = /^moz([A-Z])/;
const PREFIX_REGEXP = /^((?:x|data)[:\-_])/i;

var tbEventDirectives = {};

angular.forEach(
    'click dblclick mousedown mouseup mouseover mouseout mousemove mouseenter mouseleave contextmenu keydown keyup keypress submit focus blur copy cut paste'.split(' '),
    function(eventName) {
        var directiveName = directiveNormalize('tb-' + eventName);
        tbEventDirectives[directiveName] = ['$parse', '$rootScope', function($parse) {
            return {
                restrict: 'A',
                compile: function($element, attr) {
                    var fn = $parse(attr[directiveName], /* interceptorFn */ null, /* expensiveChecks */ true);
                    return function ngEventHandler(scope, element) {
                        element.on(eventName, function(event) {
                            var callback = function() {
                                fn(scope, {$event:event});
                            };
                            callback();
                        });
                    };
                }
            };
        }];
    }
);

export default angular.module('thingsboard.directives.event', [])
    .directive(tbEventDirectives)
    .name;

function camelCase(name) {
    return name.
    replace(SPECIAL_CHARS_REGEXP, function(_, separator, letter, offset) {
        return offset ? letter.toUpperCase() : letter;
    }).
    replace(MOZ_HACK_REGEXP, 'Moz$1');
}

function directiveNormalize(name) {
    return camelCase(name.replace(PREFIX_REGEXP, ''));
}