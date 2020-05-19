import Dimension from '../tree/dimension';

const MathHelper = (fn, unit, n) => {
    if (!(n instanceof Dimension)) {
        throw { type: 'Argument', message: 'argument must be a number' };
    }
    if (unit == null) {
        unit = n.unit;
    } else {
        n = n.unify();
    }
    return new Dimension(fn(parseFloat(n.value)), unit);
};

export default MathHelper;