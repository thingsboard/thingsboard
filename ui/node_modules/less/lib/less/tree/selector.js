import Node from './node';
import Element from './element';
import LessError from '../less-error';

class Selector extends Node {
    constructor(elements, extendList, condition, index, currentFileInfo, visibilityInfo) {
        super();

        this.extendList = extendList;
        this.condition = condition;
        this.evaldCondition = !condition;
        this._index = index;
        this._fileInfo = currentFileInfo;
        this.elements = this.getElements(elements);
        this.mixinElements_ = undefined;
        this.copyVisibilityInfo(visibilityInfo);
        this.setParent(this.elements, this);
    }

    accept(visitor) {
        if (this.elements) {
            this.elements = visitor.visitArray(this.elements);
        }
        if (this.extendList) {
            this.extendList = visitor.visitArray(this.extendList);
        }
        if (this.condition) {
            this.condition = visitor.visit(this.condition);
        }
    }

    createDerived(elements, extendList, evaldCondition) {
        elements = this.getElements(elements);
        const newSelector = new Selector(elements, extendList || this.extendList,
            null, this.getIndex(), this.fileInfo(), this.visibilityInfo());
        newSelector.evaldCondition = (evaldCondition != null) ? evaldCondition : this.evaldCondition;
        newSelector.mediaEmpty = this.mediaEmpty;
        return newSelector;
    }

    getElements(els) {
        if (!els) {
            return [new Element('', '&', false, this._index, this._fileInfo)];
        }
        if (typeof els === 'string') {
            this.parse.parseNode(
                els, 
                ['selector'],
                this._index, 
                this._fileInfo, 
                function(err, result) {
                    if (err) {
                        throw new LessError({
                            index: err.index,
                            message: err.message
                        }, this.parse.imports, this._fileInfo.filename);
                    }
                    els = result[0].elements;
                });
        }
        return els;
    }

    createEmptySelectors() {
        const el = new Element('', '&', false, this._index, this._fileInfo);
        const sels = [new Selector([el], null, null, this._index, this._fileInfo)];
        sels[0].mediaEmpty = true;
        return sels;
    }

    match(other) {
        const elements = this.elements;
        const len = elements.length;
        let olen;
        let i;

        other = other.mixinElements();
        olen = other.length;
        if (olen === 0 || len < olen) {
            return 0;
        } else {
            for (i = 0; i < olen; i++) {
                if (elements[i].value !== other[i]) {
                    return 0;
                }
            }
        }

        return olen; // return number of matched elements
    }

    mixinElements() {
        if (this.mixinElements_) {
            return this.mixinElements_;
        }

        let elements = this.elements.map( v => v.combinator.value + (v.value.value || v.value)).join('').match(/[,&#\*\.\w-]([\w-]|(\\.))*/g);

        if (elements) {
            if (elements[0] === '&') {
                elements.shift();
            }
        } else {
            elements = [];
        }

        return (this.mixinElements_ = elements);
    }

    isJustParentSelector() {
        return !this.mediaEmpty &&
            this.elements.length === 1 &&
            this.elements[0].value === '&' &&
            (this.elements[0].combinator.value === ' ' || this.elements[0].combinator.value === '');
    }

    eval(context) {
        const evaldCondition = this.condition && this.condition.eval(context);
        let elements = this.elements;
        let extendList = this.extendList;

        elements = elements && elements.map(e => e.eval(context));
        extendList = extendList && extendList.map(extend => extend.eval(context));

        return this.createDerived(elements, extendList, evaldCondition);
    }

    genCSS(context, output) {
        let i;
        let element;
        if ((!context || !context.firstSelector) && this.elements[0].combinator.value === '') {
            output.add(' ', this.fileInfo(), this.getIndex());
        }
        for (i = 0; i < this.elements.length; i++) {
            element = this.elements[i];
            element.genCSS(context, output);
        }
    }

    getIsOutput() {
        return this.evaldCondition;
    }
}

Selector.prototype.type = 'Selector';
export default Selector;
