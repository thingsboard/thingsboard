/* Javascript plotting library for jQuery, version 0.9.0-alpha.

Copyright (c) 2007-2014 IOLA and Ole Laursen.
Licensed under the MIT license.

*/

(function($) {

    // A jquery-esque isNumeric method since we currently support 1.4.4
    // and $.isNumeric was introduced on in 1.7
    var isNumeric = $.isNumeric || function(obj) {
        return obj - parseFloat( obj ) >= 0;
    };

    /**
     * The Canvas object is a wrapper around an HTML5 <canvas> tag.
     *
     * @constructor
     * @param {string} cls List of classes to apply to the canvas.
     * @param {element} container Element onto which to append the canvas.
     *
     * Requiring a container is a little iffy, but unfortunately canvas
     * operations don't work unless the canvas is attached to the DOM.
     */
    function Canvas(cls, container) {

        var element = container.children("." + cls)[0];

        if (element == null) {

            element = document.createElement("canvas");
            element.className = cls;

            $(element).css({ direction: "ltr", position: "absolute", left: 0, top: 0 })
                .appendTo(container);

            // If HTML5 Canvas isn't available, fall back to [Ex|Flash]canvas
            if (!element.getContext) {
                if (window.G_vmlCanvasManager) {
                    element = window.G_vmlCanvasManager.initElement(element);
                } else {
                    throw new Error("Canvas is not available. If you're using IE with a fall-back such as Excanvas, then there's either a mistake in your conditional include, or the page has no DOCTYPE and is rendering in Quirks Mode.");
                }
            }
        }

        this.element = element;

        var context = this.context = element.getContext("2d"),

            // Determine the screen's ratio of physical to device-independent
            // pixels.  This is the ratio between the canvas width that the browser
            // advertises and the number of pixels actually present in that space.

            // The iPhone 4, for example, has a device-independent width of 320px,
            // but its screen is actually 640px wide.  It therefore has a pixel
            // ratio of 2, while most normal devices have a ratio of 1.
            devicePixelRatio = window.devicePixelRatio || 1,
            backingStoreRatio =
                context.webkitBackingStorePixelRatio ||
                context.mozBackingStorePixelRatio ||
                context.msBackingStorePixelRatio ||
                context.oBackingStorePixelRatio ||
                context.backingStorePixelRatio || 1;

        this.pixelRatio = devicePixelRatio / backingStoreRatio;

        // Size the canvas to match the internal dimensions of its container
        this.resize(container.width(), container.height());

        // Collection of HTML div layers for text overlaid onto the canvas
        this.textContainer = null;
        this.text = {};

        // Cache of text fragments and metrics, so we can avoid expensively
        // re-calculating them when the plot is re-rendered in a loop.
        this._textCache = {};
    }

    /**
     * Resizes the canvas to the given dimensions.
     *
     * @param {number} width New width of the canvas, in pixels.
     * @param {number} height New height of the canvas, in pixels.
     */
    Canvas.prototype.resize = function(width, height) {

        if (width <= 0 || height <= 0) {
            throw new Error("Invalid dimensions for plot, width = " + width + ", height = " + height);
        }

        var element = this.element,
            context = this.context,
            pixelRatio = this.pixelRatio;

        // Resize the canvas, increasing its density based on the display's
        // pixel ratio; basically giving it more pixels without increasing the
        // size of its element, to take advantage of the fact that retina
        // displays have that many more pixels in the same advertised space.

        // Resizing should reset the state (excanvas seems to be buggy though)
        if (this.width !== width) {
            element.width = width * pixelRatio;
            element.style.width = width + "px";
            this.width = width;
        }

        if (this.height !== height) {
            element.height = height * pixelRatio;
            element.style.height = height + "px";
            this.height = height;
        }

        // Save the context, so we can reset in case we get replotted.  The
        // restore ensure that we're really back at the initial state, and
        // should be safe even if we haven't saved the initial state yet.
        context.restore();
        context.save();

        // Scale the coordinate space to match the display density; so even though we
        // may have twice as many pixels, we still want lines and other drawing to
        // appear at the same size; the extra pixels will just make them crisper.

        context.scale(pixelRatio, pixelRatio);
    };

    /**
     * Clears the entire canvas area, not including any overlaid HTML text
     */
    Canvas.prototype.clear = function() {
        this.context.clearRect(0, 0, this.width, this.height);
    };

    /**
     * Finishes rendering the canvas, including managing the text overlay.
     */
    Canvas.prototype.render = function() {

        var cache = this._textCache;

        // For each text layer, add elements marked as active that haven't
        // already been rendered, and remove those that are no longer active.
        for (var layerKey in cache) {
            if (Object.prototype.hasOwnProperty.call(cache, layerKey)) {

                var layer = this.getTextLayer(layerKey),
                    layerCache = cache[layerKey];

                layer.hide();

                for (var styleKey in layerCache) {
                    if (Object.prototype.hasOwnProperty.call(layerCache, styleKey)) {
                        var styleCache = layerCache[styleKey];
                        for (var angleKey in styleCache) {
                            if (Object.prototype.hasOwnProperty.call(styleCache, angleKey)) {
                                var angleCache = styleCache[angleKey];
                                for (var key in angleCache) {
                                    if (Object.prototype.hasOwnProperty.call(angleCache, key)) {

                                        var positions = angleCache[key].positions;

                                        for (var i = 0, position; position = positions[i]; i++) {
                                            if (position.active) {
                                                if (!position.rendered) {
                                                    layer.append(position.element);
                                                    position.rendered = true;
                                                }
                                            } else {
                                                positions.splice(i--, 1);
                                                if (position.rendered) {
                                                    position.element.detach();
                                                }
                                            }
                                        }

                                        if (positions.length === 0) {
                                            delete angleCache[key];
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                layer.show();
            }
        }
    };

    /**
     * Creates (if necessary) and returns the text overlay container.
     *
     * @param {string} classes String of space-separated CSS classes used to
     *     uniquely identify the text layer.
     * @return {object} The jQuery-wrapped text-layer div.
     */
    Canvas.prototype.getTextLayer = function(classes) {

        var layer = this.text[classes];

        // Create the text layer if it doesn't exist
        if (layer == null) {

            // Create the text layer container, if it doesn't exist
            if (this.textContainer == null) {
                this.textContainer = $("<div class='flot-text'></div>")
                    .css({
                        position: "absolute",
                        top: 0,
                        left: 0,
                        bottom: 0,
                        right: 0,
                        "font-size": "smaller",
                        color: "#545454"
                    })
                    .insertAfter(this.element);
            }

            layer = this.text[classes] = $("<div></div>")
                .addClass(classes)
                .css({
                    position: "absolute",
                    top: 0,
                    left: 0,
                    bottom: 0,
                    right: 0
                })
                .appendTo(this.textContainer);
        }

        return layer;
    };

    /**
     * Creates (if necessary) and returns a text info object.
     *
     * The object looks like this:
     *
     * {
     *     width: Width of the text's wrapper div.
     *     height: Height of the text's wrapper div.
     *     element: The jQuery-wrapped HTML div containing the text.
     *     positions: Array of positions at which this text is drawn.
     * }
     *
     * The positions array contains objects that look like this:
     *
     * {
     *     active: Flag indicating whether the text should be visible.
     *     rendered: Flag indicating whether the text is currently visible.
     *     element: The jQuery-wrapped HTML div containing the text.
     *     x: X coordinate at which to draw the text.
     *     y: Y coordinate at which to draw the text.
     * }
     *
     * Each position after the first receives a clone of the original element.
     *
     * The idea is that that the width, height, and general 'identity' of the
     * text is constant no matter where it is placed; the placements are a
     * secondary property.
     *
     * Canvas maintains a cache of recently-used text info objects; getTextInfo
     * either returns the cached element or creates a new entry.
     *
     * @param {string} layer A string of space-separated CSS classes uniquely
     *     identifying the layer containing this text.
     * @param {string} text Text string to retrieve info for.
     * @param {(string|object)=} font Either a string of space-separated CSS
     *     classes or a font-spec object, defining the text's font and style.
     * @param {number=} angle Angle at which to rotate the text, in degrees.
     * @param {number=} width Maximum width of the text before it wraps.
     * @return {object} a text info object.
     */
    Canvas.prototype.getTextInfo = function(layer, text, font, angle, width) {

        var textStyle, layerCache, styleCache, angleCache, info;

        // Cast to string in case we have a number or such
        text = "" + text;

        // Normalize the angle to 0...359
        angle = (360 + (angle || 0)) % 360;

        // If the font is a font-spec object, generate a CSS font definition
        if (typeof font === "object") {
            textStyle = font.style + " " + font.variant + " " + font.weight + " " + font.size + "px/" + font.lineHeight + "px " + font.family;
        } else {
            textStyle = font;
        }

        // Retrieve or create the caches for the text's layer, style, and angle
        layerCache = this._textCache[layer];
        if (layerCache == null) {
            layerCache = this._textCache[layer] = {};
        }

        styleCache = layerCache[textStyle];
        if (styleCache == null) {
            styleCache = layerCache[textStyle] = {};
        }

        angleCache = styleCache[angle];
        if (angleCache == null) {
            angleCache = styleCache[angle] = {};
        }

        info = angleCache[text];

        // If we can't find a matching element in our cache, create a new one
        if (info == null) {

            var element = $("<div></div>").html(text)
                .css({
                    position: "absolute",
                    "max-width": width,
                    top: -9999
                })
                .appendTo(this.getTextLayer(layer));

            if (typeof font === "object") {
                element.css({
                    font: textStyle,
                    color: font.color
                });
            } else if (typeof font === "string") {
                element.addClass(font);
            }

            // Save the original dimensions of the text; we'll modify these
            // later to take into account rotation, if there is any.
            var textWidth = element.outerWidth(true),
                textHeight = element.outerHeight(true);

            // Apply rotation to the text using CSS3/IE matrix transforms

            // Note how we also set the element's width, as a work-around for
            // the way most browsers resize the div on rotate, which may cause
            // the contents to wrap differently. The extra +1 is because IE
            // rounds the width differently and needs a little extra help.
            if (angle) {

                var radians = angle * Math.PI / 180,
                    sin = Math.sin(radians),
                    cos = Math.cos(radians),

                    // Use fixed-point so these don't show up in scientific
                    // notation when we add them to the string
                    a = cos.toFixed(6),
                    b = (-sin).toFixed(6),
                    c = sin.toFixed(6),

                    transformRule;

                // Detect IE7/8 to use microsofts proprietary matrix filter
                // $.support.leadingWhitespace is false on IE7/8, true on other browsers and
                // undefined with jQuery greater 2.0, which dropped support for IE7/8.
                if ($.support.leadingWhitespace !== false) {

                    // The transform origin defaults to '50% 50%', producing
                    // blurry text on some browsers (Chrome) when the width or
                    // height happens to be odd, making 50% fractional. Avoid
                    // this by setting the origin to rounded values.
                    var cx = textWidth / 2,
                        cy = textHeight / 2,
                        transformOrigin = Math.floor(cx) + "px " + Math.floor(cy) + "px",

                        // Transforms alter the div's appearance without changing
                        // its origin. This will make it difficult to position it
                        // later, since we'll be positioning the new bounding box
                        // with respect to the old origin. We can work around this
                        // by adding a translation to align the new bounding box's
                        // top-left corner with the origin, using the same matrix.

                        // Rather than examining all four points, we can use the
                        // angle to figure out in advance which two points are in
                        // the top-left quadrant; we can then use the x-coordinate
                        // of the first (left-most) point and the y-coordinate of
                        // the second (top-most) point as the bounding box corner.
                        x, y;

                    if (angle < 90) {
                        x = Math.floor(cx * cos + cy * sin - cx);
                        y = Math.floor(cx * sin + cy * cos - cy);
                    } else if (angle < 180) {
                        x = Math.floor(cy * sin - cx * cos - cx);
                        y = Math.floor(cx * sin - cy * cos - cy);
                    } else if (angle < 270) {
                        x = Math.floor(-cx * cos - cy * sin - cx);
                        y = Math.floor(-cx * sin - cy * cos - cy);
                    } else {
                        x = Math.floor(cx * cos - cy * sin - cx);
                        y = Math.floor(cy * cos - cx * sin - cy);
                    }

                    transformRule = "matrix(" + a + "," + c + "," + b + "," + a + "," + x + "," + y + ")";

                    element.css({
                        width: textWidth + 1,
                        transform: transformRule,
                        "-o-transform": transformRule,
                        "-ms-transform": transformRule,
                        "-moz-transform": transformRule,
                        "-webkit-transform": transformRule,
                        "transform-origin": transformOrigin,
                        "-o-transform-origin": transformOrigin,
                        "-ms-transform-origin": transformOrigin,
                        "-moz-transform-origin": transformOrigin,
                        "-webkit-transform-origin": transformOrigin
                    });

                } else {

                    // The IE7/8 matrix filter produces very ugly aliasing for
                    // text with a transparent background. Using a solid color
                    // greatly improves text clarity, although it does result
                    // in ugly boxes for plots using a non-white background.

                    // TODO: Instead of white use the actual background color?
                    // This still wouldn't solve the problem when the plot has
                    // a gradient background, but it would at least help.
                    transformRule = "progid:DXImageTransform.Microsoft.Matrix(M11=" + a + ", M12=" + b + ", M21=" + c + ", M22=" + a + ",sizingMethod='auto expand')";

                    element.css({
                        width: textWidth + 1,
                        filter: transformRule,
                        "-ms-filter": transformRule,
                        "background-color": "#fff"
                    });
                }

                // Compute the final dimensions of the text's bounding box
                var ac = Math.abs(cos),
                    as = Math.abs(sin),
                    originalWidth = textWidth;
                textWidth = Math.round(ac * textWidth + as * textHeight);
                textHeight = Math.round(as * originalWidth + ac * textHeight);
            }

            info = angleCache[text] = {
                width: textWidth,
                height: textHeight,
                element: element,
                positions: []
            };

            element.detach();
        }

        return info;
    };

    /**
     * Adds a text string to the canvas text overlay.
     *
     * The text isn't drawn immediately; it is marked as rendering, which will
     * result in its addition to the canvas on the next render pass.
     *
     * @param {string} layer A string of space-separated CSS classes uniquely
     *     identifying the layer containing this text.
     * @param {number} x X coordinate at which to draw the text.
     * @param {number} y Y coordinate at which to draw the text.
     * @param {string} text Text string to draw.
     * @param {(string|object)=} font Either a string of space-separated CSS
     *     classes or a font-spec object, defining the text's font and style.
     * @param {number=} angle Angle at which to rotate the text, in degrees.
     * @param {number=} width Maximum width of the text before it wraps.
     * @param {string=} halign Horizontal alignment of the text; either "left",
     *     "center" or "right".
     * @param {string=} valign Vertical alignment of the text; either "top",
     *     "middle" or "bottom".
     */
    Canvas.prototype.addText = function(layer, x, y, text, font, angle, width, halign, valign) {

        var info = this.getTextInfo(layer, text, font, angle, width),
            positions = info.positions;

        // Tweak the div's position to match the text's alignment
        if (halign === "center") {
            x -= info.width / 2;
        } else if (halign === "right") {
            x -= info.width;
        }

        if (valign === "middle") {
            y -= info.height / 2;
        } else if (valign === "bottom") {
            y -= info.height;
        }

        // Determine whether this text already exists at this position.
        // If so, mark it for inclusion in the next render pass.
        for (var i = 0, position; position = positions[i]; i++) {
            if (position.x === x && position.y === y) {
                position.active = true;
                return;
            }
        }

        // If the text doesn't exist at this position, create a new entry

        // For the very first position we'll re-use the original element,
        // while for subsequent ones we'll clone it.
        position = {
            active: true,
            rendered: false,
            element: positions.length ? info.element.clone() : info.element,
            x: x,
            y: y
        };

        positions.push(position);

        // Move the element to its final position within the container
        position.element.css({
            top: Math.round(y),
            left: Math.round(x),

            // In case the text wraps

            "text-align": halign
        });
    };

    /**
     * Removes one or more text strings from the canvas text overlay.
     *
     * If no parameters are given, all text within the layer is removed.
     *
     * Note that the text is not immediately removed; it is simply marked as
     * inactive, which will result in its removal on the next render pass.
     * This avoids the performance penalty for 'clear and redraw' behavior,
     * where we potentially get rid of all text on a layer, but will likely
     * add back most or all of it later, as when redrawing axes, for example.
     *
     * @param {string} layer A string of space-separated CSS classes uniquely
     *     identifying the layer containing this text.
     * @param {number=} x X coordinate of the text.
     * @param {number=} y Y coordinate of the text.
     * @param {string=} text Text string to remove.
     * @param {(string|object)=} font Either a string of space-separated CSS
     *     classes or a font-spec object, defining the text's font and style.
     * @param {number=} angle Angle at which the text is rotated, in degrees.
     *     Angle is currently unused, it will be implemented in the future.
     */
    Canvas.prototype.removeText = function(layer, x, y, text, font, angle) {
        var i, positions, position;
        if (text == null) {
            var layerCache = this._textCache[layer];
            if (layerCache != null) {
                for (var styleKey in layerCache) {
                    if (Object.prototype.hasOwnProperty.call(layerCache, styleKey)) {
                        var styleCache = layerCache[styleKey];
                        for (var angleKey in styleCache) {
                            if (Object.prototype.hasOwnProperty.call(styleCache, angleKey)) {
                                var angleCache = styleCache[angleKey];
                                for (var key in angleCache) {
                                    if (Object.prototype.hasOwnProperty.call(angleCache, key)) {
                                        positions = angleCache[key].positions;
                                        for (i = 0; position = positions[i]; i++) {
                                            position.active = false;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            positions = this.getTextInfo(layer, text, font, angle).positions;
            for (i = 0; position = positions[i]; i++) {
                if (position.x === x && position.y === y) {
                    position.active = false;
                }
            }
        }
    };

    /**
     * The top-level container for the entire plot.
     */
    function Plot(placeholder, data_, options_, plugins) {

        // data is on the form:
        //   [ series1, series2 ... ]
        // where series is either just the data as [ [x1, y1], [x2, y2], ... ]
        // or { data: [ [x1, y1], [x2, y2], ... ], label: "some label", ... }
        var series = [],
            options = {

                // the color theme used for graphs
                colors: ["#edc240", "#afd8f8", "#cb4b4b", "#4da74d", "#9440ed"],
                legend: {
                    show: true,

                    // number of colums in legend table
                    noColumns: 1,

                    // fn: string -> string
                    labelFormatter: null,

                    // border color for the little label boxes
                    labelBoxBorderColor: "#ccc",

                    // container (as jQuery object) to put legend in, null means default on top of graph
                    container: null,

                    // position of default legend container within plot
                    position: "ne",

                    // distance from grid edge to default legend container within plot
                    margin: 5,

                    // null means auto-detect
                    backgroundColor: null,

                    // set to 0 to avoid background
                    backgroundOpacity: 0.85,

                    // default to no legend sorting
                    sorted: null
                },
                xaxis: {

                    // null = auto-detect, true = always, false = never

                    show: null,

                    // or "top"
                    position: "bottom",

                    // null or "time"
                    mode: null,

                    // base color, labels, ticks
                    color: null,

                    // null (derived from CSS in placeholder) or
                    // object like { size: 11, lineHeight: 13, style: "italic", weight: "bold", family: "sans-serif", variant: "small-caps" }
                    font: null,

                    // min. value to show, null means set automatically
                    min: null,

                    // max. value to show, null means set automatically
                    max: null,

                    // margin in % to add if auto-setting min/max
                    autoscaleMargin: null,

                    // null or f: number -> number to transform axis
                    transform: null,

                    // if transform is set, this should be the inverse function
                    inverseTransform: null,

                    // either [1, 3] or [[1, "a"], 3] or (fn: axis info -> ticks) or app. number of ticks for auto-ticks
                    ticks: null,

                    // number or [number, "unit"]
                    tickSize: null,

                    // number or [number, "unit"]
                    minTickSize: null,

                    // fn: number -> string
                    tickFormatter: null,

                    // no. of decimals, null means auto
                    tickDecimals: null,

                    // possibly different color of ticks, e.g. "rgba(0,0,0,0.15)"
                    tickColor: null,

                    // size in pixels of ticks, or "full" for whole line
                    tickLength: null,

                    // width of tick labels in pixels
                    tickWidth: null,

                    // height of tick labels in pixels
                    tickHeight: null,

                    // null or font-spec object (see font, above)
                    tickFont: null,

                    // null or an axis label string
                    label: null,

                    // null or font-spec object (see font, above)
                    labelFont: null,

                    // spacing between the axis and its label
                    labelPadding: 2,

                    // whether to reserve space even if axis isn't shown
                    reserveSpace: null,

                    // axis number or null for no sync
                    alignTicksWithAxis: null
                },
                yaxis: {

                    // also accepts "right"
                    position: "left",
                    autoscaleMargin: 0.02,
                    labelPadding: 2
                },
                xaxes: [],
                yaxes: [],
                series: {
                    points: {
                        show: false,
                        radius: 3,

                        // value in pixels
                        lineWidth: 2,
                        fill: true,
                        fillColor: "#ffffff",
                        strokeColor: null,

                        // type or callback
                        symbol: "circle"
                    },


                    lines: {

                        // we don't put in show: false so we can see
                        // whether lines were actively disabled

                        // value in pixels
                        lineWidth: 2,
                        fill: false,
                        fillColor: null,
                        steps: false

                        // Omit 'zero', so we can later default its value to
                        // match that of the 'fill' option.
                    },
                    bars: {
                        show: false,

                        // in pixels
                        lineWidth: 2,

                        // in units of the x axis
                        barWidth: 1,

                        fill: true,
                        fillColor: null,

                        // "left", "right", or "center"
                        align: "left",
                        horizontal: false,
                        zero: true
                    },
                    shadowSize: 3,
                    highlightColor: null
                },
                grid: {
                    show: true,
                    aboveData: false,

                    // primary color used for outline and labels
                    color: "#545454",

                    // null for transparent, else color
                    backgroundColor: null,

                    // set if different from the grid color
                    borderColor: null,

                    // color for the ticks, e.g. "rgba(0,0,0,0.15)"
                    tickColor: null,

                    // distance from the canvas edge to the grid
                    margin: 0,

                    // value in pixels
                    labelMargin: 5,

                    // value in pixels
                    axisMargin: 8,

                    // value in pixels
                    borderWidth: 2,

                    // value in pixels, null means taken from points radius
                    minBorderMargin: null,

                    // array of ranges or fn: axes -> array of ranges
                    markings: null,
                    markingsColor: "#f4f4f4",
                    markingsLineWidth: 2,

                    // interactive stuff
                    clickable: false,
                    hoverable: false,

                    // highlight in case mouse is near
                    autoHighlight: true,

                    // how far the mouse can be away to activate an item
                    mouseActiveRadius: 10
                },
                interaction: {

                    // time between updates, -1 means in same flow
                    redrawOverlayInterval: 1000 / 60
                },
                hooks: {}
            },

            // interactive features
            highlights = [],
            redrawTimeout = null,

            // the canvas for the plot itself
            surface = null,

            // canvas for interactive stuff on top of plot
            overlay = null,

            // jQuery object that events should be bound to
            eventHolder = null,
            ctx = null,
            octx = null,
            xaxes = [],
            yaxes = [],
            plotOffset = {
                left: 0,
                right: 0,
                top: 0,
                bottom: 0
            },
            plotWidth = 0,
            plotHeight = 0,
            hooks = {
                processOptions: [],
                processRawData: [],
                processDatapoints: [],
                processOffset: [],
                drawBackground: [],
                drawSeries: [],
                draw: [],
                bindEvents: [],
                drawOverlay: [],
                shutdown: []
            },
            plot = this;

        // public functions
        plot.setData = setData;
        plot.setupGrid = setupGrid;
        plot.draw = draw;
        plot.getPlaceholder = function() { return placeholder; };
        plot.getCanvas = function() { return surface.element; };
        plot.getPlotOffset = function() { return plotOffset; };
        plot.width = function() { return plotWidth; };
        plot.height = function() { return plotHeight; };
        plot.offset = function() {
            var o = eventHolder.offset();
            o.left += plotOffset.left;
            o.top += plotOffset.top;
            return o;
        };
        plot.getData = function() { return series; };
        plot.getAxes = function() {
            var res = {};
            $.each(xaxes.concat(yaxes), function(_, axis) {
                if (axis) {
                    res[axis.direction + (axis.n !== 1 ? axis.n : "") + "axis"] = axis;
                }
            });
            return res;
        };
        plot.getXAxes = function() { return xaxes; };
        plot.getYAxes = function() { return yaxes; };
        plot.c2p = canvasToAxisCoords;
        plot.p2c = axisToCanvasCoords;
        plot.getOptions = function() { return options; };
        plot.highlight = highlight;
        plot.unhighlight = unhighlight;
        plot.triggerRedrawOverlay = triggerRedrawOverlay;
        plot.pointOffset = function(point) {
            return {
                left: parseInt(xaxes[axisNumber(point, "x") - 1].p2c(+point.x) + plotOffset.left, 10),
                top: parseInt(yaxes[axisNumber(point, "y") - 1].p2c(+point.y) + plotOffset.top, 10)
            };
        };
        plot.shutdown = shutdown;
        plot.destroy = function() {
            shutdown();
            placeholder.removeData("plot").empty();

            series = [];
            options = null;
            surface = null;
            overlay = null;
            eventHolder = null;
            ctx = null;
            octx = null;
            xaxes = [];
            yaxes = [];
            hooks = null;
            highlights = [];
            plot = null;
        };
        plot.resize = function(width, height) {
            width = width || placeholder.width();
            height = height || placeholder.height();
            surface.resize(width, height);
            overlay.resize(width, height);
        };

        // public attributes
        plot.hooks = hooks;

        // initialize
        initPlugins(plot);
        parseOptions(options_);
        setupCanvases();
        setData(data_);
        setupGrid();
        draw();
        bindEvents();

        function executeHooks(hook, args) {
            args = [plot].concat(args);
            for (var i = 0; i < hook.length; ++i) {
                hook[i].apply(this, args);
            }
        }

        function initPlugins() {

            // References to key classes, allowing plugins to modify them
            var classes = {
                Canvas: Canvas
            };

            for (var i = 0; i < plugins.length; ++i) {
                var p = plugins[i];
                p.init(plot, classes);
                if (p.options) {
                    $.extend(true, options, p.options);
                }
            }
        }

        function parseOptions(opts) {

            $.extend(true, options, opts);

            // $.extend merges arrays, rather than replacing them.  When less
            // colors are provided than the size of the default palette, we
            // end up with those colors plus the remaining defaults, which is
            // not expected behavior; avoid it by replacing them here.
            if (opts && opts.colors) {
                options.colors = opts.colors;
            }

            if (options.xaxis.color == null) {
                options.xaxis.color = $.color.parse(options.grid.color).scale("a", 0.22).toString();
            }
            if (options.yaxis.color == null) {
                options.yaxis.color = $.color.parse(options.grid.color).scale("a", 0.22).toString();
            }

            if (options.xaxis.tickColor == null) {

                // grid.tickColor for back-compatibility
                options.xaxis.tickColor = options.grid.tickColor || options.xaxis.color;
            }
            if (options.yaxis.tickColor == null) {

                // grid.tickColor for back-compatibility
                options.yaxis.tickColor = options.grid.tickColor || options.yaxis.color;
            }

            if (options.grid.borderColor == null) {
                options.grid.borderColor = options.grid.color;
            }
            if (options.grid.tickColor == null) {
                options.grid.tickColor = $.color.parse(options.grid.color).scale("a", 0.22).toString();
            }

            // Fill in defaults for axis options, including any unspecified
            // font-spec fields, if a font-spec was provided.

            // If no x/y axis options were provided, create one of each anyway,
            // since the rest of the code assumes that they exist.
            var i, axisOptions, axisCount,
                fontSize = placeholder.css("font-size"),
                fontSizeDefault = fontSize ? +fontSize.replace("px", "") : 13,
                fontDefaults = {
                    style: placeholder.css("font-style"),
                    size: Math.round(0.8 * fontSizeDefault),
                    variant: placeholder.css("font-variant"),
                    weight: placeholder.css("font-weight"),
                    family: placeholder.css("font-family")
                },
                markings;

            axisCount = options.xaxes.length || 1;
            for (i = 0; i < axisCount; ++i) {

                axisOptions = options.xaxes[i];
                if (axisOptions && !axisOptions.tickColor) {
                    axisOptions.tickColor = axisOptions.color;
                }

                // Compatibility with markrcote/flot-axislabels
                if (axisOptions) {
                    if (!axisOptions.label && axisOptions.axisLabel) {
                        axisOptions.label = axisOptions.axisLabel;
                    }
                    if (!axisOptions.labelPadding && axisOptions.axisLabelPadding) {
                        axisOptions.labelPadding = axisOptions.axisLabelPadding;
                    }
                }

                axisOptions = $.extend(true, {}, options.xaxis, axisOptions);
                options.xaxes[i] = axisOptions;

                if (axisOptions.font) {
                    axisOptions.font = $.extend({}, fontDefaults, axisOptions.font);
                }
                if (axisOptions.tickFont || axisOptions.font) {
                    axisOptions.tickFont = $.extend({}, axisOptions.font || fontDefaults, axisOptions.tickFont);
                    if (!axisOptions.tickFont.color) {
                        axisOptions.tickFont.color = axisOptions.color;
                    }
                    if (!axisOptions.tickFont.lineHeight) {
                        axisOptions.tickFont.lineHeight = Math.round(axisOptions.tickFont.size * 1.15);
                    }
                }
                if (axisOptions.label && (axisOptions.labelFont || axisOptions.font)) {
                    axisOptions.labelFont = $.extend({}, axisOptions.font || fontDefaults, axisOptions.labelFont);
                    if (!axisOptions.labelFont.color) {
                        axisOptions.labelFont.color = axisOptions.color;
                    }
                    if (!axisOptions.labelFont.lineHeight) {
                        axisOptions.labelFont.lineHeight = Math.round(axisOptions.labelFont.size * 1.15);
                    }
                }
                if (axisOptions.font) {
                    if (!axisOptions.font.color) {
                        axisOptions.font.color = axisOptions.color;
                    }
                    if (!axisOptions.font.lineHeight) {
                        axisOptions.font.lineHeight = Math.round(axisOptions.font.size * 1.15);
                    }
                }
            }

            axisCount = options.yaxes.length || 1;
            for (i = 0; i < axisCount; ++i) {

                axisOptions = options.yaxes[i];
                if (axisOptions && !axisOptions.tickColor) {
                    axisOptions.tickColor = axisOptions.color;
                }

                // Compatibility with markrcote/flot-axislabels
                if (axisOptions) {
                    if (!axisOptions.label && axisOptions.axisLabel) {
                        axisOptions.label = axisOptions.axisLabel;
                    }
                    if (!axisOptions.labelPadding && axisOptions.axisLabelPadding) {
                        axisOptions.labelPadding = axisOptions.axisLabelPadding;
                    }
                }

                axisOptions = $.extend(true, {}, options.yaxis, axisOptions);
                options.yaxes[i] = axisOptions;

                if (axisOptions.font) {
                    axisOptions.font = $.extend({}, fontDefaults, axisOptions.font);
                }
                if (axisOptions.tickFont || axisOptions.font) {
                    axisOptions.tickFont = $.extend({}, axisOptions.font || fontDefaults, axisOptions.tickFont);
                    if (!axisOptions.tickFont.color) {
                        axisOptions.tickFont.color = axisOptions.color;
                    }
                    if (!axisOptions.tickFont.lineHeight) {
                        axisOptions.tickFont.lineHeight = Math.round(axisOptions.tickFont.size * 1.15);
                    }
                }
                if (axisOptions.label && (axisOptions.labelFont || axisOptions.font)) {
                    axisOptions.labelFont = $.extend({}, axisOptions.font || fontDefaults, axisOptions.labelFont);
                    if (!axisOptions.labelFont.color) {
                        axisOptions.labelFont.color = axisOptions.color;
                    }
                    if (!axisOptions.labelFont.lineHeight) {
                        axisOptions.labelFont.lineHeight = Math.round(axisOptions.labelFont.size * 1.15);
                    }
                }
                if (axisOptions.font) {
                    if (!axisOptions.font.color) {
                        axisOptions.font.color = axisOptions.color;
                    }
                    if (!axisOptions.font.lineHeight) {
                        axisOptions.font.lineHeight = Math.round(axisOptions.font.size * 1.15);
                    }
                }
            }

            markings = options.grid.markings;
            if ($.isArray(markings)) {
                for (i = 0; i < markings.length; ++i) {

                    if (markings[i].font) {
                        markings[i].font = $.extend({}, fontDefaults, markings[i].font);
                    }
                }
            }

            // backwards compatibility, to be removed in future
            if (options.xaxis.noTicks && options.xaxis.ticks == null) {
                options.xaxis.ticks = options.xaxis.noTicks;
            }
            if (options.yaxis.noTicks && options.yaxis.ticks == null) {
                options.yaxis.ticks = options.yaxis.noTicks;
            }
            if (options.x2axis) {
                options.xaxes[1] = $.extend(true, {}, options.xaxis, options.x2axis);
                options.xaxes[1].position = "top";
                // Override the inherit to allow the axis to auto-scale
                if (options.x2axis.min == null) {
                    options.xaxes[1].min = null;
                }
                if (options.x2axis.max == null) {
                    options.xaxes[1].max = null;
                }
            }
            if (options.y2axis) {
                options.yaxes[1] = $.extend(true, {}, options.yaxis, options.y2axis);
                options.yaxes[1].position = "right";
                // Override the inherit to allow the axis to auto-scale
                if (options.y2axis.min == null) {
                    options.yaxes[1].min = null;
                }
                if (options.y2axis.max == null) {
                    options.yaxes[1].max = null;
                }
            }
            if (options.grid.coloredAreas) {
                options.grid.markings = options.grid.coloredAreas;
            }
            if (options.grid.coloredAreasColor) {
                options.grid.markingsColor = options.grid.coloredAreasColor;
            }
            if (options.lines) {
                $.extend(true, options.series.lines, options.lines);
            }
            if (options.points) {
                $.extend(true, options.series.points, options.points);
            }
            if (options.bars) {
                $.extend(true, options.series.bars, options.bars);
            }
            if (options.shadowSize != null) {
                options.series.shadowSize = options.shadowSize;
            }
            if (options.highlightColor != null) {
                options.series.highlightColor = options.highlightColor;
            }

            // save options on axes for future reference
            for (i = 0; i < options.xaxes.length; ++i) {
                getOrCreateAxis(xaxes, i + 1).options = options.xaxes[i];
            }
            for (i = 0; i < options.yaxes.length; ++i) {
                getOrCreateAxis(yaxes, i + 1).options = options.yaxes[i];
            }

            // add hooks from options
            for (var n in hooks) {
                if (options.hooks[n] && options.hooks[n].length) {
                    hooks[n] = hooks[n].concat(options.hooks[n]);
                }
            }

            executeHooks(hooks.processOptions, [options]);
        }

        function setData(d) {
            series = parseData(d);
            fillInSeriesOptions();
            processData();
        }

        function parseData(d) {
            var res = [];
            for (var i = 0; i < d.length; ++i) {
                var s = $.extend(true, {}, options.series);

                if (d[i].data != null) {

                    // move the data instead of deep-copy
                    s.data = d[i].data;
                    delete d[i].data;

                    $.extend(true, s, d[i]);

                    d[i].data = s.data;
                } else {
                    s.data = d[i];
                }
                res.push(s);
            }

            return res;
        }

        function axisNumber(obj, coord) {
            var a = obj[coord + "axis"];

            if (typeof a === "object") {

                // if we got a real axis, extract number
                a = a.n;
            }
            if (!isNumeric(a)) {

                // default to first axis
                a = 1;
            }
            return a;
        }

        function allAxes() {

            // return flat array without annoying null entries
            return $.grep(xaxes.concat(yaxes), function(a) { return a; });
        }

        function canvasToAxisCoords(pos) {

            // return an object with x/y corresponding to all used axes
            var res = {}, i, axis;
            for (i = 0; i < xaxes.length; ++i) {
                axis = xaxes[i];
                if (axis && axis.used) {
                    res["x" + axis.n] = axis.c2p(pos.left);
                }
            }

            for (i = 0; i < yaxes.length; ++i) {
                axis = yaxes[i];
                if (axis && axis.used) {
                    res["y" + axis.n] = axis.c2p(pos.top);
                }
            }

            if (res.x1 !== undefined) {
                res.x = res.x1;
            }
            if (res.y1 !== undefined) {
                res.y = res.y1;
            }

            return res;
        }

        function axisToCanvasCoords(pos) {

            // get canvas coords from the first pair of x/y found in pos
            var res = {}, i, axis, key;

            for (i = 0; i < xaxes.length; ++i) {
                axis = xaxes[i];
                if (axis && axis.used) {
                    key = "x" + axis.n;
                    if (pos[key] == null && axis.n === 1) {
                        key = "x";
                    }

                    if (pos[key] != null) {
                        res.left = axis.p2c(pos[key]);
                        break;
                    }
                }
            }

            for (i = 0; i < yaxes.length; ++i) {
                axis = yaxes[i];
                if (axis && axis.used) {
                    key = "y" + axis.n;
                    if (pos[key] == null && axis.n === 1) {
                        key = "y";
                    }

                    if (pos[key] != null) {
                        res.top = axis.p2c(pos[key]);
                        break;
                    }
                }
            }

            return res;
        }

        function getOrCreateAxis(axes, number) {
            if (!axes[number - 1]) {
                axes[number - 1] = {

                    // save the number for future reference
                    n: number,
                    direction: axes === xaxes ? "x" : "y",
                    options: $.extend(true, {}, axes === xaxes ? options.xaxis : options.yaxis)
                };
            }

            return axes[number - 1];
        }

        function fillInSeriesOptions() {

            var neededColors = series.length, maxIndex = -1, i;

            // Subtract the number of series that already have fixed colors or
            // color indexes from the number that we still need to generate.
            for (i = 0; i < series.length; ++i) {
                var sc = series[i].color;
                if (sc != null) {
                    neededColors--;
                    if (isNumeric(sc) && sc > maxIndex) {
                        maxIndex = sc;
                    }
                }
            }

            // If any of the series have fixed color indexes, then we need to
            // generate at least as many colors as the highest index.
            if (neededColors <= maxIndex) {
                neededColors = maxIndex + 1;
            }

            // Generate all the colors, using first the option colors and then
            // variations on those colors once they're exhausted.
            var c, colors = [], colorPool = options.colors,
                colorPoolSize = colorPool.length, variation = 0;

            for (i = 0; i < neededColors; i++) {

                c = $.color.parse(colorPool[i % colorPoolSize] || "#666");

                // Each time we exhaust the colors in the pool we adjust
                // a scaling factor used to produce more variations on
                // those colors. The factor alternates negative/positive
                // to produce lighter/darker colors.

                // Reset the variation after every few cycles, or else
                // it will end up producing only white or black colors.
                if (i % colorPoolSize === 0 && i) {
                    if (variation >= 0) {
                        if (variation < 0.5) {
                            variation = -variation - 0.2;
                        } else {
                            variation = 0;
                        }
                    } else {
                        variation = -variation;
                    }
                }

                colors[i] = c.scale("rgb", 1 + variation);
            }

            // Finalize the series options, filling in their colors
            var colori = 0, s;
            for (i = 0; i < series.length; ++i) {
                s = series[i];

                // assign colors
                if (s.color == null) {
                    s.color = colors[colori].toString();
                    ++colori;
                } else if (isNumeric(s.color)) {
                    s.color = colors[s.color].toString();
                }

                // turn on lines automatically in case nothing is set
                if (s.lines.show == null) {
                    var v, show = true;
                    for (v in s) {
                        if (s[v] && s[v].show) {
                            show = false;
                            break;
                        }
                    }
                    if (show) {
                        s.lines.show = true;
                    }
                }

                // If nothing was provided for lines.zero, default it to match
                // lines.fill, since areas by default should extend to zero.
                if (s.lines.zero == null) {
                    s.lines.zero = !!s.lines.fill;
                }

                // setup axes
                s.xaxis = getOrCreateAxis(xaxes, axisNumber(s, "x"));
                s.yaxis = getOrCreateAxis(yaxes, axisNumber(s, "y"));
            }
        }

        function processData() {
            var topSentry = Number.POSITIVE_INFINITY,
                bottomSentry = Number.NEGATIVE_INFINITY,
                fakeInfinity = Number.MAX_VALUE,
                i, j, k, m, s, points, ps, val, f, p, data, format;

            function updateAxis(axis, min, max) {
                if (min < axis.datamin && min !== -fakeInfinity) {
                    axis.datamin = min;
                }
                if (max > axis.datamax && max !== fakeInfinity) {
                    axis.datamax = max;
                }
            }

            $.each(allAxes(), function(_, axis) {

                // init axis
                axis.datamin = topSentry;
                axis.datamax = bottomSentry;
                axis.used = false;
            });

            for (i = 0; i < series.length; ++i) {
                s = series[i];
                s.datapoints = { points: [] };
                executeHooks(hooks.processRawData, [s, s.data, s.datapoints]);
            }

            // first pass: clean and copy data
            for (i = 0; i < series.length; ++i) {
                s = series[i];

                data = s.data;
                format = s.datapoints.format;

                if (!format) {
                    format = [];

                    // find out how to copy
                    format.push({ x: true, number: true, required: true });
                    format.push({ y: true, number: true, required: true });

                    if (s.bars.show || (s.lines.show && s.lines.fill)) {
                        var autoscale = !!((s.bars.show && s.bars.zero) || (s.lines.show && s.lines.zero));
                        format.push({ y: true, number: true, required: false, defaultValue: 0, autoscale: autoscale });
                        if (s.bars.horizontal) {
                            delete format[format.length - 1].y;
                            format[format.length - 1].x = true;
                        }
                    }

                    s.datapoints.format = format;
                }

                if (s.datapoints.pointsize != null) {

                    // already filled in
                    continue;
                }

                s.datapoints.pointsize = format.length;

                ps = s.datapoints.pointsize;
                points = s.datapoints.points;

                s.xaxis.used = s.yaxis.used = true;

                for (j = k = 0; j < data.length; ++j, k += ps) {
                    p = data[j];

                    var nullify = p == null;
                    if (!nullify) {
                        for (m = 0; m < ps; ++m) {
                            val = p[m];
                            f = format[m];

                            if (f) {
                                if (f.number && val != null) {

                                    // convert to number
                                    val = +val;
                                    if (isNaN(val)) {
                                        val = null;
                                    } else if (val === Infinity) {
                                        val = fakeInfinity;
                                    } else if (val === -Infinity) {
                                        val = -fakeInfinity;
                                    }
                                }

                                if (val == null) {
                                    if (f.required) {
                                        nullify = true;
                                    }

                                    if (f.defaultValue != null) {
                                        val = f.defaultValue;
                                    }
                                }
                            }

                            points[k + m] = val;
                        }
                    }

                    if (nullify) {
                        for (m = 0; m < ps; ++m) {
                            val = points[k + m];
                            if (val != null) {
                                f = format[m];

                                // extract min/max info
                                if (f.autoscale !== false) {
                                    if (f.x) {
                                        updateAxis(s.xaxis, val, val);
                                    }
                                    if (f.y) {
                                        updateAxis(s.yaxis, val, val);
                                    }
                                }
                            }
                            points[k + m] = null;
                        }
                    }
                }
            }

            // give the hooks a chance to run
            for (i = 0; i < series.length; ++i) {
                s = series[i];

                executeHooks(hooks.processDatapoints, [s, s.datapoints]);
            }

            // second pass: find datamax/datamin for auto-scaling
            for (i = 0; i < series.length; ++i) {
                s = series[i];
                points = s.datapoints.points;
                ps = s.datapoints.pointsize;
                format = s.datapoints.format;

                var xmin = topSentry, ymin = topSentry,
                    xmax = bottomSentry, ymax = bottomSentry;

                for (j = 0; j < points.length; j += ps) {
                    if (points[j] == null) {
                        continue;
                    }

                    for (m = 0; m < ps; ++m) {
                        val = points[j + m];
                        f = format[m];
                        if (!f || f.autoscale === false || val === fakeInfinity || val === -fakeInfinity) {
                            continue;
                        }

                        if (f.x) {
                            if (val < xmin) {
                                xmin = val;
                            }
                            if (val > xmax) {
                                xmax = val;
                            }
                        }
                        if (f.y) {
                            if (val < ymin) {
                                ymin = val;
                            }
                            if (val > ymax) {
                                ymax = val;
                            }
                        }
                    }
                }

                if (s.bars.show) {

                    // make sure we got room for the bar on the dancing floor
                    var delta;

                    switch (s.bars.align) {
                    case "left":
                        delta = 0;
                        break;
                    case "right":
                        delta = -s.bars.barWidth;
                        break;
                    default:
                        delta = -s.bars.barWidth / 2;
                    }

                    if (s.bars.horizontal) {
                        ymin += delta;
                        ymax += delta + s.bars.barWidth;
                    } else {
                        xmin += delta;
                        xmax += delta + s.bars.barWidth;
                    }
                }

                updateAxis(s.xaxis, xmin, xmax);
                updateAxis(s.yaxis, ymin, ymax);
            }

            $.each(allAxes(), function(_, axis) {
                if (axis.datamin === topSentry) {
                    axis.datamin = null;
                }
                if (axis.datamax === bottomSentry) {
                    axis.datamax = null;
                }
            });
        }

        function setupCanvases() {

            // Make sure the placeholder is clear of everything except canvases
            // from a previous plot in this container that we'll try to re-use.

            // padding messes up the positioning
            placeholder.css("padding", 0)
                .children().filter(function() {
                    return !$(this).hasClass("flot-overlay") && !$(this).hasClass("flot-base");
                }).remove();

            if (placeholder.css("position") === "static") {

                // for positioning labels and overlay
                placeholder.css("position", "relative");
            }

            surface = new Canvas("flot-base", placeholder);

            // overlay canvas for interactive features
            overlay = new Canvas("flot-overlay", placeholder);

            ctx = surface.context;
            octx = overlay.context;

            // define which element we're listening for events on
            eventHolder = $(overlay.element).unbind();

            // If we're re-using a plot object, shut down the old one
            var existing = placeholder.data("plot");

            if (existing) {
                existing.shutdown();
                overlay.clear();
            }

            // save in case we get replotted
            placeholder.data("plot", plot);
        }

        function bindEvents() {

            // bind events
            if (options.grid.hoverable) {
                eventHolder.mousemove(onMouseMove);

                // Use bind, rather than .mouseleave, because we officially
                // still support jQuery 1.2.6, which doesn't define a shortcut
                // for mouseenter or mouseleave.  This was a bug/oversight that
                // was fixed somewhere around 1.3.x.  We can return to using
                // .mouseleave when we drop support for 1.2.6.
                eventHolder.bind("mouseleave", onMouseLeave);
            }

            if (options.grid.clickable) {
                eventHolder.click(onClick);
            }

            executeHooks(hooks.bindEvents, [eventHolder]);
        }

        function shutdown() {
            if (redrawTimeout) {
                clearTimeout(redrawTimeout);
            }

            eventHolder.unbind("mousemove", onMouseMove);
            eventHolder.unbind("mouseleave", onMouseLeave);
            eventHolder.unbind("click", onClick);

            executeHooks(hooks.shutdown, [eventHolder]);
        }

        function setTransformationHelpers(axis) {

            // set helper functions on the axis, assumes plot area
            // has been computed already
            function identity(x) { return x; }

            var s, m, t = axis.options.transform || identity,
                it = axis.options.inverseTransform;

            // precompute how much the axis is scaling a point
            // in canvas space
            if (axis.direction === "x") {
                s = axis.scale = plotWidth / Math.abs(t(axis.max) - t(axis.min));
                m = Math.min(t(axis.max), t(axis.min));
            } else {
                s = axis.scale = plotHeight / Math.abs(t(axis.max) - t(axis.min));
                s = -s;
                m = Math.max(t(axis.max), t(axis.min));
            }

            // data point to canvas coordinate
            if (t === identity) {

                // slight optimization
                axis.p2c = function(p) { return (p - m) * s; };
            } else {
                axis.p2c = function(p) { return (t(p) - m) * s; };
            }

            // canvas coordinate to data point
            if (!it) {
                axis.c2p = function(c) { return m + c / s; };
            } else {
                axis.c2p = function(c) { return it(m + c / s); };
            }
        }

        function measureTickLabels(axis) {

            var opts = axis.options,
                ticks = axis.ticks || [],

                // Label width & height are deprecated; remove in 1.0!
                tickWidth = opts.tickWidth || opts.labelWidth || 0,
                tickHeight = opts.tickHeight || opts.labelHeight || 0,
                maxWidth = tickWidth || axis.direction === "x" ? Math.floor(surface.width / (ticks.length || 1)) : null,
                legacyStyles = axis.direction + "Axis " + axis.direction + axis.n + "Axis",
                layer = "flot-" + axis.direction + "-axis flot-" + axis.direction + axis.n + "-axis " + legacyStyles,
                font = opts.tickFont || "flot-tick-label tickLabel";

            for (var i = 0; i < ticks.length; ++i) {

                var t = ticks[i];

                if (!t.label) {
                    continue;
                }

                var info = surface.getTextInfo(layer, t.label, font, null, maxWidth);

                tickWidth = Math.max(tickWidth, info.width);
                tickHeight = Math.max(tickHeight, info.height);
            }

            axis.tickWidth = opts.tickWidth || opts.labelWidth || tickWidth;
            axis.tickHeight = opts.tickHeight || opts.labelHeight || tickHeight;

            // Label width/height properties are deprecated; remove in 1.0!
            axis.labelWidth = axis.tickWidth;
            axis.labelHeight = axis.tickHeight;
        }

        /**
         * Compute the axis bounding box based on the dimensions of its label
         * and tick labels, then adjust the plotOffset to make room for it.
         *
         * This first phase only considers one dimension per axis; the other
         * dimension depends on the other axes, and will be calculated later.
         */
        function allocateAxisBoxFirstPhase(axis) {

            var contentWidth = axis.tickWidth,
                contentHeight = axis.tickHeight,
                axisOptions = axis.options,
                tickLength = axisOptions.tickLength,
                axisPosition = axisOptions.position,
                axisMargin = options.grid.axisMargin,
                padding = options.grid.labelMargin,
                isXAxis = axis.direction === "x",
                innermost = true,
                outermost = true,
                first = true,
                found = false;

            // Determine the axis's position in its direction and on its side
            $.each(isXAxis ? xaxes : yaxes, function(i, a) {
                if (a && (a.show || a.reserveSpace)) {
                    if (a === axis) {
                        found = true;
                    } else if (a.options.position === axisPosition) {
                        if (found) {
                            outermost = false;
                        } else {
                            innermost = false;
                        }
                    }
                    if (!found) {
                        first = false;
                    }
                }
            });

            // The outermost axis on each side has no margin
            if (outermost) {
                axisMargin = 0;
            }

            // The ticks for the first axis in each direction stretch across
            if (tickLength == null) {
                tickLength = first ? "full" : 5;
            }

            if (!isNaN(+tickLength)) {
                padding += +tickLength;
            }

            // Measure the dimensions of the axis label, if it has one
            if (axisOptions.label) {
                var legacyStyles = "Axis " + axis.direction + axis.n + "Axis",
                    layer = "flot-" + axis.direction + "-axis flot-" + axis.direction + axis.n + "-axis " + axis.direction + legacyStyles,
                    font = axisOptions.labelFont || "flot-axis-label axisLabels " + axis.direction + axis.n + "axisLabel",
                    angle = isXAxis ? 0 : axisOptions.position === "right" ? 90 : -90,
                    labelInfo = surface.getTextInfo(layer, axisOptions.label, font, angle);
                contentWidth += labelInfo.width + axisOptions.labelPadding;
                contentHeight += labelInfo.height + axisOptions.labelPadding;
            }

            // Compute the axis bounding box and update the plot bounds
            if (isXAxis) {
                contentHeight += padding;
                if (axisPosition === "top") {
                    axis.box = { top: plotOffset.top + axisMargin, height: contentHeight };
                    plotOffset.top += contentHeight + axisMargin;
                } else {
                    plotOffset.bottom += contentHeight + axisMargin;
                    axis.box = { top: surface.height - plotOffset.bottom, height: contentHeight };
                }
            } else {
                contentWidth += padding;
                if (axisPosition === "right") {
                    plotOffset.right += contentWidth + axisMargin;
                    axis.box = { left: surface.width - plotOffset.right, width: contentWidth };
                } else {
                    axis.box = { left: plotOffset.left + axisMargin, width: contentWidth };
                    plotOffset.left += contentWidth + axisMargin;
                }
            }

            axis.position = axisPosition;
            axis.tickLength = tickLength;
            axis.box.padding = padding;
            axis.innermost = innermost;
        }

        function allocateAxisBoxSecondPhase(axis) {

            // now that all axis boxes have been placed in one
            // dimension, we can set the remaining dimension coordinates
            if (axis.direction === "x") {
                axis.box.left = plotOffset.left - axis.tickWidth / 2;
                axis.box.width = surface.width - plotOffset.left - plotOffset.right + axis.tickWidth;
            } else {
                axis.box.top = plotOffset.top - axis.tickHeight / 2;
                axis.box.height = surface.height - plotOffset.bottom - plotOffset.top + axis.tickHeight;
            }
        }

        function adjustLayoutForThingsStickingOut() {

            // possibly adjust plot offset to ensure everything stays
            // inside the canvas and isn't clipped off
            var minMargin = options.grid.minBorderMargin;

            // check stuff from the plot (FIXME: this should just read
            // a value from the series, otherwise it's impossible to
            // customize)
            if (minMargin == null) {
                minMargin = 0;
                for (var i = 0; i < series.length; ++i) {
                    minMargin = Math.max(minMargin, 2 * (series[i].points.radius + series[i].points.lineWidth / 2));
                }
            }

            var margins = {
                left: minMargin,
                right: minMargin,
                top: minMargin,
                bottom: minMargin
            };

            // check axis labels, note we don't check the actual
            // labels but instead use the overall width/height to not
            // jump as much around with replots
            $.each(allAxes(), function(_, axis) {
                if (axis.reserveSpace && axis.ticks && axis.ticks.length) {
                    if (axis.direction === "x") {
                        margins.left = Math.max(margins.left, axis.tickWidth / 2);
                        margins.right = Math.max(margins.right, axis.tickWidth / 2);
                    } else {
                        margins.bottom = Math.max(margins.bottom, axis.tickHeight / 2);
                        margins.top = Math.max(margins.top, axis.tickHeight / 2);
                    }
                }
            });

            plotOffset.left = Math.ceil(Math.max(margins.left, plotOffset.left));
            plotOffset.right = Math.ceil(Math.max(margins.right, plotOffset.right));
            plotOffset.top = Math.ceil(Math.max(margins.top, plotOffset.top));
            plotOffset.bottom = Math.ceil(Math.max(margins.bottom, plotOffset.bottom));
        }

        function setupGrid() {
            var axes = allAxes(),
                showGrid = options.grid.show,
                margin = options.grid.margin || 0,
                i, a;

            // Initialize the plot's offset from the edge of the canvas
            for (a in plotOffset) {
                if (Object.prototype.hasOwnProperty.call(plotOffset, a)) {
                    plotOffset[a] = isNumeric(margin) ? margin : margin[a] || 0;
                }
            }

            executeHooks(hooks.processOffset, [plotOffset]);

            // If the grid is visible, add its border width to the offset
            for (a in plotOffset) {
                if (typeof(options.grid.borderWidth) === "object") {
                    plotOffset[a] += showGrid ? options.grid.borderWidth[a] : 0;
                } else {
                    plotOffset[a] += showGrid ? options.grid.borderWidth : 0;
                }
            }

            $.each(axes, function(_, axis) {
                var axisOpts = axis.options;
                axis.show = axisOpts.show == null ? axis.used : axisOpts.show;
                axis.reserveSpace = axisOpts.reserveSpace == null ? axis.show : axisOpts.reserveSpace;
                setRange(axis);
            });

            if (showGrid) {

                var allocatedAxes = $.grep(axes, function(axis) {
                    return axis.show || axis.reserveSpace;
                });

                $.each(allocatedAxes, function(_, axis) {
                    // make the ticks
                    setupTickGeneration(axis);
                    setTicks(axis);
                    snapRangeToTicks(axis, axis.ticks);
                    measureTickLabels(axis);
                });

                // with all dimensions calculated, we can compute the
                // axis bounding boxes, start from the outside
                // (reverse order)
                for (i = allocatedAxes.length - 1; i >= 0; --i) {
                    allocateAxisBoxFirstPhase(allocatedAxes[i]);
                }

                // make sure we've got enough space for things that
                // might stick out
                adjustLayoutForThingsStickingOut();

                $.each(allocatedAxes, function(_, axis) {
                    allocateAxisBoxSecondPhase(axis);
                });
            }

            plotWidth = surface.width - plotOffset.left - plotOffset.right;
            plotHeight = surface.height - plotOffset.bottom - plotOffset.top;

            // now we got the proper plot dimensions, we can compute the scaling
            $.each(axes, function(_, axis) {
                setTransformationHelpers(axis);
            });

            if (showGrid) {
                drawAxisLabels();
            }

            insertLegend();
        }

        function setRange(axis) {
            var opts = axis.options,
                min = +(opts.min != null ? opts.min : axis.datamin),
                max = +(opts.max != null ? opts.max : axis.datamax),
                delta = max - min;

            if (delta === 0.0) {

                // degenerate case
                var widen = max === 0 ? 1 : 0.01;

                if (opts.min == null) {
                    min -= widen;
                }

                // always widen max if we couldn't widen min to ensure we
                // don't fall into min == max which doesn't work

                if (opts.max == null || opts.min != null) {
                    max += widen;
                }
            } else {

                // consider autoscaling
                var margin = opts.autoscaleMargin;
                if (margin != null) {
                    if (opts.min == null) {
                        min -= delta * margin;

                        // make sure we don't go below zero if all values
                        // are positive
                        if (min < 0 && axis.datamin != null && axis.datamin >= 0) {
                            min = 0;
                        }
                    }
                    if (opts.max == null) {
                        max += delta * margin;
                        if (max > 0 && axis.datamax != null && axis.datamax <= 0) {
                            max = 0;
                        }
                    }
                }
            }
            axis.min = min;
            axis.max = max;
        }

        function setupTickGeneration(axis) {
            var opts = axis.options,

                // estimate number of ticks
                noTicks;

            if (isNumeric(opts.ticks) && opts.ticks > 0) {
                noTicks = opts.ticks;
            } else {

                // heuristic based on the model a*sqrt(x) fitted to
                // some data points that seemed reasonable
                noTicks = 0.3 * Math.sqrt(axis.direction === "x" ? surface.width : surface.height);
            }

            var delta = (axis.max - axis.min) / noTicks,
                dec = -Math.floor(Math.log(delta) / Math.LN10),
                maxDec = opts.tickDecimals;

            if (maxDec != null && dec > maxDec) {
                dec = maxDec;
            }

            var magn = Math.pow(10, -dec),

                // norm is between 1.0 and 10.0
                norm = delta / magn,
                size;

            if (norm < 1.5) {
                size = 1;
            } else if (norm < 3) {
                size = 2;

                // special case for 2.5, requires an extra decimal
                if (norm > 2.25 && (maxDec == null || dec + 1 <= maxDec)) {
                    size = 2.5;
                    ++dec;
                }
            } else if (norm < 7.5) {
                size = 5;
            } else {
                size = 10;
            }

            size *= magn;

            if (opts.minTickSize != null && size < opts.minTickSize) {
                size = opts.minTickSize;
            }

            axis.delta = delta;
            axis.tickDecimals = Math.max(0, maxDec != null ? maxDec : dec);
            axis.tickSize = opts.tickSize || size;

            // Time mode was moved to a plug-in in 0.8, but since so many people use this
            // we'll add an especially friendly make sure they remembered to include it.
            if (opts.mode === "time" && !axis.tickGenerator) {
                throw new Error("Time mode requires the flot.time plugin.");
            }

            // Flot supports base-10 axes; any other mode else is handled by a plug-in,
            // like flot.time.js.
            if (!axis.tickGenerator) {

                axis.tickGenerator = function(axis) {

                    var ticks = [],
                        start = floorInBase(axis.min, axis.tickSize),
                        i = 0,
                        v = Number.NaN,
                        prev;

                    do {
                        prev = v;
                        v = start + i * axis.tickSize;
                        ticks.push(v);
                        ++i;
                    } while (v < axis.max && v !== prev);
                    return ticks;
                };

                axis.tickFormatter = function(value, axis) {

                    var factor = axis.tickDecimals ? Math.pow(10, axis.tickDecimals) : 1,
                        formatted = "" + Math.round(value * factor) / factor;

                    // If tickDecimals was specified, ensure that we have exactly that
                    // much precision; otherwise default to the value's own precision.
                    if (axis.tickDecimals != null) {
                        var decimal = formatted.indexOf("."),
                            precision = decimal === -1 ? 0 : formatted.length - decimal - 1;

                        if (precision < axis.tickDecimals) {
                            return (precision ? formatted : formatted + ".") + ("" + factor).substr(1, axis.tickDecimals - precision);
                        }
                    }

                    return formatted;
                };
            }

            if ($.isFunction(opts.tickFormatter)) {
                axis.tickFormatter = function(v, axis) { return "" + opts.tickFormatter(v, axis); };
            }

            if (opts.alignTicksWithAxis != null) {
                var otherAxis = (axis.direction === "x" ? xaxes : yaxes)[opts.alignTicksWithAxis - 1];
                if (otherAxis && otherAxis.used && otherAxis !== axis) {

                    // consider snapping min/max to outermost nice ticks
                    var niceTicks = axis.tickGenerator(axis);
                    if (niceTicks.length > 0) {
                        if (opts.min == null) {
                            axis.min = Math.min(axis.min, niceTicks[0]);
                        }
                        if (opts.max == null && niceTicks.length > 1) {
                            axis.max = Math.max(axis.max, niceTicks[niceTicks.length - 1]);
                        }
                    }

                    axis.tickGenerator = function(axis) {

                        // copy ticks, scaled to this axis
                        var ticks = [], v, i;
                        for (i = 0; i < otherAxis.ticks.length; ++i) {
                            v = (otherAxis.ticks[i].v - otherAxis.min) / (otherAxis.max - otherAxis.min);
                            v = axis.min + v * (axis.max - axis.min);
                            ticks.push(v);
                        }
                        return ticks;
                    };

                    // we might need an extra decimal since forced
                    // ticks don't necessarily fit naturally
                    if (!axis.mode && opts.tickDecimals == null) {
                        var extraDec = Math.max(0, -Math.floor(Math.log(axis.delta) / Math.LN10) + 1),
                            ts = axis.tickGenerator(axis);

                        // only proceed if the tick interval rounded
                        // with an extra decimal doesn't give us a
                        // zero at end
                        if (!(ts.length > 1 && /\..*0$/.test((ts[1] - ts[0]).toFixed(extraDec)))) {
                            axis.tickDecimals = extraDec;
                        }
                    }
                }
            }
        }

        function setTicks(axis) {
            var oticks = axis.options.ticks, ticks = [];
            if (oticks == null || (isNumeric(oticks) && oticks > 0)) {
                ticks = axis.tickGenerator(axis);
            } else if (oticks) {
                if ($.isFunction(oticks)) {

                    // generate the ticks
                    ticks = oticks(axis);
                } else {
                    ticks = oticks;
                }
            }

            // clean up/labelify the supplied ticks, copy them over
            var i, v;
            axis.ticks = [];
            for (i = 0; i < ticks.length; ++i) {
                var label = null,
                    t = ticks[i];

                if (typeof t === "object") {
                    v = +t[0];
                    if (t.length > 1) {
                        label = t[1];
                    }
                } else {
                    v = +t;
                }
                if (label == null) {
                    label = axis.tickFormatter(v, axis);
                }
                if (!isNaN(v)) {
                    axis.ticks.push({ v: v, label: label });
                }
            }
        }

        function snapRangeToTicks(axis, ticks) {
            if (axis.options.autoscaleMargin && ticks.length > 0) {

                // snap to ticks
                if (axis.options.min == null) {
                    axis.min = Math.min(axis.min, ticks[0].v);
                }
                if (axis.options.max == null && ticks.length > 1) {
                    axis.max = Math.max(axis.max, ticks[ticks.length - 1].v);
                }
            }
        }

        function draw() {

            surface.clear();

            executeHooks(hooks.drawBackground, [ctx]);

            var grid = options.grid;

            // draw background, if any
            if (grid.show && grid.backgroundColor) {
                drawBackground();
            }

            if (grid.show && !grid.aboveData) {
                drawGrid();
            }

            for (var i = 0; i < series.length; ++i) {
                executeHooks(hooks.drawSeries, [ctx, series[i]]);
                drawSeries(series[i]);
            }

            executeHooks(hooks.draw, [ctx]);

            if (grid.show && grid.aboveData) {
                drawGrid();
            }

            surface.render();

            // A draw implies that either the axes or data have changed, so we
            // should probably update the overlay highlights as well.

            triggerRedrawOverlay();
        }

        function extractRange(ranges, coord) {
            var axis, from, to, key, axes = allAxes();

            for (var i = 0; i < axes.length; ++i) {
                axis = axes[i];
                if (axis.direction === coord) {
                    key = coord + axis.n + "axis";
                    if (!ranges[key] && axis.n === 1) {

                        // support x1axis as xaxis
                        key = coord + "axis";
                    }
                    if (ranges[key]) {
                        from = ranges[key].from;
                        to = ranges[key].to;
                        break;
                    }
                }
            }

            // backwards-compat stuff - to be removed in future
            if (!ranges[key]) {
                axis = coord === "x" ? xaxes[0] : yaxes[0];
                from = ranges[coord + "1"];
                to = ranges[coord + "2"];
            }

            // auto-reverse as an added bonus
            if (from != null && to != null && from > to) {
                var tmp = from;
                from = to;
                to = tmp;
            }

            return { from: from, to: to, axis: axis };
        }

        function drawBackground() {
            ctx.save();
            ctx.translate(plotOffset.left, plotOffset.top);

            ctx.fillStyle = getColorOrGradient(options.grid.backgroundColor, plotHeight, 0, "rgba(255, 255, 255, 0)");
            ctx.fillRect(0, 0, plotWidth, plotHeight);
            ctx.restore();
        }

        function drawGrid() {
            var i, axes, bw, bc;

            ctx.save();
            ctx.translate(plotOffset.left, plotOffset.top);

            var markingLayer = "flot-markings";
            surface.removeText(markingLayer);

            // process markings
            var markingsUnderGrid = [],
                markingsAboveGrid = [],
                markings = options.grid.markings;

            if (markings) {
                if ($.isFunction(markings)) {
                    axes = plot.getAxes();

                    // xmin etc. is backwards compatibility, to be
                    // removed in the future
                    axes.xmin = axes.xaxis.min;
                    axes.xmax = axes.xaxis.max;
                    axes.ymin = axes.yaxis.min;
                    axes.ymax = axes.yaxis.max;

                    markings = markings(axes);
                }

                for (i = 0; i < markings.length; ++i) {
                    var m = markings[i];

                    if (m.aboveGrid) {
                        markingsAboveGrid.push(m);
                    } else {
                        markingsUnderGrid.push(m);
                    }
                }
            }

            drawMarkings(markingsUnderGrid, markingLayer);

            // draw the ticks
            axes = allAxes();
            bw = options.grid.borderWidth;

            for (var j = 0; j < axes.length; ++j) {
                var axis = axes[j], box = axis.box,
                    t = axis.tickLength, x, y, xoff, yoff;
                if (!axis.show || axis.ticks.length === 0) {
                    continue;
                }

                ctx.lineWidth = 1;

                // find the edges
                if (axis.direction === "x") {
                    x = 0;
                    if (t === "full") {
                        y = (axis.position === "top" ? 0 : plotHeight);
                    } else {
                        y = box.top - plotOffset.top + (axis.position === "top" ? box.height : 0);
                    }
                } else {
                    y = 0;
                    if (t === "full") {
                        x = (axis.position === "left" ? 0 : plotWidth);
                    } else {
                        x = box.left - plotOffset.left + (axis.position === "left" ? box.width : 0);
                    }
                }

                // draw tick bar
                if (!axis.innermost) {
                    ctx.strokeStyle = axis.options.color;
                    ctx.beginPath();
                    xoff = yoff = 0;
                    if (axis.direction === "x") {
                        xoff = plotWidth + 1;
                    } else {
                        yoff = plotHeight + 1;
                    }

                    if (ctx.lineWidth === 1) {
                        if (axis.direction === "x") {
                            y = Math.floor(y) + 0.5;
                        } else {
                            x = Math.floor(x) + 0.5;
                        }
                    }

                    ctx.moveTo(x, y);
                    ctx.lineTo(x + xoff, y + yoff);
                    ctx.stroke();
                }

                // draw ticks
                ctx.strokeStyle = axis.options.tickColor;

                ctx.beginPath();
                for (i = 0; i < axis.ticks.length; ++i) {
                    var v = axis.ticks[i].v;

                    xoff = yoff = 0;

                    if (isNaN(v) || v < axis.min || v > axis.max || (

                        // skip those lying on the axes if we got a border
                        t === "full" && ((typeof bw === "object" && bw[axis.position] > 0) || bw > 0) &&
                        (v === axis.min || v === axis.max)
                    )) {
                        continue;
                    }

                    if (axis.direction === "x") {
                        x = axis.p2c(v);
                        yoff = t === "full" ? -plotHeight : t;

                        if (axis.position === "top") {
                            yoff = -yoff;
                        }
                    } else {
                        y = axis.p2c(v);
                        xoff = t === "full" ? -plotWidth : t;

                        if (axis.position === "left") {
                            xoff = -xoff;
                        }
                    }

                    if (ctx.lineWidth === 1) {
                        if (axis.direction === "x") {
                            x = Math.floor(x) + 0.5;
                        } else {
                            y = Math.floor(y) + 0.5;
                        }
                    }

                    ctx.moveTo(x, y);
                    ctx.lineTo(x + xoff, y + yoff);
                }

                ctx.stroke();
            }

            drawMarkings(markingsAboveGrid, markingLayer);

            // draw border
            if (bw) {

                // If either borderWidth or borderColor is an object, then draw the border
                // line by line instead of as one rectangle
                bc = options.grid.borderColor;
                if (typeof bw === "object" || typeof bc === "object") {
                    if (typeof bw !== "object") {
                        bw = { top: bw, right: bw, bottom: bw, left: bw };
                    }
                    if (typeof bc !== "object") {
                        bc = { top: bc, right: bc, bottom: bc, left: bc };
                    }

                    if (bw.top > 0) {
                        ctx.strokeStyle = bc.top;
                        ctx.lineWidth = bw.top;
                        ctx.beginPath();
                        ctx.moveTo(0 - bw.left, 0 - bw.top / 2);
                        ctx.lineTo(plotWidth, 0 - bw.top / 2);
                        ctx.stroke();
                    }

                    if (bw.right > 0) {
                        ctx.strokeStyle = bc.right;
                        ctx.lineWidth = bw.right;
                        ctx.beginPath();
                        ctx.moveTo(plotWidth + bw.right / 2, 0 - bw.top);
                        ctx.lineTo(plotWidth + bw.right / 2, plotHeight);
                        ctx.stroke();
                    }

                    if (bw.bottom > 0) {
                        ctx.strokeStyle = bc.bottom;
                        ctx.lineWidth = bw.bottom;
                        ctx.beginPath();
                        ctx.moveTo(plotWidth + bw.right, plotHeight + bw.bottom / 2);
                        ctx.lineTo(0, plotHeight + bw.bottom / 2);
                        ctx.stroke();
                    }

                    if (bw.left > 0) {
                        ctx.strokeStyle = bc.left;
                        ctx.lineWidth = bw.left;
                        ctx.beginPath();
                        ctx.moveTo(0 - bw.left / 2, plotHeight + bw.bottom);
                        ctx.lineTo(0 - bw.left / 2, 0);
                        ctx.stroke();
                    }
                } else {
                    ctx.lineWidth = bw;
                    ctx.strokeStyle = options.grid.borderColor;
                    ctx.strokeRect(-bw / 2, -bw / 2, plotWidth + bw, plotHeight + bw);
                }
            }

            ctx.restore();
        }

        function drawMarkings(markings, markingLayer) {
            if (!markings) {
                return;
            }

            for (var i = 0; i < markings.length; i++) {
                drawMarking(markings[i], markingLayer);
            }
        }

        function drawMarking(m, markingLayer) {
            var xrange = extractRange(m, "x"),
                yrange = extractRange(m, "y");

            // fill in missing
            if (xrange.from == null) {
                xrange.from = xrange.axis.min;
            }
            if (xrange.to == null) {
                xrange.to = xrange.axis.max;
            }
            if (yrange.from == null) {
                yrange.from = yrange.axis.min;
            }
            if (yrange.to == null) {
                yrange.to = yrange.axis.max;
            }

            // clip
            if (xrange.to < xrange.axis.min || xrange.from > xrange.axis.max ||
                yrange.to < yrange.axis.min || yrange.from > yrange.axis.max) {
                return;
            }

            xrange.from = Math.max(xrange.from, xrange.axis.min);
            xrange.to = Math.min(xrange.to, xrange.axis.max);
            yrange.from = Math.max(yrange.from, yrange.axis.min);
            yrange.to = Math.min(yrange.to, yrange.axis.max);

            var xequal = xrange.from === xrange.to,
                yequal = yrange.from === yrange.to;

            if (xequal && yequal) {
                return;
            }

            // then draw
            xrange.from = Math.floor(xrange.axis.p2c(xrange.from));
            xrange.to = Math.floor(xrange.axis.p2c(xrange.to));
            yrange.from = Math.floor(yrange.axis.p2c(yrange.from));
            yrange.to = Math.floor(yrange.axis.p2c(yrange.to));

            if (xequal || yequal) {
                var lineWidth = m.lineWidth || options.grid.markingsLineWidth,
                    subPixel = lineWidth % 2 ? 0.5 : 0;
                ctx.beginPath();
                ctx.strokeStyle = m.color || options.grid.markingsColor;
                ctx.lineWidth = lineWidth;
                if (xequal) {
                    ctx.moveTo(xrange.to + subPixel, yrange.from);
                    ctx.lineTo(xrange.to + subPixel, yrange.to);
                } else {
                    ctx.moveTo(xrange.from, yrange.to + subPixel);
                    ctx.lineTo(xrange.to, yrange.to + subPixel);
                }
                ctx.stroke();
            } else {
                ctx.fillStyle = m.color || options.grid.markingsColor;
                if (m.rounded) {
                    roundRect(
                        ctx, xrange.from, yrange.to, xrange.to - xrange.from,
                        yrange.from - yrange.to, m.rounded);
                    ctx.fill();
                } else {
                    ctx.fillRect(
                        xrange.from, yrange.to, xrange.to - xrange.from,
                        yrange.from - yrange.to);
                }
            }

            if (m.text) {

                // left aligned horizontal position:
                var xPos = xrange.from + plotOffset.left,

                    // top baselined vertical position:
                    yPos = (yrange.to + plotOffset.top);

                if (!!m.textAlign) {
                    switch (m.textAlign.toLowerCase()) {
                    case "right":
                        xPos = xrange.to + plotOffset.left;
                        break;
                    case "center":
                        xPos = (xrange.from + plotOffset.left + xrange.to + plotOffset.left) / 2;
                        break;
                    }
                }

                if (!!m.textBaseline) {
                    switch (m.textBaseline.toLowerCase()) {
                    case "bottom":
                        yPos = (yrange.from + plotOffset.top);
                        break;
                    case "middle":
                        yPos = (yrange.from + plotOffset.top + yrange.to + plotOffset.top) / 2;
                        break;
                    }
                }

                surface.addText(markingLayer, xPos, yPos, m.text, m.font || "flot-marking", 0, null, m.textAlign, m.textBaseline);
            }
        }

        function drawAxisLabels() {

            $.each(allAxes(), function(_, axis) {

                var box = axis.box,
                    axisOptions = axis.options,
                    legacyStyles = axis.direction + "Axis " + axis.direction + axis.n + "Axis",
                    layer = "flot-" + axis.direction + "-axis flot-" + axis.direction + axis.n + "-axis " + legacyStyles,
                    labelFont = axisOptions.labelFont || "flot-axis-label axisLabels " + axis.direction + axis.n + "axisLabel",
                    tickFont = axisOptions.tickFont || "flot-tick-label tickLabel",
                    tick, x, y, halign, valign;

                // Remove text before checking for axis.show and ticks.length;
                // otherwise plugins, like flot-tickrotor, that draw their own
                // tick labels will end up with both theirs and the defaults.
                surface.removeText(layer);

                if (!axis.show || axis.ticks.length === 0) {
                    return;
                }

                if (axisOptions.label) {
                    if (axis.direction === "x") {
                        if (axisOptions.position === "top") {
                            surface.addText(layer, box.left + box.width / 2, box.top, axisOptions.label, labelFont, 0, null, "center", "top");
                        } else {
                            surface.addText(layer, box.left + box.width / 2, box.top + box.height, axisOptions.label, labelFont, 0, null, "center", "bottom");
                        }
                    } else {
                        if (axisOptions.position === "right") {
                            surface.addText(layer, box.left + box.width, box.top + box.height / 2, axisOptions.label, labelFont, 90, null, "right", "middle");
                        } else {
                            surface.addText(layer, box.left, box.top + box.height / 2, axisOptions.label, labelFont, -90, null, "left", "middle");
                        }
                    }
                }

                // Add labels for the ticks on this axis
                for (var i = 0; i < axis.ticks.length; ++i) {

                    tick = axis.ticks[i];
                    if (!tick.label || tick.v < axis.min || tick.v > axis.max) {
                        continue;
                    }

                    if (axis.direction === "x") {
                        halign = "center";
                        x = plotOffset.left + axis.p2c(tick.v);
                        if (axis.position === "bottom") {
                            y = box.top + box.padding;
                        } else {
                            y = box.top + box.height - box.padding;
                            valign = "bottom";
                        }
                    } else {
                        valign = "middle";
                        y = plotOffset.top + axis.p2c(tick.v);
                        if (axis.position === "left") {
                            x = box.left + box.width - box.padding;
                            halign = "right";
                        } else {
                            x = box.left + box.padding;
                        }
                    }

                    surface.addText(layer, x, y, tick.label, tickFont, null, null, halign, valign);
                }
            });
        }

        function drawSeries(series) {
            if (series.lines.show) {
                drawSeriesLines(series);
            }
            if (series.bars.show) {
                drawSeriesBars(series);
            }
            if (series.points.show) {
                drawSeriesPoints(series);
            }
        }

        function drawSeriesLines(series) {
            function plotLine(datapoints, xoffset, yoffset, axisx, axisy) {
                var points = datapoints.points,
                    ps = datapoints.pointsize,
                    showSteps = series.lines.steps,
                    prevx = null, prevy = null;

                ctx.beginPath();
                for (var i = ps; i < points.length; i += ps) {
                    var x1 = points[i - ps], y1 = points[i - ps + 1],
                        x2 = points[i], y2 = points[i + 1];

                    if (x1 == null || x2 == null) {
                        continue;
                    }

                    // clip with ymin
                    if (y1 <= y2 && y1 < axisy.min) {
                        if (y2 < axisy.min) {

                            // line segment is outside
                            continue;
                        }

                        // compute new intersection point
                        x1 = showSteps ? x2 : (axisy.min - y1) / (y2 - y1) * (x2 - x1) + x1;
                        y1 = axisy.min;
                    } else if (y2 <= y1 && y2 < axisy.min) {
                        if (y1 < axisy.min) {
                            continue;
                        }
                        x2 = showSteps ? x2 : (axisy.min - y1) / (y2 - y1) * (x2 - x1) + x1;
                        y2 = axisy.min;
                    }

                    // clip with ymax
                    if (y1 >= y2 && y1 > axisy.max) {
                        if (y2 > axisy.max) {
                            continue;
                        }
                        x1 = showSteps ? x2 : (axisy.max - y1) / (y2 - y1) * (x2 - x1) + x1;
                        y1 = axisy.max;
                    } else if (y2 >= y1 && y2 > axisy.max) {
                        if (y1 > axisy.max) {
                            continue;
                        }
                        x2 = showSteps ? x2 : (axisy.max - y1) / (y2 - y1) * (x2 - x1) + x1;
                        y2 = axisy.max;
                    }

                    // clip with xmin
                    if (x1 <= x2 && x1 < axisx.min) {
                        if (x2 < axisx.min) {
                            continue;
                        }
                        y1 = showSteps ? y1 : (axisx.min - x1) / (x2 - x1) * (y2 - y1) + y1;
                        x1 = axisx.min;
                    } else if (x2 <= x1 && x2 < axisx.min) {
                        if (x1 < axisx.min) {
                            continue;
                        }
                        y2 = showSteps ? y1 : (axisx.min - x1) / (x2 - x1) * (y2 - y1) + y1;
                        x2 = axisx.min;
                    }

                    // clip with xmax
                    if (x1 >= x2 && x1 > axisx.max) {
                        if (x2 > axisx.max) {
                            continue;
                        }
                        y1 = showSteps ? y1 : (axisx.max - x1) / (x2 - x1) * (y2 - y1) + y1;
                        x1 = axisx.max;
                    } else if (x2 >= x1 && x2 > axisx.max) {
                        if (x1 > axisx.max) {
                            continue;
                        }
                        y2 = showSteps ? y1 : (axisx.max - x1) / (x2 - x1) * (y2 - y1) + y1;
                        x2 = axisx.max;
                    }

                    if (x1 !== prevx || y1 !== prevy) {
                        ctx.moveTo(axisx.p2c(x1) + xoffset, axisy.p2c(y1) + yoffset);
                    }

                    prevx = x2;
                    prevy = y2;

                    // Draw a step
                    if (showSteps) {
                        if (x1 < x2) {
                            ctx.lineTo(axisx.p2c(x2) + xoffset, axisy.p2c(y1) + yoffset);
                        } else {
                            ctx.lineTo(axisx.p2c(x1) + xoffset, axisy.p2c(y2) + yoffset);
                        }
                    }
                    ctx.lineTo(axisx.p2c(x2) + xoffset, axisy.p2c(y2) + yoffset);
                }
                ctx.stroke();
            }

            function plotLineArea(datapoints, axisx, axisy) {
                var points = datapoints.points,
                    ps = datapoints.pointsize,
                    showSteps = series.lines.steps,
                    bottom = Math.min(Math.max(0, axisy.min), axisy.max),
                    i = 0, areaOpen = false,
                    ypos = 1, segmentStart = 0, segmentEnd = 0;

                // we process each segment in two turns, first forward
                // direction to sketch out top, then once we hit the
                // end we go backwards to sketch the bottom
                while (true) {
                    if (ps > 0 && i > points.length + ps) {
                        break;
                    }

                    // ps is negative if going backwards
                    i += ps;

                    var x1 = points[i - ps],
                        y1 = points[i - ps + ypos],
                        x2 = points[i], y2 = points[i + ypos];

                    if (areaOpen) {
                        if (ps > 0 && x1 != null && x2 == null) {

                            // at turning point
                            segmentEnd = i;
                            ps = -ps;
                            ypos = 2;
                            continue;
                        }

                        if (ps < 0 && i === segmentStart + ps) {

                            // done with the reverse sweep
                            ctx.fill();
                            areaOpen = false;
                            ps = -ps;
                            ypos = 1;
                            i = segmentStart = segmentEnd + ps;
                            continue;
                        }
                    }

                    if (x1 == null || x2 == null) {
                        continue;
                    }

                    // clip x values

                    // clip with xmin
                    if (x1 <= x2 && x1 < axisx.min) {
                        if (x2 < axisx.min) {
                            continue;
                        }
                        y1 = showSteps ? y1 : (axisx.min - x1) / (x2 - x1) * (y2 - y1) + y1;
                        x1 = axisx.min;
                    } else if (x2 <= x1 && x2 < axisx.min) {
                        if (x1 < axisx.min) {
                            continue;
                        }
                        y2 = showSteps ? y1 : (axisx.min - x1) / (x2 - x1) * (y2 - y1) + y1;
                        x2 = axisx.min;
                    }

                    // clip with xmax
                    if (x1 >= x2 && x1 > axisx.max) {
                        if (x2 > axisx.max) {
                            continue;
                        }
                        y1 = showSteps ? y1 : (axisx.max - x1) / (x2 - x1) * (y2 - y1) + y1;
                        x1 = axisx.max;
                    } else if (x2 >= x1 && x2 > axisx.max) {
                        if (x1 > axisx.max) {
                            continue;
                        }
                        y2 = showSteps ? y1 : (axisx.max - x1) / (x2 - x1) * (y2 - y1) + y1;
                        x2 = axisx.max;
                    }

                    if (!areaOpen) {

                        // open area
                        ctx.beginPath();
                        ctx.moveTo(axisx.p2c(x1), axisy.p2c(bottom));
                        areaOpen = true;
                    }

                    // now first check the case where both is outside
                    if (y1 >= axisy.max && y2 >= axisy.max) {
                        ctx.lineTo(axisx.p2c(x1), axisy.p2c(axisy.max));
                        ctx.lineTo(axisx.p2c(x2), axisy.p2c(axisy.max));
                        continue;
                    } else if (y1 <= axisy.min && y2 <= axisy.min) {
                        ctx.lineTo(axisx.p2c(x1), axisy.p2c(axisy.min));
                        ctx.lineTo(axisx.p2c(x2), axisy.p2c(axisy.min));
                        continue;
                    }

                    // else it's a bit more complicated, there might
                    // be a flat maxed out rectangle first, then a
                    // triangular cutout or reverse; to find these
                    // keep track of the current x values
                    var x1old = x1, x2old = x2;

                    // clip the y values, without shortcutting, we
                    // go through all cases in turn

                    // clip with ymin
                    if (y1 <= y2 && y1 < axisy.min && y2 >= axisy.min) {
                        x1 = showSteps ? x2 : (axisy.min - y1) / (y2 - y1) * (x2 - x1) + x1;
                        y1 = axisy.min;
                    } else if (y2 <= y1 && y2 < axisy.min && y1 >= axisy.min) {
                        x2 = showSteps ? x2 : (axisy.min - y1) / (y2 - y1) * (x2 - x1) + x1;
                        y2 = axisy.min;
                    }

                    // clip with ymax
                    if (y1 >= y2 && y1 > axisy.max && y2 <= axisy.max) {
                        x1 = showSteps ? x2 : (axisy.max - y1) / (y2 - y1) * (x2 - x1) + x1;
                        y1 = axisy.max;
                    } else if (y2 >= y1 && y2 > axisy.max && y1 <= axisy.max) {
                        x2 = showSteps ? x2 : (axisy.max - y1) / (y2 - y1) * (x2 - x1) + x1;
                        y2 = axisy.max;
                    }

                    // if the x value was changed we got a rectangle to fill
                    if (x1 !== x1old) {

                        // it goes to (x1, y1), but we fill that below
                        ctx.lineTo(axisx.p2c(x1old), axisy.p2c(y1));
                    }

                    // fill triangular section, this sometimes result
                    // in redundant points if (x1, y1) hasn't changed
                    // from previous line to, but we just ignore that
                    ctx.lineTo(axisx.p2c(x1), axisy.p2c(y1));

                    // Draw a step
                    if (showSteps) {
                        if (x1 < x2) {
                            ctx.lineTo(axisx.p2c(x2), axisy.p2c(y1));
                        } else {
                            ctx.lineTo(axisx.p2c(x1), axisy.p2c(y2));
                        }
                    }
                    ctx.lineTo(axisx.p2c(x2), axisy.p2c(y2));

                    // fill the other rectangle if it's there
                    if (x2 !== x2old) {
                        ctx.lineTo(axisx.p2c(x2), axisy.p2c(y2));
                        ctx.lineTo(axisx.p2c(x2old), axisy.p2c(y2));
                    }
                }
            }

            ctx.save();
            ctx.translate(plotOffset.left, plotOffset.top);
            ctx.lineJoin = "round";

            var lw = series.lines.lineWidth,
                sw = series.shadowSize;

            // FIXME: consider another form of shadow when filling is turned on
            if (lw > 0 && sw > 0) {

                // draw shadow as a thick and thin line with transparency
                ctx.lineWidth = sw;
                ctx.strokeStyle = "rgba(0,0,0,0.1)";

                // position shadow at angle from the mid of line
                var angle = Math.PI / 18;
                plotLine(series.datapoints, Math.sin(angle) * (lw / 2 + sw / 2), Math.cos(angle) * (lw / 2 + sw / 2), series.xaxis, series.yaxis);
                ctx.lineWidth = sw / 2;
                plotLine(series.datapoints, Math.sin(angle) * (lw / 2 + sw / 4), Math.cos(angle) * (lw / 2 + sw / 4), series.xaxis, series.yaxis);
            }

            ctx.lineWidth = lw;
            ctx.strokeStyle = series.color;
            var fillStyle = getFillStyle(series.lines, series.color, 0, plotHeight);
            if (fillStyle) {
                ctx.fillStyle = fillStyle;
                plotLineArea(series.datapoints, series.xaxis, series.yaxis);
            }

            if (lw > 0) {
                plotLine(series.datapoints, 0, 0, series.xaxis, series.yaxis);
            }
            ctx.restore();
        }

        function drawSeriesPoints(series) {
            function plotPoints(datapoints, radius, fillStyle, offset, shadow, axisx, axisy, symbol) {
                var points = datapoints.points, ps = datapoints.pointsize;

                for (var i = 0; i < points.length; i += ps) {
                    var x = points[i], y = points[i + 1];
                    if (x == null || x < axisx.min || x > axisx.max || y < axisy.min || y > axisy.max) {
                        continue;
                    }

                    ctx.beginPath();
                    x = axisx.p2c(x);
                    y = axisy.p2c(y) + offset;
                    if (symbol === "circle") {
                        ctx.arc(x, y, radius, 0, shadow ? Math.PI : Math.PI * 2, false);
                    } else {
                        symbol(ctx, x, y, radius, shadow);
                    }
                    ctx.closePath();

                    if (fillStyle) {
                        ctx.fillStyle = fillStyle;
                        ctx.fill();
                    }
                    ctx.stroke();
                }
            }

            ctx.save();
            ctx.translate(plotOffset.left, plotOffset.top);

            var lw = series.points.lineWidth,
                sw = series.shadowSize,
                radius = series.points.radius,
                symbol = series.points.symbol;

            // If the user sets the line width to 0, we change it to a very
            // small value. A line width of 0 seems to force the default of 1.
            // Doing the conditional here allows the shadow setting to still be
            // optional even with a lineWidth of 0.
            if (lw === 0) {
                lw = 0.0001;
            }

            if (lw > 0 && sw > 0) {

                // draw shadow in two steps
                var w = sw / 2;
                ctx.lineWidth = w;
                ctx.strokeStyle = "rgba(0,0,0,0.1)";
                plotPoints(series.datapoints, radius, null, w + w / 2, true,
                           series.xaxis, series.yaxis, symbol);

                ctx.strokeStyle = "rgba(0,0,0,0.2)";
                plotPoints(series.datapoints, radius, null, w / 2, true,
                           series.xaxis, series.yaxis, symbol);
            }

            ctx.lineWidth = lw;
            ctx.strokeStyle = series.points.strokeColor || series.color;
            plotPoints(series.datapoints, radius,
                       getFillStyle(series.points, series.color), 0, false,
                       series.xaxis, series.yaxis, symbol);
            ctx.restore();
        }

        function drawBar(x, y, b, barLeft, barRight, fillStyleCallback, axisx, axisy, c, horizontal, lineWidth) {
            var left, right, bottom, top,
                drawLeft, drawRight, drawTop, drawBottom,
                tmp;

            // in horizontal mode, we start the bar from the left
            // instead of from the bottom so it appears to be
            // horizontal rather than vertical
            if (horizontal) {
                drawBottom = drawRight = drawTop = true;
                drawLeft = false;
                left = b;
                right = x;
                top = y + barLeft;
                bottom = y + barRight;

                // account for negative bars
                if (right < left) {
                    tmp = right;
                    right = left;
                    left = tmp;
                    drawLeft = true;
                    drawRight = false;
                }
            } else {
                drawLeft = drawRight = drawTop = true;
                drawBottom = false;
                left = x + barLeft;
                right = x + barRight;
                bottom = b;
                top = y;

                // account for negative bars
                if (top < bottom) {
                    tmp = top;
                    top = bottom;
                    bottom = tmp;
                    drawBottom = true;
                    drawTop = false;
                }
            }

            // clip
            if (right < axisx.min || left > axisx.max ||
                top < axisy.min || bottom > axisy.max) {
                return;
            }

            if (left < axisx.min) {
                left = axisx.min;
                drawLeft = false;
            }

            if (right > axisx.max) {
                right = axisx.max;
                drawRight = false;
            }

            if (bottom < axisy.min) {
                bottom = axisy.min;
                drawBottom = false;
            }

            if (top > axisy.max) {
                top = axisy.max;
                drawTop = false;
            }

            left = axisx.p2c(left);
            bottom = axisy.p2c(bottom);
            right = axisx.p2c(right);
            top = axisy.p2c(top);

            // fill the bar
            if (fillStyleCallback) {
                c.fillStyle = fillStyleCallback(bottom, top);
                c.fillRect(left, top, right - left, bottom - top);
            }

            // draw outline
            if (lineWidth > 0 && (drawLeft || drawRight || drawTop || drawBottom)) {
                c.beginPath();

                // FIXME: inline moveTo is buggy with excanvas
                c.moveTo(left, bottom);
                if (drawLeft) {
                    c.lineTo(left, top);
                } else {
                    c.moveTo(left, top);
                }
                if (drawTop) {
                    c.lineTo(right, top);
                } else {
                    c.moveTo(right, top);
                }
                if (drawRight) {
                    c.lineTo(right, bottom);
                } else {
                    c.moveTo(right, bottom);
                }
                if (drawBottom) {
                    c.lineTo(left, bottom);
                } else {
                    c.moveTo(left, bottom);
                }
                c.stroke();
            }
        }

        function drawSeriesBars(series) {
            function plotBars(datapoints, barLeft, barRight, fillStyleCallback, axisx, axisy) {
                var points = datapoints.points, ps = datapoints.pointsize;

                for (var i = 0; i < points.length; i += ps) {
                    if (points[i] == null) {
                        continue;
                    }
                    drawBar(points[i], points[i + 1], points[i + 2], barLeft, barRight, fillStyleCallback, axisx, axisy, ctx, series.bars.horizontal, series.bars.lineWidth);
                }
            }

            ctx.save();
            ctx.translate(plotOffset.left, plotOffset.top);

            // FIXME: figure out a way to add shadows (for instance along the right edge)
            ctx.lineWidth = series.bars.lineWidth;
            ctx.strokeStyle = series.color;

            var barLeft;

            switch (series.bars.align) {
            case "left":
                barLeft = 0;
                break;
            case "right":
                barLeft = -series.bars.barWidth;
                break;
            default:
                barLeft = -series.bars.barWidth / 2;
            }

            var fillStyleCallback = series.bars.fill ? function(bottom, top) { return getFillStyle(series.bars, series.color, bottom, top); } : null;
            plotBars(series.datapoints, barLeft, barLeft + series.bars.barWidth, fillStyleCallback, series.xaxis, series.yaxis);
            ctx.restore();
        }

        function getFillStyle(filloptions, seriesColor, bottom, top) {
            var fill = filloptions.fill;
            if (!fill) {
                return null;
            }

            if (filloptions.fillColor) {
                return getColorOrGradient(filloptions.fillColor, bottom, top, seriesColor);
            }

            var c = $.color.parse(seriesColor);
            c.a = isNumeric(fill) ? fill : 0.4;
            c.normalize();
            return c.toString();
        }

        function insertLegend() {

            if (options.legend.container != null) {
                $(options.legend.container).html("");
            } else {
                placeholder.find(".legend").remove();
            }

            if (!options.legend.show) {
                return;
            }

            var entries = [], lf = options.legend.labelFormatter, s, label, i;

            // Build a list of legend entries, with each having a label and a color
            for (i = 0; i < series.length; ++i) {
                s = series[i];
                if (s.label) {
                    label = lf ? lf(s.label, s) : s.label;
                    if (label) {
                        entries.push({
                            label: label,
                            color: s.color
                        });
                    }
                }
            }

            // No entries implies no legend
            if (entries.length === 0) {
                return;
            }

            // Sort the legend using either the default or a custom comparator
            if (options.legend.sorted) {
                if ($.isFunction(options.legend.sorted)) {
                    entries.sort(options.legend.sorted);
                } else if (options.legend.sorted === "reverse") {
                    entries.reverse();
                } else {
                    var ascending = options.legend.sorted !== "descending";
                    entries.sort(function(a, b) {
                        return a.label === b.label ? 0 : (
                            (a.label < b.label) !== ascending ? 1 : -1   // Logical XOR
                        );
                    });
                }
            }

            // Generate markup for the list of entries, in their final order
            var table = $("<table></table>").css({
                "font-size": "smaller",
                "color": options.grid.color
            }), rowBuffer = null;

            for (i = 0; i < entries.length; ++i) {

                var entry = entries[i];

                if (i % options.legend.noColumns === 0) {
                    if (rowBuffer !== null) {
                        table.append(rowBuffer);
                    }
                    rowBuffer = $("<tr></tr>");
                }

                var colorbox = $("<div></div>").css({
                    "width": "4px",
                    "height": 0,
                    "border": "5px solid " + entry.color,
                    "overflow": "hidden"
                }),

                borderbox = $("<div></div>").css({
                    "border": "1px solid " + options.legend.labelBoxBorderColor,
                    "padding": "1px"
                });

                rowBuffer.append(
                    $("<td></td>").addClass("legendColorBox").append(borderbox.append(colorbox)),
                    $("<td></td>").addClass("legendLabel").html(entry.label)
                );
            }

            table.append(rowBuffer);

            if (options.legend.container != null) {
                $(options.legend.container).html(table);
            } else {
                var pos = { "position": "absolute" },
                    p = options.legend.position,
                    m = options.legend.margin;
                if (m[0] == null) {
                    m = [m, m];
                }
                if (p.charAt(0) === "n") {
                    pos.top = (m[1] + plotOffset.top) + "px";
                } else if (p.charAt(0) === "s") {
                    pos.bottom = (m[1] + plotOffset.bottom) + "px";
                }
                if (p.charAt(1) === "e") {
                    pos.right = (m[0] + plotOffset.right) + "px";
                } else if (p.charAt(1) === "w") {
                    pos.left = (m[0] + plotOffset.left) + "px";
                }
                var legend = $("<div></div>").addClass("legend").append(table.css(pos)).appendTo(placeholder);
                if (options.legend.backgroundOpacity !== 0.0) {

                    // put in the transparent background
                    // separately to avoid blended labels and
                    // label boxes
                    var c = options.legend.backgroundColor;
                    if (c == null) {
                        c = options.grid.backgroundColor;
                        if (c && typeof c === "string") {
                            c = $.color.parse(c);
                        } else {
                            c = $.color.extract(legend, "background-color");
                        }
                        c.a = 1;
                        c = c.toString();
                    }
                    var div = legend.children();

                    // Position also applies to this
                    $("<div></div>").css(pos).css({
                        "width": div.width() + "px",
                        "height": div.height() + "px",
                        "background-color": c,
                        "opacity": options.legend.backgroundOpacity
                    }).prependTo(legend);
                }
            }
        }

        /**
         * returns the data item the mouse is over, or null if none is found
         */
        function findNearbyItem(mouseX, mouseY, seriesFilter) {
            var maxDistance = options.grid.mouseActiveRadius,
                smallestDistance = maxDistance * maxDistance + 1,
                item = null, i, j, ps;

            for (i = series.length - 1; i >= 0; --i) {
                if (!seriesFilter(series[i])) {
                    continue;
                }

                var s = series[i],
                    axisx = s.xaxis,
                    axisy = s.yaxis,
                    points = s.datapoints.points,

                    // precompute some stuff to make the loop faster
                    mx = axisx.c2p(mouseX),
                    my = axisy.c2p(mouseY),
                    maxx = maxDistance / axisx.scale,
                    maxy = maxDistance / axisy.scale,
                    x, y;

                ps = s.datapoints.pointsize;

                // with inverse transforms, we can't use the maxx/maxy
                // optimization, sadly
                if (axisx.options.inverseTransform) {
                    maxx = Number.MAX_VALUE;
                }
                if (axisy.options.inverseTransform) {
                    maxy = Number.MAX_VALUE;
                }

                if (s.lines.show || s.points.show) {
                    for (j = 0; j < points.length; j += ps) {

                        x = points[j];
                        y = points[j + 1];

                        if (x == null) {
                            continue;
                        }

                        // For points and lines, the cursor must be within a
                        // certain distance to the data point
                        if (x - mx > maxx || x - mx < -maxx ||
                            y - my > maxy || y - my < -maxy) {
                            continue;
                        }

                        // We have to calculate distances in pixels, not in
                        // data units, because the scales of the axes may be different
                        var dx = Math.abs(axisx.p2c(x) - mouseX),
                            dy = Math.abs(axisy.p2c(y) - mouseY),
                            dist = dx * dx + dy * dy; // we save the sqrt

                        // use <= to ensure last point takes precedence
                        // (last generally means on top of)
                        if (dist < smallestDistance) {
                            smallestDistance = dist;
                            item = [i, j / ps];
                        }
                    }
                }

                if (s.bars.show && !item) {

                    // no other point can be nearby
                    var barLeft, barRight;

                    switch (s.bars.align) {
                    case "left":
                        barLeft = 0;
                        break;
                    case "right":
                        barLeft = -s.bars.barWidth;
                        break;
                    default:
                        barLeft = -s.bars.barWidth / 2;
                    }

                    barRight = barLeft + s.bars.barWidth;

                    for (j = 0; j < points.length; j += ps) {
                        x = points[j];
                        y = points[j + 1];
                        var b = points[j + 2];
                        if (x == null) {
                            continue;
                        }

                        // for a bar graph, the cursor must be inside the bar
                        if (series[i].bars.horizontal ?
                            (mx <= Math.max(b, x) && mx >= Math.min(b, x) &&
                             my >= y + barLeft && my <= y + barRight) :
                            (mx >= x + barLeft && mx <= x + barRight &&
                             my >= Math.min(b, y) && my <= Math.max(b, y))) {
                            item = [i, j / ps];
                        }
                    }
                }
            }

            if (item) {
                i = item[0];
                j = item[1];
                ps = series[i].datapoints.pointsize;

                return { datapoint: series[i].datapoints.points.slice(j * ps, (j + 1) * ps),
                         dataIndex: j,
                         series: series[i],
                         seriesIndex: i };
            }

            return null;
        }

        function onMouseMove(e) {
            if (options.grid.hoverable) {
                triggerClickHoverEvent("plothover", e,
                                       function(s) { return s.hoverable !== false; });
            }
        }

        function onMouseLeave(e) {
            if (options.grid.hoverable) {
                triggerClickHoverEvent("plothover", e,
                                       function() { return false; });
            }
        }

        function onClick(e) {
            triggerClickHoverEvent("plotclick", e,
                                   function(s) { return s.clickable !== false; });
        }

        /**
         * trigger click or hover event (they send the same parameters
         * so we share their code)
         */
        function triggerClickHoverEvent(eventname, event, seriesFilter) {
            var offset = eventHolder.offset(),
                canvasX = event.pageX - offset.left - plotOffset.left,
                canvasY = event.pageY - offset.top - plotOffset.top,
            pos = canvasToAxisCoords({ left: canvasX, top: canvasY });

            pos.pageX = event.pageX;
            pos.pageY = event.pageY;

            var item = findNearbyItem(canvasX, canvasY, seriesFilter);

            if (item) {

                // fill in mouse pos for any listeners out there
                item.pageX = parseInt(item.series.xaxis.p2c(item.datapoint[0]) + offset.left + plotOffset.left, 10);
                item.pageY = parseInt(item.series.yaxis.p2c(item.datapoint[1]) + offset.top + plotOffset.top, 10);
            }

            if (options.grid.autoHighlight) {

                // clear auto-highlights
                for (var i = 0; i < highlights.length; ++i) {
                    var h = highlights[i];
                    if (h.auto === eventname && !(
                        item && h.series === item.series &&
                        h.point[0] === item.datapoint[0] &&
                        h.point[1] === item.datapoint[1]
                    )) {
                        unhighlight(h.series, h.point);
                    }
                }

                if (item) {
                    highlight(item.series, item.datapoint, eventname);
                }
            }

            placeholder.trigger(eventname, [pos, item]);
        }

        function triggerRedrawOverlay() {
            var t = options.interaction.redrawOverlayInterval;

            if (t === -1) {

                // skip event queue
                drawOverlay();
                return;
            }

            if (!redrawTimeout) {
                redrawTimeout = setTimeout(drawOverlay, t);
            }
        }

        function drawOverlay() {
            redrawTimeout = null;

            // draw highlights
            octx.save();
            overlay.clear();
            octx.translate(plotOffset.left, plotOffset.top);

            var i, hi;
            for (i = 0; i < highlights.length; ++i) {
                hi = highlights[i];

                if (hi.series.bars.show) {
                    drawBarHighlight(hi.series, hi.point);
                } else {
                    drawPointHighlight(hi.series, hi.point);
                }
            }
            octx.restore();

            executeHooks(hooks.drawOverlay, [octx]);
        }

        function highlight(s, point, auto) {
            if (isNumeric(s)) {
                s = series[s];
            }

            if (isNumeric(point)) {
                var ps = s.datapoints.pointsize;
                point = s.datapoints.points.slice(ps * point, ps * (point + 1));
            }

            var i = indexOfHighlight(s, point);
            if (i === -1) {
                highlights.push({ series: s, point: point, auto: auto });
                triggerRedrawOverlay();
            } else if (!auto) {
                highlights[i].auto = false;
            }
        }

        function unhighlight(s, point) {
            if (s == null && point == null) {
                highlights = [];
                triggerRedrawOverlay();
                return;
            }

            if (isNumeric(s)) {
                s = series[s];
            }

            if (isNumeric(point)) {
                var ps = s.datapoints.pointsize;
                point = s.datapoints.points.slice(ps * point, ps * (point + 1));
            }

            var i = indexOfHighlight(s, point);
            if (i !== -1) {
                highlights.splice(i, 1);
                triggerRedrawOverlay();
            }
        }

        function indexOfHighlight(s, p) {
            for (var i = 0; i < highlights.length; ++i) {
                var h = highlights[i];
                if (h.series === s && h.point[0] === p[0] && h.point[1] === p[1]) {
                    return i;
                }
            }
            return -1;
        }

        function drawPointHighlight(series, point) {
            var x = point[0], y = point[1],
                axisx = series.xaxis, axisy = series.yaxis,
                highlightColor = (typeof series.highlightColor === "string") ? series.highlightColor : $.color.parse(series.color).scale("a", 0.5).toString();

            if (x < axisx.min || x > axisx.max || y < axisy.min || y > axisy.max) {
                return;
            }

            var pointRadius, radius;

            if (series.points.show) {
                pointRadius = series.points.radius + series.points.lineWidth / 2;
                radius = 1.5 * pointRadius;
            } else {
                pointRadius = series.points.radius;
                radius = 0.5 * pointRadius;
            }
            octx.lineWidth = pointRadius;
            octx.strokeStyle = highlightColor;
            x = axisx.p2c(x);
            y = axisy.p2c(y);

            octx.beginPath();
            if (series.points.symbol === "circle") {
                octx.arc(x, y, radius, 0, 2 * Math.PI, false);
            } else {
                series.points.symbol(octx, x, y, radius, false);
            }
            octx.closePath();
            octx.stroke();
        }

        function drawBarHighlight(series, point) {
            var highlightColor = (typeof series.highlightColor === "string") ? series.highlightColor : $.color.parse(series.color).scale("a", 0.5).toString(),
                fillStyle = highlightColor,
                barLeft;

            switch (series.bars.align) {
            case "left":
                barLeft = 0;
                break;
            case "right":
                barLeft = -series.bars.barWidth;
                break;
            default:
                barLeft = -series.bars.barWidth / 2;
            }

            octx.lineWidth = series.bars.lineWidth;
            octx.strokeStyle = highlightColor;

            drawBar(point[0], point[1], point[2] || 0, barLeft, barLeft + series.bars.barWidth,
                    function() { return fillStyle; }, series.xaxis, series.yaxis, octx, series.bars.horizontal, series.bars.lineWidth);
        }

        function getColorOrGradient(spec, bottom, top, defaultColor) {
            if (typeof spec === "string") {
                return spec;
            } else {

                // assume this is a gradient spec; IE currently only
                // supports a simple vertical gradient properly, so that's
                // what we support too
                var gradient = ctx.createLinearGradient(0, top, 0, bottom);

                for (var i = 0, l = spec.colors.length; i < l; ++i) {
                    var c = spec.colors[i];
                    if (typeof c !== "string") {
                        var co = $.color.parse(defaultColor);
                        if (c.brightness != null) {
                            co = co.scale("rgb", c.brightness);
                        }
                        if (c.opacity != null) {
                            co.a *= c.opacity;
                        }
                        c = co.toString();
                    }
                    gradient.addColorStop(i / (l - 1), c);
                }

                return gradient;
            }
        }
    }

    /**
     * Add the plot function to the top level of the jQuery object
     */
    $.plot = function(placeholder, data, options) {
        var plot = new Plot($(placeholder), data, options, $.plot.plugins);
        return plot;
    };

    $.plot.version = "0.9.0-alpha";

    $.plot.plugins = [];

    /**
     * Also add the plot function as a chainable property
     */
    $.fn.plot = function(data, options) {
        return this.each(function() {
            $.plot(this, data, options);
        });
    };

    /**
     * round to nearby lower multiple of base
     */
    function floorInBase(n, base) {
        return base * Math.floor(n / base);
    }

    /**
     * Draw a rectangle with rounded corner on the canvas.
     *
     * @param {CanvasRenderingContext2D} ctx The canvas 2D context.
     * @param {number} x The x-axis coordinate of the upper left corner of
     *      the rectangle to be drawn.
     * @param {number} y The y-axis coordinate of the upper left corner of
     *      the rectangle to be drawn.
     * @param {number} width The width of the rectangle to be drawn.
     * @param {number} height The height of the rectangle to be drawn.
     * @param {number} radius The radius of the corner of the rectangle
     *      to be drawn.
     */
    function roundRect(ctx, x, y, width, height, radius) {
        var r = x + width,
            b = y + height;

        ctx.beginPath();
        ctx.moveTo(x + radius, y);
        ctx.lineTo(r - radius, y);
        ctx.quadraticCurveTo(r, y, r, y + radius);
        ctx.lineTo(r, y + height - radius);
        ctx.quadraticCurveTo(r, b, r - radius, b);
        ctx.lineTo(x + radius, b);
        ctx.quadraticCurveTo(x, b, x, b - radius);
        ctx.lineTo(x, y + radius);
        ctx.quadraticCurveTo(x, y, x + radius, y);
        ctx.closePath();
        return ctx;
    }

})(jQuery);
