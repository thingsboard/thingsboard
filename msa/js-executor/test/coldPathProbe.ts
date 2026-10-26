// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { JsExecutor } from '../api/jsExecutor';

// Runs one probe as the first sandbox execution of a fresh process and reports
// whether any error caught on stack overflow came from outside the sandbox realm.
const call = process.argv[2];

(async () => {
    const exec = new JsExecutor(true);
    const script = await exec.compileScript(`function(msg, metadata, msgType){
        var foreign;
        function attempt() {
            if (foreign) return;
            try { ${call}; } catch (e) {
                if (e === null || typeof e !== 'object' || (e.constructor !== RangeError && e.constructor !== TypeError && e.constructor !== Error)) {
                    foreign = String(e && e.name);
                }
            }
        }
        function recurse() { try { recurse(); } catch (e) {} attempt(); }
        recurse();
        return foreign ? 'foreign error: ' + foreign : 'isolated';
    }`);
    const out = await exec.executeScript(script, ['{}', '{}', 'X'], 20000);
    process.stdout.write(String(out));
})().catch((err) => {
    process.stdout.write('probe failed: ' + String(err && err.message));
});
