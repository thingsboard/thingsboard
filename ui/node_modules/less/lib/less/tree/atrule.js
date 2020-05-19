import Node from './node';
import Selector from './selector';
import Ruleset from './ruleset';
import Anonymous from './anonymous';

class AtRule extends Node {
    constructor(
        name,
        value,
        rules,
        index,
        currentFileInfo,
        debugInfo,
        isRooted,
        visibilityInfo
    ) {
        super();

        let i;

        this.name  = name;
        this.value = (value instanceof Node) ? value : (value ? new Anonymous(value) : value);
        if (rules) {
            if (Array.isArray(rules)) {
                this.rules = rules;
            } else {
                this.rules = [rules];
                this.rules[0].selectors = (new Selector([], null, null, index, currentFileInfo)).createEmptySelectors();
            }
            for (i = 0; i < this.rules.length; i++) {
                this.rules[i].allowImports = true;
            }
            this.setParent(this.rules, this);
        }
        this._index = index;
        this._fileInfo = currentFileInfo;
        this.debugInfo = debugInfo;
        this.isRooted = isRooted || false;
        this.copyVisibilityInfo(visibilityInfo);
        this.allowRoot = true;
    }

    accept(visitor) {
        const value = this.value;
        const rules = this.rules;
        if (rules) {
            this.rules = visitor.visitArray(rules);
        }
        if (value) {
            this.value = visitor.visit(value);
        }
    }

    isRulesetLike() {
        return this.rules || !this.isCharset();
    }

    isCharset() {
        return '@charset' === this.name;
    }

    genCSS(context, output) {
        const value = this.value;
        const rules = this.rules;
        output.add(this.name, this.fileInfo(), this.getIndex());
        if (value) {
            output.add(' ');
            value.genCSS(context, output);
        }
        if (rules) {
            this.outputRuleset(context, output, rules);
        } else {
            output.add(';');
        }
    }

    eval(context) {
        let mediaPathBackup;
        let mediaBlocksBackup;
        let value = this.value;
        let rules = this.rules;

        // media stored inside other atrule should not bubble over it
        // backpup media bubbling information
        mediaPathBackup = context.mediaPath;
        mediaBlocksBackup = context.mediaBlocks;
        // deleted media bubbling information
        context.mediaPath = [];
        context.mediaBlocks = [];

        if (value) {
            value = value.eval(context);
        }
        if (rules) {
            // assuming that there is only one rule at this point - that is how parser constructs the rule
            rules = [rules[0].eval(context)];
            rules[0].root = true;
        }
        // restore media bubbling information
        context.mediaPath = mediaPathBackup;
        context.mediaBlocks = mediaBlocksBackup;

        return new AtRule(this.name, value, rules,
            this.getIndex(), this.fileInfo(), this.debugInfo, this.isRooted, this.visibilityInfo());
    }

    variable(name) {
        if (this.rules) {
            // assuming that there is only one rule at this point - that is how parser constructs the rule
            return Ruleset.prototype.variable.call(this.rules[0], name);
        }
    }

    find(...args) {
        if (this.rules) {
            // assuming that there is only one rule at this point - that is how parser constructs the rule
            return Ruleset.prototype.find.apply(this.rules[0], args);
        }
    }

    rulesets() {
        if (this.rules) {
            // assuming that there is only one rule at this point - that is how parser constructs the rule
            return Ruleset.prototype.rulesets.apply(this.rules[0]);
        }
    }

    outputRuleset(context, output, rules) {
        const ruleCnt = rules.length;
        let i;
        context.tabLevel = (context.tabLevel | 0) + 1;

        // Compressed
        if (context.compress) {
            output.add('{');
            for (i = 0; i < ruleCnt; i++) {
                rules[i].genCSS(context, output);
            }
            output.add('}');
            context.tabLevel--;
            return;
        }

        // Non-compressed
        const tabSetStr = `\n${Array(context.tabLevel).join('  ')}`;

        const tabRuleStr = `${tabSetStr}  `;
        if (!ruleCnt) {
            output.add(` {${tabSetStr}}`);
        } else {
            output.add(` {${tabRuleStr}`);
            rules[0].genCSS(context, output);
            for (i = 1; i < ruleCnt; i++) {
                output.add(tabRuleStr);
                rules[i].genCSS(context, output);
            }
            output.add(`${tabSetStr}}`);
        }

        context.tabLevel--;
    }
}

AtRule.prototype.type = 'AtRule';
export default AtRule;
