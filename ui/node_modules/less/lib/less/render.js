let PromiseConstructor;
import * as utils from './utils';

export default (environment, ParseTree, ImportManager) => {
    const render = function (input, options, callback) {
        if (typeof options === 'function') {
            callback = options;
            options = utils.copyOptions(this.options, {});
        }
        else {
            options = utils.copyOptions(this.options, options || {});
        }

        if (!callback) {
            const self = this;
            return new Promise((resolve, reject) => {
                render.call(self, input, options, (err, output) => {
                    if (err) {
                        reject(err);
                    } else {
                        resolve(output);
                    }
                });
            });
        } else {
            this.parse(input, options, (err, root, imports, options) => {
                if (err) { return callback(err); }

                let result;
                try {
                    const parseTree = new ParseTree(root, imports);
                    result = parseTree.toCSS(options);
                }
                catch (err) { return callback(err); }

                callback(null, result);
            });
        }
    };

    return render;
};
