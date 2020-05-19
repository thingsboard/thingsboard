import * as utils from './utils';

const anonymousFunc = /(<anonymous>|Function):(\d+):(\d+)/;

/**
 * This is a centralized class of any error that could be thrown internally (mostly by the parser).
 * Besides standard .message it keeps some additional data like a path to the file where the error
 * occurred along with line and column numbers.
 *
 * @class
 * @extends Error
 * @type {module.LessError}
 *
 * @prop {string} type
 * @prop {string} filename
 * @prop {number} index
 * @prop {number} line
 * @prop {number} column
 * @prop {number} callLine
 * @prop {number} callExtract
 * @prop {string[]} extract
 *
 * @param {Object} e              - An error object to wrap around or just a descriptive object
 * @param {Object} fileContentMap - An object with file contents in 'contents' property (like importManager) @todo - move to fileManager?
 * @param {string} [currentFilename]
 */
const LessError = function LessError(e, fileContentMap, currentFilename) {
    Error.call(this);

    const filename = e.filename || currentFilename;

    this.message = e.message;
    this.stack = e.stack;

    if (fileContentMap && filename) {
        const input = fileContentMap.contents[filename];
        const loc = utils.getLocation(e.index, input);
        const line = loc.line;
        const col  = loc.column;
        const callLine = e.call && utils.getLocation(e.call, input).line;
        const lines = input ? input.split('\n') : '';

        this.type = e.type || 'Syntax';
        this.filename = filename;
        this.index = e.index;
        this.line = typeof line === 'number' ? line + 1 : null;
        this.column = col;

        if (!this.line && this.stack) {
            const found = this.stack.match(anonymousFunc);

            /**
             * We have to figure out how this environment stringifies anonymous functions
             * so we can correctly map plugin errors.
             * 
             * Note, in Node 8, the output of anonymous funcs varied based on parameters
             * being present or not, so we inject dummy params.
             */
            const func = new Function('a', 'throw new Error()');
            let lineAdjust = 0;
            try {
                func();
            } catch (e) {
                const match = e.stack.match(anonymousFunc);
                const line = parseInt(match[2]);
                lineAdjust = 1 - line;
            }

            if (found) {
                if (found[2]) {
                    this.line = parseInt(found[2]) + lineAdjust;
                }
                if (found[3]) {
                    this.column = parseInt(found[3]);
                }
            }
        }

        this.callLine = callLine + 1;
        this.callExtract = lines[callLine];

        this.extract = [
            lines[this.line - 2],
            lines[this.line - 1],
            lines[this.line]
        ];
    }

};

if (typeof Object.create === 'undefined') {
    const F = () => {};
    F.prototype = Error.prototype;
    LessError.prototype = new F();
} else {
    LessError.prototype = Object.create(Error.prototype);
}

LessError.prototype.constructor = LessError;

/**
 * An overridden version of the default Object.prototype.toString
 * which uses additional information to create a helpful message.
 *
 * @param {Object} options
 * @returns {string}
 */
LessError.prototype.toString = function(options = {}) {
    let message = '';
    const extract = this.extract || [];
    let error = [];
    let stylize = str => str;
    if (options.stylize) {
        const type = typeof options.stylize;
        if (type !== 'function') {
            throw Error(`options.stylize should be a function, got a ${type}!`);
        }
        stylize = options.stylize;
    }

    if (this.line !== null) {
        if (typeof extract[0] === 'string') {
            error.push(stylize(`${this.line - 1} ${extract[0]}`, 'grey'));
        }

        if (typeof extract[1] === 'string') {
            let errorTxt = `${this.line} `;
            if (extract[1]) {
                errorTxt += extract[1].slice(0, this.column) +
                    stylize(stylize(stylize(extract[1].substr(this.column, 1), 'bold') +
                        extract[1].slice(this.column + 1), 'red'), 'inverse');
            }
            error.push(errorTxt);
        }

        if (typeof extract[2] === 'string') {
            error.push(stylize(`${this.line + 1} ${extract[2]}`, 'grey'));
        }
        error = `${error.join('\n') + stylize('', 'reset')}\n`;
    }

    message += stylize(`${this.type}Error: ${this.message}`, 'red');
    if (this.filename) {
        message += stylize(' in ', 'red') + this.filename;
    }
    if (this.line) {
        message += stylize(` on line ${this.line}, column ${this.column + 1}:`, 'grey');
    }

    message += `\n${error}`;

    if (this.callLine) {
        message += `${stylize('from ', 'red') + (this.filename || '')}/n`;
        message += `${stylize(this.callLine, 'grey')} ${this.callExtract}/n`;
    }

    return message;
};

export default LessError;