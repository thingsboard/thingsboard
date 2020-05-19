/**@license
 *       __ _____                     ________                              __
 *      / // _  /__ __ _____ ___ __ _/__  ___/__ ___ ______ __ __  __ ___  / /
 *  __ / // // // // // _  // _// // / / // _  // _//     // //  \/ // _ \/ /
 * /  / // // // // // ___// / / // / / // ___// / / / / // // /\  // // / /__
 * \___//____ \\___//____//_/ _\_  / /_//____//_/ /_/ /_//_//_/ /_/ \__\_\___/
 *           \/              /____/
 * http://terminal.jcubic.pl
 *
 * This is example of how to create less like command for jQuery Terminal
 * the code is based on the one from leash shell and written as jQuery plugin
 *
 * Copyright (c) 2018 Jakub Jankiewicz <http://jcubic.pl/me>
 * Released under the MIT license
 *
 */
/* global jQuery, define, global, require, module */
(function(factory, undefined) {
    var root = typeof window !== 'undefined' ? window : global;
    if (typeof define === 'function' && define.amd) {
        // AMD. Register as an anonymous module.
        // istanbul ignore next
        define(['jquery', 'jquery.terminal'], factory);
    } else if (typeof module === 'object' && module.exports) {
        // Node/CommonJS
        module.exports = function(root, jQuery) {
            if (jQuery === undefined) {
                // require('jQuery') returns a factory that requires window to
                // build a jQuery instance, we normalize how we use modules
                // that require this pattern but the window provided is a noop
                // if it's defined (how jquery works)
                if (window !== undefined) {
                    jQuery = require('jquery');
                } else {
                    jQuery = require('jquery')(root);
                }
            }
            if (!jQuery.fn.terminal) {
                if (window !== undefined) {
                    require('jquery.terminal');
                } else {
                    require('jquery.terminal')(jQuery);
                }
            }
            factory(jQuery);
            return jQuery;
        };
    } else {
        // Browser
        // istanbul ignore next
        factory(root.jQuery);
    }
})(function($) {
    function less(term, text, exit) {
        var export_data = term.export_view();
        var cols, rows;
        var pos = 0;
        var original_lines;
        var lines;
        var prompt;
        var left = 0;
        function print() {
            term.clear();
            if (lines.length - pos > rows - 1) {
                prompt = ':';
            } else {
                prompt = '[[;;;inverted](END)]';
            }
            term.set_prompt(prompt);
            var to_print = lines.slice(pos, pos + rows - 1);
            to_print = to_print.map(function(line) {
                return $.terminal.substring(line, left, left + cols - 1);
            });
            if (to_print.length < rows - 1) {
                while (rows - 1 > to_print.length) {
                    to_print.push('~');
                }
            }
            term.echo(to_print.join('\n'));
        }
        function quit() {
            term.pop().import_view(export_data);
            //term.off('mousewheel', wheel);
            if ($.isFunction(exit)) {
                exit();
            }
        }
        function refresh_view() {
            cols = term.cols();
            rows = term.rows();
            if ($.isFunction(text)) {
                text(cols, function(new_lines) {
                    original_lines = new_lines;
                    lines = original_lines.slice();
                    print();
                });
            } else {
                original_lines = text.split('\n');
                lines = original_lines.slice();
                print();
            }
        }
        refresh_view();
        var scroll_by = 3;
        //term.on('mousewheel', wheel);
        var in_search = false, last_found, search_string;
        function search(start, reset) {
            var escape = $.terminal.escape_brackets(search_string),
                flag = search_string.toLowerCase() === search_string ? 'i' : '',
                start_re = new RegExp('^(' + escape + ')', flag),
                index = -1,
                prev_format = '',
                formatting = false,
                in_text = false;
            lines = original_lines.slice();
            if (reset) {
                index = pos = 0;
            }
            for (var i = 0; i < lines.length; ++i) {
                var line = lines[i];
                for (var j = 0, jlen = line.length; j < jlen; ++j) {
                    if (line[j] === '[' && line[j + 1] === '[') {
                        formatting = true;
                        in_text = false;
                        start = j;
                    } else if (formatting && line[j] === ']') {
                        if (in_text) {
                            formatting = false;
                            in_text = false;
                        } else {
                            in_text = true;
                            prev_format = line.substring(start, j + 1);
                        }
                    } else if (formatting && in_text || !formatting) {
                        if (line.substring(j).match(start_re)) {
                            var rep;
                            if (formatting && in_text) {
                                rep = '][[;;;inverted]$1]' + prev_format;
                            } else {
                                rep = '[[;;;inverted]$1]';
                            }
                            line = line.substring(0, j) +
                                line.substring(j).replace(start_re, rep);
                            j += rep.length - 2;
                            if (i > pos && index === -1) {
                                index = pos = i;
                            }
                        }
                    }
                }
                lines[i] = line;
            }
            print();
            term.set_command('');
            term.set_prompt(prompt);
            return index;
        }
        term.push($.noop, {
            resize: refresh_view,
            mousewheel: function(event, delta) {
                if (delta > 0) {
                    pos -= scroll_by;
                    if (pos < 0) {
                        pos = 0;
                    }
                } else {
                    pos += scroll_by;
                    if (pos - 1 > lines.length - rows) {
                        pos = lines.length - rows + 1;
                    }
                }
                print();
                return false;
            },
            name: 'less',
            keydown: function(e) {
                var command = term.get_command();
                if (term.get_prompt() !== '/') {
                    if (e.which === 191) {
                        term.set_prompt('/');
                    } else if (in_search &&
                               $.inArray(e.which, [78, 80]) !== -1) {
                        if (e.which === 78) { // search_string
                            if (last_found !== -1) {
                                last_found = search(last_found + 1);
                            }
                        } else if (e.which === 80) { // P
                            last_found = search(0, true);
                        }
                    } else if (e.which === 81) { //Q
                        quit();
                    } else if (e.which === 39) { // right
                        left += Math.round(cols / 2);
                        print();
                    } else if (e.which === 37) { // left
                        left -= Math.round(cols / 2);
                        if (left < 0) {
                            left = 0;
                        }
                        print();
                        // scroll
                    } else if (lines.length > rows) {
                        if (e.which === 38) { //up
                            if (pos > 0) {
                                --pos;
                                print();
                            }
                        } else if (e.which === 40) { //down
                            if (pos <= lines.length - rows) {
                                ++pos;
                                print();
                            }
                        } else if (e.which === 34) {
                            // Page up
                            pos += rows;
                            var limit = lines.length - rows + 1;
                            if (pos > limit) {
                                pos = limit;
                            }
                            print();
                        } else if (e.which === 33) {
                            //Page Down
                            pos -= rows;
                            if (pos < 0) {
                                pos = 0;
                            }
                            print();
                        }
                    }
                    if (!e.ctrlKey && !e.alKey) {
                        return false;
                    }
                    // search
                } else if (e.which === 8 && command === '') {
                    // backspace
                    term.set_prompt(prompt);
                } else if (e.which === 13) { // enter
                    // basic search find only first
                    if (command.length > 0) {
                        in_search = true;
                        pos = 0;
                        search_string = command;
                        last_found = search(0);
                    }
                    return false;
                }
            },
            prompt: prompt
        });
    }
    $.fn.less = function(text, options) {
        var settings = $.extend({
            onExit: $.noop
        }, options);
        if (!this.terminal) {
            throw new Error('This plugin require jQuery Terminal');
        }
        var term = this.terminal();
        if (!term) {
            throw new Error(
                'You need to invoke this plugin on selector that have ' +
                'jQuery Terminal or on jQuery Terminal instance'
            );
        }
        less(term, text, settings.onExit);
    };
});
