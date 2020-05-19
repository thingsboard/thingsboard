/* jshint proto: true */
import * as Constants from './constants';
import CloneHelper from 'clone';

export function getLocation(index, inputStream) {
    let n = index + 1;
    let line = null;
    let column = -1;

    while (--n >= 0 && inputStream.charAt(n) !== '\n') {
        column++;
    }

    if (typeof index === 'number') {
        line = (inputStream.slice(0, index).match(/\n/g) || '').length;
    }

    return {
        line,
        column
    };
}

export function copyArray(arr) {
    let i;
    const length = arr.length;
    const copy = new Array(length);

    for (i = 0; i < length; i++) {
        copy[i] = arr[i];
    }
    return copy;
}

export function clone(obj) {
    const cloned = {};
    for (const prop in obj) {
        if (obj.hasOwnProperty(prop)) {
            cloned[prop] = obj[prop];
        }
    }
    return cloned;
}

export function defaults(obj1, obj2) {
    let newObj = obj2 || {};
    if (!obj2._defaults) {
        newObj = {};
        const defaults = CloneHelper(obj1);
        newObj._defaults = defaults;
        const cloned = obj2 ? CloneHelper(obj2) : {};
        Object.assign(newObj, defaults, cloned);
    }
    return newObj;
}

export function copyOptions(obj1, obj2) {
    if (obj2 && obj2._defaults) {
        return obj2;
    }
    const opts = defaults(obj1, obj2);
    if (opts.strictMath) {
        opts.math = Constants.Math.STRICT_LEGACY;
    }
    // Back compat with changed relativeUrls option
    if (opts.relativeUrls) {
        opts.rewriteUrls = Constants.RewriteUrls.ALL;
    }
    if (typeof opts.math === 'string') {
        switch (opts.math.toLowerCase()) {
            case 'always':
                opts.math = Constants.Math.ALWAYS;
                break;
            case 'parens-division':
                opts.math = Constants.Math.PARENS_DIVISION;
                break;
            case 'strict':
            case 'parens':
                opts.math = Constants.Math.PARENS;
                break;
            case 'strict-legacy':
                opts.math = Constants.Math.STRICT_LEGACY;
        }
    }
    if (typeof opts.rewriteUrls === 'string') {
        switch (opts.rewriteUrls.toLowerCase()) {
            case 'off':
                opts.rewriteUrls = Constants.RewriteUrls.OFF;
                break;
            case 'local':
                opts.rewriteUrls = Constants.RewriteUrls.LOCAL;
                break;
            case 'all':
                opts.rewriteUrls = Constants.RewriteUrls.ALL;
                break;
        }
    }
    return opts;
}

export function merge(obj1, obj2) {
    for (const prop in obj2) {
        if (obj2.hasOwnProperty(prop)) {
            obj1[prop] = obj2[prop];
        }
    }
    return obj1;
}

export function flattenArray(arr, result = []) {
    for (let i = 0, length = arr.length; i < length; i++) {
        const value = arr[i];
        if (Array.isArray(value)) {
            flattenArray(value, result);
        } else {
            if (value !== undefined) {
                result.push(value);
            }
        }
    }
    return result;
}