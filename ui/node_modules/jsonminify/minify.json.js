/*! JSON.minify()
    v0.1 (c) Kyle Simpson
    MIT License
*/
/**
 * @name minify.json.js
 * @author Kei Funagayama <kei.topaz@gmail.com
 * @overview JSON.minify
 */

/**
 * @namespace JSON
 */
(function(global){
    'use strict';

    /**
     * @function
     * @memberof JSON
     * @param {Object} Transformed data. format) json-like
     * @return {String}
     *
     * @example
     * var json = { // hoge
     *     "foo": "bar",// this is cool
     *     "bar": [
     *         "baz", "bum", "zam"   // this is cool
     *     ]
     * } // hoge
     *
     */
    var minify = function (json) {

        var tokenizer = /"|(\/\*)|(\*\/)|(\/\/)|\n|\r|\[|]/g,
            in_string = false,
            in_multiline_comment = false,
            in_singleline_comment = false,
            tmp, tmp2, new_str = [], ns = 0, from = 0, lc, rc
        ;

        tokenizer.lastIndex = 0;

        while ( tmp = tokenizer.exec(json) ) {
            lc = RegExp.leftContext;
            rc = RegExp.rightContext;
            if (!in_multiline_comment && !in_singleline_comment) {
                tmp2 = lc.substring(from);
                if (!in_string) {
                    tmp2 = tmp2.replace(/(\n|\r|\s)*/g,"");
                }
                new_str[ns++] = tmp2;
            }
            from = tokenizer.lastIndex;

            if (tmp[0] === "\"" && !in_multiline_comment && !in_singleline_comment) {
                tmp2 = lc.match(/(\\)*$/);
                if (!in_string || !tmp2 || (tmp2[0].length % 2) === 0) {    // start of string with ", or unescaped " character found to end string
                    in_string = !in_string;
                }
                from--; // include " character in next catch
                rc = json.substring(from);
            }
            else if (tmp[0] === "/*" && !in_string && !in_multiline_comment && !in_singleline_comment) {
                in_multiline_comment = true;
            }
            else if (tmp[0] === "*/" && !in_string && in_multiline_comment && !in_singleline_comment) {
                in_multiline_comment = false;
            }
            else if (tmp[0] === "//" && !in_string && !in_multiline_comment && !in_singleline_comment) {
                in_singleline_comment = true;
            }
            else if ((tmp[0] === "\n" || tmp[0] === "\r") && !in_string && !in_multiline_comment && in_singleline_comment) {
                in_singleline_comment = false;
            }
            else if (!in_multiline_comment && !in_singleline_comment && !(/\n|\r|\s/.test(tmp[0]))) {
                new_str[ns++] = tmp[0];
            }
        }
        new_str[ns++] = rc;
        return new_str.join("");
    };

    if (typeof module !== 'undefined' && module.exports) {
        // node
        module.exports = minify;
        JSON.minify = minify;
    } else {
        // others, export global
        if (typeof global.JSON === "undefined" || !global.JSON) {
            global.JSON = {};
        }
        global.JSON.minify = minify;
    }
})(this);
