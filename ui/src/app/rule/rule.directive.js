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
import './rule.scss';

/* eslint-disable import/no-unresolved, import/default */

import ruleFieldsetTemplate from './rule-fieldset.tpl.html';

/* eslint-enable import/no-unresolved, import/default */

/*@ngInject*/
export default function RuleDirective($compile, $templateCache, $mdDialog, $document, $q, $translate, pluginService,
                                      componentDialogService, componentDescriptorService, types, toast) {
    var linker = function (scope, element) {
        var template = $templateCache.get(ruleFieldsetTemplate);
        element.html(template);

        scope.plugin = null;
        scope.types = types;
        scope.filters = [];

        scope.addFilter = function($event) {
            componentDialogService.openComponentDialog($event, true, false,
                'rule.filter', types.componentType.filter).then(
                function success(filter) {
                    scope.filters.push({ value: filter });
                },
                function fail() {}
            );
        }

        scope.removeFilter = function ($event, filter) {
            var index = scope.filters.indexOf(filter);
            if (index > -1) {
                scope.filters.splice(index, 1);
            }
        };

        scope.addProcessor = function($event) {
            componentDialogService.openComponentDialog($event, true, false,
                'rule.processor', types.componentType.processor).then(
                function success(processor) {
                    scope.rule.processor = processor;
                },
                function fail() {}
            );
        }

        scope.removeProcessor = function() {
            if (scope.rule.processor) {
                scope.rule.processor = null;
            }
        }

        scope.addAction = function($event) {
            componentDialogService.openComponentDialog($event, true, false,
                'rule.plugin-action', types.componentType.action, scope.plugin.clazz).then(
                function success(action) {
                    scope.rule.action = action;
                },
                function fail() {}
            );
        }

        scope.removeAction = function() {
            if (scope.rule.action) {
                scope.rule.action = null;
            }
        }

        scope.updateValidity = function () {
            if (scope.rule) {
                var valid = scope.rule.filters && scope.rule.filters.length > 0;
                scope.theForm.$setValidity('filters', valid);
                var processorDefined = angular.isDefined(scope.rule.processor) && scope.rule.processor != null;
                var pluginDefined = angular.isDefined(scope.rule.pluginToken) && scope.rule.pluginToken != null;
                var pluginActionDefined = angular.isDefined(scope.rule.action) && scope.rule.action != null;
                valid = processorDefined && !pluginDefined || (pluginDefined && pluginActionDefined);
                scope.theForm.$setValidity('processorOrPlugin', valid);
            }
        };

        scope.onRuleIdCopied = function() {
            toast.showSuccess($translate.instant('rule.idCopiedMessage'), 750, angular.element(element).parent().parent(), 'bottom left');
        };

        scope.$watch('rule', function(newVal, prevVal) {
                if (newVal) {
                    if (!scope.rule.filters) {
                        scope.rule.filters = [];
                    }
                    if (!angular.equals(newVal, prevVal)) {
                        if (scope.rule.pluginToken) {
                            pluginService.getPluginByToken(scope.rule.pluginToken).then(
                                function success(plugin) {
                                    scope.plugin = plugin;
                                },
                                function fail() {}
                            );
                        } else {
                            scope.plugin = null;
                        }
                        if (scope.filters) {
                            scope.filters.splice(0, scope.filters.length);
                        } else {
                            scope.filters = [];
                        }
                        if (scope.rule.filters) {
                            for (var i in scope.rule.filters) {
                                scope.filters.push({value: scope.rule.filters[i]});
                            }
                        }
                    }
                    scope.updateValidity();
                }
            }
        );

        scope.$watch('filters', function (newVal, prevVal) {
            if (scope.rule && scope.isEdit && !angular.equals(newVal, prevVal)) {
                if (scope.rule.filters) {
                    scope.rule.filters.splice(0, scope.rule.filters.length);
                } else {
                    scope.rule.filters = [];
                }
                if (scope.filters) {
                    for (var i in scope.filters) {
                        scope.rule.filters.push(scope.filters[i].value);
                    }
                }
                scope.theForm.$setDirty();
                scope.updateValidity();
            }
        }, true);

        scope.$watch('plugin', function(newVal, prevVal) {
            if (scope.rule && scope.isEdit && !angular.equals(newVal, prevVal)) {
                if (newVal) {
                    scope.rule.pluginToken = scope.plugin.apiToken;
                } else {
                    scope.rule.pluginToken = null;
                }
                scope.rule.action = null;
                scope.updateValidity();
            }
        }, true);

        scope.$watch('rule.processor', function(newVal, prevVal) {
            if (scope.rule && scope.isEdit && !angular.equals(newVal, prevVal)) {
                scope.theForm.$setDirty();
                scope.updateValidity();
            }
        }, true);

        scope.$watch('rule.action', function(newVal, prevVal) {
            if (scope.rule && scope.isEdit && !angular.equals(newVal, prevVal)) {
                scope.theForm.$setDirty();
                scope.updateValidity();
            }
        }, true);

        $compile(element.contents())(scope);
    }
    return {
        restrict: "E",
        link: linker,
        scope: {
            rule: '=',
            isEdit: '=',
            isReadOnly: '=',
            theForm: '=',
            onActivateRule: '&',
            onSuspendRule: '&',
            onExportRule: '&',
            onDeleteRule: '&'
        }
    };
}
