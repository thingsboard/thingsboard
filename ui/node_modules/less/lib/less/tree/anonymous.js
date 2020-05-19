import Node from './node';

class Anonymous extends Node {
    constructor(value, index, currentFileInfo, mapLines, rulesetLike, visibilityInfo) {
        super();

        this.value = value;
        this._index = index;
        this._fileInfo = currentFileInfo;
        this.mapLines = mapLines;
        this.rulesetLike = (typeof rulesetLike === 'undefined') ? false : rulesetLike;
        this.allowRoot = true;
        this.copyVisibilityInfo(visibilityInfo);
    }

    eval() {
        return new Anonymous(this.value, this._index, this._fileInfo, this.mapLines, this.rulesetLike, this.visibilityInfo());
    }

    compare(other) {
        return other.toCSS && this.toCSS() === other.toCSS() ? 0 : undefined;
    }

    isRulesetLike() {
        return this.rulesetLike;
    }

    genCSS(context, output) {
        this.nodeVisible = Boolean(this.value);
        if (this.nodeVisible) {
            output.add(this.value, this._fileInfo, this._index, this.mapLines);
        }
    }
}

Anonymous.prototype.type = 'Anonymous';
export default Anonymous;
