import Node from './node';
import Selector from './selector';
import MixinDefinition from './mixin-definition';
import defaultFunc from '../functions/default';

class MixinCall extends Node {
    constructor(elements, args, index, currentFileInfo, important) {
        super();

        this.selector = new Selector(elements);
        this.arguments = args || [];
        this._index = index;
        this._fileInfo = currentFileInfo;
        this.important = important;
        this.allowRoot = true;
        this.setParent(this.selector, this);
    }

    accept(visitor) {
        if (this.selector) {
            this.selector = visitor.visit(this.selector);
        }
        if (this.arguments.length) {
            this.arguments = visitor.visitArray(this.arguments);
        }
    }

    eval(context) {
        let mixins;
        let mixin;
        let mixinPath;
        const args = [];
        let arg;
        let argValue;
        const rules = [];
        let match = false;
        let i;
        let m;
        let f;
        let isRecursive;
        let isOneFound;
        const candidates = [];
        let candidate;
        const conditionResult = [];
        let defaultResult;
        const defFalseEitherCase = -1;
        const defNone = 0;
        const defTrue = 1;
        const defFalse = 2;
        let count;
        let originalRuleset;
        let noArgumentsFilter;

        this.selector = this.selector.eval(context);

        function calcDefGroup(mixin, mixinPath) {
            let f;
            let p;
            let namespace;

            for (f = 0; f < 2; f++) {
                conditionResult[f] = true;
                defaultFunc.value(f);
                for (p = 0; p < mixinPath.length && conditionResult[f]; p++) {
                    namespace = mixinPath[p];
                    if (namespace.matchCondition) {
                        conditionResult[f] = conditionResult[f] && namespace.matchCondition(null, context);
                    }
                }
                if (mixin.matchCondition) {
                    conditionResult[f] = conditionResult[f] && mixin.matchCondition(args, context);
                }
            }
            if (conditionResult[0] || conditionResult[1]) {
                if (conditionResult[0] != conditionResult[1]) {
                    return conditionResult[1] ?
                        defTrue : defFalse;
                }

                return defNone;
            }
            return defFalseEitherCase;
        }

        for (i = 0; i < this.arguments.length; i++) {
            arg = this.arguments[i];
            argValue = arg.value.eval(context);
            if (arg.expand && Array.isArray(argValue.value)) {
                argValue = argValue.value;
                for (m = 0; m < argValue.length; m++) {
                    args.push({value: argValue[m]});
                }
            } else {
                args.push({name: arg.name, value: argValue});
            }
        }

        noArgumentsFilter = rule => rule.matchArgs(null, context);

        for (i = 0; i < context.frames.length; i++) {
            if ((mixins = context.frames[i].find(this.selector, null, noArgumentsFilter)).length > 0) {
                isOneFound = true;

                // To make `default()` function independent of definition order we have two "subpasses" here.
                // At first we evaluate each guard *twice* (with `default() == true` and `default() == false`),
                // and build candidate list with corresponding flags. Then, when we know all possible matches,
                // we make a final decision.

                for (m = 0; m < mixins.length; m++) {
                    mixin = mixins[m].rule;
                    mixinPath = mixins[m].path;
                    isRecursive = false;
                    for (f = 0; f < context.frames.length; f++) {
                        if ((!(mixin instanceof MixinDefinition)) && mixin === (context.frames[f].originalRuleset || context.frames[f])) {
                            isRecursive = true;
                            break;
                        }
                    }
                    if (isRecursive) {
                        continue;
                    }

                    if (mixin.matchArgs(args, context)) {
                        candidate = {mixin, group: calcDefGroup(mixin, mixinPath)};

                        if (candidate.group !== defFalseEitherCase) {
                            candidates.push(candidate);
                        }

                        match = true;
                    }
                }

                defaultFunc.reset();

                count = [0, 0, 0];
                for (m = 0; m < candidates.length; m++) {
                    count[candidates[m].group]++;
                }

                if (count[defNone] > 0) {
                    defaultResult = defFalse;
                } else {
                    defaultResult = defTrue;
                    if ((count[defTrue] + count[defFalse]) > 1) {
                        throw { type: 'Runtime',
                            message: `Ambiguous use of \`default()\` found when matching for \`${this.format(args)}\``,
                            index: this.getIndex(), filename: this.fileInfo().filename };
                    }
                }

                for (m = 0; m < candidates.length; m++) {
                    candidate = candidates[m].group;
                    if ((candidate === defNone) || (candidate === defaultResult)) {
                        try {
                            mixin = candidates[m].mixin;
                            if (!(mixin instanceof MixinDefinition)) {
                                originalRuleset = mixin.originalRuleset || mixin;
                                mixin = new MixinDefinition('', [], mixin.rules, null, false, null, originalRuleset.visibilityInfo());
                                mixin.originalRuleset = originalRuleset;
                            }
                            const newRules = mixin.evalCall(context, args, this.important).rules;
                            this._setVisibilityToReplacement(newRules);
                            Array.prototype.push.apply(rules, newRules);
                        } catch (e) {
                            throw { message: e.message, index: this.getIndex(), filename: this.fileInfo().filename, stack: e.stack };
                        }
                    }
                }

                if (match) {
                    return rules;
                }
            }
        }
        if (isOneFound) {
            throw { type:    'Runtime',
                message: `No matching definition was found for \`${this.format(args)}\``,
                index:   this.getIndex(), filename: this.fileInfo().filename };
        } else {
            throw { type:    'Name',
                message: `${this.selector.toCSS().trim()} is undefined`,
                index:   this.getIndex(), filename: this.fileInfo().filename };
        }
    }

    _setVisibilityToReplacement(replacement) {
        let i;
        let rule;
        if (this.blocksVisibility()) {
            for (i = 0; i < replacement.length; i++) {
                rule = replacement[i];
                rule.addVisibilityBlock();
            }
        }
    }

    format(args) {
        return `${this.selector.toCSS().trim()}(${args ? args.map(a => {
            let argValue = '';
            if (a.name) {
                argValue += `${a.name}:`;
            }
            if (a.value.toCSS) {
                argValue += a.value.toCSS();
            } else {
                argValue += '???';
            }
            return argValue;
        }).join(', ') : ''})`;
    }
}

MixinCall.prototype.type = 'MixinCall';
export default MixinCall;
