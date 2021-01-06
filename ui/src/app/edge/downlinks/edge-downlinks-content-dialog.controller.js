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
import beautify from 'js-beautify';

/* eslint-disable angular/angularelement */

const js_beautify = beautify.js;

/*@ngInject*/
export default function EdgeDownlinksContentDialogController($mdDialog, types, content, contentType, title, showingCallback) {

    var vm = this;

    showingCallback.onShowing = function(scope, element) {
        updateEditorSize(element);
    }

    vm.content = content;
    vm.title = title;

    var mode;
    if (contentType) {
        mode = types.contentType[contentType].code;
        if (contentType == types.contentType.JSON.value && vm.content) {
            vm.content = js_beautify(vm.content, {indent_size: 4});
        }
    } else {
        mode = 'java';
    }

    vm.contentOptions = {
        useWrapMode: false,
        mode: mode,
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

    function updateEditorSize(element) {
        var newHeight = 400;
        var newWidth = 600;
        if (vm.content && vm.content.length > 0) {
            var lines = vm.content.split('\n');
            newHeight = 16 * lines.length + 16;
            var maxLineLength = 0;
            for (var i = 0; i < lines.length; i++) {
                var line = lines[i].replace(/\t/g, '    ').replace(/\n/g, '');
                var lineLength = line.length;
                maxLineLength = Math.max(maxLineLength, lineLength);
            }
            newWidth = 8 * maxLineLength + 16;
        }
        $('#tb-event-content', element).height(newHeight.toString() + "px")
            .width(newWidth.toString() + "px");
        vm.editor.resize();
    }

    vm.close = close;

    function close () {
        $mdDialog.hide();
    }

}

/* eslint-enable angular/angularelement */
