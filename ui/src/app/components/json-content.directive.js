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
import './json-content.scss';

import 'brace/ext/language_tools';
import 'brace/mode/json';
import 'brace/mode/text';
import 'brace/snippets/json';
import 'brace/snippets/text';

import fixAceEditor from './ace-editor-fix';

/* eslint-disable import/no-unresolved, import/default */

import jsonContentTemplate from './json-content.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import beautify from 'js-beautify';

const js_beautify = beautify.js;

export default angular.module('thingsboard.directives.jsonContent', [])
    .directive('tbJsonContent', JsonContent)
    .name;

/*@ngInject*/
function JsonContent($compile, $templateCache, toast, types, utils) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(jsonContentTemplate);
        element.html(template);

        scope.label = attrs.label;

        scope.validationTriggerArg = attrs.validationTriggerArg;

        scope.contentValid = true;

        scope.json_editor;

        scope.onFullscreenChanged = function () {
            updateEditorSize();
        };

        scope.beautifyJson = function () {
            var res = js_beautify(scope.contentBody, {indent_size: 4, wrap_line_length: 60});
            scope.contentBody = res;
        };

        function updateEditorSize() {
            if (scope.json_editor) {
                scope.json_editor.resize();
                scope.json_editor.renderer.updateFull();
            }
        }

        var mode;
        if (scope.contentType) {
            mode = types.contentType[scope.contentType].code;
        } else {
            mode = 'text';
        }

        scope.jsonEditorOptions = {
            useWrapMode: true,
            mode: mode,
            advanced: {
                enableSnippets: true,
                enableBasicAutocompletion: true,
                enableLiveAutocompletion: true
            },
            onLoad: function (_ace) {
                scope.json_editor = _ace;
                scope.json_editor.session.on("change", function () {
                    scope.cleanupJsonErrors();
                });
                fixAceEditor(_ace);
            }
        };

        scope.$watch('contentType', () => {
            var mode;
            if (scope.contentType) {
                mode = types.contentType[scope.contentType].code;
            } else {
                mode = 'text';
            }
            if (scope.json_editor) {
                scope.json_editor.session.setMode('ace/mode/' + mode);
            }
        });

        scope.cleanupJsonErrors = function () {
            toast.hide();
        };

        scope.updateValidity = function () {
            ngModelCtrl.$setValidity('contentBody', scope.contentValid);
        };

        scope.$watch('contentBody', function (newContent, oldContent) {
            ngModelCtrl.$setViewValue(scope.contentBody);
            if (!angular.equals(newContent, oldContent)) {
                scope.contentValid = true;
            }
            scope.updateValidity();
        });

        ngModelCtrl.$render = function () {
            scope.contentBody = ngModelCtrl.$viewValue;
        };

        scope.showError = function (error) {
            var toastParent = angular.element('#tb-json-panel', element);
            toast.showError(error, toastParent, 'bottom left');
        };

        scope.validate = function () {
            try {
                if (scope.validateContent) {
                    if (scope.contentType == types.contentType.JSON.value) {
                        angular.fromJson(scope.contentBody);
                    }
                }
                return true;
            } catch (e) {
                var details = utils.parseException(e);
                var errorInfo = 'Error:';
                if (details.name) {
                    errorInfo += ' ' + details.name + ':';
                }
                if (details.message) {
                    errorInfo += ' ' + details.message;
                }
                scope.showError(errorInfo);
                return false;
            }
        };

        scope.$on('form-submit', function (event, args) {
            if (!scope.readonly) {
                if (!args || scope.validationTriggerArg && scope.validationTriggerArg == args) {
                    scope.cleanupJsonErrors();
                    scope.contentValid = true;
                    scope.updateValidity();
                    scope.contentValid = scope.validate();
                    scope.updateValidity();
                }
            }
        });

        scope.$on('update-ace-editor-size', function () {
            updateEditorSize();
        });

        $compile(element.contents())(scope);
    }

    return {
        restrict: "E",
        require: "^ngModel",
        scope: {
            contentType: '=',
            validateContent: '=?',
            readonly:'=ngReadonly',
            fillHeight:'=?'
        },
        link: linker
    };
}
