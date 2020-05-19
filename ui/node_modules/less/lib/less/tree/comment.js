import Node from './node';
import getDebugInfo from './debug-info';

class Comment extends Node {
    constructor(value, isLineComment, index, currentFileInfo) {
        super();

        this.value = value;
        this.isLineComment = isLineComment;
        this._index = index;
        this._fileInfo = currentFileInfo;
        this.allowRoot = true;
    }

    genCSS(context, output) {
        if (this.debugInfo) {
            output.add(getDebugInfo(context, this), this.fileInfo(), this.getIndex());
        }
        output.add(this.value);
    }

    isSilent(context) {
        const isCompressed = context.compress && this.value[2] !== '!';
        return this.isLineComment || isCompressed;
    }
}

Comment.prototype.type = 'Comment';
export default Comment;
