import Node from './node';
import Variable from './variable';
import Ruleset from './ruleset';
import DetachedRuleset from './detached-ruleset';
import LessError from '../less-error';

class VariableCall extends Node {
    constructor(variable, index, currentFileInfo) {
        super();

        this.variable = variable;
        this._index = index;
        this._fileInfo = currentFileInfo;
        this.allowRoot = true;
    }

    eval(context) {
        let rules;
        let detachedRuleset = new Variable(this.variable, this.getIndex(), this.fileInfo()).eval(context);
        const error = new LessError({message: `Could not evaluate variable call ${this.variable}`});

        if (!detachedRuleset.ruleset) {
            if (detachedRuleset.rules) {
                rules = detachedRuleset;
            }
            else if (Array.isArray(detachedRuleset)) {
                rules = new Ruleset('', detachedRuleset);
            }
            else if (Array.isArray(detachedRuleset.value)) {
                rules = new Ruleset('', detachedRuleset.value);
            }
            else {
                throw error;
            }
            detachedRuleset = new DetachedRuleset(rules);
        }

        if (detachedRuleset.ruleset) {
            return detachedRuleset.callEval(context);
        }
        throw error;
    }
}

VariableCall.prototype.type = 'VariableCall';
export default VariableCall;
