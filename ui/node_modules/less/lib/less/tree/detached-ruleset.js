import Node from './node';
import contexts from '../contexts';
import * as utils from '../utils';

class DetachedRuleset extends Node {
    constructor(ruleset, frames) {
        super();

        this.ruleset = ruleset;
        this.frames = frames;
        this.setParent(this.ruleset, this);
    }

    accept(visitor) {
        this.ruleset = visitor.visit(this.ruleset);
    }

    eval(context) {
        const frames = this.frames || utils.copyArray(context.frames);
        return new DetachedRuleset(this.ruleset, frames);
    }

    callEval(context) {
        return this.ruleset.eval(this.frames ? new contexts.Eval(context, this.frames.concat(context.frames)) : context);
    }
}

DetachedRuleset.prototype.type = 'DetachedRuleset';
DetachedRuleset.prototype.evalFirst = true;
export default DetachedRuleset;
