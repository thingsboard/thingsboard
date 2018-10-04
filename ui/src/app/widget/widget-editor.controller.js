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
import $ from 'jquery';
import ace from 'brace';
import 'brace/ext/language_tools';
import 'brace/mode/javascript';
import 'brace/mode/html';
import 'brace/mode/css';
import 'brace/mode/json';
import 'brace/snippets/javascript';
import 'brace/snippets/text';
import 'brace/snippets/html';
import 'brace/snippets/css';
import 'brace/snippets/json';

/* eslint-disable import/no-unresolved, import/default */

import saveWidgetTypeAsTemplate from './save-widget-type-as.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

import Split from 'split.js';
import beautify from 'js-beautify';

const js_beautify = beautify.js;
const html_beautify = beautify.html;
const css_beautify = beautify.css;

/* eslint-disable angular/angularelement */

/*@ngInject*/
export default function WidgetEditorController(widgetService, userService, types, toast, hotkeys,
                                               $element, $rootScope, $scope, $state, $stateParams, $timeout,
                                               $window, $document, $translate, $mdDialog) {

    var Range = ace.acequire("ace/range").Range;
    var ace_editors = [];
    var js_editor;
    var iframe = $('iframe', $element);
    var gotError = false;
    var errorMarkers = [];
    var errorAnnotationId = -1;
    var elem = $($element);

    var widgetsBundleId = $stateParams.widgetsBundleId;

    var vm = this;

    vm.widgetsBundle;
    vm.isDirty = false;
    vm.fullscreen = false;
    vm.widgetType = null;
    vm.widget = null;
    vm.origWidget = null;
    vm.widgetTypes = types.widgetType;
    vm.iframeWidgetEditModeInited = false;
    vm.layoutInited = false;
    vm.htmlEditorOptions = {
        useWrapMode: true,
        mode: 'html',
        advanced: {
            enableSnippets: true,
            enableBasicAutocompletion: true,
            enableLiveAutocompletion: true
        },
        onLoad: function (_ace) {
            ace_editors.push(_ace);
        }
    };
    vm.cssEditorOptions = {
        useWrapMode: true,
        mode: 'css',
        advanced: {
            enableSnippets: true,
            enableBasicAutocompletion: true,
            enableLiveAutocompletion: true
        },
        onLoad: function (_ace) {
            ace_editors.push(_ace);
        }
    };
    vm.jsonSettingsEditorOptions = {
        useWrapMode: true,
        mode: 'json',
        advanced: {
            enableSnippets: true,
            enableBasicAutocompletion: true,
            enableLiveAutocompletion: true
        },
        onLoad: function (_ace) {
            ace_editors.push(_ace);
        }
    };
    vm.dataKeyJsonSettingsEditorOptions = {
        useWrapMode: true,
        mode: 'json',
        advanced: {
            enableSnippets: true,
            enableBasicAutocompletion: true,
            enableLiveAutocompletion: true
        },
        onLoad: function (_ace) {
            ace_editors.push(_ace);
        }
    };
    vm.jsEditorOptions = {
        useWrapMode: true,
        mode: 'javascript',
        advanced: {
            enableSnippets: true,
            enableBasicAutocompletion: true,
            enableLiveAutocompletion: true
        },
        onLoad: function (_ace) {
            ace_editors.push(_ace);
            js_editor = _ace;
            js_editor.session.on("change", function () {
                cleanupJsErrors();
            });
        }
    };

    vm.addResource = addResource;
    vm.applyWidgetScript = applyWidgetScript;
    vm.beautifyCss = beautifyCss;
    vm.beautifyDataKeyJson = beautifyDataKeyJson;
    vm.beautifyHtml = beautifyHtml;
    vm.beautifyJs = beautifyJs;
    vm.beautifyJson = beautifyJson;
    vm.removeResource = removeResource;
    vm.undoDisabled = undoDisabled;
    vm.undoWidget = undoWidget;
    vm.saveDisabled = saveDisabled;
    vm.saveWidget = saveWidget;
    vm.saveAsDisabled = saveAsDisabled;
    vm.saveWidgetAs = saveWidgetAs;
    vm.toggleFullscreen = toggleFullscreen;
    vm.isReadOnly = isReadOnly;

    initWidgetEditor();

    function initWidgetEditor() {

        $rootScope.loading = true;

        widgetService.getWidgetsBundle(widgetsBundleId).then(
            function success(widgetsBundle) {
                vm.widgetsBundle = widgetsBundle;
                if ($stateParams.widgetTypeId) {
                    widgetService.getWidgetTypeById($stateParams.widgetTypeId).then(
                        function success(widgetType) {
                            setWidgetType(widgetType)
                            widgetTypeLoaded();
                        },
                        function fail() {
                            toast.showError($translate.instant('widget.widget-type-load-failed-error'));
                            widgetTypeLoaded();
                        }
                    );
                } else {
                    var type = $stateParams.widgetType;
                    if (!type) {
                        type = types.widgetType.timeseries.value;
                    }
                    widgetService.getWidgetTemplate(type).then(
                        function success(widgetTemplate) {
                            vm.widget = angular.copy(widgetTemplate);
                            vm.widget.widgetName = null;
                            vm.origWidget = angular.copy(vm.widget);
                            vm.isDirty = true;
                            widgetTypeLoaded();
                        },
                        function fail() {
                            toast.showError($translate.instant('widget.widget-template-load-failed-error'));
                            widgetTypeLoaded();
                        }
                    );
                }
            },
            function fail() {
                toast.showError($translate.instant('widget.widget-type-load-failed-error'));
                widgetTypeLoaded();
            }
        );

    }

    function setWidgetType(widgetType) {
        vm.widgetType = widgetType;
        vm.widget = widgetService.toWidgetInfo(vm.widgetType);
        var config = angular.fromJson(vm.widget.defaultConfig);
        vm.widget.defaultConfig = angular.toJson(config)
        vm.origWidget = angular.copy(vm.widget);
        vm.isDirty = false;
    }

    function widgetTypeLoaded() {
        initHotKeys();

        initWatchers();

        angular.element($document[0]).ready(function () {
            var w = elem.width();
            if (w > 0) {
                initSplitLayout();
            } else {
                $scope.$watch(
                    function () {
                        return elem[0].offsetWidth || parseInt(elem.css('width'), 10);
                    },
                    function (newSize) {
                        if (newSize > 0) {
                            initSplitLayout();
                        }
                    }
                );
            }
        });

        iframe.attr('data-widget', angular.toJson(vm.widget));
        iframe.attr('src', '/widget-editor');
    }

    function undoDisabled() {
        return $scope.loading
                || !vm.isDirty
                || !vm.iframeWidgetEditModeInited
                || vm.saveWidgetPending
                || vm.saveWidgetAsPending;
    }

    function saveDisabled() {
        return vm.isReadOnly()
                || $scope.loading
                || !vm.isDirty
                || !vm.iframeWidgetEditModeInited
                || vm.saveWidgetPending
                || vm.saveWidgetAsPending;
    }

    function saveAsDisabled() {
        return $scope.loading
            || !vm.iframeWidgetEditModeInited
            || vm.saveWidgetPending
            || vm.saveWidgetAsPending;
    }

    function initHotKeys() {
        $translate(['widget.undo', 'widget.save', 'widget.saveAs', 'widget.toggle-fullscreen', 'widget.run']).then(function (translations) {
            hotkeys.bindTo($scope)
                .add({
                    combo: 'ctrl+q',
                    description: translations['widget.undo'],
                    allowIn: ['INPUT', 'SELECT', 'TEXTAREA'],
                    callback: function (event) {
                        if (!undoDisabled()) {
                            event.preventDefault();
                            undoWidget();
                        }
                    }
                })
                .add({
                    combo: 'ctrl+s',
                    description: translations['widget.save'],
                    allowIn: ['INPUT', 'SELECT', 'TEXTAREA'],
                    callback: function (event) {
                        if (!saveDisabled()) {
                            event.preventDefault();
                            saveWidget();
                        }
                    }
                })
                .add({
                    combo: 'shift+ctrl+s',
                    description: translations['widget.saveAs'],
                    allowIn: ['INPUT', 'SELECT', 'TEXTAREA'],
                    callback: function (event) {
                        if (!saveAsDisabled()) {
                            event.preventDefault();
                            saveWidgetAs();
                        }
                    }
                })
                .add({
                    combo: 'shift+ctrl+f',
                    description: translations['widget.toggle-fullscreen'],
                    allowIn: ['INPUT', 'SELECT', 'TEXTAREA'],
                    callback: function (event) {
                        event.preventDefault();
                        toggleFullscreen();
                    }
                })
                .add({
                    combo: 'ctrl+enter',
                    description: translations['widget.run'],
                    allowIn: ['INPUT', 'SELECT', 'TEXTAREA'],
                    callback: function (event) {
                        event.preventDefault();
                        applyWidgetScript();
                    }
            });
        });
    }

    function initWatchWidget() {
        $scope.widgetWatcher = $scope.$watch('vm.widget', function (newVal, oldVal) {
            if (!angular.equals(newVal, oldVal)) {
                vm.isDirty = true;
            }
        }, true);
    }

    function initWatchers() {
        initWatchWidget();

        $scope.$watch('vm.widget.type', function (newVal, oldVal) {
            if (!angular.equals(newVal, oldVal)) {
                var config = angular.fromJson(vm.widget.defaultConfig);
                if (vm.widget.type !== types.widgetType.rpc.value
                    && vm.widget.type !== types.widgetType.alarm.value) {
                    if (config.targetDeviceAliases) {
                        delete config.targetDeviceAliases;
                    }
                    if (config.alarmSource) {
                        delete config.alarmSource;
                    }
                    if (!config.datasources) {
                        config.datasources = [];
                    }
                    if (!config.timewindow) {
                        config.timewindow = {
                            realtime: {
                                timewindowMs: 60000
                            }
                        };
                    }
                    for (var i = 0; i < config.datasources.length; i++) {
                        var datasource = config.datasources[i];
                        datasource.type = vm.widget.type;
                    }
                } else if (vm.widget.type == types.widgetType.rpc.value) {
                    if (config.datasources) {
                        delete config.datasources;
                    }
                    if (config.alarmSource) {
                        delete config.alarmSource;
                    }
                    if (config.timewindow) {
                        delete config.timewindow;
                    }
                    if (!config.targetDeviceAliases) {
                        config.targetDeviceAliases = [];
                    }
                } else { // alarm
                    if (config.datasources) {
                        delete config.datasources;
                    }
                    if (config.targetDeviceAliases) {
                        delete config.targetDeviceAliases;
                    }
                    if (!config.alarmSource) {
                        config.alarmSource = {};
                        config.alarmSource.type = vm.widget.type
                    }
                    if (!config.timewindow) {
                        config.timewindow = {
                            realtime: {
                                timewindowMs: 24 * 60 * 60 * 1000
                            }
                        };
                    }
                }
                vm.widget.defaultConfig = angular.toJson(config);
            }
        });

        $scope.$on('widgetEditModeInited', function () {
            vm.iframeWidgetEditModeInited = true;
            if (vm.saveWidgetPending || vm.saveWidgetAsPending) {
                if (!vm.saveWidgetTimeout) {
                    vm.saveWidgetTimeout = $timeout(function () {
                        if (!gotError) {
                            if (vm.saveWidgetPending) {
                                commitSaveWidget();
                            } else if (vm.saveWidgetAsPending) {
                                commitSaveWidgetAs();
                            }
                        } else {
                            toast.showError($translate.instant('widget.unable-to-save-widget-error'));
                            vm.saveWidgetPending = false;
                            vm.saveWidgetAsPending = false;
                            initWatchWidget();
                        }
                        vm.saveWidgetTimeout = undefined;
                    }, 1500);
                }
            }
        });

        $scope.$on('widgetEditUpdated', function (event, widget) {
            vm.widget.sizeX = widget.sizeX / 2;
            vm.widget.sizeY = widget.sizeY / 2;
            vm.widget.defaultConfig = angular.toJson(widget.config);
            iframe.attr('data-widget', angular.toJson(vm.widget));
        });

        $scope.$on('widgetException', function (event, details) {
            if (!gotError) {
                gotError = true;
                var errorInfo = 'Error:';
                if (details.name) {
                    errorInfo += ' ' + details.name + ':';
                }
                if (details.message) {
                    errorInfo += ' ' + details.message;
                }
                if (details.lineNumber) {
                    errorInfo += '<br>Line ' + details.lineNumber;
                    if (details.columnNumber) {
                        errorInfo += ' column ' + details.columnNumber;
                    }
                    errorInfo += ' of script.';
                }
                if (!vm.saveWidgetPending && !vm.saveWidgetAsPending) {
                    toast.showError(errorInfo, $('#javascript_panel', $element)[0]);
                }
                if (js_editor && details.lineNumber) {
                    var line = details.lineNumber - 1;
                    var column = 0;
                    if (details.columnNumber) {
                        column = details.columnNumber;
                    }

                    var errorMarkerId = js_editor.session.addMarker(new Range(line, 0, line, Infinity), "ace_active-line", "screenLine");
                    errorMarkers.push(errorMarkerId);
                    var annotations = js_editor.session.getAnnotations();
                    var errorAnnotation = {
                        row: line,
                        column: column,
                        text: details.message,
                        type: "error"
                    };
                    errorAnnotationId = annotations.push(errorAnnotation) - 1;
                    js_editor.session.setAnnotations(annotations);
                }
            }
        });
    }

    function cleanupJsErrors() {
        toast.hide();
        for (var i = 0; i < errorMarkers.length; i++) {
            js_editor.session.removeMarker(errorMarkers[i]);
        }
        errorMarkers = [];
        if (errorAnnotationId && errorAnnotationId > -1) {
            var annotations = js_editor.session.getAnnotations();
            annotations.splice(errorAnnotationId, 1);
            js_editor.session.setAnnotations(annotations);
            errorAnnotationId = -1;
        }
    }

    function onDividerDrag() {
        for (var i = 0; i < ace_editors.length; i++) {
            var ace = ace_editors[i];
            ace.resize();
            ace.renderer.updateFull();
        }
    }

    function initSplitLayout() {
        if (!vm.layoutInited) {
            Split([$('#top_panel', $element)[0], $('#bottom_panel', $element)[0]], {
                sizes: [35, 65],
                gutterSize: 8,
                cursor: 'row-resize',
                direction: 'vertical',
                onDrag: function () {
                    onDividerDrag()
                }
            });

            Split([$('#top_left_panel', $element)[0], $('#top_right_panel', $element)[0]], {
                sizes: [50, 50],
                gutterSize: 8,
                cursor: 'col-resize',
                onDrag: function () {
                    onDividerDrag()
                }
            });

            Split([$('#javascript_panel', $element)[0], $('#frame_panel', $element)[0]], {
                sizes: [50, 50],
                gutterSize: 8,
                cursor: 'col-resize',
                onDrag: function () {
                    onDividerDrag()
                }
            });

            onDividerDrag();

            $scope.$applyAsync(function () {
                vm.layoutInited = true;
                $rootScope.loading = false;
                var w = angular.element($window);
                $timeout(function () {
                    w.triggerHandler('resize')
                });
            });

        }
    }

    function removeResource(index) {
        if (index > -1) {
            vm.widget.resources.splice(index, 1);
        }
    }

    function addResource() {
        vm.widget.resources.push({url: ''});
    }

    function applyWidgetScript() {
        cleanupJsErrors();
        gotError = false;
        vm.iframeWidgetEditModeInited = false;
        var config = angular.fromJson(vm.widget.defaultConfig);
        config.title = vm.widget.widgetName;
        vm.widget.defaultConfig = angular.toJson(config);
        iframe.attr('data-widget', angular.toJson(vm.widget));
        iframe[0].contentWindow.location.reload(true);
    }

    function toggleFullscreen() {
        vm.fullscreen = !vm.fullscreen;
    }

    function isReadOnly() {
        if (userService.getAuthority() === 'TENANT_ADMIN') {
            return !vm.widgetsBundle || vm.widgetsBundle.tenantId.id === types.id.nullUid;
        } else {
            return userService.getAuthority() != 'SYS_ADMIN';
        }
    }

    function undoWidget() {
        if ($scope.widgetWatcher) {
            $scope.widgetWatcher();
        }
        vm.widget = angular.copy(vm.origWidget);
        vm.isDirty = false;
        initWatchWidget();
        applyWidgetScript();
    }

    function saveWidget() {
        if (!vm.widget.widgetName) {
            toast.showError($translate.instant('widget.missing-widget-title-error'));
        } else {
            $scope.widgetWatcher();
            vm.saveWidgetPending = true;
            applyWidgetScript();
        }
    }

    function saveWidgetAs($event) {
        $scope.widgetWatcher();
        vm.saveWidgetAsPending = true;
        vm.saveWidgetAsEvent = $event;
        applyWidgetScript();
    }

    function commitSaveWidget() {
        var id = (vm.widgetType && vm.widgetType.id) ? vm.widgetType.id : undefined;
        widgetService.saveWidgetType(vm.widget, id, vm.widgetsBundle.alias).then(
            function success(widgetType) {
                setWidgetType(widgetType)
                vm.saveWidgetPending = false;
                initWatchWidget();
                toast.showSuccess($translate.instant('widget.widget-saved'), 500);
            }, function fail() {
                vm.saveWidgetPending = false;
                initWatchWidget();
            }
        );
    }

    function commitSaveWidgetAs() {
        $mdDialog.show({
            controller: 'SaveWidgetTypeAsController',
            controllerAs: 'vm',
            templateUrl: saveWidgetTypeAsTemplate,
            parent: angular.element($document[0].body),
            fullscreen: true,
            targetEvent: vm.saveWidgetAsEvent
        }).then(function (saveWidgetAsData) {
            vm.widget.widgetName = saveWidgetAsData.widgetName;
            vm.widget.alias = undefined;
            var config = angular.fromJson(vm.widget.defaultConfig);
            config.title = vm.widget.widgetName;
            vm.widget.defaultConfig = angular.toJson(config);

            vm.saveWidgetAsPending = false;
            vm.isDirty = false;
            initWatchWidget();
            widgetService.saveWidgetType(vm.widget, undefined, saveWidgetAsData.bundleAlias).then(
                function success(widgetType) {
                    $state.go('home.widgets-bundles.widget-types.widget-type',
                        {widgetsBundleId: saveWidgetAsData.bundleId, widgetTypeId: widgetType.id.id});
                },
                function fail() {
                    vm.saveWidgetAsPending = false;
                    initWatchWidget();
                }
            );
        }, function () {
            vm.saveWidgetAsPending = false;
            initWatchWidget();
        });
    }

    function beautifyJs() {
        var res = js_beautify(vm.widget.controllerScript, {indent_size: 4, wrap_line_length: 60});
        vm.widget.controllerScript = res;
    }

    function beautifyHtml() {
        var res = html_beautify(vm.widget.templateHtml, {indent_size: 4, wrap_line_length: 60});
        vm.widget.templateHtml = res;
    }

    function beautifyCss() {
        var res = css_beautify(vm.widget.templateCss, {indent_size: 4});
        vm.widget.templateCss = res;
    }

    function beautifyJson() {
        var res = js_beautify(vm.widget.settingsSchema, {indent_size: 4});
        vm.widget.settingsSchema = res;
    }

    function beautifyDataKeyJson() {
        var res = js_beautify(vm.widget.dataKeySettingsSchema, {indent_size: 4});
        vm.widget.dataKeySettingsSchema = res;
    }

}

/* eslint-enable angular/angularelement */
