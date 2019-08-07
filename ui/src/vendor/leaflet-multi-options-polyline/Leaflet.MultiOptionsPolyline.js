/*
 * L.MultiOptionsPolyline is a MultiPolyLine which parts can be styled differently.
 * options: {
 *     multiOptions: {
 *         optionIdxFn: function (latLng, prevLatLng, index, allLatlngs),
 *         fnContext: ctx, // the context to call optionIdxFn (optional)
 *         options: [{}, {}, {}] or function, // options for the index returned by optionIdxFn. If supplied with a function then it will be called with the index
 *         copyBaseOptions: true
 *     },
 *     // other options from Polyline
 * }
 */
export default L.FeatureGroup.extend({

    initialize: function (latlngs, options) {
        var copyBaseOptions = options.multiOptions.copyBaseOptions;

        this._layers = {};
        this._options = options;
        if (copyBaseOptions === undefined || copyBaseOptions) {
            this._copyBaseOptions();
        }

        this.setLatLngs(latlngs);
    },

    _copyBaseOptions: function () {
        var multiOptions = this._options.multiOptions,
            baseOptions,
            optionsArray = multiOptions.options,
            i, len = optionsArray.length;

        baseOptions = L.extend({}, this._options);
        delete baseOptions.multiOptions;

        for (i = 0; i < len; ++i) {
            optionsArray[i] = L.extend({}, baseOptions, optionsArray[i]);
        }
    },

    setLatLngs: function (latlngs) {
        var i, len = latlngs.length,
            multiOptions = this._options.multiOptions,
            optionIdxFn = multiOptions.optionIdxFn,
            fnContext = multiOptions.fnContext || this,
            nextOptionsIdx, optionIdx,
            segmentLatlngs = [];

        this._originalLatlngs = latlngs;

        this.eachLayer(function (layer) {
            this.removeLayer(layer);
        }, this);

        for (i = 0; i < len; ++i) {

            // If there is next point
            if (latlngs[i + 1]) {
                nextOptionsIdx = optionIdxFn.call(fnContext, latlngs[i + 1], latlngs[i], i + 1, latlngs);
            }
            optionIdx = optionIdxFn.call(fnContext, latlngs[i], latlngs[i - 1] || latlngs[i], i, latlngs);

            segmentLatlngs.push(latlngs[i]);

            // is there a change in options or is it the last point?
            if (optionIdx !== nextOptionsIdx || i === len - 1) {
                // Check if options is a function or an array
                if (typeof multiOptions.options === "function") {
                    this.addLayer(L.polyline(segmentLatlngs, multiOptions.options(optionIdx)));
                } else {
                    this.addLayer(L.polyline(segmentLatlngs, multiOptions.options[optionIdx]));
                }

                segmentLatlngs = [latlngs[i]];
            }
        }

        return this;
    },

    getLatLngs: function () {
        return this._originalLatlngs;
    },

    getLatLngsSegments: function () {
        var latlngs = [];

        this.eachLayer(function (layer) {
            latlngs.push(layer.getLatLngs());
        });

        return latlngs;
    }
});