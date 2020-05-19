// Array.indexOf polyfill for IE8
if (!Array.prototype.indexOf) {
        Array.prototype.indexOf = function (searchElement, fromIndex) {
                var k;

                // 1. Let O be the result of calling ToObject passing
                //    the this value as the argument.
                if (this == null) {
                        throw new TypeError('"this" is null or not defined');
                }

                var O = Object(this);

                // 2. Let lenValue be the result of calling the Get
                //    internal method of O with the argument "length".
                // 3. Let len be ToUint32(lenValue).
                var len = O.length >>> 0;

                // 4. If len is 0, return -1.
                if (len === 0) {
                        return -1;
                }

                // 5. If argument fromIndex was passed let n be
                //    ToInteger(fromIndex); else let n be 0.
                var n = +fromIndex || 0;

                if (Math.abs(n) === Infinity) {
                        n = 0;
                }

                // 6. If n >= len, return -1.
                if (n >= len) {
                        return -1;
                }

                // 7. If n >= 0, then Let k be n.
                // 8. Else, n<0, Let k be len - abs(n).
                //    If k is less than 0, then let k be 0.
                k = Math.max(n >= 0 ? n : len - Math.abs(n), 0);

                // 9. Repeat, while k < len
                while (k < len) {
                        // a. Let Pk be ToString(k).
                        //   This is implicit for LHS operands of the in operator
                        // b. Let kPresent be the result of calling the
                        //    HasProperty internal method of O with argument Pk.
                        //   This step can be combined with c
                        // c. If kPresent is true, then
                        //    i.  Let elementK be the result of calling the Get
                        //        internal method of O with the argument ToString(k).
                        //   ii.  Let same be the result of applying the
                        //        Strict Equality Comparison Algorithm to
                        //        searchElement and elementK.
                        //  iii.  If same is true, return k.
                        if (k in O && O[k] === searchElement) {
                                return k;
                        }
                        k++;
                }
                return -1;
        };
}