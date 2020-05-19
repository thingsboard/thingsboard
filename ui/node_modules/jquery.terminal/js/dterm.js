/**@license
 *       __ _____                     ________                              __
 *      / // _  /__ __ _____ ___ __ _/__  ___/__ ___ ______ __ __  __ ___  / /
 *  __ / // // // // // _  // _// // / / // _  // _//     // //  \/ // _ \/ /
 * /  / // // // // // ___// / / // / / // ___// / / / / // // /\  // // / /__
 * \___//____ \\___//____//_/ _\_  / /_//____//_/ /_/ /_//_//_/ /_/ \__\_\___/
 *           \/              /____/
 * Example plugin using JQuery Terminal Emulator
 * Copyright (c) 2014-2018 Jakub Jankiewicz <http://jcubic.pl/me>
 * Released under the MIT license
 *
 */
/* global jQuery, setTimeout, IntersectionObserver, define, global, require, module */
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
    $.extend_if_has = function(desc, source, array) {
        for (var i = array.length; i--;) {
            if (typeof source[array[i]] !== 'undefined') {
                desc[array[i]] = source[array[i]];
            }
        }
        return desc;
    };
    var defaults = Object.keys($.terminal.defaults).concat(['greetings']);
    $.fn.dterm = function(interpreter, options) {
        var op = $.extend_if_has({}, options, defaults);
        op.enabled = false;
        this.addClass('dterm');
        var terminal = $('<div/>').appendTo(this).terminal(interpreter, op);
        if (!options.title) {
            options.title = 'JQuery Terminal Emulator';
        }
        var close = options.close || $.noop;
        if (options.logoutOnClose) {
            options.close = function() {
                terminal.logout();
                terminal.clear();
                close();
            };
        } else {
            options.close = function() {
                terminal.disable();
                close();
            };
        }
        var self = this;
        if (window.IntersectionObserver) {
            var visibility_observer = new IntersectionObserver(function() {
                if (self.is(':visible')) {
                    terminal.focus().resize();
                } else {
                    terminal.disable();
                }
            }, {
                root: document.body
            });
            visibility_observer.observe(terminal[0]);
        }
        this.dialog($.extend({}, options, {
            open: function(event, ui) {
                if (!window.IntersectionObserver) {
                    setTimeout(function() {
                        terminal.enable().resize();
                    }, 100);
                }
                if (typeof options.open === 'function') {
                    options.open(event, ui);
                }
            },
            show: 'fade',
            closeOnEscape: false
        }));
        self.terminal = terminal;
        return self;
    };
});
