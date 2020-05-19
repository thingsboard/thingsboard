import Node from './node';
import unitConversions from '../data/unit-conversions';
import * as utils from '../utils';

class Unit extends Node {
    constructor(numerator, denominator, backupUnit) {
        super();

        this.numerator = numerator ? utils.copyArray(numerator).sort() : [];
        this.denominator = denominator ? utils.copyArray(denominator).sort() : [];
        if (backupUnit) {
            this.backupUnit = backupUnit;
        } else if (numerator && numerator.length) {
            this.backupUnit = numerator[0];
        }
    }

    clone() {
        return new Unit(utils.copyArray(this.numerator), utils.copyArray(this.denominator), this.backupUnit);
    }

    genCSS(context, output) {
        // Dimension checks the unit is singular and throws an error if in strict math mode.
        const strictUnits = context && context.strictUnits;
        if (this.numerator.length === 1) {
            output.add(this.numerator[0]); // the ideal situation
        } else if (!strictUnits && this.backupUnit) {
            output.add(this.backupUnit);
        } else if (!strictUnits && this.denominator.length) {
            output.add(this.denominator[0]);
        }
    }

    toString() {
        let i;
        let returnStr = this.numerator.join('*');
        for (i = 0; i < this.denominator.length; i++) {
            returnStr += `/${this.denominator[i]}`;
        }
        return returnStr;
    }

    compare(other) {
        return this.is(other.toString()) ? 0 : undefined;
    }

    is(unitString) {
        return this.toString().toUpperCase() === unitString.toUpperCase();
    }

    isLength() {
        return RegExp('^(px|em|ex|ch|rem|in|cm|mm|pc|pt|ex|vw|vh|vmin|vmax)$', 'gi').test(this.toCSS());
    }

    isEmpty() {
        return this.numerator.length === 0 && this.denominator.length === 0;
    }

    isSingular() {
        return this.numerator.length <= 1 && this.denominator.length === 0;
    }

    map(callback) {
        let i;

        for (i = 0; i < this.numerator.length; i++) {
            this.numerator[i] = callback(this.numerator[i], false);
        }

        for (i = 0; i < this.denominator.length; i++) {
            this.denominator[i] = callback(this.denominator[i], true);
        }
    }

    usedUnits() {
        let group;
        const result = {};
        let mapUnit;
        let groupName;

        mapUnit = atomicUnit => {
            /* jshint loopfunc:true */
            if (group.hasOwnProperty(atomicUnit) && !result[groupName]) {
                result[groupName] = atomicUnit;
            }

            return atomicUnit;
        };

        for (groupName in unitConversions) {
            if (unitConversions.hasOwnProperty(groupName)) {
                group = unitConversions[groupName];

                this.map(mapUnit);
            }
        }

        return result;
    }

    cancel() {
        const counter = {};
        let atomicUnit;
        let i;

        for (i = 0; i < this.numerator.length; i++) {
            atomicUnit = this.numerator[i];
            counter[atomicUnit] = (counter[atomicUnit] || 0) + 1;
        }

        for (i = 0; i < this.denominator.length; i++) {
            atomicUnit = this.denominator[i];
            counter[atomicUnit] = (counter[atomicUnit] || 0) - 1;
        }

        this.numerator = [];
        this.denominator = [];

        for (atomicUnit in counter) {
            if (counter.hasOwnProperty(atomicUnit)) {
                const count = counter[atomicUnit];

                if (count > 0) {
                    for (i = 0; i < count; i++) {
                        this.numerator.push(atomicUnit);
                    }
                } else if (count < 0) {
                    for (i = 0; i < -count; i++) {
                        this.denominator.push(atomicUnit);
                    }
                }
            }
        }

        this.numerator.sort();
        this.denominator.sort();
    }
}

Unit.prototype.type = 'Unit';
export default Unit;
