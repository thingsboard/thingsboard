/*
 * Copyright © 2016-2019 The Thingsboard Authors
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

const vm = require('vm');

function JsExecutor() {
}

JsExecutor.prototype.compileScript = function(code) {
    return new Promise(function(resolve, reject) {
        try {
            code = "("+code+")(...args)";
            var script = new vm.Script(code);
            resolve(script);
        } catch (err) {
            reject(err);
        }
    });
}

JsExecutor.prototype.executeScript = function(script, args, timeout) {
    return new Promise(function(resolve, reject) {
        try {
            var sandbox = Object.create(null);
            sandbox.args = args;
            var result = script.runInNewContext(sandbox, {timeout: timeout});
            resolve(result);
        } catch (err) {
            reject(err);
        }
    });
}

module.exports = JsExecutor;
