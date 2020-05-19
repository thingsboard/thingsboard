/**@license
 *       __ _____                     ________                              __
 *      / // _  /__ __ _____ ___ __ _/__  ___/__ ___ ______ __ __  __ ___  / /
 *  __ / // // // // // _  // _// // / / // _  // _//     // //  \/ // _ \/ /
 * /  / // // // // // ___// / / // / / // ___// / / / / // // /\  // // / /__
 * \___//____ \\___//____//_/ _\_  / /_//____//_/ /_/ /_//_//_/ /_/ \__\_\___/
 *           \/              /____/
 * http://terminal.jcubic.pl
 *
 * utility that renders simple ascii table, like the one from mysql cli tool
 * it was first created for leash shell https://leash.jcubic.pl
 *
 * usage:
 *
 *        var arr = [[1,2,3,4,5], ["lorem", "ipsum", "dolor", "sit", "amet"]];
 *        term.echo(ascii_table(arr));
 *        // or
 *        term.echo(ascii_table(arr, true)); // this will render first row as header
 *
 * Copyright (c) 2018 Jakub Jankiewicz <http://jcubic.pl/me>
 * Released under the MIT license
 *
 */
/* global define, module, global, wcwidth, require */
(function(factory) {
    var root = typeof window !== 'undefined' ? window : global;
    if (typeof define === 'function' && define.amd) {
        // AMD. Register as an anonymous module.
        define(['wcwidth'], function(wcwidth) {
            return (root.ascii_table = factory(wcwidth));
        });
    } else if (typeof module === 'object' && module.exports) {
        // Node/CommonJS
        module.exports = factory(require('wcwidth'));
    } else {
        root.ascii_table = factory(root.wcwidth);
    }
})(function(wcwidth, undefined) {
    var strlen = (function() {
        if (typeof wcwidth === 'undefined') {
            return function(string) {
                return string.length;
            };
        } else {
            return wcwidth;
        }
    })();
    function ascii_table(array, header) {
        if (!array.length) {
            return '';
        }
        for (var i = array.length - 1; i >= 0; i--) {
            var row = array[i];
            var stacks = [];
            for (var j = 0; j < row.length; j++) {
                var new_lines = row[j].toString().split("\n");
                row[j] = new_lines.shift();
                stacks.push(new_lines);
            }
            var stack_lengths = stacks.map(function(column) {
                return column.length;
            });
            var new_rows_count = Math.max.apply(Math, stack_lengths);
            for (var k = new_rows_count - 1; k >= 0; k--) {
                array.splice(i + 1, 0, stacks.map(function(column) {
                    return column[k] || "";
                }));
            }
        }
        var lengths = array[0].map(function(_, i) {
            var col = array.map(function(row) {
                if (row[i] != undefined) {
                    var len = strlen(row[i]);
                    if (row[i].match(/\t/g)) {
                        // tab is 4 spaces
                        len += row[i].match(/\t/g).length*3;
                    }
                    return len;
                } else {
                    return 0;
                }
            });
            return Math.max.apply(Math, col);
        });
        // column padding
        array = array.map(function(row) {
            return '| ' + row.map(function(item, i) {
                var size = strlen(item);
                if (item.match(/\t/g)) {
                    // tab is 4 spaces
                    size += item.match(/\t/g).length*3;
                }
                if (size < lengths[i]) {
                    item += new Array(lengths[i] - size + 1).join(' ');
                }
                return item;
            }).join(' | ') + ' |';
        });
        array = array.map(function(line) {
            return line.replace(/&/g, '&amp;');
        });
        var sep = '+' + lengths.map(function(length) {
            return new Array(length + 3).join('-');
        }).join('+') + '+';
        if (header) {
            return sep + '\n' + array[0] + '\n' + sep + '\n' +
                array.slice(1).join('\n') + '\n' + sep;
        } else {
            return sep + '\n' + array.join('\n') + '\n' + sep;
        }
    }
    return ascii_table;
});
