// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import vm, { Script } from 'vm';
import atob from 'atob';
import btoa from 'btoa';
import {TextDecoder, types} from "util";
import { _logger } from '../config/logger';

export type TbScript = Script | Function;

type SandboxErrors = {
    Error: ErrorConstructor;
    TypeError: TypeErrorConstructor;
    RangeError: RangeErrorConstructor;
};

// The only values host helpers may return to the sandbox.
type SandboxPrimitive = string | boolean | undefined;

const typedArrayPrototype = Object.getPrototypeOf(Uint8Array.prototype);

function intrinsicGetter(prototype: object, name: string): (this: unknown) => any {
    return Object.getOwnPropertyDescriptor(prototype, name)!.get!;
}

const typedArrayBuffer = intrinsicGetter(typedArrayPrototype, 'buffer');
const typedArrayByteOffset = intrinsicGetter(typedArrayPrototype, 'byteOffset');
const typedArrayByteLength = intrinsicGetter(typedArrayPrototype, 'byteLength');
const dataViewBuffer = intrinsicGetter(DataView.prototype, 'buffer');
const dataViewByteOffset = intrinsicGetter(DataView.prototype, 'byteOffset');
const dataViewByteLength = intrinsicGetter(DataView.prototype, 'byteLength');

// Sandbox-realm btoa, atob and TextDecoder delegating to the host helpers of installSandboxGlobals.
// Arguments must be coerced here, with intrinsics captured before user code runs.
// invoke() rethrows sandbox-realm errors only; anything else becomes a sandbox RangeError.
const sandboxGlobalsShim = new vm.Script(`(function (h) {
    'use strict';
    var S = String;
    var getPrototypeOf = Object.getPrototypeOf;
    var errorPrototype = Error.prototype;
    var SandboxRangeError = RangeError;
    function isSandboxError(e) {
        if (e === null || typeof e !== 'object') {
            return false;
        }
        for (var p = getPrototypeOf(e); p !== null; p = getPrototypeOf(p)) {
            if (p === errorPrototype) {
                return true;
            }
        }
        return false;
    }
    function invoke(fn, a, b, c, d) {
        try {
            return fn(a, b, c, d);
        } catch (e) {
            if (isSandboxError(e)) {
                throw e;
            }
            throw new SandboxRangeError('Maximum call stack size exceeded');
        }
    }
    globalThis.btoa = function btoa(data) { return invoke(h.btoa, S(data)); };
    globalThis.atob = function atob(data) { return invoke(h.atob, S(data)); };
    class TextDecoder {
        constructor(label, options) { invoke(h.create, this, label === undefined ? 'utf-8' : S(label), !!(options && options.fatal), !!(options && options.ignoreBOM)); }
        get encoding() { return invoke(h.prop, this, 'encoding'); }
        get fatal() { return invoke(h.prop, this, 'fatal'); }
        get ignoreBOM() { return invoke(h.prop, this, 'ignoreBOM'); }
        decode(input, options) { return invoke(h.decode, this, input, !!(options && options.stream)); }
    }
    Object.defineProperty(TextDecoder.prototype, Symbol.toStringTag, {value: 'TextDecoder', configurable: true});
    globalThis.TextDecoder = TextDecoder;
})`);

function toSandboxError(errors: SandboxErrors, hostError: unknown): Error {
    const source = hostError instanceof Error ? hostError : new Error(String(hostError));
    const errorType = source instanceof RangeError ? errors.RangeError : source instanceof TypeError ? errors.TypeError : errors.Error;
    const err = new errorType(source.message);
    const code = (source as NodeJS.ErrnoException).code;
    if (typeof code === 'string') {
        Object.defineProperty(err, 'code', {value: code, writable: true, enumerable: true, configurable: true});
    }
    return err;
}

function requireString(value: unknown, name: string): string {
    if (typeof value !== 'string') {
        throw new TypeError(`The "${name}" argument must be of type string`);
    }
    return value;
}

// Reads the input through host intrinsics only; the value itself is never passed on or property-accessed.
function toHostBytes(input: unknown): Uint8Array | undefined {
    if (input === undefined) {
        return undefined;
    }
    if (types.isAnyArrayBuffer(input)) {
        return new Uint8Array(input);
    }
    if (types.isTypedArray(input)) {
        return new Uint8Array(typedArrayBuffer.call(input), typedArrayByteOffset.call(input), typedArrayByteLength.call(input));
    }
    if (types.isDataView(input)) {
        return new Uint8Array(dataViewBuffer.call(input), dataViewByteOffset.call(input), dataViewByteLength.call(input));
    }
    throw new TypeError('The "input" argument must be an instance of ArrayBuffer or ArrayBufferView');
}

function installSandboxGlobals(sandbox: vm.Context): void {
    // Captured before user code runs, so later monkeypatching inside the sandbox has no effect.
    const errors: SandboxErrors = vm.runInContext('({Error: Error, TypeError: TypeError, RangeError: RangeError})', sandbox);
    const decoders = new WeakMap<object, TextDecoder>(); // host realm: user code cannot patch it
    const decoderOf = (self: unknown): TextDecoder => {
        const decoder = typeof self === 'object' && self !== null ? decoders.get(self) : undefined;
        if (!decoder) {
            throw new TypeError('Illegal invocation');
        }
        return decoder;
    };
    // The one place host results and errors cross into the sandbox.
    const guard = <A extends unknown[], R extends SandboxPrimitive>(fn: (...args: A) => R) => (...args: A): R => {
        let result: R;
        try {
            result = fn(...args);
        } catch (e) {
            throw toSandboxError(errors, e);
        }
        if ((typeof result === 'object' && result !== null) || typeof result === 'function') {
            throw new errors.Error('Host value must not be returned to the sandbox');
        }
        return result;
    };
    const host = Object.assign(Object.create(null), {
        btoa: guard((data: unknown) => btoa(requireString(data, 'data'))),
        atob: guard((data: unknown) => atob(requireString(data, 'data'))),
        create: guard((self: unknown, label: unknown, fatal: unknown, ignoreBOM: unknown) => {
            if (typeof self !== 'object' || self === null || decoders.has(self)) {
                throw new TypeError('Illegal invocation');
            }
            decoders.set(self, new TextDecoder(requireString(label, 'label'), {fatal: fatal === true, ignoreBOM: ignoreBOM === true}));
            return undefined;
        }),
        prop: guard((self: unknown, name: unknown): SandboxPrimitive => {
            const decoder = decoderOf(self);
            switch (name) {
                case 'encoding':
                    return decoder.encoding;
                case 'fatal':
                    return decoder.fatal;
                case 'ignoreBOM':
                    return decoder.ignoreBOM;
                default:
                    return undefined;
            }
        }),
        decode: guard((self: unknown, input: unknown, stream: unknown) =>
            decoderOf(self).decode(toHostBytes(input), {stream: stream === true})),
    });
    sandboxGlobalsShim.runInContext(sandbox)(host);
}

export class JsExecutor {
    useSandbox: boolean;
    private logger = _logger('JsExecutor');

    constructor(useSandbox: boolean) {
        this.useSandbox = useSandbox;
        if (!useSandbox) {
            this.logger.warn(
                'script.use_sandbox=false: dangerous by design — user-supplied scripts run in the host realm with no isolation. ' +
                'Use only as a performance trade-off in trusted, non-public clusters.'
            );
        }
    }

    compileScript(code: string): Promise<TbScript> {
        if (this.useSandbox) {
            return this.createScript(code);
        } else {
            return this.createFunction(code);
        }
    }

    executeScript(script: TbScript, args: string[], timeout?: number): Promise<any> {
        if (this.useSandbox) {
            return this.invokeScript(script as Script, args, timeout);
        } else {
            return this.invokeFunction(script as Function, args);
        }
    }

    private createScript(code: string): Promise<Script> {
        return new Promise((resolve, reject) => {
            try {
                code = "("+code+")(...args)";
                const script = new vm.Script(code);
                resolve(script);
            } catch (err) {
                reject(err);
            }
        });
    }

    private invokeScript(script: Script, args: string[], timeout: number | undefined): Promise<any> {
        return new Promise((resolve, reject) => {
            try {
                const sandbox = vm.createContext(Object.create(null));
                // Construct args inside the sandbox context so it inherits sandbox-realm
                // prototypes; prevents prototype-based escapes from the host realm.
                const ctxArgs = vm.runInContext('[]', sandbox) as string[];
                for (let i = 0; i < args.length; i++) {
                    ctxArgs[i] = String(args[i]);
                }
                sandbox.args = ctxArgs;
                installSandboxGlobals(sandbox);
                const result = script.runInContext(sandbox, {timeout: timeout});
                resolve(result);
            } catch (err) {
                reject(err);
            }
        });
    }


    // DANGEROUS BY DESIGN: the non-sandbox path. vm.compileFunction's
    // parsingContext only isolates *parsing*, not *execution* — the resulting
    // function runs in the host realm with full access to host globals
    // (process, require, etc.). Enabled only via script.use_sandbox=false as
    // a performance trade-off in trusted clusters.
    private createFunction(code: string): Promise<Function> {
        return new Promise((resolve, reject) => {
            try {
                code = "return ("+code+")(...args)";
                const parsingContext = vm.createContext({btoa: btoa, atob: atob, TextDecoder: TextDecoder});
                const func = vm.compileFunction(code, ['args'], {parsingContext: parsingContext});
                resolve(func);
            } catch (err) {
                reject(err);
            }
        });
    }

    private invokeFunction(func: Function, args: string[]): Promise<any> {
        return new Promise((resolve, reject) => {
            try {
                const result = func(args);
                resolve(result);
            } catch (err) {
                reject(err);
            }
        });
    }
}
