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
(function () {
    if (!String.prototype.startsWith) {
        String.prototype.startsWith = function(searchString, position) {
            position = position || 0;
            return this.indexOf(searchString, position) === position;
        };
    }
    if (!String.prototype.endsWith) {
        String.prototype.endsWith = function (suffix) {
            return this.indexOf(suffix, this.length - suffix.length) !== -1;
        };
    }
    if (!String.prototype.repeat) {
        String.prototype.repeat = function(count) {
            if (this == null) {
                throw TypeError();
            }
            var string = String(this);
            // `ToInteger`
            var n = count ? Number(count) : 0;
            if (n != n) { // better `isNaN`
                n = 0;
            }
            // Account for out-of-bounds indices
            if (n < 0 || n == Infinity) {
                throw RangeError();
            }
            var result = '';
            while (n) {
                if (n % 2 == 1) {
                    result += string;
                }
                if (n > 1) {
                    string += string;
                }
                n >>= 1;
            }
            return result;
        };
    }
    if (!String.prototype.includes) {
        String.prototype.includes = function(search, start) {
            if (angular.isNumber(start)) {
                start = 0;
            }

            if (start + search.length > this.length) {
                return false;
            } else {
                return this.indexOf(search, start) !== -1;
            }
        };
    }

    (function (arr) {
        arr.forEach(function (item) {
            if (Object.prototype.hasOwnProperty.call(item, 'remove')) {
                return;
            }
            Object.defineProperty(item, 'remove', {
                configurable: true,
                enumerable: true,
                writable: true,
                value: function remove() {
                    if (this.parentNode !== null)
                        this.parentNode.removeChild(this);
                }
            });
        });
    })([Element.prototype, CharacterData.prototype, DocumentType.prototype]); //eslint-disable-line

})();