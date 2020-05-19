import Node from './node';

class Attribute extends Node {
    constructor(key, op, value) {
        super();

        this.key = key;
        this.op = op;
        this.value = value;
    }

    eval(context) {
        return new Attribute(this.key.eval ? this.key.eval(context) : this.key,
            this.op, (this.value && this.value.eval) ? this.value.eval(context) : this.value);
    }

    genCSS(context, output) {
        output.add(this.toCSS(context));
    }

    toCSS(context) {
        let value = this.key.toCSS ? this.key.toCSS(context) : this.key;

        if (this.op) {
            value += this.op;
            value += (this.value.toCSS ? this.value.toCSS(context) : this.value);
        }

        return `[${value}]`;
    }
}

Attribute.prototype.type = 'Attribute';
export default Attribute;
