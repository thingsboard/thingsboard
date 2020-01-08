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
'use strict';

const Long = require('long'),
      uuidParse = require('uuid-parse');

exports.toUUIDString = function(mostSigBits, leastSigBits) {
    var msbBytes = Long.fromValue(mostSigBits, false).toBytes(false);
    var lsbBytes = Long.fromValue(leastSigBits, false).toBytes(false);
    var uuidBytes = msbBytes.concat(lsbBytes);
    return uuidParse.unparse(uuidBytes);
}

exports.UUIDFromBuffer = function(buf) {
    return uuidParse.unparse(buf);
}

exports.UUIDToBits = function(uuidString) {
    const bytes = uuidParse.parse(uuidString);
    var msb = Long.fromBytes(bytes.slice(0,8), false, false).toString();
    var lsb = Long.fromBytes(bytes.slice(-8), false, false).toString();
    return [msb, lsb];
}
