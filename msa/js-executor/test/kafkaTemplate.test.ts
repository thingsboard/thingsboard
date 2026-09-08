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

import { describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { KafkaTemplate } from '../queue/kafkaTemplate';

// Same suite name as jsExecutor.test.ts so every case lands under
// <testsuite name="js-executor"> in the JUnit XML.
describe('js-executor', () => {

const template = new KafkaTemplate();
const resolveSsl = (configuredSsl: boolean, useSasl: boolean, protocol?: any): boolean =>
    (template as any).resolveSslEnabled(configuredSsl, useSasl, protocol);
const isTrue = (value: any): boolean => (template as any).isTrue(value);

test('kafka ssl.enabled is parsed as a string, not by truthiness', () => {
    // node-config hands back environment overrides as strings; Boolean("false") is true
    assert.equal(isTrue('false'), false, '"false" must not enable SSL');
    assert.equal(isTrue(false), false);
    assert.equal(isTrue('true'), true);
    assert.equal(isTrue(true), true);
    assert.equal(isTrue('TRUE'), true, 'value must be case-insensitive, like the tb-node flags');
    assert.equal(isTrue(' true '), true);
    assert.equal(isTrue('yes'), false);
    assert.equal(isTrue(undefined), false);
});

test('kafka ssl is enabled for the SSL security protocols', () => {
    assert.equal(resolveSsl(false, true, 'SASL_SSL'), true);
    assert.equal(resolveSsl(false, true, 'SSL'), true);
    assert.equal(resolveSsl(false, true, 'sasl_ssl'), true, 'protocol must be case-insensitive');
});

test('kafka ssl stays off for the plaintext security protocols', () => {
    assert.equal(resolveSsl(false, true, 'SASL_PLAINTEXT'), false);
    assert.equal(resolveSsl(false, true, 'PLAINTEXT'), false);
    assert.equal(resolveSsl(false, true, ' sasl_plaintext '), false);
});

test('kafka ssl.enabled overrides the security protocol', () => {
    assert.equal(resolveSsl(true, true, 'SASL_PLAINTEXT'), true,
        'an explicit ssl.enabled=true must win, as it does in tb-node');
    assert.equal(resolveSsl(true, false, 'PLAINTEXT'), true);
});

test('kafka security protocol is ignored when SASL is off', () => {
    assert.equal(resolveSsl(false, false, 'SASL_SSL'), false,
        'the protocol only applies when use_confluent_cloud is true');
});

test('kafka ssl stays on for an unusable security protocol', () => {
    // Failing open would put SASL credentials on an unencrypted connection
    assert.equal(resolveSsl(false, true, 'SSL_SASL'), true, 'a typo must not disable TLS');
    assert.equal(resolveSsl(false, true, ''), true);
    assert.equal(resolveSsl(false, true, null), true, 'a blank key in a preserved default.yml');
    assert.equal(resolveSsl(false, true, undefined), true);
    assert.equal(resolveSsl(false, true, 1), true, 'a non-string value must not throw');
});

});
