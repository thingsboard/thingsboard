import Node from './node';

class Keyword extends Node {
    constructor(value) {
        super();

        this.value = value;
    }

    genCSS(context, output) {
        if (this.value === '%') { throw { type: 'Syntax', message: 'Invalid % without number' }; }
        output.add(this.value);
    }
}

Keyword.prototype.type = 'Keyword';

Keyword.True = new Keyword('true');
Keyword.False = new Keyword('false');

export default Keyword;
