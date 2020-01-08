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
export default angular.module('thingsboard.clipboard', [])
    .factory('clipboardService', ClipboardService)
    .name;

/*@ngInject*/
function ClipboardService($q) {

    var fakeHandler, fakeHandlerCallback, fakeElem;

    var service = {
        copyToClipboard: copyToClipboard
    };

    return service;

    /* eslint-disable */
    function copyToClipboard(trigger, text) {
        var deferred = $q.defer();
        const isRTL = document.documentElement.getAttribute('dir') == 'rtl';
        removeFake();
        fakeHandlerCallback = () => removeFake();
        fakeHandler = document.body.addEventListener('click', fakeHandlerCallback) || true;
        fakeElem = document.createElement('textarea');
        fakeElem.style.fontSize = '12pt';
        fakeElem.style.border = '0';
        fakeElem.style.padding = '0';
        fakeElem.style.margin = '0';
        fakeElem.style.position = 'absolute';
        fakeElem.style[ isRTL ? 'right' : 'left' ] = '-9999px';
        let yPosition = window.pageYOffset || document.documentElement.scrollTop;
        fakeElem.style.top = `${yPosition}px`;
        fakeElem.setAttribute('readonly', '');
        fakeElem.value = text;
        document.body.appendChild(fakeElem);
        var selectedText = select(fakeElem);

        let succeeded;
        try {
            succeeded = document.execCommand('copy');
        }
        catch (err) {
            succeeded = false;
        }
        if (trigger) {
            trigger.focus();
        }
        window.getSelection().removeAllRanges();
        removeFake();
        if (succeeded) {
            deferred.resolve(selectedText);
        } else {
            deferred.reject();
        }
        return deferred.promise;
    }

    function removeFake() {
        if (fakeHandler) {
            document.body.removeEventListener('click', fakeHandlerCallback);
            fakeHandler = null;
            fakeHandlerCallback = null;
        }
        if (fakeElem) {
            document.body.removeChild(fakeElem);
            fakeElem = null;
        }
    }

    function select(element) {
        var selectedText;

        if (element.nodeName === 'SELECT') {
            element.focus();

            selectedText = element.value;
        }
        else if (element.nodeName === 'INPUT' || element.nodeName === 'TEXTAREA') {
            var isReadOnly = element.hasAttribute('readonly');

            if (!isReadOnly) {
                element.setAttribute('readonly', '');
            }

            element.select();
            element.setSelectionRange(0, element.value.length);

            if (!isReadOnly) {
                element.removeAttribute('readonly');
            }

            selectedText = element.value;
        }
        else {
            if (element.hasAttribute('contenteditable')) {
                element.focus();
            }

            var selection = window.getSelection();
            var range = document.createRange();

            range.selectNodeContents(element);
            selection.removeAllRanges();
            selection.addRange(range);

            selectedText = selection.toString();
        }

        return selectedText;
    }

    /* eslint-enable */

}