///
/// Copyright © 2016-2026 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import Long from 'long';
import uuidParse from 'uuid-parse';

export function UUIDFromBuffer(buf: Buffer): string {
    return uuidParse.unparse(buf);
}

export function UUIDToBits(uuidString: string): [string, string] {
    const bytes = Array.from(uuidParse.parse(uuidString));
    const msb = Long.fromBytes(bytes.slice(0, 8), false, false).toString();
    const lsb = Long.fromBytes(bytes.slice(-8), false, false).toString();
    return [msb, lsb];
}

export function isString(value: any): boolean {
    return typeof value === 'string';
}

export function parseJsErrorDetails(err: any): string | undefined {
    if (!err) {
        return undefined;
    }
    let details = err.name + ': ' + err.message;
    if (err.stack) {
        const lines = err.stack.split('\n');
        if (lines && lines.length) {
            const line = lines[0];
            const split = line.split(':');
            if (split && split.length === 2) {
                if (!isNaN(split[1])) {
                    details += ' in at line number ' + split[1];
                }
            }
        }
    }
    return details;
}

export function isNotEmptyStr(value: any): boolean {
    return typeof value === 'string' && value.trim().length > 0;
}
