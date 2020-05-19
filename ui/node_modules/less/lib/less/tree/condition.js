import Node from './node';

class Condition extends Node {
    constructor(op, l, r, i, negate) {
        super();

        this.op = op.trim();
        this.lvalue = l;
        this.rvalue = r;
        this._index = i;
        this.negate = negate;
    }

    accept(visitor) {
        this.lvalue = visitor.visit(this.lvalue);
        this.rvalue = visitor.visit(this.rvalue);
    }

    eval(context) {
        const result = ((op, a, b) => {
            switch (op) {
                case 'and': return a && b;
                case 'or':  return a || b;
                default:
                    switch (Node.compare(a, b)) {
                        case -1:
                            return op === '<' || op === '=<' || op === '<=';
                        case 0:
                            return op === '=' || op === '>=' || op === '=<' || op === '<=';
                        case 1:
                            return op === '>' || op === '>=';
                        default:
                            return false;
                    }
            }
        })(this.op, this.lvalue.eval(context), this.rvalue.eval(context));

        return this.negate ? !result : result;
    }
}

Condition.prototype.type = 'Condition';
export default Condition;
