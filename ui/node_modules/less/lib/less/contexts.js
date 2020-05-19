const contexts = {};
export default contexts;
import * as Constants from './constants';

const copyFromOriginal = function copyFromOriginal(original, destination, propertiesToCopy) {
    if (!original) { return; }

    for (let i = 0; i < propertiesToCopy.length; i++) {
        if (original.hasOwnProperty(propertiesToCopy[i])) {
            destination[propertiesToCopy[i]] = original[propertiesToCopy[i]];
        }
    }
};

/*
 parse is used whilst parsing
 */
const parseCopyProperties = [
    // options
    'paths',            // option - unmodified - paths to search for imports on
    'rewriteUrls',      // option - whether to adjust URL's to be relative
    'rootpath',         // option - rootpath to append to URL's
    'strictImports',    // option -
    'insecure',         // option - whether to allow imports from insecure ssl hosts
    'dumpLineNumbers',  // option - whether to dump line numbers
    'compress',         // option - whether to compress
    'syncImport',       // option - whether to import synchronously
    'chunkInput',       // option - whether to chunk input. more performant but causes parse issues.
    'mime',             // browser only - mime type for sheet import
    'useFileCache',     // browser only - whether to use the per file session cache
    // context
    'processImports',   // option & context - whether to process imports. if false then imports will not be imported.
    // Used by the import manager to stop multiple import visitors being created.
    'pluginManager'     // Used as the plugin manager for the session
];

contexts.Parse = function(options) {
    copyFromOriginal(options, this, parseCopyProperties);

    if (typeof this.paths === 'string') { this.paths = [this.paths]; }
};

const evalCopyProperties = [
    'paths',             // additional include paths
    'compress',          // whether to compress
    'math',              // whether math has to be within parenthesis
    'strictUnits',       // whether units need to evaluate correctly
    'sourceMap',         // whether to output a source map
    'importMultiple',    // whether we are currently importing multiple copies
    'urlArgs',           // whether to add args into url tokens
    'javascriptEnabled', // option - whether Inline JavaScript is enabled. if undefined, defaults to false
    'pluginManager',     // Used as the plugin manager for the session
    'importantScope',    // used to bubble up !important statements
    'rewriteUrls'        // option - whether to adjust URL's to be relative
];

function isPathRelative(path) {
    return !/^(?:[a-z-]+:|\/|#)/i.test(path);
}

function isPathLocalRelative(path) {
    return path.charAt(0) === '.';
}

contexts.Eval = class {
    constructor(options, frames) {
        copyFromOriginal(options, this, evalCopyProperties);

        if (typeof this.paths === 'string') { this.paths = [this.paths]; }

        this.frames = frames || [];
        this.importantScope = this.importantScope || [];
        this.inCalc = false;
        this.mathOn = true;
    }

    enterCalc() {
        if (!this.calcStack) {
            this.calcStack = [];
        }
        this.calcStack.push(true);
        this.inCalc = true;
    }

    exitCalc() {
        this.calcStack.pop();
        if (!this.calcStack) {
            this.inCalc = false;
        }
    }

    inParenthesis() {
        if (!this.parensStack) {
            this.parensStack = [];
        }
        this.parensStack.push(true);
    };

    outOfParenthesis() {
        this.parensStack.pop();
    };

    isMathOn(op) {
        if (!this.mathOn) {
            return false;
        }
        if (op === '/' && this.math !== Constants.Math.ALWAYS && (!this.parensStack || !this.parensStack.length)) {
            return false;
        }
        if (this.math > Constants.Math.PARENS_DIVISION) {
            return this.parensStack && this.parensStack.length;
        }
        return true;
    }

    pathRequiresRewrite(path) {
        const isRelative = this.rewriteUrls === Constants.RewriteUrls.LOCAL ? isPathLocalRelative : isPathRelative;

        return isRelative(path);
    }

    rewritePath(path, rootpath) {
        let newPath;

        rootpath = rootpath || '';
        newPath = this.normalizePath(rootpath + path);

        // If a path was explicit relative and the rootpath was not an absolute path
        // we must ensure that the new path is also explicit relative.
        if (isPathLocalRelative(path) &&
            isPathRelative(rootpath) &&
            isPathLocalRelative(newPath) === false) {
            newPath = `./${newPath}`;
        }

        return newPath;
    }

    normalizePath(path) {
        const segments = path.split('/').reverse();
        let segment;

        path = [];
        while (segments.length !== 0) {
            segment = segments.pop();
            switch ( segment ) {
                case '.':
                    break;
                case '..':
                    if ((path.length === 0) || (path[path.length - 1] === '..')) {
                        path.push( segment );
                    } else {
                        path.pop();
                    }
                    break;
                default:
                    path.push(segment);
                    break;
            }
        }

        return path.join('/');
    }
}
