/* Flot plugin for thresholding data.

Copyright (c) 2007-2014 IOLA and Ole Laursen.
Licensed under the MIT license.

The plugin supports these options:

    series: {
        threshold: {
            below: number,
            above: mumber,
            color: colorspec
        }
    }

It can also be applied to a single series, like this:

    $.plot( $("#placeholder"), [{
        data: [ ... ],
        threshold: { ... }
    }])

An array can be passed for multiple thresholding, like this:

    threshold: [{
        below: number1,
        color: color1
    },{
        above: number2,
        color: color2
    }]

These multiple threshold objects can be passed in any order since they are
sorted by the processing function.

The data points below "below" are drawn with the specified color. This makes
it easy to mark points below 0, e.g. for budget data.

Internally, the plugin works by splitting the data into two series, above and
below the threshold. The extra series below the threshold will have its label
cleared and the special "originSeries" attribute set to the original series.
You may need to check for this in hover events.

*/

(function($) {
    var options = {
        series: { threshold: null } // or { below: number, color: color spec}
    };

    function init(plot) {
        function thresholdData(plot, s, datapoints, below, above, color) {

            var origpoints = datapoints.points,
                ps = datapoints.pointsize,
                addCrossingPoints = s.lines.show,
                thresholded = $.extend({}, s), // note: shallow copy
                threspoints = [],
                newpoints = [],
                prevp, i, x, y, p, m;

            thresholded.datapoints = { points: [], pointsize: ps, format: datapoints.format };
            thresholded.label = null;
            thresholded.color = color;
            thresholded.threshold = null;
            thresholded.originSeries = s;
            thresholded.data = [];

            for (i = 0; i < origpoints.length; i += ps) {
                x = origpoints[i];
                y = origpoints[i + 1];

                prevp = p;
                if (y < below || y > above) {
                    p = threspoints;
                } else {
                    p = newpoints;
                }

                if (addCrossingPoints && prevp !== p && x != null && i > 0 && origpoints[i - ps] != null) {
                    var interx = x + (below - y) * (x - origpoints[i - ps]) / (y - origpoints[i - ps + 1]);
                    prevp.push(interx);
                    prevp.push(below);
                    for (m = 2; m < ps; ++m) {
                        prevp.push(origpoints[i + m]);
                    }

                    p.push(null); // start new segment
                    p.push(null);
                    for (m = 2; m < ps; ++m) {
                        p.push(origpoints[i + m]);
                    }
                    p.push(interx);
                    p.push(below);
                    for (m = 2; m < ps; ++m) {
                        p.push(origpoints[i + m]);
                    }
                }

                p.push(x);
                p.push(y);
                for (m = 2; m < ps; ++m) {
                    p.push(origpoints[i + m]);
                }
            }

            datapoints.points = newpoints;
            thresholded.datapoints.points = threspoints;

            if (thresholded.datapoints.points.length > 0) {
                var origIndex = $.inArray(s, plot.getData());
                // Insert newly-generated series right after original one (to prevent it from becoming top-most)
                plot.getData().splice(origIndex + 1, 0, thresholded);
            }

            // FIXME: there are probably some edge cases left in bars
        }

        function processThresholds(plot, s, datapoints) {
            if (!s.threshold) {
                return;
            }

            if (s.threshold instanceof Array) {
                s.threshold.sort(function(a, b) {
                    return a.below - b.below;
                });

                $(s.threshold).each(function(i, th) {
                    thresholdData(plot, s, datapoints, th.below, th.above, th.color);
                });
            } else {
                thresholdData(plot, s, datapoints, s.threshold.below, s.threshold.above, s.threshold.color);
            }
        }

        plot.hooks.processDatapoints.push(processThresholds);

        function processThresholdsLegend(ctx, canvas, s) {
            if (!s.threshold) {
                return;
            }

            var color = s.threshold.color ? s.threshold.color : "black";

            $(".legendLabel").each(function() {
                if ($(this).text() === s.label)
                {
                    var legend = $(this).prev().find("div > div");
                    legend.css("border-right-color", color);
                    legend.css("border-bottom-color", color);
                }
            });
        }

        plot.hooks.drawSeries.push(processThresholdsLegend);
    }

    $.plot.plugins.push({
        init: init,
        options: options,
        name: "threshold",
        version: "1.3"
    });
})(jQuery);
