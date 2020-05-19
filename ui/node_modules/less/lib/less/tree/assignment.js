import Node from './node';

class Assignment extends Node {
    constructor(key, val) {
        super();

        this.key = key;
        this.value = val;
    }

    accept(visitor) {
        this.value = visitor.visit(this.value);
    }

    eval(context) {
        if (this.value.eval) {
            return new Assignment(this.key, this.value.eval(context));
        }
        return this;
    }

    genCSS(context, output) {
        output.add(`${this.key}=`);
        if (this.value.genCSS) {
            this.value.genCSS(context, output);
        } else {
            output.add(this.value);
        }
    }
}

Assignment.prototype.type = 'Assignment';
export default Assignment;
