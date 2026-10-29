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

function validateField(name, v, d, errors, prefix) {
    errors = errors || invalid;
    prefix = prefix || '';
    var t = d.t;
    if (!typeOk(t, v)) {
        errors.push(prefix + name + ':' + (t || 'unknown'));
        return false;
    }
    if (t === 'int' && (v < -2147483648 || v > 2147483647)) {
        errors.push(prefix + name + ':int32');
        return false;
    }
    if ((t === 'enum' || t === 'enum_string') && !mappingHas(d.map, v)) {
        errors.push(prefix + name + ':enum');
        return false;
    }
    if (t === 'struct') {
        var fieldNames = Object.keys(v);
        for (var i = 0; i < fieldNames.length; i++) {
            if (isObject(v[fieldNames[i]])) {
                errors.push(prefix + name + '.' + fieldNames[i] + ':nested');
                return false;
            }
        }
    }
    if (isNumber(v)) {
        if (isNumber(d.min) && v < d.min) {
            errors.push(prefix + name + ':min');
            return false;
        }
        if (isNumber(d.max) && v > d.max) {
            errors.push(prefix + name + ':max');
            return false;
        }
    }
    return true;
}

function normalizeEvent(item, defaultTs, prefix) {
    var errors = [];
    var idField = 'event_id';
    var id = item.event_id;
    var type = item.event_type;
    var def = null;
    var allowed = {};
    Object.keys(keys).forEach(function (k) {
        allowed[k] = keys[k];
    });
    if (id === undefined || id === null) {
        if (typeof item.id === 'string' && (events[item.id]
                || (type !== undefined && type !== null))) {
            id = item.id;
            idField = 'id';
        }
    }
    if (id !== undefined && id !== null) {
        if (typeof id !== 'string' || !/^[A-Za-z_][A-Za-z0-9_]{0,49}$/.test(id)) {
            errors.push(prefix + 'event_id:invalid');
        } else {
            def = events[id];
            if (!def) {
                errors.push(prefix + 'event_id:unknown');
            } else if (def.params) {
                Object.keys(def.params).forEach(function (k) {
                    if (!(k in allowed)) {
                        allowed[k] = def.params[k];
                    }
                });
            }
        }
    } else if (type !== undefined && type !== null) {
        errors.push(prefix + 'event_type:orphan');
    } else {
        errors.push(prefix + 'event_id:missing');
    }

    var values = {};
    Object.keys(item).forEach(function (k) {
        if (reserved[k] || k === idField) {
            return;
        }
        var v = item[k];
        var d = allowed[k];
        if (!d) {
            if (strict) {
                errors.push(prefix + k + ':unknown');
            } else {
                dropped.push(prefix + k);
            }
            return;
        }
        if (!validateField(k, v, d, errors, prefix)) {
            return;
        }
        if (d.t === 'bool' && (v === 0 || v === 1)) {
            v = v === 1;
        } else if (d.t === 'timestamp') {
            v = normalizeTimestamp(v);
        }
        values[k] = v;
    });

    if (type !== undefined && type !== null && type !== ''
            && type !== 'info' && type !== 'alert' && type !== 'fault') {
        errors.push(prefix + 'event_type:value');
    }
    if (def) {
        if (type === undefined || type === null || type === '') {
            type = def.type || 'info';
        } else {
            if (def.type && type !== def.type) {
                errors.push(prefix + 'event_type:mismatch');
            }
            type = def.type || type;
        }
    }

    var eventTs = item.event_ts;
    if (eventTs !== undefined && eventTs !== null) {
        if (!isInteger(eventTs)) {
            errors.push(prefix + 'event_ts:type');
            eventTs = defaultTs;
        } else {
            eventTs = normalizeTimestamp(eventTs);
        }
    } else {
        eventTs = defaultTs;
    }

    var data = {};
    if (def && def.params) {
        Object.keys(def.params).forEach(function (k) {
            if (k in values) {
                data[k] = values[k];
            }
        });
    }

    errors.forEach(function (e) {
        invalid.push(e);
    });
    if (errors.length > 0 || !def) {
        return null;
    }
    var eventType = type || 'info';
    return {
        id: id,
        type: eventType,
        ts: eventTs,
        data: Object.keys(data).length ? data : null
    };
}

var batch = payloadOk && Array.isArray(msg.events);
var eventId = null;
var eventType = null;
var eventIdField = 'event_id';
var eventDef = null;
var allowed = {};
var batchEvents = [];

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

if (batch) {
    var batchTs = meta.ts ? Number(meta.ts) : Date.now();
    if (!isNumber(batchTs)) {
        batchTs = Date.now();
    }
    batchTs = normalizeTimestamp(batchTs);
    if (msg.events.length === 0) {
        invalid.push('events:empty');
    }
    for (var index = 0; index < msg.events.length; index++) {
        var item = msg.events[index];
        if (!isObject(item)) {
            invalid.push('events[' + index + ']:type');
            continue;
        }
        var normalized = normalizeEvent(item, batchTs, 'events[' + index + '].');
        if (normalized) {
            batchEvents.push(normalized);
        }
    }
    if (batchEvents.length > 1) {
        var firstEventId = batchEvents[0].id;
        for (var eventIndex = 1; eventIndex < batchEvents.length; eventIndex++) {
            if (batchEvents[eventIndex].id !== firstEventId) {
                invalid.push('events:mixed');
                break;
            }
        }
    }
} else if (payloadOk) {
    eventId = msg.event_id;
    eventType = msg.event_type;
    if (eventId === undefined || eventId === null) {
        if (typeof msg.id === 'string' && (events[msg.id]
                || (eventType !== undefined && eventType !== null))) {
            eventId = msg.id;
            eventIdField = 'id';
        }
    }
    if (eventId !== undefined && eventId !== null) {
        if (typeof eventId !== 'string' || !/^[A-Za-z_][A-Za-z0-9_]{0,49}$/.test(eventId)) {
            invalid.push('event_id:invalid');
        } else {
            eventDef = events[eventId];
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

    Object.keys(msg).forEach(function (k) {
        if (reserved[k] || k === eventIdField) {
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
}

var ts = meta.ts ? Number(meta.ts) : Date.now();
if (!isNumber(ts)) {
    ts = Date.now();
}
ts = normalizeTimestamp(ts);

var eventTs = payloadOk && !batch ? msg.event_ts : undefined;
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

var kind = (eventId || batch) ? 'event' : 'telemetry';
if (invalid.length > 0) {
    kind = 'invalid';
}

var eventData = {};
if (!batch && eventId && eventDef && eventDef.params) {
    Object.keys(eventDef.params).forEach(function (k) {
        if (k in values) {
            eventData[k] = values[k];
            delete values[k];
        }
    });
}
if (kind === 'event' && !batch) {
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
    events: null,
    invalid: invalid.length ? invalid : null
};

if (batch) {
    envelope.events = batchEvents.length ? batchEvents : null;
} else if (eventId) {
    envelope.event = {
        id: eventId,
        type: eventType || 'info',
        ts: eventTs,
        data: Object.keys(eventData).length ? eventData : null
    };
}

meta.tb_kind = envelope.kind;
meta.tsl_normalized = JSON.stringify(envelope);
return out;
