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
import $ from 'jquery';
import 'brace/ext/language_tools';
import 'brace/ext/searchbox';
import 'brace/mode/java';
import 'brace/theme/github';

/* eslint-disable angular/angularelement */

import './audit-log-details-dialog.scss';

/*@ngInject*/
export default function AuditLogDetailsDialogController($mdDialog, types, auditLog, showingCallback) {

    var vm = this;

    showingCallback.onShowing = function(scope, element) {
        updateEditorSize(element, vm.actionData, 'tb-audit-log-action-data');
        vm.actionDataEditor.resize();
        if (vm.displayFailureDetails) {
            updateEditorSize(element, vm.actionFailureDetails, 'tb-audit-log-failure-details');
            vm.failureDetailsEditor.resize();
        }
    };

    vm.types = types;
    vm.auditLog = auditLog;
    vm.displayFailureDetails = auditLog.actionStatus == types.auditLogActionStatus.FAILURE.value;
    vm.actionData = auditLog.actionDataText;
    vm.actionFailureDetails = auditLog.actionFailureDetails;

    vm.actionDataContentOptions = {
        useWrapMode: false,
        mode: 'java',
        showGutter: false,
        showPrintMargin: false,
        theme: 'github',
        advanced: {
            enableSnippets: false,
            enableBasicAutocompletion: false,
            enableLiveAutocompletion: false
        },
        onLoad: function (_ace) {
            vm.actionDataEditor = _ace;
        }
    };

    vm.failureDetailsContentOptions = {
        useWrapMode: false,
        mode: 'java',
        showGutter: false,
        showPrintMargin: false,
        theme: 'github',
        advanced: {
            enableSnippets: false,
            enableBasicAutocompletion: false,
            enableLiveAutocompletion: false
        },
        onLoad: function (_ace) {
            vm.failureDetailsEditor = _ace;
        }
    };

    function updateEditorSize(element, content, editorId) {
        var newHeight = 200;
        var newWidth = 600;
        if (content && content.length > 0) {
            var lines = content.split('\n');
            newHeight = 16 * lines.length + 16;
            var maxLineLength = 0;
            for (var i in lines) {
                var line = lines[i].replace(/\t/g, '    ').replace(/\n/g, '');
                var lineLength = line.length;
                maxLineLength = Math.max(maxLineLength, lineLength);
            }
            newWidth = 8 * maxLineLength + 16;
        }
        $('#'+editorId, element).height(newHeight.toString() + "px").css('min-height', newHeight.toString() + "px")
            .width(newWidth.toString() + "px");
    }

    vm.close = close;

    function close () {
        $mdDialog.hide();
    }

}

/* eslint-enable angular/angularelement */
