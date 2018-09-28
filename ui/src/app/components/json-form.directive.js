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
import './json-form.scss';

import tinycolor from 'tinycolor2';
import ObjectPath from 'objectpath';
import inspector from 'schema-inspector';
import ReactSchemaForm from './react/json-form-react.jsx';
import jsonFormTemplate from './json-form.tpl.html';
import { utils } from 'react-schema-form';

export default angular.module('thingsboard.directives.jsonForm', [])
    .directive('tbJsonForm', JsonForm)
    .value('ReactSchemaForm', ReactSchemaForm)
    .name;

/*@ngInject*/
function JsonForm($compile, $templateCache, $mdColorPicker) {

    var linker = function (scope, element) {

        var template = $templateCache.get(jsonFormTemplate);

        element.html(template);

        var childScope;

        var destroyModelChangeWatches = function() {
            if (scope.modelWatchHandle) {
                scope.modelWatchHandle();
            }
            if (scope.modelRefWatchHandle) {
                scope.modelRefWatchHandle();
            }
        }

        var initModelChangeWatches = function() {
            scope.modelWatchHandle = scope.$watch('model',function(newValue, prevValue) {
                if (newValue && prevValue && !angular.equals(newValue,prevValue)) {
                    scope.validate();
                    if (scope.formControl) {
                        scope.formControl.$setDirty();
                    }
                }
            }, true);
            scope.modelRefWatchHandle = scope.$watch('model',function(newValue, prevValue) {
                if (newValue && newValue != prevValue) {
                    scope.updateValues();
                }
            });
        };

        var recompile = function() {
            if (childScope) {
                childScope.$destroy();
            }
            childScope = scope.$new();
            $compile(element.contents())(childScope);
        }

        scope.formProps = {
            option: {
                formDefaults: {
                    startEmpty: true
                }
            },
            onModelChange: function(key, val) {
                if (angular.isString(val) && val === '') {
                    val = undefined;
                }
                selectOrSet(key, scope.model, val);
                scope.formProps.model = scope.model;
            },
            onColorClick: function(event, key, val) {
                scope.showColorPicker(event, val);
            }
        };

        scope.showColorPicker = function (event, color) {
            $mdColorPicker.show({
                value: tinycolor(color).toRgbString(),
                defaultValue: '#fff',
                random: tinycolor.random(),
                clickOutsideToClose: false,
                hasBackdrop: false,
                skipHide: true,
                preserveScope: false,

                mdColorAlphaChannel: true,
                mdColorSpectrum: true,
                mdColorSliders: true,
                mdColorGenericPalette: false,
                mdColorMaterialPalette: true,
                mdColorHistory: false,
                mdColorDefaultTab: 2,

                $event: event

            }).then(function (color) {
                if (event.data && event.data.onValueChanged) {
                    event.data.onValueChanged(tinycolor(color).toRgb());
                }
            });
        }

        scope.validate = function(){
            if (scope.schema && scope.model) {
                var result = utils.validateBySchema(scope.schema, scope.model);
                if (scope.formControl) {
                    scope.formControl.$setValidity('jsonForm', result.valid);
                }
            }
        }

        scope.updateValues = function(skipRerender) {
            destroyModelChangeWatches();
            if (!skipRerender) {
                element.html(template);
            }
            var readonly = (scope.readonly && scope.readonly === true) ? true : false;
            var schema = scope.schema ? angular.copy(scope.schema) : {
                    type: 'object'
                };
            schema.strict = true;
            var form = scope.form ? angular.copy(scope.form) : [ "*" ];
            var model = scope.model || {};
            scope.model = inspector.sanitize(schema, model).data;
            scope.formProps.option.formDefaults.readonly = readonly;
            scope.formProps.schema = schema;
            scope.formProps.form = form;
            scope.formProps.model = angular.copy(scope.model);
            if (!skipRerender) {
                recompile();
            }
            initModelChangeWatches();
        }

        scope.updateValues(true);

        scope.$watch('readonly',function() {
            scope.updateValues();
        });

        scope.$watch('schema',function(newValue, prevValue) {
            if (newValue && newValue != prevValue) {
                scope.updateValues();
                scope.validate();
            }
        });

        scope.$watch('form',function(newValue, prevValue) {
            if (newValue && newValue != prevValue) {
                scope.updateValues();
            }
        });

        scope.validate();

        recompile();

    }

    return {
        restrict: "E",
        scope: {
            schema: '=',
            form: '=',
            model: '=',
            formControl: '=',
            readonly: '='
        },
        link: linker
    };

}

function setValue(obj, key, val) {
    var changed = false;
    if (obj) {
        if (angular.isUndefined(val)) {
            if (angular.isDefined(obj[key])) {
                delete obj[key];
                changed = true;
            }
        } else {
            changed = !angular.equals(obj[key], val);
            obj[key] = val;
        }
    }
    return changed;
}

function selectOrSet(projection, obj, valueToSet) {
    var numRe = /^\d+$/;

    if (!obj) {
        obj = this;
    }

    if (!obj) {
        return false;
    }

    var parts = angular.isString(projection) ? ObjectPath.parse(projection) : projection;

    if (parts.length === 1) {
        return setValue(obj, parts[0], valueToSet);
    }

    if (angular.isUndefined(obj[parts[0]])) {
        obj[parts[0]] = parts.length > 2 && numRe.test(parts[1]) ? [] : {};
    }

    var value = obj[parts[0]];
    for (var i = 1; i < parts.length; i++) {
        if (parts[i] === '') {
            return false;
        }
        if (i === parts.length - 1) {
            return setValue(value, parts[i], valueToSet);
        } else {
            var tmp = value[parts[i]];
            if (angular.isUndefined(tmp) || tmp === null) {
                tmp = numRe.test(parts[i + 1]) ? [] : {};
                value[parts[i]] = tmp;
            }
            value = tmp;
        }
    }
    return value;
}
