/**@license
 *       __ _____                     ________                              __
 *      / // _  /__ __ _____ ___ __ _/__  ___/__ ___ ______ __ __  __ ___  / /
 *  __ / // // // // // _  // _// // / / // _  // _//     // //  \/ // _ \/ /
 * /  / // // // // // ___// / / // / / // ___// / / / / // // /\  // // / /__
 * \___//____ \\___//____//_/ _\_  / /_//____//_/ /_/ /_//_//_/ /_/ \__\_\___/
 *           \/              /____/
 * http://terminal.jcubic.pl
 *
 * This is example of how to create custom formatter for jQuery Terminal
 *
 * Copyright (c) 2014-2018 Jakub Jankiewicz <http://jcubic.pl/me>
 * Released under the MIT license
 *
 */
/* global jQuery, define, global, require, module */
(function(factory) {
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
                if (typeof window !== 'undefined') {
                    jQuery = require('jquery');
                } else {
                    jQuery = require('jquery')(root);
                }
            }
            if (!jQuery.fn.terminal) {
                if (typeof window !== 'undefined') {
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
    $.terminal.defaults.unixFormattingEscapeBrackets = false;
    // we match characters and html entities because command line escape brackets
    // echo don't, when writing formatter always process html entitites so it work
    // for cmd plugin as well for echo
    var chr = '[^\\x08]|[\\r\\n]{2}|&[^;]+;';
    var backspace_re = new RegExp('^(' + chr + ')?\\x08');
    var overtyping_re = new RegExp('^(?:(' + chr + ')?\\x08(_|\\1)|' +
                                   '(_)\\x08(' + chr + '))');
    var new_line_re = /^(\r\n|\n\r|\r|\n)/;
    var clear_line_re = /[^\r\n]+\r\x1B\[K/g;
    // ---------------------------------------------------------------------
    function length(string) {
        return $.terminal.length(string);
    }
    // ---------------------------------------------------------------------
    // :: Replace overtyping (from man) formatting with terminal formatting
    // ---------------------------------------------------------------------
    $.terminal.overtyping = function overtyping(string, options) {
        string = $.terminal.unescape_brackets(string);
        var settings = $.extend({
            unixFormattingEscapeBrackets: false,
            position: 0
        }, options);
        var removed_chars = [];
        var new_position;
        var char_count = 0;
        var backspaces = [];
        function replace(string, position) {
            var result = '';
            var push = 0;
            var start;
            char_count = 0;
            function correct_position(start, match, rep_string) {
                // logic taken from $.terminal.tracking_replace
                if (start < position) {
                    var last_index = start + length(match);
                    if (last_index < position) {
                        // It's after the replacement, move it
                        new_position = Math.max(
                            0,
                            new_position +
                            length(rep_string) -
                            length(match)
                        );
                    } else {
                        // It's *in* the replacement, put it just after
                        new_position += length(rep_string) - (position - start);
                    }
                }
            }
            for (var i = 0; i < string.length; ++i) {
                var partial = string.substring(i);
                var match = partial.match(backspace_re);
                var removed_char = removed_chars[0];
                if (match) {
                    // we remove backspace and character or html entity before it
                    // but we keep it in removed array so we can put it back
                    // when we have caritage return or line feed
                    if (match[1]) {
                        start = i - match[1].length + push;
                        removed_chars.push({
                            index: start,
                            string: match[1],
                            overtyping: partial.match(overtyping_re)
                        });
                        correct_position(start, match[0], '', 1);
                    }
                    if (char_count < 0) {
                        char_count = 0;
                    }
                    backspaces = backspaces.map(function(b) {
                        return b - 1;
                    });
                    backspaces.push(start);
                    return result + partial.replace(backspace_re, '');
                } else if (partial.match(new_line_re)) {
                    // if newline we need to add at the end all characters
                    // removed by backspace but only if there are no more
                    // other characters than backspaces added between
                    // backspaces and newline
                    if (removed_chars.length) {
                        var chars = removed_chars;
                        removed_chars = [];
                        chars.reverse().forEach(function(char) {
                            if (i > char.index) {
                                if (--char_count <= 0) {
                                    correct_position(char.index, '', char.string, 2);
                                    result += char.string;
                                }
                            } else {
                                removed_chars.unshift(char);
                            }
                        });
                    }
                    var m = partial.match(new_line_re);
                    result += m[1];
                    i += m[1].length - 1;
                } else {
                    if (backspaces.length) {
                        var backspace = backspaces[0];
                        if (i === backspace) {
                            backspaces.shift();
                        }
                        if (i >= backspace) {
                            char_count++;
                        }
                    }
                    if (removed_chars.length) {
                        // if we are in index of removed character we check if the
                        // character is the same it will be bold or if removed char
                        // or char at index is underscore then it will
                        // be terminal formatting with underscore
                        if (i > removed_char.index && removed_char.overtyping) {
                            removed_chars.shift();
                            correct_position(removed_char.index, '', removed_char.string);
                            // if we add special character we need to correct
                            // next push to removed_char array
                            push++;
                            // we use special characters instead of terminal
                            // formatting so it's easier to proccess when removing
                            // backspaces
                            if (removed_char.string === string[i]) {
                                result += string[i] + '\uFFF1';
                                continue;
                            } else if (removed_char.string === '_' ||
                                       string[i] === '_') {
                                var chr;
                                if (removed_char.string === '_') {
                                    chr = string[i];
                                } else {
                                    chr = removed_char.string;
                                }
                                result += chr + '\uFFF2';
                                continue;
                            }
                        }
                    }
                    result += string[i];
                }
            }
            return result;
        }
        var break_next = false;
        // loop until not more backspaces
        new_position = settings.position;
        // we need to clear line \x1b[K in overtyping because it need to be before
        // overtyping and from_ansi need to be called after so it escape stuff
        // between Escape Code and cmd will have escaped formatting typed by user
        var rep = $.terminal.tracking_replace(string, clear_line_re, '', new_position);
        string = rep[0];
        new_position = rep[1];
        while (string.match(/\x08/) || removed_chars.length) {
            string = replace(string, new_position);
            if (break_next) {
                break;
            }
            if (!string.match(/\x08/)) {
                // we break the loop so if removed_chars still chave items
                // we don't have infite loop
                break_next = true;
            }
        }
        function format(string, chr, style) {
            var re = new RegExp('((:?.' + chr + ')+)', 'g');
            return string.replace(re, function(_, string) {
                var re = new RegExp(chr, 'g');
                return '[[' + style + ']' + string.replace(re, '') + ']';
            });
        }
        // replace special characters with terminal formatting
        string = format(string, '\uFFF1', 'b;#fff;');
        string = format(string, '\uFFF2', 'u;;');
        if (settings.unixFormattingEscapeBrackets) {
            string = $.terminal.escape_brackets(string);
        }
        if (options && typeof options.position === 'number') {
            return [string, new_position];
        }
        return string;
    };
    // ---------------------------------------------------------------------
    // :: Html colors taken from ANSI formatting in Linux Terminal
    // ---------------------------------------------------------------------
    $.terminal.ansi_colors = {
        normal: {
            black: '#000',
            red: '#A00',
            green: '#008400',
            yellow: '#A50',
            blue: '#00A',
            magenta: '#A0A',
            cyan: '#0AA',
            white: '#AAA'
        },
        faited: {
            black: '#000',
            red: '#640000',
            green: '#006100',
            yellow: '#737300',
            blue: '#000087',
            magenta: '#650065',
            cyan: '#008787',
            white: '#818181'
        },
        bold: {
            black: '#000',
            red: '#F55',
            green: '#44D544',
            yellow: '#FF5',
            blue: '#55F',
            magenta: '#F5F',
            cyan: '#5FF',
            white: '#FFF'
        },
        // XTerm 8-bit pallete
        palette: [
            '#000000', '#AA0000', '#00AA00', '#AA5500', '#0000AA', '#AA00AA',
            '#00AAAA', '#AAAAAA', '#555555', '#FF5555', '#55FF55', '#FFFF55',
            '#5555FF', '#FF55FF', '#55FFFF', '#FFFFFF', '#000000', '#00005F',
            '#000087', '#0000AF', '#0000D7', '#0000FF', '#005F00', '#005F5F',
            '#005F87', '#005FAF', '#005FD7', '#005FFF', '#008700', '#00875F',
            '#008787', '#0087AF', '#0087D7', '#0087FF', '#00AF00', '#00AF5F',
            '#00AF87', '#00AFAF', '#00AFD7', '#00AFFF', '#00D700', '#00D75F',
            '#00D787', '#00D7AF', '#00D7D7', '#00D7FF', '#00FF00', '#00FF5F',
            '#00FF87', '#00FFAF', '#00FFD7', '#00FFFF', '#5F0000', '#5F005F',
            '#5F0087', '#5F00AF', '#5F00D7', '#5F00FF', '#5F5F00', '#5F5F5F',
            '#5F5F87', '#5F5FAF', '#5F5FD7', '#5F5FFF', '#5F8700', '#5F875F',
            '#5F8787', '#5F87AF', '#5F87D7', '#5F87FF', '#5FAF00', '#5FAF5F',
            '#5FAF87', '#5FAFAF', '#5FAFD7', '#5FAFFF', '#5FD700', '#5FD75F',
            '#5FD787', '#5FD7AF', '#5FD7D7', '#5FD7FF', '#5FFF00', '#5FFF5F',
            '#5FFF87', '#5FFFAF', '#5FFFD7', '#5FFFFF', '#870000', '#87005F',
            '#870087', '#8700AF', '#8700D7', '#8700FF', '#875F00', '#875F5F',
            '#875F87', '#875FAF', '#875FD7', '#875FFF', '#878700', '#87875F',
            '#878787', '#8787AF', '#8787D7', '#8787FF', '#87AF00', '#87AF5F',
            '#87AF87', '#87AFAF', '#87AFD7', '#87AFFF', '#87D700', '#87D75F',
            '#87D787', '#87D7AF', '#87D7D7', '#87D7FF', '#87FF00', '#87FF5F',
            '#87FF87', '#87FFAF', '#87FFD7', '#87FFFF', '#AF0000', '#AF005F',
            '#AF0087', '#AF00AF', '#AF00D7', '#AF00FF', '#AF5F00', '#AF5F5F',
            '#AF5F87', '#AF5FAF', '#AF5FD7', '#AF5FFF', '#AF8700', '#AF875F',
            '#AF8787', '#AF87AF', '#AF87D7', '#AF87FF', '#AFAF00', '#AFAF5F',
            '#AFAF87', '#AFAFAF', '#AFAFD7', '#AFAFFF', '#AFD700', '#AFD75F',
            '#AFD787', '#AFD7AF', '#AFD7D7', '#AFD7FF', '#AFFF00', '#AFFF5F',
            '#AFFF87', '#AFFFAF', '#AFFFD7', '#AFFFFF', '#D70000', '#D7005F',
            '#D70087', '#D700AF', '#D700D7', '#D700FF', '#D75F00', '#D75F5F',
            '#D75F87', '#D75FAF', '#D75FD7', '#D75FFF', '#D78700', '#D7875F',
            '#D78787', '#D787AF', '#D787D7', '#D787FF', '#D7AF00', '#D7AF5F',
            '#D7AF87', '#D7AFAF', '#D7AFD7', '#D7AFFF', '#D7D700', '#D7D75F',
            '#D7D787', '#D7D7AF', '#D7D7D7', '#D7D7FF', '#D7FF00', '#D7FF5F',
            '#D7FF87', '#D7FFAF', '#D7FFD7', '#D7FFFF', '#FF0000', '#FF005F',
            '#FF0087', '#FF00AF', '#FF00D7', '#FF00FF', '#FF5F00', '#FF5F5F',
            '#FF5F87', '#FF5FAF', '#FF5FD7', '#FF5FFF', '#FF8700', '#FF875F',
            '#FF8787', '#FF87AF', '#FF87D7', '#FF87FF', '#FFAF00', '#FFAF5F',
            '#FFAF87', '#FFAFAF', '#FFAFD7', '#FFAFFF', '#FFD700', '#FFD75F',
            '#FFD787', '#FFD7AF', '#FFD7D7', '#FFD7FF', '#FFFF00', '#FFFF5F',
            '#FFFF87', '#FFFFAF', '#FFFFD7', '#FFFFFF', '#080808', '#121212',
            '#1C1C1C', '#262626', '#303030', '#3A3A3A', '#444444', '#4E4E4E',
            '#585858', '#626262', '#6C6C6C', '#767676', '#808080', '#8A8A8A',
            '#949494', '#9E9E9E', '#A8A8A8', '#B2B2B2', '#BCBCBC', '#C6C6C6',
            '#D0D0D0', '#DADADA', '#E4E4E4', '#EEEEEE'
        ]
    };
    // ---------------------------------------------------------------------
    // :: Replace ANSI formatting with terminal formatting
    // ---------------------------------------------------------------------
    $.terminal.from_ansi = (function() {
        var color_list = {
            30: 'black',
            31: 'red',
            32: 'green',
            33: 'yellow',
            34: 'blue',
            35: 'magenta',
            36: 'cyan',
            37: 'white',

            39: 'inherit' // default color
        };
        var background_list = {
            40: 'black',
            41: 'red',
            42: 'green',
            43: 'yellow',
            44: 'blue',
            45: 'magenta',
            46: 'cyan',
            47: 'white',

            49: 'transparent' // default background
        };
        function format_ansi(code) {
            var controls = code.split(';');
            var num;
            var faited = false;
            var reverse = false;
            var bold = false;
            var styles = [];
            var output_color = '';
            var output_background = '';
            var _process_true_color = -1;
            var _ex_color = false;
            var _ex_background = false;
            var _process_8bit = false;
            var palette = $.terminal.ansi_colors.palette;
            function set_styles(num) {
                switch (num) {
                    case 1:
                        styles.push('b');
                        bold = true;
                        faited = false;
                        break;
                    case 4:
                        styles.push('u');
                        break;
                    case 3:
                        styles.push('i');
                        break;
                    case 5:
                        if (_ex_color || _ex_background) {
                            _process_8bit = true;
                        }
                        break;
                    case 38:
                        _ex_color = true;
                        break;
                    case 48:
                        _ex_background = true;
                        break;
                    case 2:
                        if (_ex_color || _ex_background) {
                            _process_true_color = 0;
                        } else {
                            faited = true;
                            bold = false;
                        }
                        break;
                    case 7:
                        reverse = true;
                        break;
                    default:
                        if (controls.indexOf('5') === -1) {
                            if (color_list[num]) {
                                output_color = color_list[num];
                            }
                            if (background_list[num]) {
                                output_background = background_list[num];
                            }
                        }
                }
            }
            // -----------------------------------------------------------------
            function process_true_color() {
                if (_ex_color) {
                    if (!output_color) {
                        output_color = '#';
                    }
                    if (output_color.length < 7) {
                        output_color += ('0' + num.toString(16)).slice(-2);
                    }
                }
                if (_ex_background) {
                    if (!output_background) {
                        output_background = '#';
                    }
                    if (output_background.length < 7) {
                        output_background += ('0' + num.toString(16)).slice(-2);
                    }
                }
                if (_process_true_color === 2) {
                    _process_true_color = -1;
                } else {
                    _process_true_color++;
                }
            }
            // -----------------------------------------------------------------
            function should__process_8bit() {
                return _process_8bit && ((_ex_background && !output_background) ||
                                        (_ex_color && !output_color));
            }
            // -----------------------------------------------------------------
            function process_8bit() {
                if (_ex_color && palette[num] && !output_color) {
                    output_color = palette[num];
                }
                if (_ex_background && palette[num] && !output_background) {
                    output_background = palette[num];
                }
                _process_8bit = false;
            }
            // -----------------------------------------------------------------
            for (var i in controls) {
                if (controls.hasOwnProperty(i)) {
                    num = parseInt(controls[i], 10);
                    if (_process_true_color > -1) {
                        process_true_color();
                    } else if (should__process_8bit()) {
                        process_8bit();
                    } else {
                        set_styles(num);
                    }
                }
            }
            if (reverse) {
                if (output_color || output_background) {
                    var tmp = output_background;
                    output_background = output_color;
                    output_color = tmp;
                } else {
                    output_color = 'black';
                    output_background = 'white';
                }
            }
            var colors, color, background, backgrounds;
            if (bold) {
                colors = backgrounds = $.terminal.ansi_colors.bold;
            } else if (faited) {
                colors = backgrounds = $.terminal.ansi_colors.faited;
            } else {
                colors = backgrounds = $.terminal.ansi_colors.normal;
            }
            if (_ex_color) {
                color = output_color;
            } else if (output_color === 'inherit') {
                color = output_color;
            } else {
                color = colors[output_color];
            }
            if (_ex_background) {
                background = output_background;
            } else if (output_background === 'transparent') {
                background = output_background;
            } else {
                background = backgrounds[output_background];
            }
            return [styles.join(''), color, background];
        }
        return function from_ansi(input, options) {
            options = options || {};
            input = $.terminal.unescape_brackets(input);
            var settings = $.extend({
                unixFormattingEscapeBrackets: false,
                position: 0
            }, options);
            var new_position = settings.position;
            var position = new_position;
            var result;
            //merge multiple codes
            /*input = input.replace(/((?:\x1B\[[0-9;]*[A-Za-z])*)/g, function(group) {
              return group.replace(/m\x1B\[/g, ';');
              });*/
            var splitted = input.split(/(\x1B\[[0-9;]*[A-Za-z])/g);
            if (splitted.length === 1) {
                if (settings.unixFormattingEscapeBrackets) {
                    result = $.terminal.escape_formatting(input);
                } else {
                    result = input;
                }
                if (typeof options.position === 'number') {
                    return [result, new_position];
                }
                return result;
            }
            var output = [];
            //skip closing at the begining
            if (splitted.length > 3) {
                var str = splitted.slice(0, 3).join('');
                if (str.match(/^\[0*m$/)) {
                    splitted = splitted.slice(3);
                }
            }
            var prev_color, prev_background, code, match;
            var inside = false;
            for (var i = 0; i < splitted.length; ++i) {
                match = splitted[i].match(/^\x1B\[([0-9;]*)([A-Za-z])$/);
                if (match) {
                    var start = match.index;
                    // logic taken from $.terminal.tracking_replace
                    if (start < position) {
                        var last_index = start + $.terminal.length(match[0]);
                        if (last_index < position) {
                            // It's after the replacement, move it
                            new_position = Math.max(
                                0,
                                new_position +
                                    $.terminal.length(match[0])
                            );
                        } else {
                            // It's *in* the replacement, put it just after
                            new_position += position - start;
                        }
                    }
                    switch (match[2]) {
                        case 'm':
                            if (+match[1] !== 0) {
                                code = format_ansi(match[1]);
                            }
                            if (inside) {
                                output.push(']');
                                if (+match[1] === 0) {
                                    //just closing
                                    inside = false;
                                    prev_color = prev_background = '';
                                } else {
                                    // someone forget to close - move to next
                                    code[1] = code[1] || prev_color;
                                    code[2] = code[2] || prev_background;
                                    output.push('[[' + code.join(';') + ']');
                                    // store colors to next use
                                    if (code[1]) {
                                        prev_color = code[1];
                                    }
                                    if (code[2]) {
                                        prev_background = code[2];
                                    }
                                }
                            } else if (+match[1] !== 0) {
                                inside = true;
                                code[1] = code[1] || prev_color;
                                code[2] = code[2] || prev_background;
                                output.push('[[' + code.join(';') + ']');
                                // store colors to next use
                                if (code[1]) {
                                    prev_color = code[1];
                                }
                                if (code[2]) {
                                    prev_background = code[2];
                                }
                            }
                            break;
                    }
                } else if (settings.unixFormattingEscapeBrackets) {
                    output.push($.terminal.escape_formatting(splitted[i]));
                } else {
                    output.push(splitted[i]);
                }
            }
            if (inside) {
                output.push(']');
            }
            result = output.join('');
            if (options && typeof options.position === 'number') {
                return [result, new_position];
            }
            return result;
        };
    })();

    $.terminal.defaults.formatters.unshift($.terminal.from_ansi);
    $.terminal.defaults.formatters.unshift($.terminal.overtyping);
});
