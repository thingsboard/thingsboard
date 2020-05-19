import Node from './node';

class UnicodeDescriptor extends Node {
    constructor(value) {
        super();

        this.value = value;
    }
}

UnicodeDescriptor.prototype.type = 'UnicodeDescriptor';

export default UnicodeDescriptor;
