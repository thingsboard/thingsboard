class SetTreeVisibilityVisitor {
    constructor(visible) {
        this.visible = visible;
    }

    run(root) {
        this.visit(root);
    }

    visitArray(nodes) {
        if (!nodes) {
            return nodes;
        }

        const cnt = nodes.length;
        let i;
        for (i = 0; i < cnt; i++) {
            this.visit(nodes[i]);
        }
        return nodes;
    }

    visit(node) {
        if (!node) {
            return node;
        }
        if (node.constructor === Array) {
            return this.visitArray(node);
        }

        if (!node.blocksVisibility || node.blocksVisibility()) {
            return node;
        }
        if (this.visible) {
            node.ensureVisibility();
        } else {
            node.ensureInvisibility();
        }

        node.accept(this);
        return node;
    }
}

export default SetTreeVisibilityVisitor;