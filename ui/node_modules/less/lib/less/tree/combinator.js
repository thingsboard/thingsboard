import Node from './node';
const _noSpaceCombinators = {
    '': true,
    ' ': true,
    '|': true
};

class Combinator extends Node {
    constructor(value) {
        super();

        if (value === ' ') {
            this.value = ' ';
            this.emptyOrWhitespace = true;
        } else {
            this.value = value ? value.trim() : '';
            this.emptyOrWhitespace = this.value === '';
        }
    }

    genCSS(context, output) {
        const spaceOrEmpty = (context.compress || _noSpaceCombinators[this.value]) ? '' : ' ';
        output.add(spaceOrEmpty + this.value + spaceOrEmpty);
    }
}

Combinator.prototype.type = 'Combinator';

export default Combinator;
