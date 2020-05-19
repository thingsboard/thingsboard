// pos-to-linecolumn.js
// MIT licensed, see LICENSE file
// Copyright (c) 2014-2016 Olov Lassus <olov.lassus@gmail.com>

"use strict";

const assert = require("assert");

module.exports = class PosToLineColumn {
    constructor(str) {
        str = String(str);

        const newlines = [];
        let pos = -1;
        while ((pos = str.indexOf("\n", pos + 1)) >= 0) {
            newlines.push(pos);
        }

        let line = 1;
        let column = 0;
        const columns = [];
        const lines = [];
        let i;
        let j = 0;
        for (i = 0; i < str.length; i++) {
            columns[i] = column;
            lines[i] = line;

            if (i === newlines[j]) {
                ++j;
                ++line;
                column = 0;
            } else {
                ++column;
            }
        }

        // add extra entry to support pos === str.length
        columns[i] = column;
        lines[i] = line;

        this.len = str.length;
        this.columns = columns;
        this.lines = lines;
    }

    toLine(pos) {
        assert(pos >= 0 && pos <= this.len);
        return this.lines[pos];
    }

    toColumn(pos) {
        assert(pos >= 0 && pos <= this.len);
        return this.columns[pos];
    }

    toLineColumn(pos) {
        return {
            line: this.toLine(pos),
            column: this.toColumn(pos),
        };
    }
};

/*
const tst = "asdf\n" +
    "abc\n" +
    "d\n" +
    "\n\n" +
    "efghi a\r\n" +
    "x";
const instance = new PosToLineColumn(tst);
console.dir(instance.toLineColumn(0));
console.dir(instance.toLineColumn(tst.length));
*/
