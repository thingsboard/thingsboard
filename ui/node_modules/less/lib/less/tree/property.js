import Node from './node';
import Declaration from './declaration';

class Property extends Node {
    constructor(name, index, currentFileInfo) {
        super();

        this.name = name;
        this._index = index;
        this._fileInfo = currentFileInfo;
    }

    eval(context) {
        let property;
        const name = this.name;
        // TODO: shorten this reference
        const mergeRules = context.pluginManager.less.visitors.ToCSSVisitor.prototype._mergeRules;

        if (this.evaluating) {
            throw { type: 'Name',
                message: `Recursive property reference for ${name}`,
                filename: this.fileInfo().filename,
                index: this.getIndex() };
        }

        this.evaluating = true;

        property = this.find(context.frames, frame => {
            let v;
            const vArr = frame.property(name);
            if (vArr) {
                for (let i = 0; i < vArr.length; i++) {
                    v = vArr[i];

                    vArr[i] = new Declaration(v.name,
                        v.value,
                        v.important,
                        v.merge,
                        v.index,
                        v.currentFileInfo,
                        v.inline,
                        v.variable
                    );
                }
                mergeRules(vArr);

                v = vArr[vArr.length - 1];
                if (v.important) {
                    const importantScope = context.importantScope[context.importantScope.length - 1];
                    importantScope.important = v.important;
                }
                v = v.value.eval(context);
                return v;
            }
        });
        if (property) {
            this.evaluating = false;
            return property;
        } else {
            throw { type: 'Name',
                message: `Property '${name}' is undefined`,
                filename: this.currentFileInfo.filename,
                index: this.index };
        }
    }

    find(obj, fun) {
        for (let i = 0, r; i < obj.length; i++) {
            r = fun.call(obj, obj[i]);
            if (r) { return r; }
        }
        return null;
    }
}

Property.prototype.type = 'Property';
export default Property;
