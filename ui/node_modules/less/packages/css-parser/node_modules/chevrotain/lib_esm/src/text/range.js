var Range = /** @class */ (function () {
    function Range(start, end) {
        this.start = start;
        this.end = end;
        if (!isValidRange(start, end)) {
            throw new Error("INVALID RANGE");
        }
    }
    Range.prototype.contains = function (num) {
        return this.start <= num && this.end >= num;
    };
    Range.prototype.containsRange = function (other) {
        return this.start <= other.start && this.end >= other.end;
    };
    Range.prototype.isContainedInRange = function (other) {
        return other.containsRange(this);
    };
    Range.prototype.strictlyContainsRange = function (other) {
        return this.start < other.start && this.end > other.end;
    };
    Range.prototype.isStrictlyContainedInRange = function (other) {
        return other.strictlyContainsRange(this);
    };
    return Range;
}());
export { Range };
export function isValidRange(start, end) {
    return !(start < 0 || end < start);
}
//# sourceMappingURL=range.js.map