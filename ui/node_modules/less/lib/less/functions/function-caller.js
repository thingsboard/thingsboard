import Expression from '../tree/expression';

class functionCaller {
    constructor(name, context, index, currentFileInfo) {
        this.name = name.toLowerCase();
        this.index = index;
        this.context = context;
        this.currentFileInfo = currentFileInfo;

        this.func = context.frames[0].functionRegistry.get(this.name);
    }

    isValid() {
        return Boolean(this.func);
    }

    call(args) {
        // This code is terrible and should be replaced as per this issue...
        // https://github.com/less/less.js/issues/2477
        if (Array.isArray(args)) {
            args = args.filter(item => {
                if (item.type === 'Comment') {
                    return false;
                }
                return true;
            })
                .map(item => {
                    if (item.type === 'Expression') {
                        const subNodes = item.value.filter(item => {
                            if (item.type === 'Comment') {
                                return false;
                            }
                            return true;
                        });
                        if (subNodes.length === 1) {
                            return subNodes[0];
                        } else {
                            return new Expression(subNodes);
                        }
                    }
                    return item;
                });
        }

        return this.func(...args);
    }
}

export default functionCaller;
