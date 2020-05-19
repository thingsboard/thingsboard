import Node from './node';

class URL extends Node {
    constructor(val, index, currentFileInfo, isEvald) {
        super();

        this.value = val;
        this._index = index;
        this._fileInfo = currentFileInfo;
        this.isEvald = isEvald;
    }

    accept(visitor) {
        this.value = visitor.visit(this.value);
    }

    genCSS(context, output) {
        output.add('url(');
        this.value.genCSS(context, output);
        output.add(')');
    }

    eval(context) {
        const val = this.value.eval(context);
        let rootpath;

        if (!this.isEvald) {
            // Add the rootpath if the URL requires a rewrite
            rootpath = this.fileInfo() && this.fileInfo().rootpath;
            if (typeof rootpath === 'string' &&
                typeof val.value === 'string' &&
                context.pathRequiresRewrite(val.value)) {
                if (!val.quote) {
                    rootpath = escapePath(rootpath);
                }
                val.value = context.rewritePath(val.value, rootpath);
            } else {
                val.value = context.normalizePath(val.value);
            }

            // Add url args if enabled
            if (context.urlArgs) {
                if (!val.value.match(/^\s*data:/)) {
                    const delimiter = val.value.indexOf('?') === -1 ? '?' : '&';
                    const urlArgs = delimiter + context.urlArgs;
                    if (val.value.indexOf('#') !== -1) {
                        val.value = val.value.replace('#', `${urlArgs}#`);
                    } else {
                        val.value += urlArgs;
                    }
                }
            }
        }

        return new URL(val, this.getIndex(), this.fileInfo(), true);
    }
}

URL.prototype.type = 'Url';

function escapePath(path) {
    return path.replace(/[\(\)'"\s]/g, match => `\\${match}`);
}

export default URL;
