var out = {msg: msg, metadata: metadata || {}, msgType: msgType};
var meta = out.metadata;
var invalid = [];
var dropped = [];
var values = {};

var schema = null;
try {
    schema = JSON.parse(__TSL_SCHEMA__);
} catch (e) {
    invalid.push('_schema:parse');
}

var keys = schema && schema.keys ? schema.keys : {};
var events = schema && schema.events ? schema.events : {};
var strict = schema ? schema.strictUnknown === true : false;
var reserved = {event_id: true, event_type: true, event_ts: true, event_uid: true};
var payloadOk = msg !== null && typeof msg === 'object' && !Array.isArray(msg);

function isInteger(v) {
    return typeof v === 'number' && isFinite(v) && v % 1 === 0;
}

function isNumber(v) {
    return typeof v === 'number' && isFinite(v);
}

function isObject(v) {
    return v !== null && typeof v === 'object' && !Array.isArray(v);
}

function mappingKeys(map) {
    if (!map) {
        return null;
    }
    var result = [];
    if (Array.isArray(map)) {
        map.forEach(function (item) {
            if (item !== null && typeof item === 'object') {
                if (item.key !== undefined) {
                    result.push(String(item.key));
                } else if (item.id !== undefined) {
                    result.push(String(item.id));
                }
            } else {
                result.push(String(item));
            }
        });
        return result;
    }
    if (typeof map === 'object') {
        return Object.keys(map);
    }
    return null;
}

function mappingHas(map, value) {
    var candidate = mappingKeys(map);
    if (!candidate) {
        return true;
    }
    return candidate.indexOf(String(value)) >= 0;
}

function normalizeTimestamp(v) {
    if (isNumber(v) && v < 1000000000000) {
        return v * 1000;
    }
    return v;
}

function typeOk(t, v) {
    if (t === 'bool') {
        return typeof v === 'boolean' || v === 0 || v === 1;
    }
    if (t === 'int') {
        return isInteger(v);
    }
    if (t === 'float') {
        return isNumber(v);
    }
    if (t === 'string') {
        return typeof v === 'string';
    }
    if (t === 'enum') {
        return isInteger(v);
    }
    if (t === 'enum_string') {
        return typeof v === 'string';
    }
    if (t === 'timestamp') {
        return isInteger(v);
    }
    if (t === 'struct') {
        return isObject(v);
    }
    return true;
}

function validateField(name, v, d) {
    var t = d.t;
    if (!typeOk(t, v)) {
        invalid.push(name + ':' + (t || 'unknown'));
        return false;
    }
    if (t === 'int' && (v < -2147483648 || v > 2147483647)) {
        invalid.push(name + ':int32');
        return false;
    }
    if ((t === 'enum' || t === 'enum_string') && !mappingHas(d.map, v)) {
        invalid.push(name + ':enum');
        return false;
    }
    if (t === 'struct') {
        var fieldNames = Object.keys(v);
        for (var i = 0; i < fieldNames.length; i++) {
            if (isObject(v[fieldNames[i]])) {
                invalid.push(name + '.' + fieldNames[i] + ':nested');
                return false;
            }
        }
    }
    if (isNumber(v)) {
        if (isNumber(d.min) && v < d.min) {
            invalid.push(name + ':min');
            return false;
        }
        if (isNumber(d.max) && v > d.max) {
            invalid.push(name + ':max');
            return false;
        }
    }
    return true;
}

var eventCode = payloadOk ? msg.event_id : undefined;
var eventType = payloadOk ? msg.event_type : undefined;
var eventId = payloadOk && typeof msg.event_uid === 'string' && msg.event_uid
    ? msg.event_uid : null;
var eventDef = null;
var allowed = {};

if (payloadOk) {
    Object.keys(keys).forEach(function (k) {
        allowed[k] = keys[k];
    });
}

if (!payloadOk) {
    invalid.push('payload:type');
} else if (JSON.stringify(msg).length > 4096) {
    invalid.push('payload:size');
}

if (eventCode !== undefined && eventCode !== null) {
    if (typeof eventCode !== 'string' || !/^[A-Za-z_][A-Za-z0-9_]{0,49}$/.test(eventCode)) {
        invalid.push('event_id:invalid');
    } else {
        eventDef = events[eventCode];
        if (!eventDef) {
            invalid.push('event_id:unknown');
        } else if (eventDef.params) {
            Object.keys(eventDef.params).forEach(function (k) {
                if (!(k in allowed)) {
                    allowed[k] = eventDef.params[k];
                }
            });
        }
    }
} else if (eventType !== undefined && eventType !== null) {
    invalid.push('event_type:orphan');
}

if (payloadOk) {
    Object.keys(msg).forEach(function (k) {
        if (reserved[k]) {
            return;
        }
        var v = msg[k];
        var d = allowed[k];
        if (!d) {
            if (strict) {
                invalid.push(k + ':unknown');
            } else {
                dropped.push(k);
            }
            return;
        }
        if (!validateField(k, v, d)) {
            return;
        }
        if (d.t === 'bool' && (v === 0 || v === 1)) {
            v = v === 1;
        } else if (d.t === 'timestamp') {
            v = normalizeTimestamp(v);
        }
        values[k] = v;
    });

    Object.keys(keys).forEach(function (k) {
        var d = keys[k];
        if (d && d.req === true && !(k in msg)) {
            invalid.push(k + ':missing');
        }
    });
}

if (eventType !== undefined && eventType !== null && eventType !== ''
        && eventType !== 'info' && eventType !== 'alert' && eventType !== 'fault') {
    invalid.push('event_type:value');
}

if (eventDef) {
    if (eventType === undefined || eventType === null || eventType === '') {
        eventType = eventDef.type || 'info';
    } else {
        if (eventDef.type && eventType !== eventDef.type) {
            invalid.push('event_type:mismatch');
        }
        eventType = eventDef.type || eventType;
    }
}

var ts = meta.ts ? Number(meta.ts) : Date.now();
if (!isNumber(ts)) {
    ts = Date.now();
}
ts = normalizeTimestamp(ts);

var eventTs = payloadOk ? msg.event_ts : undefined;
if (eventTs !== undefined && eventTs !== null) {
    if (!isInteger(eventTs)) {
        invalid.push('event_ts:type');
        eventTs = ts;
    } else {
        eventTs = normalizeTimestamp(eventTs);
    }
} else {
    eventTs = ts;
}

var kind = eventCode ? 'event' : 'telemetry';
if (invalid.length > 0) {
    kind = 'invalid';
}

var eventData = {};
if (eventCode && eventDef && eventDef.params) {
    Object.keys(eventDef.params).forEach(function (k) {
        if (k in values) {
            eventData[k] = values[k];
            delete values[k];
        }
    });
}
if (kind === 'event') {
    values = {};
}

var envelope = {
    v: 1,
    tenantId: meta.ss_tenant_id || meta.tenantId || null,
    deviceId: meta.ss_device_uuid || meta.deviceId || null,
    deviceName: meta.deviceName || null,
    productId: schema ? schema.productId : null,
    tslVersion: schema ? schema.tslVersion : null,
    siteCode: meta.ss_site_code || null,
    ts: ts,
    kind: kind,
    values: Object.keys(values).length ? values : null,
    dropped: dropped.length ? dropped : null,
    event: null,
    invalid: invalid.length ? invalid : null
};

if (eventCode) {
    envelope.event = {
        id: eventId || (eventCode + '-' + eventTs + '-' + Math.random().toString(36).slice(2, 10)),
        code: eventCode,
        type: eventType || 'info',
        ts: eventTs,
        level: eventType === 'alert' ? 'MAJOR' : (eventType === 'fault' ? 'CRITICAL' : 'INFO'),
        data: Object.keys(eventData).length ? eventData : null
    };
}

meta.tb_kind = envelope.kind;
meta.tsl_normalized = JSON.stringify(envelope);
return out;
