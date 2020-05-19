import Anonymous from '../tree/anonymous';
import Keyword from '../tree/keyword';

function boolean(condition) {
    return condition ? Keyword.True : Keyword.False;
}

function If(condition, trueValue, falseValue) {
    return condition ? trueValue
        : (falseValue || new Anonymous);
}

export default { boolean, 'if': If };
