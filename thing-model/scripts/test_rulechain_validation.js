#!/usr/bin/env node
'use strict';

const assert = require('assert');
const fs = require('fs');
const path = require('path');
const vm = require('vm');

const rulechainDir = path.join(__dirname, '..', 'src', 'main', 'resources', 'rulechain');
const validationTemplate = fs.readFileSync(
    path.join(rulechainDir, 'telemetry_to_kafka.js'), 'utf8');
const normalizeScript = fs.readFileSync(
    path.join(rulechainDir, 'apply_envelope.js'), 'utf8');

const schema = {
    schemaVersion: '1.0',
    productId: 'EVTPRODUCT01',
    tslVersion: '1.0',
    strictUnknown: false,
    keys: {
        speed: {t: 'float', min: 0, max: 200, req: true},
        count: {t: 'int', min: 0},
        online: {t: 'bool'},
        mode: {t: 'enum', map: {'0': 'auto', '1': 'manual'}},
        ntp: {t: 'timestamp'},
        label: {t: 'string'},
        id: {t: 'string'}
    },
    events: {
        overSpeed: {
            type: 'alert',
            params: {
                speed: {t: 'float', min: 0, max: 300},
                limit: {t: 'int', min: 0, max: 200}
            }
        },
        underSpeed: {
            type: 'info',
            params: {
                speed: {t: 'float', min: 0, max: 300},
                limit: {t: 'int', min: 0, max: 200}
            }
        }
    }
};

function renderValidationScript(compactSchema) {
    return validationTemplate.replace(
        '__TSL_SCHEMA__',
        JSON.stringify(JSON.stringify(compactSchema)));
}

function runValidation(msg, metadata, compactSchema) {
    const sandbox = {
        msg,
        metadata: JSON.parse(JSON.stringify(metadata || {})),
        msgType: 'POST_TELEMETRY_REQUEST',
        JSON,
        Date,
        isFinite,
        Array,
        Object,
        String,
        Number
    };
    const script = new vm.Script(
        '(function() {\n' + renderValidationScript(compactSchema || schema) + '\n})()');
    const out = script.runInNewContext(sandbox);
    return {
        out,
        envelope: JSON.parse(out.metadata.tsl_normalized)
    };
}

function runNormalize(msg, metadata) {
    const sandbox = {
        msg,
        metadata: metadata || {},
        msgType: 'POST_TELEMETRY_REQUEST',
        JSON,
        Object
    };
    return new vm.Script('(function() {\n' + normalizeScript + '\n})()')
        .runInNewContext(sandbox);
}

function expectInvalid(payload, code, metadata, compactSchema) {
    const result = runValidation(payload, metadata, compactSchema);
    assert.strictEqual(result.envelope.kind, 'invalid', code + ' should be invalid');
    assert.ok(
        result.envelope.invalid.includes(code),
        code + ' not found in ' + JSON.stringify(result.envelope.invalid));
    return result.envelope;
}

const telemetryMetadata = {
    ts: '1699999999000',
    ss_tenant_id: 'tenant-1',
    ss_device_uuid: 'device-1',
    deviceName: 'sim-device',
    ss_site_code: 'SIM'
};

const validTelemetry = runValidation({
    speed: 80.5,
    count: 10,
    online: 1,
    mode: 1,
    ntp: 1700000000,
    label: 'ok'
}, telemetryMetadata);

assert.strictEqual(validTelemetry.envelope.kind, 'telemetry');
assert.strictEqual(validTelemetry.envelope.v, 1);
assert.strictEqual(validTelemetry.envelope.dropped, null);
assert.strictEqual(validTelemetry.envelope.invalid, null);
assert.strictEqual(validTelemetry.envelope.productId, 'EVTPRODUCT01');
assert.strictEqual(validTelemetry.envelope.tslVersion, '1.0');
assert.strictEqual(validTelemetry.envelope.deviceName, 'sim-device');
assert.strictEqual(validTelemetry.envelope.values.online, true);
assert.strictEqual(validTelemetry.envelope.values.ntp, 1700000000000);
assert.strictEqual(validTelemetry.envelope.ts, 1699999999000);

expectInvalid({speed: 'fast'}, 'speed:float', telemetryMetadata);
expectInvalid({speed: 300}, 'speed:max', telemetryMetadata);
expectInvalid({count: 3000000000}, 'count:int32', telemetryMetadata);
expectInvalid({mode: 9}, 'mode:enum', telemetryMetadata);
expectInvalid({count: 1}, 'speed:missing', telemetryMetadata);
expectInvalid({speed: 1, label: 'x'.repeat(5000)}, 'payload:size', telemetryMetadata);

const dropped = runValidation({speed: 1, unknownKey: 'x'}, telemetryMetadata);
assert.strictEqual(dropped.envelope.kind, 'telemetry');
assert.deepStrictEqual(dropped.envelope.dropped, ['unknownKey']);
assert.strictEqual(dropped.envelope.invalid, null);

const strictSchema = Object.assign({}, schema, {strictUnknown: true});
const strict = runValidation({speed: 1, unknownKey: 'x'}, telemetryMetadata, strictSchema);
assert.strictEqual(strict.envelope.kind, 'invalid');
assert.ok(strict.envelope.invalid.includes('unknownKey:unknown'));

const eventMetadata = Object.assign({}, telemetryMetadata, {ts: '1700000000000'});
const validEvent = runValidation({
    event_id: 'overSpeed',
    event_type: 'alert',
    event_ts: 1700000000000,
    event_uid: 'uid-001',
    speed: 120,
    limit: 100
}, eventMetadata);

assert.strictEqual(validEvent.envelope.kind, 'event');
assert.strictEqual(validEvent.envelope.v, 1);
assert.strictEqual(validEvent.envelope.values, null);
assert.strictEqual(validEvent.envelope.dropped, null);
assert.strictEqual(validEvent.envelope.invalid, null);
assert.strictEqual(validEvent.envelope.event.id, 'overSpeed');
assert.strictEqual(validEvent.envelope.event.code, undefined);
assert.strictEqual(validEvent.envelope.event.data.speed, 120);
assert.strictEqual(validEvent.envelope.event.data.limit, 100);
assert.strictEqual(validEvent.envelope.event.type, 'alert');
assert.strictEqual(validEvent.envelope.event.level, undefined);
assert.strictEqual(validEvent.envelope.event.ts, 1700000000000);

const fallbackEvent = runValidation({
    event_id: 'overSpeed',
    event_type: 'alert',
    event_ts: 1700000000000,
    speed: 120,
    limit: 100
}, eventMetadata);
assert.strictEqual(fallbackEvent.envelope.kind, 'event');
assert.strictEqual(fallbackEvent.envelope.event.id, 'overSpeed');

const idAliasEvent = runValidation({
    id: 'overSpeed',
    speed: 120,
    limit: 100
}, eventMetadata);
assert.strictEqual(idAliasEvent.envelope.kind, 'event');
assert.strictEqual(idAliasEvent.envelope.values, null);
assert.strictEqual(idAliasEvent.envelope.event.id, 'overSpeed');
assert.strictEqual(idAliasEvent.envelope.event.type, 'alert');
assert.strictEqual(idAliasEvent.envelope.event.data.speed, 120);
assert.strictEqual(idAliasEvent.envelope.event.data.limit, 100);

const eventIdPrecedence = runValidation({
    event_id: 'overSpeed',
    id: 'other',
    event_type: 'alert',
    speed: 120,
    limit: 100
}, eventMetadata);
assert.strictEqual(eventIdPrecedence.envelope.kind, 'event');
assert.strictEqual(eventIdPrecedence.envelope.event.id, 'overSpeed');

const idProperty = runValidation({id: 'device-1', speed: 1}, telemetryMetadata);
assert.strictEqual(idProperty.envelope.kind, 'telemetry');
assert.strictEqual(idProperty.envelope.values.id, 'device-1');

expectInvalid({
    event_id: 'unknownEvent',
    event_type: 'alert',
    speed: 1
}, 'event_id:unknown', eventMetadata);
expectInvalid({
    id: 'unknownEvent',
    event_type: 'alert',
    speed: 1
}, 'event_id:unknown', eventMetadata);

const mismatch = runValidation({
    event_id: 'overSpeed',
    event_type: 'info',
    speed: 120,
    limit: 100
}, eventMetadata);
assert.strictEqual(mismatch.envelope.kind, 'invalid');
assert.ok(mismatch.envelope.invalid.includes('event_type:mismatch'));
assert.strictEqual(mismatch.envelope.event.type, 'alert');

expectInvalid({event_type: 'alert', speed: 1}, 'event_type:orphan', eventMetadata);
expectInvalid({
    event_id: 'overSpeed',
    event_type: 'alert',
    speed: 1,
    limit: 300
}, 'limit:max', eventMetadata);
expectInvalid({event_id: 'bad-id', speed: 1}, 'event_id:invalid', eventMetadata);

const eventBatch = runValidation({
    events: [
        {
            event_id: 'overSpeed',
            event_type: 'alert',
            event_ts: 1700000000000,
            speed: 120,
            limit: 100
        },
        {
            id: 'overSpeed',
            event_ts: 1700000000001,
            speed: 130,
            limit: 90
        }
    ]
}, eventMetadata);

assert.strictEqual(eventBatch.envelope.kind, 'event');
assert.strictEqual(eventBatch.envelope.event, null);
assert.strictEqual(eventBatch.envelope.values, null);
assert.strictEqual(eventBatch.envelope.invalid, null);
assert.strictEqual(eventBatch.envelope.events.length, 2);
assert.strictEqual(eventBatch.envelope.events[0].id, 'overSpeed');
assert.strictEqual(eventBatch.envelope.events[0].type, 'alert');
assert.strictEqual(eventBatch.envelope.events[0].level, undefined);
assert.strictEqual(eventBatch.envelope.events[0].data.speed, 120);
assert.strictEqual(eventBatch.envelope.events[1].id, 'overSpeed');
assert.strictEqual(eventBatch.envelope.events[1].ts, 1700000000001);

const invalidEventBatch = runValidation({
    events: [
        {event_id: 'unknownEvent', speed: 1},
        {event_id: 'overSpeed', speed: 1, limit: 300}
    ]
}, eventMetadata);
assert.strictEqual(invalidEventBatch.envelope.kind, 'invalid');
assert.ok(invalidEventBatch.envelope.invalid.includes('events[0].event_id:unknown'));
assert.ok(invalidEventBatch.envelope.invalid.includes('events[1].limit:max'));
assert.strictEqual(invalidEventBatch.envelope.event, null);

const mixedEventBatch = runValidation({
    events: [
        {event_id: 'overSpeed', speed: 120, limit: 100},
        {event_id: 'underSpeed', speed: 40, limit: 100}
    ]
}, eventMetadata);
assert.strictEqual(mixedEventBatch.envelope.kind, 'invalid');
assert.ok(mixedEventBatch.envelope.invalid.includes('events:mixed'));

expectInvalid({events: []}, 'events:empty', eventMetadata);
expectInvalid({events: [1]}, 'events[0]:type', eventMetadata);

const normalized = runNormalize(validTelemetry.out.msg, validTelemetry.out.metadata);
const normalizedEnvelope = typeof normalized.msg === 'string'
    ? JSON.parse(normalized.msg)
    : normalized.msg;
assert.strictEqual(normalizedEnvelope.kind, 'telemetry');
assert.strictEqual(normalizedEnvelope.v, 1);
assert.strictEqual(normalizedEnvelope.productId, 'EVTPRODUCT01');
assert.strictEqual(normalized.metadata.tsl_normalized, undefined);

console.log('rule chain validation tests passed');
