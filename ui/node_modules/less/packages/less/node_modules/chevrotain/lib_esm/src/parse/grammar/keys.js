// Lookahead keys are 32Bit integers in the form
// TTTTTTTT-ZZZZZZZZZZZZ-YYYY-XXXXXXXX
// XXXX -> Occurrence Index bitmap.
// YYYY -> DSL Method Type bitmap.
// ZZZZZZZZZZZZZZZ -> Rule short Index bitmap.
// TTTTTTTTT -> alternation alternative index bitmap
export var BITS_FOR_METHOD_TYPE = 4;
export var BITS_FOR_OCCURRENCE_IDX = 8;
export var BITS_FOR_RULE_IDX = 12;
// TODO: validation, this means that there may at most 2^8 --> 256 alternatives for an alternation.
export var BITS_FOR_ALT_IDX = 8;
// short string used as part of mapping keys.
// being short improves the performance when composing KEYS for maps out of these
// The 5 - 8 bits (16 possible values, are reserved for the DSL method indices)
/* tslint:disable */
export var OR_IDX = 1 << BITS_FOR_OCCURRENCE_IDX;
export var OPTION_IDX = 2 << BITS_FOR_OCCURRENCE_IDX;
export var MANY_IDX = 3 << BITS_FOR_OCCURRENCE_IDX;
export var AT_LEAST_ONE_IDX = 4 << BITS_FOR_OCCURRENCE_IDX;
export var MANY_SEP_IDX = 5 << BITS_FOR_OCCURRENCE_IDX;
export var AT_LEAST_ONE_SEP_IDX = 6 << BITS_FOR_OCCURRENCE_IDX;
/* tslint:enable */
// this actually returns a number, but it is always used as a string (object prop key)
export function getKeyForAutomaticLookahead(ruleIdx, dslMethodIdx, occurrence) {
    /* tslint:disable */
    return occurrence | dslMethodIdx | ruleIdx;
    /* tslint:enable */
}
var BITS_START_FOR_ALT_IDX = 32 - BITS_FOR_ALT_IDX;
export function getKeyForAltIndex(ruleIdx, dslMethodIdx, occurrence, altIdx) {
    /* tslint:disable */
    // alternative indices are zero based, thus must always add one (turn on one bit) to guarantee uniqueness.
    var altIdxBitMap = (altIdx + 1) << BITS_START_FOR_ALT_IDX;
    return (getKeyForAutomaticLookahead(ruleIdx, dslMethodIdx, occurrence) |
        altIdxBitMap);
    /* tslint:enable */
}
//# sourceMappingURL=keys.js.map