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
import 'brace/ext/language_tools';
import 'brace/mode/json';
import 'brace/theme/github';
import beautify from 'js-beautify';

import './alarm-details-dialog.scss';

const js_beautify = beautify.js;

/*@ngInject*/
export default function AlarmDetailsDialogController($mdDialog, $filter, $translate, types,
                                                     alarmService, alarmId, allowAcknowledgment, allowClear, showingCallback) {

    var vm = this;

    vm.alarmId = alarmId;
    vm.allowAcknowledgment = allowAcknowledgment;
    vm.allowClear = allowClear;
    vm.types = types;
    vm.alarm = null;

    vm.alarmUpdated = false;

    showingCallback.onShowing = function(scope, element) {
        updateEditorSize(element);
    }

    vm.alarmDetailsOptions = {
        useWrapMode: false,
        mode: 'json',
        showGutter: false,
        showPrintMargin: false,
        theme: 'github',
        advanced: {
            enableSnippets: false,
            enableBasicAutocompletion: false,
            enableLiveAutocompletion: false
        },
        onLoad: function (_ace) {
            vm.editor = _ace;
        }
    };

    vm.close = close;
    vm.acknowledge = acknowledge;
    vm.clear = clear;

    loadAlarm();

    function updateEditorSize(element) {
        var newWidth = 600;
        var newHeight = 200;
        angular.element('#tb-alarm-details', element).height(newHeight.toString() + "px")
            .width(newWidth.toString() + "px");
        vm.editor.resize();
    }

    function loadAlarm() {
        alarmService.getAlarmInfo(vm.alarmId).then(
            function success(alarm) {
                vm.alarm = alarm;
                loadAlarmFields();
            },
            function fail() {
                vm.alarm = null;
            }
        );
    }

    function loadAlarmFields() {
        vm.createdTime = $filter('date')(vm.alarm.createdTime, 'yyyy-MM-dd HH:mm:ss');
        vm.startTime = null;
        if (vm.alarm.startTs) {
            vm.startTime = $filter('date')(vm.alarm.startTs, 'yyyy-MM-dd HH:mm:ss');
        }
        vm.endTime = null;
        if (vm.alarm.endTs) {
            vm.endTime = $filter('date')(vm.alarm.endTs, 'yyyy-MM-dd HH:mm:ss');
        }
        vm.ackTime = null;
        if (vm.alarm.ackTs) {
            vm.ackTime = $filter('date')(vm.alarm.ackTs, 'yyyy-MM-dd HH:mm:ss')
        }
        vm.clearTime = null;
        if (vm.alarm.clearTs) {
            vm.clearTime = $filter('date')(vm.alarm.clearTs, 'yyyy-MM-dd HH:mm:ss');
        }

        vm.alarmSeverity = $translate.instant(types.alarmSeverity[vm.alarm.severity].name);

        vm.alarmStatus = $translate.instant('alarm.display-status.' + vm.alarm.status);

        vm.alarmDetails = null;
        if (vm.alarm.details) {
            vm.alarmDetails = angular.toJson(vm.alarm.details);
            vm.alarmDetails = js_beautify(vm.alarmDetails, {indent_size: 4});
        }
    }

    function acknowledge () {
        alarmService.ackAlarm(vm.alarmId).then(
            function success() {
                vm.alarmUpdated = true;
                loadAlarm();
            }
        );
    }

    function clear () {
        alarmService.clearAlarm(vm.alarmId).then(
            function success() {
                vm.alarmUpdated = true;
                loadAlarm();
            }
        );
    }

    function close () {
        $mdDialog.hide(vm.alarmUpdated ? vm.alarm : null);
    }

}
