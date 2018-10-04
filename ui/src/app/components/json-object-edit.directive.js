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
import './json-object-edit.scss';

import 'brace/ext/language_tools';
import 'brace/mode/json';
import 'brace/snippets/json';

import fixAceEditor from './ace-editor-fix';

/* eslint-disable import/no-unresolved, import/default */

import jsonObjectEditTemplate from './json-object-edit.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

export default angular.module('thingsboard.directives.jsonObjectEdit', [])
    .directive('tbJsonObjectEdit', JsonObjectEdit)
    .name;

/*@ngInject*/
function JsonObjectEdit($compile, $templateCache, $document, toast, utils) {

    var linker = function (scope, element, attrs, ngModelCtrl) {
        var template = $templateCache.get(jsonObjectEditTemplate);
        element.html(template);

        scope.label = attrs.label;

        scope.objectValid = true;
        scope.validationError = '';

        scope.json_editor;

        scope.onFullscreenChanged = function () {
            updateEditorSize();
        };

        function updateEditorSize() {
            if (scope.json_editor) {
                scope.json_editor.resize();
                scope.json_editor.renderer.updateFull();
            }
        }

        scope.jsonEditorOptions = {
            useWrapMode: true,
            mode: 'json',
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

        scope.cleanupJsonErrors = function () {
            toast.hide();
        };

        scope.updateValidity = function () {
            ngModelCtrl.$setValidity('objectValid', scope.objectValid);
        };

        scope.$watch('contentBody', function (newVal, prevVal) {
            if (!angular.equals(newVal, prevVal)) {
                var object = scope.validate();
                if (scope.objectValid) {
                    if (object == null) {
                        scope.object = null;
                    } else {
                        if (scope.object == null) {
                            scope.object = {};
                        }
                        Object.keys(scope.object).forEach(function (key) {
                            delete scope.object[key];
                        });
                        Object.keys(object).forEach(function (key) {
                            scope.object[key] = object[key];
                        });
                    }
                    ngModelCtrl.$setViewValue(scope.object);
                }
                scope.updateValidity();
            }
        });

        ngModelCtrl.$render = function () {
            scope.object = ngModelCtrl.$viewValue;
            var content = '';
            try {
                if (scope.object) {
                    content = angular.toJson(scope.object, true);
                }
            } catch (e) {
                //
            }
            scope.contentBody = content;
        };

        scope.showError = function (error) {
            var toastParent = angular.element('#tb-json-panel', element);
            toast.showError(error, toastParent, 'bottom left');
        };

        scope.validate = function () {
            if (!scope.contentBody || !scope.contentBody.length) {
                if (scope.required) {
                    scope.validationError = 'Json object is required.';
                    scope.objectValid = false;
                } else {
                    scope.validationError = '';
                    scope.objectValid = true;
                }
                return null;
            } else {
                try {
                    var object = angular.fromJson(scope.contentBody);
                    scope.validationError = '';
                    scope.objectValid = true;
                    return object;
                } catch (e) {
                    var details = utils.parseException(e);
                    var errorInfo = 'Error:';
                    if (details.name) {
                        errorInfo += ' ' + details.name + ':';
                    }
                    if (details.message) {
                        errorInfo += ' ' + details.message;
                    }
                    scope.validationError = errorInfo;
                    scope.objectValid = false;
                    return null;
                }
            }
        };

        scope.$on('form-submit', function () {
            if (!scope.readonly) {
                scope.cleanupJsonErrors();
                if (!scope.objectValid) {
                    scope.showError(scope.validationError);
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
            required:'=ngRequired',
            readonly:'=ngReadonly',
            fillHeight:'=?'
        },
        link: linker
    };
}
