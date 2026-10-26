var out = {msg: msg, metadata: metadata || {}, msgType: msgType};
var raw = metadata.ss_tsl_schema;
var schema = null;
var strict = false;
if (raw) {
    try {
        schema = (typeof raw === 'string') ? JSON.parse(raw) : raw;
        strict = schema.strictUnknown === true;
    } catch (e) {
        metadata.tsl_invalid = '_schema:parse';
    }
}
var keys = schema ? (schema.keys || schema) : null;
var productId = schema ? schema.productId : null;
var values = {};
var dropped = [];
var invalid = [];
var reserved = {event_id: true, event_type: true, event_ts: true};

Object.keys(msg).forEach(function (k) {
    if (reserved[k]) {
        return;
    }
    var v = msg[k];
    if (keys) {
        var d = keys[k];
        if (!d) {
            if (strict) {
                invalid.push(k + ':unknown');
            } else {
                dropped.push(k);
            }
            return;
        }
        var t = d.t;
        var ok = true;
        if (t === 'int' || t === 'timestamp' || t === 'enum') {
            ok = (typeof v === 'number' && v % 1 === 0);
        } else if (t === 'float') {
            ok = (typeof v === 'number');
        } else if (t === 'bool') {
            ok = (typeof v === 'boolean' || v === 0 || v === 1);
        } else if (t === 'string') {
            ok = (typeof v === 'string');
        }
        if (!ok) {
            invalid.push(k + ':' + t);
            return;
        }
        if (typeof v === 'number') {
            if (typeof d.min === 'number' && v < d.min) {
                invalid.push(k + ':min');
            }
            if (typeof d.max === 'number' && v > d.max) {
                invalid.push(k + ':max');
            }
        }
    }
    values[k] = v;
});

if (keys) {
    Object.keys(keys).forEach(function (k) {
        var d = keys[k];
        if (d && d.req === true && !(k in msg)) {
            invalid.push(k + ':missing');
        }
    });
}

var eventId = msg.event_id;
var eventType = msg.event_type || 'info';
var eventTs = msg.event_ts;
if (eventId && eventType !== 'info' && eventType !== 'alert' && eventType !== 'fault') {
    invalid.push('event_type:value');
}

var ts = metadata.ts ? Number(metadata.ts) : Date.now();
var envelope = {
    v: 1,
    tenantId: metadata.ss_tenant_id || metadata.tenantId || null,
    deviceId: metadata.ss_device_uuid || metadata.deviceId || null,
    deviceName: metadata.deviceName || null,
    productId: productId,
    tslVersion: metadata.ss_tsl_version || null,
    siteCode: metadata.ss_site_code || null,
    ts: ts,
    kind: 'telemetry',
    values: values,
    dropped: dropped,
    event: null,
    invalid: invalid
};

if (eventId) {
    envelope.kind = 'event';
    envelope.event = {
        id: eventId,
        type: eventType,
        ts: eventTs ? Number(eventTs) : ts,
        level: eventType === 'alert' ? 'MAJOR' : (eventType === 'fault' ? 'CRITICAL' : 'INFO')
    };
}
if (invalid.length > 0) {
    envelope.kind = 'invalid';
}
metadata.tb_kind = envelope.kind;
out.msg = JSON.stringify(envelope);
out.metadata = metadata;
out.msgType = 'POST_TELEMETRY_REQUEST';
return out;
