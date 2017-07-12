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

export function utf8Encode(str) {
    var result;

    if (angular.isUndefined(Uint8Array)) { // eslint-disable-line no-undef
        result = utf8ToBytes(str);
    } else {
        result = new Uint8Array(utf8ToBytes(str)); // eslint-disable-line no-undef
    }

    return result;
}

export function utf8Decode(bytes) {
    return utf8Slice(bytes, 0, bytes.length);
}

function utf8Slice (buf, start, end) {
    var res = ''
    var tmp = ''
    end = Math.min(buf.length, end || Infinity)
    start = start || 0;

    for (var i = start; i < end; i++) {
        if (buf[i] <= 0x7F) {
            res += decodeUtf8Char(tmp) + String.fromCharCode(buf[i])
            tmp = ''
        } else {
            tmp += '%' + buf[i].toString(16)
        }
    }

    return res + decodeUtf8Char(tmp)
}

function decodeUtf8Char (str) {
    try {
        return decodeURIComponent(str)
    } catch (err) {
        return String.fromCharCode(0xFFFD) // UTF 8 invalid char
    }
}

function utf8ToBytes (string, units) {
    units = units || Infinity
    var codePoint
    var length = string.length
    var leadSurrogate = null
    var bytes = []
    var i = 0

    for (; i < length; i++) {
        codePoint = string.charCodeAt(i)

        // is surrogate component
        if (codePoint > 0xD7FF && codePoint < 0xE000) {
            // last char was a lead
            if (leadSurrogate) {
                // 2 leads in a row
                if (codePoint < 0xDC00) {
                    if ((units -= 3) > -1) bytes.push(0xEF, 0xBF, 0xBD)
                    leadSurrogate = codePoint
                    continue
                } else {
                    // valid surrogate pair
                    codePoint = leadSurrogate - 0xD800 << 10 | codePoint - 0xDC00 | 0x10000
                    leadSurrogate = null
                }
            } else {
                // no lead yet

                if (codePoint > 0xDBFF) {
                    // unexpected trail
                    if ((units -= 3) > -1) bytes.push(0xEF, 0xBF, 0xBD)
                    continue
                } else if (i + 1 === length) {
                    // unpaired lead
                    if ((units -= 3) > -1) bytes.push(0xEF, 0xBF, 0xBD)
                    continue
                } else {
                    // valid lead
                    leadSurrogate = codePoint
                    continue
                }
            }
        } else if (leadSurrogate) {
            // valid bmp char, but last char was a lead
            if ((units -= 3) > -1) bytes.push(0xEF, 0xBF, 0xBD)
            leadSurrogate = null
        }

        // encode utf8
        if (codePoint < 0x80) {
            if ((units -= 1) < 0) break
            bytes.push(codePoint)
        } else if (codePoint < 0x800) {
            if ((units -= 2) < 0) break
            bytes.push(
                codePoint >> 0x6 | 0xC0,
                codePoint & 0x3F | 0x80
            )
        } else if (codePoint < 0x10000) {
            if ((units -= 3) < 0) break
            bytes.push(
                codePoint >> 0xC | 0xE0,
                codePoint >> 0x6 & 0x3F | 0x80,
                codePoint & 0x3F | 0x80
            )
        } else if (codePoint < 0x200000) {
            if ((units -= 4) < 0) break
            bytes.push(
                codePoint >> 0x12 | 0xF0,
                codePoint >> 0xC & 0x3F | 0x80,
                codePoint >> 0x6 & 0x3F | 0x80,
                codePoint & 0x3F | 0x80
            )
        } else {
            throw new Error('Invalid code point')
        }
    }

    return bytes
}