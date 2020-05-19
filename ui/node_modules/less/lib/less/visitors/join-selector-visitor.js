import Visitor from './visitor';

class JoinSelectorVisitor {
    constructor() {
        this.contexts = [[]];
        this._visitor = new Visitor(this);
    }

    run(root) {
        return this._visitor.visit(root);
    }

    visitDeclaration(declNode, visitArgs) {
        visitArgs.visitDeeper = false;
    }

    visitMixinDefinition(mixinDefinitionNode, visitArgs) {
        visitArgs.visitDeeper = false;
    }

    visitRuleset(rulesetNode, visitArgs) {
        const context = this.contexts[this.contexts.length - 1];
        const paths = [];
        let selectors;

        this.contexts.push(paths);

        if (!rulesetNode.root) {
            selectors = rulesetNode.selectors;
            if (selectors) {
                selectors = selectors.filter(selector => selector.getIsOutput());
                rulesetNode.selectors = selectors.length ? selectors : (selectors = null);
                if (selectors) { rulesetNode.joinSelectors(paths, context, selectors); }
            }
            if (!selectors) { rulesetNode.rules = null; }
            rulesetNode.paths = paths;
        }
    }

    visitRulesetOut(rulesetNode) {
        this.contexts.length = this.contexts.length - 1;
    }

    visitMedia(mediaNode, visitArgs) {
        const context = this.contexts[this.contexts.length - 1];
        mediaNode.rules[0].root = (context.length === 0 || context[0].multiMedia);
    }

    visitAtRule(atRuleNode, visitArgs) {
        const context = this.contexts[this.contexts.length - 1];
        if (atRuleNode.rules && atRuleNode.rules.length) {
            atRuleNode.rules[0].root = (atRuleNode.isRooted || context.length === 0 || null);
        }
    }
}

export default JoinSelectorVisitor;
