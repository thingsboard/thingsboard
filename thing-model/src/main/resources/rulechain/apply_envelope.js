var out = {msg: msg, metadata: metadata || {}, msgType: msgType};
if (metadata && metadata.tsl_normalized) {
    out.msg = JSON.parse(metadata.tsl_normalized);
    delete metadata.tsl_normalized;
}
return out;
