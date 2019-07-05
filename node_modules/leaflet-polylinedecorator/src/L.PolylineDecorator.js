import L from 'leaflet';
import {
    projectPatternOnPointPath,
    parseRelativeOrAbsoluteValue,
} from './patternUtils.js';
import './L.Symbol.js';

const isCoord = c =>
    (c instanceof L.LatLng) ||
    (Array.isArray(c) && c.length === 2 && typeof c[0] === 'number');

const isCoordArray = ll => Array.isArray(ll) && isCoord(ll[0]);

L.PolylineDecorator = L.FeatureGroup.extend({
    options: {
        patterns: []
    },

    initialize: function(paths, options) {
        L.FeatureGroup.prototype.initialize.call(this);
        L.Util.setOptions(this, options);
        this._map = null;
        this._paths = this._initPaths(paths);
        this._bounds = this._initBounds();
        this._patterns = this._initPatterns(this.options.patterns);
    },

    /**
    * Deals with all the different cases. input can be one of these types:
    * array of LatLng, array of 2-number arrays, Polyline, Polygon,
    * array of one of the previous.
    */
    _initPaths: function(input, isPolygon) {
        if (isCoordArray(input)) {
            // Leaflet Polygons don't need the first point to be repeated, but we do
            const coords = isPolygon ? input.concat([input[0]]) : input;
            return [coords];
        }
        if (input instanceof L.Polyline) {
            // we need some recursivity to support multi-poly*
            return this._initPaths(input.getLatLngs(), (input instanceof L.Polygon));
        }
        if (Array.isArray(input)) {
            // flatten everything, we just need coordinate lists to apply patterns
            return input.reduce((flatArray, p) =>
                flatArray.concat(this._initPaths(p, isPolygon)),
            []);
        }
        return [];
    },

    // parse pattern definitions and precompute some values
    _initPatterns: function(patternDefs) {
        return patternDefs.map(this._parsePatternDef);
    },

    /**
    * Changes the patterns used by this decorator
    * and redraws the new one.
    */
    setPatterns: function(patterns) {
        this.options.patterns = patterns;
        this._patterns = this._initPatterns(this.options.patterns);
        this.redraw();
    },

    /**
    * Changes the patterns used by this decorator
    * and redraws the new one.
    */
    setPaths: function(paths) {
        this._paths = this._initPaths(paths);
        this._bounds = this._initBounds();
        this.redraw();
    },

    /**
    * Parse the pattern definition
    */
    _parsePatternDef: function(patternDef, latLngs) {
        return {
            symbolFactory: patternDef.symbol,
            // Parse offset and repeat values, managing the two cases:
            // absolute (in pixels) or relative (in percentage of the polyline length)
            offset: parseRelativeOrAbsoluteValue(patternDef.offset),
            endOffset: parseRelativeOrAbsoluteValue(patternDef.endOffset),
            repeat: parseRelativeOrAbsoluteValue(patternDef.repeat),
        };
    },

    onAdd: function (map) {
        this._map = map;
        this._draw();
        this._map.on('moveend', this.redraw, this);
    },

    onRemove: function (map) {
        this._map.off('moveend', this.redraw, this);
        this._map = null;
        L.FeatureGroup.prototype.onRemove.call(this, map);
    },

    /**
    * As real pattern bounds depends on map zoom and bounds,
    * we just compute the total bounds of all paths decorated by this instance.
    */
    _initBounds: function() {
        const allPathCoords = this._paths.reduce((acc, path) => acc.concat(path), []);
        return L.latLngBounds(allPathCoords);
    },

    getBounds: function() {
        return this._bounds;
    },

    /**
    * Returns an array of ILayers object
    */
    _buildSymbols: function(latLngs, symbolFactory, directionPoints) {
        return directionPoints.map((directionPoint, i) =>
            symbolFactory.buildSymbol(directionPoint, latLngs, this._map, i, directionPoints.length)
        );
    },

    /**
    * Compute pairs of LatLng and heading angle,
    * that define positions and directions of the symbols on the path
    */
    _getDirectionPoints: function(latLngs, pattern) {
        if (latLngs.length < 2) {
            return [];
        }
        const pathAsPoints = latLngs.map(latLng => this._map.project(latLng));
        return projectPatternOnPointPath(pathAsPoints, pattern)
            .map(point => ({
                latLng: this._map.unproject(L.point(point.pt)),
                heading: point.heading,
            }));
    },

    redraw: function() {
        if (!this._map) {
            return;
        }
        this.clearLayers();
        this._draw();
    },

    /**
    * Returns all symbols for a given pattern as an array of FeatureGroup
    */
    _getPatternLayers: function(pattern) {
        const mapBounds = this._map.getBounds().pad(0.1);
        return this._paths.map(path => {
            const directionPoints = this._getDirectionPoints(path, pattern)
                // filter out invisible points
                .filter(point => mapBounds.contains(point.latLng));
            return L.featureGroup(this._buildSymbols(path, pattern.symbolFactory, directionPoints));
        });
    },

    /**
    * Draw all patterns
    */
    _draw: function () {
        this._patterns
            .map(pattern => this._getPatternLayers(pattern))
            .forEach(layers => { this.addLayer(L.featureGroup(layers)); });
    }
});
/*
 * Allows compact syntax to be used
 */
L.polylineDecorator = function (paths, options) {
    return new L.PolylineDecorator(paths, options);
};
