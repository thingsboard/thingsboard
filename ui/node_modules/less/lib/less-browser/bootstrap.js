/**
 * Kicks off less and compiles any stylesheets
 * used in the browser distributed version of less
 * to kick-start less using the browser api
 */
/* global window, document */

import defaultOptions from '../less/default-options';
import addDefaultOptions from './add-default-options';
import root from './index';

const options = defaultOptions();

if (window.less) {
    for (const key in window.less) {
        if (window.less.hasOwnProperty(key)) {
            options[key] = window.less[key];
        }
    }
}
addDefaultOptions(window, options);

options.plugins = options.plugins || [];

if (window.LESS_PLUGINS) {
    options.plugins = options.plugins.concat(window.LESS_PLUGINS);
}

const less = root(window, options);
export default less;

window.less = less;

let css;
let head;
let style;

// Always restore page visibility
function resolveOrReject(data) {
    if (data.filename) {
        console.warn(data);
    }
    if (!options.async) {
        head.removeChild(style);
    }
}

if (options.onReady) {
    if (/!watch/.test(window.location.hash)) {
        less.watch();
    }
    // Simulate synchronous stylesheet loading by hiding page rendering
    if (!options.async) {
        css = 'body { display: none !important }';
        head = document.head || document.getElementsByTagName('head')[0];
        style = document.createElement('style');

        style.type = 'text/css';
        if (style.styleSheet) {
            style.styleSheet.cssText = css;
        } else {
            style.appendChild(document.createTextNode(css));
        }

        head.appendChild(style);
    }
    less.registerStylesheetsImmediately();
    less.pageLoadFinished = less.refresh(less.env === 'development').then(resolveOrReject, resolveOrReject);
}
