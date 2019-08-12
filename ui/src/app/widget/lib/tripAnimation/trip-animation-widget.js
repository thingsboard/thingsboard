/*
 * Copyright © 2016-2019 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import './trip-animation-widget.scss';
import template from "./trip-animation-widget.tpl.html";
import TbOpenStreetMap from '../openstreet-map';
import L from 'leaflet';
import 'leaflet-polylinedecorator'
import tinycolor from "tinycolor2";
import {fillPatternWithActions, isNumber, padValue, processPattern} from "../widget-utils";

(function () {
    // save these original methods before they are overwritten
    var proto_initIcon = L.Marker.prototype._initIcon;
    var proto_setPos = L.Marker.prototype._setPos;
    var oldIE = (L.DomUtil.TRANSFORM === 'msTransform');

    L.Marker.addInitHook(function () {
        var iconOptions = this.options.icon && this.options.icon.options;
        var iconAnchor = iconOptions && this.options.icon.options.iconAnchor;
        if (iconAnchor) {
            iconAnchor = (iconAnchor[0] + 'px ' + iconAnchor[1] + 'px');
        }
        this.options.rotationOrigin = this.options.rotationOrigin || iconAnchor || 'center bottom';
        this.options.rotationAngle = this.options.rotationAngle || 0;

        // Ensure marker keeps rotated during dragging
        this.on('drag', function (e) {
            e.target._applyRotation();
        });
    });

    L.Marker.include({
        _initIcon: function () {
            proto_initIcon.call(this);
        },

        _setPos: function (pos) {
            proto_setPos.call(this, pos);
            this._applyRotation();
        },

        _applyRotation: function () {
            if (this.options.rotationAngle) {
                this._icon.style[L.DomUtil.TRANSFORM + 'Origin'] = this.options.rotationOrigin;

                if (oldIE) {
                    // for IE 9, use the 2D rotation
                    this._icon.style[L.DomUtil.TRANSFORM] = 'rotate(' + this.options.rotationAngle + 'deg)';
                } else {
                    // for modern browsers, prefer the 3D accelerated version
                    let rotation = ' rotateZ(' + this.options.rotationAngle + 'deg)';
                    if (!this._icon.style[L.DomUtil.TRANSFORM].includes(rotation)) {
                        this._icon.style[L.DomUtil.TRANSFORM] += rotation;
                    }
                }
            }
        },

        setRotationAngle: function (angle) {
            this.options.rotationAngle = angle;
            this.update();
            return this;
        },

        setRotationOrigin: function (origin) {
            this.options.rotationOrigin = origin;
            this.update();
            return this;
        }
    });
})();


export default angular.module('thingsboard.widgets.tripAnimation', [])
    .directive('tripAnimation', tripAnimationWidget)
    .filter('tripAnimation', function ($filter) {
        return function (label) {
            label = label.toString();

            let translateSelector = "widgets.tripAnimation." + label;
            let translation = $filter('translate')(translateSelector);

            if (translation !== translateSelector) {
                return translation;
            }

            return label;
        }
    })
    .name;


/*@ngInject*/
function tripAnimationWidget() {
    return {
        restrict: "E",
        scope: true,
        bindToController: {
            ctx: '=',
            self: '='
        },
        controller: tripAnimationController,
        controllerAs: 'vm',
        templateUrl: template
    };
}

/*@ngInject*/
function tripAnimationController($document, $scope, $log, $http, $timeout, $filter, $sce) {
    let vm = this;

    vm.initBounds = true;

    vm.markers = [];
    vm.index = 0;
    vm.dsIndex = 0;
    vm.minTime = 0;
    vm.maxTime = 0;
    vm.isPlaying = false;
    vm.trackingLine = {
        "type": "FeatureCollection",
        features: []
    };
    vm.speeds = [1, 5, 10, 25];
    vm.speed = 1;
    vm.trips = [];
    vm.activeTripIndex = 0;

    vm.showHideTooltip = showHideTooltip;
    vm.recalculateTrips = recalculateTrips;

    $scope.$watch('vm.ctx', function () {
        if (vm.ctx) {
            vm.utils = vm.ctx.$scope.$injector.get('utils');
            vm.settings = vm.ctx.settings;
            vm.widgetConfig = vm.ctx.widgetConfig;
            vm.data = vm.ctx.data;
            vm.datasources = vm.ctx.datasources;
            configureStaticSettings();
            initialize();
            initializeCallbacks();
        }
    });


    function initializeCallbacks() {
        vm.self.onDataUpdated = function () {
            createUpdatePath(true);
        };

        vm.self.onResize = function () {
            resize();
        };

        vm.self.typeParameters = function () {
            return {
                maxDatasources: 1, // Maximum allowed datasources for this widget, -1 - unlimited
                maxDataKeys: -1 //Maximum allowed data keys for this widget, -1 - unlimited
            }
        };
        return true;
    }


    function resize() {
        if (vm.map) {
            vm.map.invalidateSize();
        }
    }

    function initCallback() {
        //createUpdatePath();
        //resize();
    }

    vm.playMove = function (play) {
        if (play && vm.isPlaying) return;
        if (play || vm.isPlaying) vm.isPlaying = true;
        if (vm.isPlaying) {
            moveInc(1);
            vm.timeout = $timeout(function () {
                vm.playMove();
            }, 1000 / vm.speed)
        }
    };

    vm.moveNext = function () {
        vm.stopPlay();
        if (vm.staticSettings.usePointAsAnchor) {
            let newIndex = vm.maxTime;
            for (let index = vm.index+1; index < vm.maxTime; index++) {
                if (vm.trips.some(function (trip) {
                    return trip.timeRange[index].hasAnchor;
                })) {
                    newIndex = index;
                    break;
                }
            }
            moveToIndex(newIndex);
        } else moveInc(1);
    };

    vm.movePrev = function () {
        vm.stopPlay();
        if (vm.staticSettings.usePointAsAnchor) {
            let newIndex = vm.minTime;
            for (let index = vm.index-1; index > vm.minTime; index--) {
                if (vm.trips.some(function (trip) {
                        return trip.timeRange[index].hasAnchor;
                    })) {
                    newIndex = index;
                    break;
                }
            }
            moveToIndex(newIndex);
        } else  moveInc(-1);
    };

    vm.moveStart = function () {
        vm.stopPlay();
        moveToIndex(vm.minTime);
    };

    vm.moveEnd = function () {
        vm.stopPlay();
        moveToIndex(vm.maxTime);
    };

    vm.stopPlay = function () {
        if (vm.isPlaying) {
            vm.isPlaying = false;
            $timeout.cancel(vm.timeout);
        }
    };

    function moveInc(inc) {
        let newIndex = vm.index + inc;
        moveToIndex(newIndex);
    }

    function moveToIndex(newIndex) {
        if (newIndex > vm.maxTime || newIndex < vm.minTime) return;
        vm.index = newIndex;
        recalculateTrips();
    }

    function recalculateTrips() {
        vm.trips.forEach(function (value) {
            moveMarker(value);
        })
    }

    function findAngle(lat1, lng1, lat2, lng2) {
        let angle = Math.atan2(0, 0) - Math.atan2(lat2 - lat1, lng2 - lng1);
        angle = angle * 180 / Math.PI;
        return parseInt(angle.toFixed(2));
    }

    function initialize() {
        $scope.currentDate = $filter('date')(0, "yyyy.MM.dd HH:mm:ss");

        vm.self.actionSources = [vm.searchAction];
        vm.endpoint = vm.ctx.settings.endpointUrl;
        $scope.title = vm.ctx.widgetConfig.title;
        vm.utils = vm.self.ctx.$scope.$injector.get('utils');

        vm.showTimestamp = vm.settings.showTimestamp !== false;
        vm.ctx.$element = angular.element("#trip-animation-map", vm.ctx.$container);
        vm.defaultZoomLevel = 2;
        if (vm.ctx.settings.defaultZoomLevel) {
            if (vm.ctx.settings.defaultZoomLevel > 0 && vm.ctx.settings.defaultZoomLevel < 21) {
                vm.defaultZoomLevel = Math.floor(vm.ctx.settings.defaultZoomLevel);
            }
        }
        vm.dontFitMapBounds = vm.ctx.settings.fitMapBounds === false;
        vm.map = new TbOpenStreetMap(vm.ctx.$element, vm.utils, initCallback, vm.defaultZoomLevel, vm.dontFitMapBounds, vm.staticSettings.disableScrollZooming, null, vm.staticSettings.mapProvider);
        vm.map.bounds = vm.map.createBounds();
        vm.map.invalidateSize(true);
        vm.map.bounds = vm.map.createBounds();

        vm.tooltipActionsMap = {};
        var descriptors = vm.ctx.actionsApi.getActionDescriptors('tooltipAction');
        descriptors.forEach(function (descriptor) {
            if (descriptor) vm.tooltipActionsMap[descriptor.name] = descriptor;
        });
    }

    function configureStaticSettings() {
        let staticSettings = {};
        vm.staticSettings = staticSettings;
        //Calculate General Settings
        staticSettings.normalizationStep = vm.ctx.settings.normalizationStep || 1000;
        staticSettings.buttonColor = tinycolor(vm.widgetConfig.color).setAlpha(0.54).toRgbString();
        staticSettings.disabledButtonColor = tinycolor(vm.widgetConfig.color).setAlpha(0.3).toRgbString();
        staticSettings.polygonColor = tinycolor(vm.ctx.settings.polygonColor).toHexString();
        staticSettings.polygonStrokeColor = tinycolor(vm.ctx.settings.polygonStrokeColor).toHexString();
        staticSettings.mapProvider = vm.ctx.settings.mapProvider ? {name: vm.ctx.settings.mapProvider} : {name: "OpenStreetMap.Mapnik"};
        staticSettings.disableScrollZooming = vm.ctx.settings.disableScrollZooming || false;
        staticSettings.latKeyName = vm.ctx.settings.latKeyName || "latitude";
        staticSettings.lngKeyName = vm.ctx.settings.lngKeyName || "longitude";
        staticSettings.polKeyName = vm.ctx.settings.polKeyName || "coordinates";
        staticSettings.rotationAngle = vm.ctx.settings.rotationAngle || 0;
        staticSettings.polygonOpacity = vm.ctx.settings.polygonOpacity || 0.5;
        staticSettings.polygonStrokeOpacity = vm.ctx.settings.polygonStrokeOpacity || 1;
        staticSettings.polygonStrokeWeight = vm.ctx.settings.polygonStrokeWeight || 1;
        staticSettings.showPolygon = vm.ctx.settings.showPolygon || false;
        staticSettings.usePolygonColorFunction = vm.ctx.settings.usePolygonColorFunction || false;
        staticSettings.usePolygonTooltipFunction = vm.ctx.settings.usePolygonTooltipFunction || false;
        staticSettings.displayTooltip = vm.ctx.settings.showTooltip || false;
        staticSettings.defaultZoomLevel = vm.ctx.settings.defaultZoomLevel || true;
        staticSettings.showTooltip = false;
        staticSettings.label = vm.ctx.settings.label || "${entityName}";
        staticSettings.useLabelFunction = vm.ctx.settings.useLabelFunction || false;
        staticSettings.autocloseTooltip = vm.ctx.settings.autocloseTooltip || false;
        staticSettings.pointTooltipOnRightPanel = vm.ctx.settings.pointTooltipOnRightPanel || false;
        staticSettings.usePointAsAnchor = vm.ctx.settings.usePointAsAnchor || false;
        staticSettings.showLabel = vm.ctx.settings.showLabel || false;
        staticSettings.useTooltipFunction = vm.ctx.settings.useTooltipFunction || false;
        staticSettings.usePolylineDecorator = vm.ctx.settings.usePolylineDecorator || false;
        staticSettings.useDecoratorCustomColor = vm.ctx.settings.useDecoratorCustomColor || false;
        staticSettings.decoratorCustomColor = tinycolor(vm.ctx.settings.decoratorCustomColor).toHexString();
        staticSettings.decoratorSymbol = vm.ctx.settings.decoratorSymbol || "arrowHead";
        staticSettings.decoratorSymbolSize = vm.ctx.settings.decoratorSymbolSize || 10;
        staticSettings.decoratorOffset = vm.ctx.settings.decoratorOffset || "20px";
        staticSettings.endDecoratorOffset = vm.ctx.settings.endDecoratorOffset || "20px";
        staticSettings.decoratorRepeat = vm.ctx.settings.decoratorRepeat || "20px";
        staticSettings.tooltipPattern = vm.ctx.settings.tooltipPattern || "<span style=\"font-size: 26px; color: #666; font-weight: bold;\">${entityName}</span>\n" +
            "<br/>\n" +
            "<span style=\"font-size: 12px; color: #666; font-weight: bold;\">Time:</span><span style=\"font-size: 12px;\"> ${formattedTs}</span>\n" +
            "<span style=\"font-size: 12px; color: #666; font-weight: bold;\">Latitude:</span> ${latitude:7}\n" +
            "<span style=\"font-size: 12px; color: #666; font-weight: bold;\">Longitude:</span> ${longitude:7}";
        staticSettings.polygonTooltipPattern = vm.ctx.settings.polygonTooltipPattern || "<span style=\"font-size: 26px; color: #666; font-weight: bold;\">${entityName}</span>\n" +
            "<br/>\n" +
            "<span style=\"font-size: 12px; color: #666; font-weight: bold;\">Time:</span><span style=\"font-size: 12px;\"> ${formattedTs}</span>\n";
        staticSettings.tooltipOpacity = angular.isNumber(vm.ctx.settings.tooltipOpacity) ? vm.ctx.settings.tooltipOpacity : 1;
        staticSettings.tooltipColor = vm.ctx.settings.tooltipColor ? tinycolor(vm.ctx.settings.tooltipColor).toRgbString() : "#ffffff";
        staticSettings.tooltipFontColor = vm.ctx.settings.tooltipFontColor ? tinycolor(vm.ctx.settings.tooltipFontColor).toRgbString() : "#000000";
        staticSettings.pathColor = vm.ctx.settings.color ? tinycolor(vm.ctx.settings.color).toHexString() : "#ff6300";
        staticSettings.pathWeight = vm.ctx.settings.strokeWeight || 1;
        staticSettings.pathOpacity = vm.ctx.settings.strokeOpacity || 1;
        staticSettings.usePathColorFunction = vm.ctx.settings.useColorFunction || false;
        staticSettings.showPoints = vm.ctx.settings.showPoints || false;
        staticSettings.pointSize = vm.ctx.settings.pointSize || 1;
        staticSettings.markerImageSize = vm.ctx.settings.markerImageSize || 20;
        staticSettings.useMarkerImageFunction = vm.ctx.settings.useMarkerImageFunction || false;
        staticSettings.pointColor = vm.ctx.settings.pointColor ? tinycolor(vm.ctx.settings.pointColor).toHexString() : "#ff6300";
        staticSettings.markerImages = vm.ctx.settings.markerImages || [];
        staticSettings.icon = L.icon({
            iconUrl: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAKoAAACqCAYAAAA9dtSCAAAAhnpUWHRSYXcgcHJvZmlsZSB0eXBlIGV4aWYAAHjadY7LDcAwCEPvTNERCBA+40RVI3WDjl+iNMqp7wCWBZbheu4Ox6AggVRzDVVMJCSopXCcMGIhLGPnnHybSyraNjBNoeGGsg/l8xeV1bWbmGnVU0/KdLqY2HPmH4xUHDVih7S2Gv34q8ULVzos2Vmq5r4AAAoGaVRYdFhNTDpjb20uYWRvYmUueG1wAAAAAAA8P3hwYWNrZXQgYmVnaW49Iu+7vyIgaWQ9Ilc1TTBNcENlaGlIenJlU3pOVGN6a2M5ZCI/Pgo8eDp4bXBtZXRhIHhtbG5zOng9ImFkb2JlOm5zOm1ldGEvIiB4OnhtcHRrPSJYTVAgQ29yZSA0LjQuMC1FeGl2MiI+CiA8cmRmOlJERiB4bWxuczpyZGY9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkvMDIvMjItcmRmLXN5bnRheC1ucyMiPgogIDxyZGY6RGVzY3JpcHRpb24gcmRmOmFib3V0PSIiCiAgICB4bWxuczpleGlmPSJodHRwOi8vbnMuYWRvYmUuY29tL2V4aWYvMS4wLyIKICAgIHhtbG5zOnRpZmY9Imh0dHA6Ly9ucy5hZG9iZS5jb20vdGlmZi8xLjAvIgogICBleGlmOlBpeGVsWERpbWVuc2lvbj0iMTcwIgogICBleGlmOlBpeGVsWURpbWVuc2lvbj0iMTcwIgogICB0aWZmOkltYWdlV2lkdGg9IjE3MCIKICAgdGlmZjpJbWFnZUhlaWdodD0iMTcwIgogICB0aWZmOk9yaWVudGF0aW9uPSIxIi8+CiA8L3JkZjpSREY+CjwveDp4bXBtZXRhPgogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgCjw/eHBhY2tldCBlbmQ9InciPz7hlLlNAAAABHNCSVQICAgIfAhkiAAAIABJREFUeNrtnXmcXFWZ97/Pubeq1+wJSQghhHQ2FUFlFVdUlEXAAVFBQZh3cAy4ESGbCBHIQiTKxHUW4FVwGcAFFMRhkWEZfZkBZJQkTRASIGHJQpbequ49z/vHre7cqrpVXd1da3edz6c+XX1rv+d3f+f3/M5zniPUW87WulAnTnN5i8IBYpnsttKoSZoVpitMFssoIIbSqIILxFIvVVGsQjdCQoSEKvtweMkIrwh0+gm6fMtWA7s2XicP1s92/ib1U7C/TfmCThrXyvm+5TgnzgwRWtUyDksL0GJigIJq8LevaWFnWMz+YzYJCHsRusSwHej2etiA8HT7Klld7406UAFou1yPdVyOclxmWZ8TRDnMaQT1AQtqU4AsBIxFOPsigAnAbBzwulAV/kdiPGK7+RuWJzaukUfrQB0hbfYSXRlz+TDKAWo50LiIpoBZEjAOoWckBVybRMXwCg6bvSSPta+UhXWgDrM293I912niSJvgTLeJ6eoFrJk1fNdAT4mAOCAueF28JA6/tsp/bbxWbq0DtUbb/EV6tTRwuvWY5bg0q59izeHScSYArfXpFHjB8/hd++rhy7TDCqjzlupFOJxshNNFQL0aZM7BNAPGDS5Ea7kf5ZcbVsh360CtsjZniV7qOFyIMscYYtYbwdGxC+qhEuOvtosbN6yRb9WBWsE26/M6PT6Bi9VykRNnnCaH19BeDGlg4uD3sAfhRr+Lm9vXyp/rQC1ngLRElxuHCwSmo3WA5u3glO0FvOz73LZxpXylDtTSa9ArUL7gNDDJJkaA9ixyT5sY2CR71GXdhm/I1+pALXKbvUSPcR1+KMrhUGfQoQZeKZZ9xvNY3r5C/r0O1CG2Q7+ih8UbucaJc5r6qVmjeiuuhk3wB7+Hq9u/KQ/UgTqYSH6xLnDiLBHlIPXqwCoZABwAdvvKDzdeK4vqQC00ml+o74g3scqJ8UG/p65Dy6pffdYrfG3DNfKLOlDzB0vnAd82hnH1Yb4CYAgmDfZ6Pt97dpUsrgM1CqSL9U4nzketV2fRSiPCaQCb5DGviy+3Xy+P14EKzPyqvrWxkR8JHF5n0erSrtbnDQsXtK+UX1WBWVFBFl2klzY28l91kFZfUx/EMNZ1+eX8ZfqvI5ZR5y3V60S5VAxO3RetbikgDvge921cKR8aMUCdgTY2f50bjeFT9dml2mmp7KxNPQk+8dwaeWJYA3XM+Tp22sHcCby77o3WpiuA8LyX5JL2lXL3sATqmy/Vw/0mfua4zLOJeqfXcpClSg8+X16/Sn4wrIA6c6HOaGjgYWOYXg+ahgFYDSh0+B5fb18ta4dF1D97iR7TGOePxqmDdNg4AhZEaXHjXD9vsZZl2rWkjDpzoc5ojPNHcZhSB+nwdASApK98vX2lrKpJRp19mR7REOcRcesgHb7UCkDMjbFy9mJdWHNAPfgcHefG+Llx6plPIwGs6oFruHruIv1C7QD1Q9rScih3mRhz6kw6csAqQpOJsXbOUj27JoA67zhuEuH4ugU1IgMs13G5fvYifU9VA3XuIr3ecfh4fbgf0WA9yI1zc9UCde5Svcw4fNl21zusigKdsk9RqwcCM+cv0UeKbzAMNcL/qh7jNvKQKA31BJMKAbIiPZ/n7V2wPndsWCFnVQmjaizWxL8IdZCWDZjhWz9PK+h9SsSsjsuZcxbr/6kKRp23VO82DifVdWllmFMHwaoiZWJYAYR9fpIz2q+T+yvGqLOX6medWB2kZWHQ8CHdf2Mgt4zXD4yCB/fdBVqdGEMu2DZooM5ZrEe5wlqbrGOpHABNA2cuCdAPQCPfs1iaN48TYGLMnb9kaKsEBg1Ux+VbIoyrJz6XB6D5gKi6v5R7rltOAOdi1yI22wMI581dop8vK1DnLtbLnBjH12eeShAgZQK0AHAWMuynsWeEdaUlJhwRYo7DFWUD6vzL9VjjstDvqeOrlPozckhnP0D7HrMFANWmAzYnWEssAcQwdd5SvbUsQCXGchEm14f8EutPMtgwDNDe4zbErDbi/xyyoY9doy6IEjabBGM4c95i/VRJgTp7kZ5iYpxYj/KLBFAigGQzwJl53EaDtk9v2pAzpMGNXolgye2fankkANDAICRA4e7ZRRqbP4mnBebVjf1BADTz3xxDsBId7IjsB6HmYUBV6PRhaweQTPVwHKa3QJOb8lBDtz5PVUJokBAwSuCvOo2Q6GJp+ypZWXRGnT+eq0XqIC16gNQLRiJYLxQsaYhpsxjWBgUjXu+EriScMEO5/0seO7/Xw+vfSfDgFz2Omqbs7AEbEWSRJ8gqRfN7wHG5+NBFeljRGXX+Mt2DMqquTQfBoLkAEAaepMAXAm7acBwGV+gxa2FnAt7ohCXvt5zxLp+j59v9gVaIjq7+qcsNDztMaKBvw7U+Zs1g1TSmLYULEGyK8eP1K+W8ogF13lL9thPjS/Uc00GAMwdb5Quc0t5Dyd5/VQOAvtQF05vg9MMtXz7d46DJGoDPy9HTDpx7fZyHXhBaYukyQDKH/hIDtfdCSexmzKZ1smfIQJ37FT3MaeVe9ZlaZ9Mh6E+JAGcOzam5hmcLnoUXOgKWXHuKz6nv9Jl9UIpm+/O1XfjLc4bDvhFj7vhU75t04JQNqClWxfLrZ66VM4asUZ1WLhTqIC04gs8VJNmQtrTpGjOsR9M2De7do9VCTwJe7IAJMeWGM3xev6GHr5ztMXuaBgxayOSLD9MnW2jcL4V7v59IDm1awn5XHxTe/+ZF+lH6v8b6eTPL57UO0jRmzDmjk3G/71+bgzEz9ahNj/pVIeHDi7vgiCnK6jN9Pni0z6RxClaih/j+WEygwQn9ltDvCGdV9T0spT2fxmG0H+PTwF2DBurcpbrKuDTUtWkejRkB0KzhPAKgaWC12c/fm4BOD46aqPzgfJ/3HuHT0pwCvTdI9BjYuVvo2QNmUuozK1wh13pgXM4GPjGooX/Ml3SsMZw9orOj+kuRy9SSmWZ8hknfZ0PZ0FAf2oLd+tDZA5tehaMPVH55scc9KxKc/E6floZUkDQUe1DgX37vMra1/xG9nPgVgbmL9ZZBAXVKMwtQZo5YbZprDr6QKD4jcaRXo6pNacmI+zu6oCsBRx6kPLkyyS++nuADR/nEnRRAh9oPDjzzvPCTxw0T4tV1qm2wGuB98y/Vtw146BfhNJERVr40XwRfqMWUaStFBVSpm29hdwJ27oUFx1s++xGfo+bZvsAHr7i/7aZ7Xd5IwiQ3B4NKRc/7NJo5GXiycKCepA1uA8f43SMYpPkAGiELMhlVyW0x+Qovd8DsFjj5cMuiT3ocNCmPBzrU5sJT7YZvPmRomxj6nqHxVCWVF2Aqev4vAK4tGKhzjuDGEbGVeCEeaC6AQpYRr1HmfeiW9GFzF8QsXPNhnzNP8Jk1zQYo8YeoP/OJTYVrfuYydUzEHL5UAaOmRhuniVkzLtW3bV4rT/YL1IkX6VTX5ehhnRSdI5ljwBF85nNs9tAvCj0ebOmEd05SLn6v5eIzPJqbU8/xSowOB+58xOHeF4QpzaQnn0Qwa0W7JQmNjVwJnNEvUCeM4yz1aBsR4ByIBo2ymMiO+sPP7/HhxT3QNhZuOsfntHf5jB+txdefedjU82D1r1zGxDIAGpo2lYgovOyhP4Hr4bgc3vYFnbVpnTyXF6gmzvtNKhIbUUN8Ln80l0kf9kAJ2VHAvgS8koCTpiurzrJ88gMexqQeL+d5deDWe1we2wazx6a+s4kAYoWAGdVP6nNIbBR/B6zJC1T1ONXKCARoLv0p0UN6VnRvoSMJr+yAjx6mXPABn1OO84nHGbr/OZhmYMdu4Ya7DYeMCmRweIiXzGE/pWWlwjLAGLDKu/ICdc4iPcltJFbz0X4hETwR0XkOiykzMNKMpJLXuiAmcNocy4Vf9HnXYX5AW0Uf4m3hgtLALb9z2LBHmNYaETBJP4xaIbKyQWx0Wl6NKvC5mh3yhxIghSLPXGDNnPK0Fnb1wK7XYeHJljOO93nX4ak3KHqA1GvAFvi+Dry2Q/jyXQ4zR2Xo0ah0vioKqNBgBcCcJXpyeIugNKA6DcytufVQ+QDaX4AEheeBhkz75zpgZhP8wzGWBad5zDgw9WDRz53N+IEFokmUq2+NMTYGTig5Oi0puj9GrWCzHhjDRUA2UOdermepz4HDRn/2asvQWqO0/id/Hmjmfd8GFpPtgG9+3Ocjx1jePDMVSZUcoAMAqQtPPevwyz8bxjdGsKeka1IM6V5qFQBWfTAucyN/vWniMOMwuurnTAvNA7URw7nNON6bWmez80B7n5v04Lm9MDEOaz7qs/OmHhZ+0uPNh9jC80AL/mF+6qbZPlMh6EldlNff4dKtQWACQYAUNfSH0/qkygJo9Tho9mX7l1X3MaomOUqdGg+QNOOp/eWBRllMqeNJH7bshrGNcOunPd7zdstBk+yg80D7v/I0PwIL1KaPP2O45XGhbXIGADOCqKzhv0rYtPeUSIzWuOHgLKAKvBO/RgHajwYtKA80BerOBLyWgBOmKDec63PysSmLaSh5oDl/WKGeVWFsmkjAFTe7HDSB/ctMQktMcrGmSHX2u6+8F1idBlSniTF+Z40yaH9ZTOTIaArJgG4PXt4Fxx6s/PhTHke/2dLaTAkspoFW0C1Qmzpw76MOj74qTGkJsabJZtaqZtPwaGh5exqjzlusl1RNtD8UBu0HoH0gDUmCnSnP+H2HKEsu9ThyfonS7AbEoAPXpns74Iu3ukxozLG6NAc4RaoQpCnycJuZnHa5qnJkxQtLDCaTPqoMTub9zIVyPvg+vNEFm16Bk2db7v5qktu/lghA6lGcROU0ahhsalShbKrccp/LC3sJEq0zI/ywNiU3aKuu+dB2uZ7Vx6hOA2+pmD4daKrdQBk0dN8qbEnlgZ5yhGXhxz0OmZpKsyv6PLwdItoLD6Ce32ZY9zuHmaPJLtcTWvosUal9VC9g1YIjnADc7o4/V0cDLRVZadpfRZFCE0U0O2rvS7MDEh5s7YZkJyw/2XL2CR7zZqQAWvQAqVg1xk3BeP75Ay7rO6CtdT8wpR9zX2ogn0N9MHHeCuCOO5B3qs8BVQ/SXACNiu5TN8+HF/bB8ZODPNAFZ/i0NGsJ8kCLvcVI4Wy6eZuw5HbDrCnsz4yKAKfkGuqrGLAaJMmMAnAdw0QsLdUG0pzLPfph0L480L2BsLn5HJ8Tj/GZOlFLMLyXag+cwtn0+p+5jB8XXZVPckX2NZQdpz4T5izUo1wnxlRjaCjbsuhCQNrfco8IDSopD/TlLjjlEGXFmZZPn+jtl4sVj+CLDFIXHnzCcMvTqalSs7/omeRi0xrRphmnehQuB7omRpwKRfwDAmnofuaxTg+27YB3HKx873yP971NGd2i0bORVcmgg6E7Zd2dLrEQQMPaU2rMjspzukepMNm1SeaUTVhr4SDNXNaRWSdUCNbCJy2cNVf57AKP9xzhBz1QFR5oadn0rkddfvlXoW0S6eZ+ZqHeGhzu085IHIkrY1yxTCt3LmLO3TkiQBoubqsEeaB7ErD9NTjvnZYFp/q8Y57FdSlhHmg5mhT8tI4u+KffOEwfF/3SnKxZi2ANCKrBtTDKKBVCKv3P0YfK4mzphMkNcMHbLQvO8DikNw+0qAxa6uF96JH+bQ+43LdFaBtN3jS+rOK8NQjY1MTNdFdgTCWCqCytmiOC9yxs74K9b8DXTrF8+oM+cw8uZx5ouUBa2FRpMgGX/bvDjJZU0Yio1L2It6opbZqBGwvTXFUmVpDSs4EcKvm9aR8cNU656F2Wi0/zGdVcSxZTCbSpAyt/FGO7hbEZEb5kAFpqZZq0kLOjtLrAmHL2keZi01Amfo8Hm/fC9//O49TjKpkHWl0g/dvLws//ZDikaT+bZpFyPjat0abQ6EoBxXxLyqQZx5MWxjTA35YmmDlVK5wHWl1B1Lf/3WVzB0xpyWPw51MStRpMBTsPVc2XQYDnd8FDlyaZOdVW+TRnee2o514S1v3BcOiUbOBlDvsow2LID3VdY0WB2hfpp4b9Tg8+dZhyzJss+FJEgFbr5liFr4O69Icxpo7P/Qsj1z9JDQdRGZeqW94LI6KfQlOg3RbmTbU4Eiyqq80IvrhDvuvAfY8bHnhOmNQcKg2Zq2iEpvQrGfdrlUwDfLhuNX0pA3T7sGNfEPXXd2KBmAvPbRMSNg+2q6DCSckptdwcErnVdmoPpgYDL7welLl2JFUvaUiwr0ZdmnN8yckoJx/v85M/OLywT4i7RFt7IW06nLAqAQ48U+kv0XuiRaDZhZ/+1fDU84bGeLEuDRN4O1VTs2ZgYLUKY5ph6bkeL+6KYE4NFTzrHe5DBDBMQOtVR+/J/m47ZBR8/TaX13ZDU0MxPUCpQsAWxqrdCTh6nuXEucreRIaJkUv7Z4K2lmWU0G20vBU7c2eap05w3IFtHcJblzdwx8MO+7qFxpIBtgoKgg5AAnztk14wFObYoSVqNUQaRrUmQQrQZYDdlewvidg7vsmBqY3wj7e5/P0/xbjlPgcxSlPD/jI1xZMFlWbYwqwzz4fDZvmcNM+yJ5ED5/1tE1mjOkCg24iwvSJTbPlmTySoQjejBTbvES75lcsBFzfyvbtcXtkZMKxTVMBWmmELozrPE5adm2R7935WTUviyShhhA7qY6oRqfucie++6jMiTCv5j5ACgioy7EEBIzAhDuMa4M71hgefMOzYYXjTDGVsaxBsFGcFbSWzOAorUKoKo5sh3i38/lnD6Fjwst5zJVFZ/qGTWovmvwT7tv7BGNhb9i8e5ftF/BX2E50xML0Vunzh+48Y2i6Js+ymGO0vGmKxwG8sriRwytyjhUmApA+fOdHnTeMUL1RsI22TYO0nkKohZpWg7190Jr73qg+IcHg5Zxmj9jqSHI+Hk38l9X+zC+Nb4bEtwq8fd3h5i2H6ZGXaxFAZypLqk1K1wlh1bCvYhPCzpw3j4xmsmqH9s5Kna4xVJSCg3xkTo71sGwzkmI8mPHT1nmCTflzCx1O3A5qh0YVfPmM4bmmc89bE+X/PGHylSD5suRm2MLbo7oHPnOhx+ISguFvmVpbkWFZei80m0ESC3cYmSVTqR+QFK+kFaMMgFbP/vnFgVAO0TQ0Y9uwfxPjSd2Lc96RDU6MScwOdWzzGK6UXWxiiep9x+WkeryTS91jVPHsU5DtetdaUYa8orxo/yTabpKci+rQAZs1i1NCKSwkvFTYwuhEmN8FjLxrO+o7LsYsauPuPgRfb3FgrXmzhdtWJR/ucMcvSldoeKLy9eppWzbgGaspXFfbisdVY5XUMHeXdoL0wsPYBtpdJQ/9LmGGdEHgdaIrD7Amwr0f4zI9d/uGGGDff62IMNMRrwYstgFVT+RALTvPZuivipREbZQyAtKsp6t/Rfr08buzz/FEcXqsIrecCa56s9TQ5EAoacNLBqwYaYtA2Cp7dJXzxDodJX2jgxrtdXn9DaKpqL7YwVk0k4W1tlgXvsWzvZH+Jd0vezYVrxQGQIBd3L4D5222yG+iomOkfBdYMwPaB02QcM+mPp/0f0rFxF9pGB1vuLLzT4fxvxbjuZzH2dAtNcXCcUjCslJxVIaj1es77fcbEU6mRNkKrRthTWgOsKgasx9N9fojfw18qNpMo2W6A5NhdTsgB2FygDoEVBxwX2sbA9h5h3SOGWV+Kc+WPYjz3shCPQ6yqAFu4Vj1ituWcYyzbutLBmetv1qVQpaAVB/B5oI/T5i3WS5wG1tlEFXy7ge5+EqHDBlLY9/VOOKAJ3jPLcvHpHrOnKUkv2FequKwzmExwKUj/GgN7O2HW4gZmNgcXZNaoE958ArLyK6rRV3Wa4S9XSN+8DxtWyXekWrbuiWBYovRrmGEzCCxNBmR4r2nWloHJqUIO92wwHLkszvnXx3ny2QAcDbFi9t9grK3CgG0tjBsF3z/T47WuCLtKcwdS1cqqYsDr4tXw2QuGkG72VNX674xSNJGSIGO4TwNtoYB1Ai+2tRHaJsGjm4VPfC/GBWvj/GmDwXUpshc70LzYwiXAh4/2ees4xfOJLjqXY6q1KlvQb09kARXhUapxQ7QBaNi0gCyDYbPA64Qsr9BzxzQGCTD/+6rwkTUxPnRFnAeeMHT0CM0VyYstnFUnjoavfMxn8xukZVRpf/mq1TgJIOA4PJQFVHF5vKorahQC2DDDEu27Skin9bkDmY85wbA/eyK81il89qYY562JccfDDr4GLoJTVi+2MFbtSsB73+rzoVlKRyKHl1oLCSsC6rPPT7AlC6i2h7+oz56qT1bIBdh8myqEY5IMSZCpW8OTB2qCnIEpLbBln3DBj1wOvbSBnz/osG2n0NqkRZ48yMWwBU6tarC8euEZHj02VL7TZgeVmcFptdlV4vLShlXy0yygblwlt+GwlVppmYAlArDh4Z50cGZpWzIAm+HHxlxoGwtTGuHLd7icc32ctbfH2NspxGMBQErLsAWmAXpw9HzLBzNZlYj7uRi0wqAVB2yS9kzJut88TrBRHGqr5QMsRG5WK1FgzvRmnQgv1gQlO6a3QoeFlfc5zPpinHW/cNmwxdDSVGzAOqRnbRWuV5d+MskrIQdAbcQkQD4rsJJxlAt+kn9Js6rC/0x691X7nDjnVM12k0UAbL+TBxkyIW1yLCoZJvV8IzA6DuOb4Z52w73/47DlJcOMKcqksYH1ZW2xf1xhRaWsBnaV2Sc8/LyhOR4aMcgIOsnI/620r5oqYbRxlZybeQbS2rylmhCI1XyVEo3WcFmP5UmLy9pHgBz+pA1AuTcJr++Az73PctqxPse+xWIUepLl//mOA6/uEs5aHadHg0kACedEZASa4dTK3OgoD5tay13rr5XTcg79qS93t3Go/Sa5Ay+J2pQhc8MGMnIIInRreLrWuIG11XYg3Pa04bx/jnHxt2M8/BdDQzzQuOV0VXwfDj5A+ex7fJ7vzNakCumrVntr1FaYoGwgUR6NslUzWed+6zO8Wq5tbCJuWfkEmZWdw5o2c/Ig9fzxTTC2Af5zs+G0tTHOXhXnnj85eJYgL7ZMP7srtRJg3mhI+BEGQrVZU8F53ZzYxy+yRojMA+OOuGprrJnTVRkPwxuw+QKvKI2btcqT7H1HwytCY06gYbfuFm59yvDE0w7WE950iO0LukrNYA0xmNgIv/hfQ2ssu5R62grWCq+tMg5Yy8PPflO+2y+jblonL3k9/Lc4DN8mEfjNMXkQmcQdISH6ZrucdJsLA80NcEgLbNol/ONPHY5b2sAt/+GyuyNYeWBKCAbPh1OO8zl2qtLjkb9oRaVtqTgkLcsL6LJUu1Ib32Lo8rsZGW2ogVfETI+GCkJkmu0JD7Z0wzvGKae+zXLhSR4tTfRtNFwKVn3sGcMp62K0jUvX1mlBVYi6ys2oYkCV59evkEMj2TbyVcul2+vh8WHNqgPVsOQJvDLzYMnOiQ0zbDwWbFm+o1tY9aDD/MsauOF2l82vCs2NWsTp2aD1JOGYeZbzjwiVA8oo/yNK7pJA5dKnws05ZUHO1/ncqSOtkG7E5raRU7S5Aq+MGbC0mS+TvfLAjcH0liDV8Nv/6fCptXEW/iDO1p2C4xSzqEbQLviQT4MTrYuVyoIUYWuPz29z2m25Hph4xFV/Nc2cjTKWkdhyMSwMaPIgHLykJSyHcg+MQGsMPIU/bxPW/s5hx6uGyWNg2qSAYYc6eWAVZkyxbNlqeHKb9O3ekFWkrgIJ1RID3+O3z66SdQV2R3qbt0zXOC5frYrM/2rVsGTr1SwdG7F+KefKA4KJht0JcAXecaCy8GMebzrEEneDufzBjnSuA8+/Ihy1Is7c8Snr1EQDNW0lQBmA+sw3MCkBMrChH2DDtXKZeiSGa134oWrYnDkFOVyEyERuZ78sCN8f2wgtMXhqm3DCiiCR+8EngjTDwRY49i1MHa9MHg1+lWzxY1xQy+35QNovUFMn+ofGod4iNGx/1lbm6lgyVsmmadrwzFcqD6WvRsEkeOoV4e9vdvnEtXF+80eHhCeDyotVpLDXaHnOp1X2+Elu6RfQ/T0h0clNVnl1xLNqPyybL5Eb6T8vVjKcg8y82JY4TGgK8mLP/VeXj14T4/b/dNi1D1qbtSAv1jGwfTds3R0Ur0DyXIBSesCmSko+1L5afj1koG5aK09iud3E6tgcEGBzDf1EMGzGsu++93Ci82Jnj4Od3cLCO1w+fl2cdb+Ksacz0KD50gwb4sq/3e/2LWgMa9C+DStkIFHM0FvHTs4fRGybu81fpvtQWup7Pw1s2MyqsicEEwG9mxSHvctQtpYSHXhpaheU3mO+wvNd0OrCkvf7fPhIn/kHK109gSbtZdLGuHLrH1yu+I3L2DjpeQu56n+VEKgSZEn9dMO1ck5BgWDh/gbfFYfL1a/jsKDLP7S/a3jXl75xTFNACWcthf6XEEh7mVY1dbz3mAbx1+yWALCrHnC48TGHk95s+cz7fQ4cb1GE7Xvgxvtj/PzPhjHxDCAWEDiW6Pxs007WDtItzN0OvUjHxCfx3wba1NaxOBR7K9/CuvC28FkMG7K7sgz61H2rsMeD1ztgythAi768ByY3kw7SKL80qrByCUDrNEJPJ8ufXS1XFR2oAPMW65lOI7fbnjr2hgrWNMBGgC6rAG+uWlI58kjDOO/dBVEiAieRfqy1UgRQlk3PrJDZA7KxBvLkDavkDj/B/eLWcVcUayvPCtqo6dlwjYK0fIPQe4Xf06T2PtAcn5MXpKW7YBOe8o2BvmzA6Q++shxle92uKqJTQP/5BGnLu6McA0NkGXnppzByOSL7PrDFwCp3ta+UHw/mlA24zVmsV8Sb+MaISQOslIbtTxbkkRW5ejmytGeux4p5bQbM/tr6a2TyoEA+mBe1r5KrveQISgMsN8NGrHoNPzdrZYGJZuPMYV4KMfhLdS0qvk2yYtBsPNgX2i4uVa0OvmekAAAFN0lEQVTs9pTDHbBpOjIHGDOH/MxJg3xpijlBWuQ+NQ2A8JONq+WGoZyaQbf5y3SBcfnuiM+uqrQsKDThOV+F71IO+coL61fIzCGBfSgvXn+tfM9P8EDdBSgxyxItC6Sf+XrILQHKAVICW6zTT3LxkFl5qG+wewefUFhf16tlkAQRQIoEbmjZS+SK2gIkR7GifJRl7Wvk7iJfr4Nrcy7XD7hxfq8WU88FqKw0KF+v9/MRLqjH3etXyilFAX0x3qT9Ornf97myjpoKs22eNMNybp6dAunjxQJp0YAKsHGlXKPwfaexjp1q0rRl//hAAr68ew8nFVVGFPPNNqyQBdbj7npwNUKvkZSpn0jw+a3flR1VC1SA5D4uUsvTUk+0HpESxPos2bRa7goyDKoYqM9+S17u6OAj1uOluhMwouSG53ks3rhCbuRKNf0t1qs4UAG2fFu2+cpZ1rK9DtbhD1ITA2u5qn2VrC42k5ZFerddprNcl8eNYVw92Xr4Bm5quWrDKlnOlWq4CkVEawqoAHOX6PEi/MYYxtaXsQw/JvW6Wb6xL1NfpdhDflnNjLbLdFbM5U9imFBn1mGAUQELSfVYvvE6ubYcn1mWPaU3rZHnSHKKtWytW1c1DtLUigG1LCsXSMvGqL1t+iV6YOsY7hXDW2py55WRDtKggssryR4WP3ud/N9yfrYp54e9+B3Z2rOXUxTurc9g1RhIXVDhRT/JBeUGadkZNdzmL9MfolwUaPA6EKq5GRd8jz9teIr3co9UZA1yRWeG5y3WK43DVSmLo96qMbIPdtG7c8NKOb2iF0slP3zDKlnueZyMQ3s9yKpKPdrhWRZUGqQVZ9Te1vYFPcht5SdOnHfbZF0KVHyobwCboN3zuOTZ6+Q/qoTcq6fNXarLHMNioLU+OVABMATrm5IKP96wQv6+ylRIdbW2y/REt4Hr3BiH+z11di0bi8ZAfbb5HldvXC3fr0K5XJ1t7hJdaQyfM4Zxtu65llSLKnSrzx0bVsqnq/ZCqtYvtnGlLFHlE1Z51DRU8zet3YjeaQSrPOMluaCaQVrVjBpusxfpZ43LVY4wQ21dDgy1x1Ms+hIJ1q1fLdfVyNeujTZpgU6ZNJ6volwiDg3WqwN2wAANdiBBfW5KdnP9c9+Sv9bQ16+9Nm+Z3miEUxEmqV8HbEEMannDWh7ojnPu5uXSXYM/ozbbrK/qcfEGLgQuMA5OnWGze9a4QQkg9fmZ7/Oj9tVyTw3/nNpvc5fqdxzDqcAMgJHswfYu/VF40U9yX/tquXCYXHfDo02/XA9sjfE54GNOA4fZZAqwI4FlBYwTaFC/h3Yc/i2xj9/WkgYdMUBNcwm+otPcBtaJy9vVMkOcYQjaFDitD+LwgrU8Tg/f2PBN+cswvRaHb5t5qc5pbOIjwIeBE00cV72QNNDa66k+5uzGF5c7reWRnje44/nvyuZhPmiMnDZ7sX7MgQtNjNlqOdA4jFIF/MHv2FzSzknt3oeAeuwTl5d8j/We5Z+fWyW/G2Gx4Qhsp2rznLfw0VgDb7IeR6rleLeJMWqBwGfMvbVOCXsgbdNeB7wutiP8t+PwB7+LLRvWyE9HsIlRb72tbbH+o1jeEWvgbUCjWg5AaRZDS2qD2bTtHiHP5mahs5tZO79vJz4PrNJhDHsx7ETZ7Sd4Rg0PDWbnkDpQR2ibc7l+QAzjxXCQG8OxPgep5WCECWppQhiD0gK0oDQBjgiuQhLFqtAp0IWwG9iH0iGGXeKwSYTtNkmPtWy1Pq88u0Yerp/x3O3/A6qXxURxUsm4AAAAAElFTkSuQmCC",
            iconSize: [30, 30],
            iconAnchor: [15, 15]
        });

        if (vm.ctx.settings.useCustomProvider && vm.ctx.settings.customProviderTileUrl) {
            staticSettings.mapProvider.name = vm.ctx.settings.customProviderTileUrl;
            staticSettings.mapProvider.isCustom = true;
        }

        if (angular.isDefined(vm.ctx.settings.markerImage)) {
            staticSettings.icon = L.icon({
                iconUrl: vm.ctx.settings.markerImage,
                iconSize: [staticSettings.markerImageSize, staticSettings.markerImageSize],
                iconAnchor: [(staticSettings.markerImageSize / 2), (staticSettings.markerImageSize / 2)]
            })
        }

        if (staticSettings.usePathColorFunction && angular.isDefined(vm.ctx.settings.colorFunction)) {
            staticSettings.colorFunction = new Function('data, dsData, dsIndex', vm.ctx.settings.colorFunction);
        }

        if (staticSettings.usePointAsAnchor && angular.isDefined(vm.ctx.settings.pointAsAnchorFunction)) {
            staticSettings.pointAsAnchorFunction = new Function('data, dsData, dsIndex', vm.ctx.settings.pointAsAnchorFunction);
        }

        if (staticSettings.usePolygonTooltipFunction && angular.isDefined(vm.ctx.settings.polygonTooltipFunction)) {
            staticSettings.polygonTooltipFunction = new Function('data, dsData, dsIndex', vm.ctx.settings.polygonTooltipFunction);
        }

        if (staticSettings.useLabelFunction && angular.isDefined(vm.ctx.settings.labelFunction)) {
            staticSettings.labelFunction = new Function('data, dsData, dsIndex', vm.ctx.settings.labelFunction);
        }

        if (staticSettings.useTooltipFunction && angular.isDefined(vm.ctx.settings.tooltipFunction)) {
            staticSettings.tooltipFunction = new Function('data, dsData, dsIndex', vm.ctx.settings.tooltipFunction);
        }

        if (staticSettings.usePolygonColorFunction && angular.isDefined(vm.ctx.settings.polygonColorFunction)) {
            staticSettings.polygonColorFunction = new Function('data, dsData, dsIndex', vm.ctx.settings.polygonColorFunction);
        }

        if (staticSettings.useMarkerImageFunction && angular.isDefined(vm.ctx.settings.markerImageFunction)) {
            staticSettings.markerImageFunction = new Function('data, images, dsData, dsIndex', vm.ctx.settings.markerImageFunction);
        }

        if (!staticSettings.useMarkerImageFunction &&
            angular.isDefined(vm.ctx.settings.markerImage) &&
            vm.ctx.settings.markerImage.length > 0) {
            staticSettings.useMarkerImage = true;
            let url = vm.ctx.settings.markerImage;
            let size = staticSettings.markerImageSize || 20;
            staticSettings.currentImage = {
                url: url,
                size: size
            };
            vm.utils.loadImageAspect(staticSettings.currentImage.url).then(
                (aspect) => {
                    if (aspect) {
                        let width;
                        let height;
                        if (aspect > 1) {
                            width = staticSettings.currentImage.size;
                            height = staticSettings.currentImage.size / aspect;
                        } else {
                            width = staticSettings.currentImage.size * aspect;
                            height = staticSettings.currentImage.size;
                        }
                        staticSettings.icon = L.icon({
                            iconUrl: staticSettings.currentImage.url,
                            iconSize: [width, height],
                            iconAnchor: [width / 2, height / 2]
                        });
                    }
                    if (vm.trips) {
                        vm.trips.forEach(function (trip) {
                            if (trip.marker) {
                                trip.marker.setIcon(staticSettings.icon);
                            }
                        });
                    }
                }
            )
        }
    }

    function configureTripSettings(trip, index, apply) {
        trip.settings = {};
        trip.settings.color = calculateColor(trip);
        trip.settings.polygonColor = calculatePolygonColor(trip);
        trip.settings.strokeWeight = vm.staticSettings.pathWeight;
        trip.settings.strokeOpacity = vm.staticSettings.pathOpacity;
        trip.settings.pointColor = vm.staticSettings.pointColor;
        trip.settings.polygonStrokeColor = vm.staticSettings.polygonStrokeColor;
        trip.settings.polygonStrokeOpacity = vm.staticSettings.polygonStrokeOpacity;
        trip.settings.polygonOpacity = vm.staticSettings.polygonOpacity;
        trip.settings.polygonStrokeWeight = vm.staticSettings.polygonStrokeWeight;
        trip.settings.pointSize = vm.staticSettings.pointSize;
        trip.settings.icon = calculateIcon(trip);
        if (apply) {
            $timeout(() => {
                trip.settings.labelText = calculateLabel(trip);
                trip.settings.tooltipText = $sce.trustAsHtml(calculateTooltip(trip));
                trip.settings.polygonTooltipText = $sce.trustAsHtml(calculatePolygonTooltip(trip));
            }, 0, true);
        } else {
            trip.settings.labelText = calculateLabel(trip);
            trip.settings.tooltipText = $sce.trustAsHtml(calculateTooltip(trip));
            trip.settings.polygonTooltipText = $sce.trustAsHtml(calculatePolygonTooltip(trip));
        }
    }

    function calculateLabel(trip) {
        let label = '';
        if (vm.staticSettings.showLabel) {
            let labelReplaceInfo;
            let labelText = vm.staticSettings.label;
            if (vm.staticSettings.useLabelFunction && angular.isDefined(vm.staticSettings.labelFunction)) {
                try {
                    labelText = vm.staticSettings.labelFunction(vm.ctx.data, trip.timeRange[vm.index], trip.dsIndex);
                } catch (e) {
                    labelText = null;
                }
            }
            labelText = vm.utils.createLabelFromDatasource(trip.dataSource, labelText);
            labelReplaceInfo = processPattern(labelText, vm.ctx.datasources, trip.dSIndex);
            label = fillPattern(labelText, labelReplaceInfo, trip.timeRange[vm.index]);
            if (vm.staticSettings.useLabelFunction && angular.isDefined(vm.staticSettings.labelFunction)) {
                try {
                    labelText = vm.staticSettings.labelFunction(vm.ctx.data, trip.timeRange[vm.index], trip.dSIndex);
                } catch (e) {
                    labelText = null;
                }
            }
        }
        return label;
    }

    function calculateTooltip(trip) {
        let tooltip = '';
        if (vm.staticSettings.displayTooltip) {
            let tooltipReplaceInfo;
            let tooltipText = vm.staticSettings.tooltipPattern;
            if (vm.staticSettings.useTooltipFunction && angular.isDefined(vm.staticSettings.tooltipFunction)) {
                try {
                    tooltipText = vm.staticSettings.tooltipFunction(vm.ctx.data, trip.timeRange[vm.index], trip.dSIndex);
                } catch (e) {
                    tooltipText = null;
                }
            }
            tooltipText = vm.utils.createLabelFromDatasource(trip.dataSource, tooltipText);
            tooltipReplaceInfo = processPattern(tooltipText, vm.ctx.datasources, trip.dSIndex);
            tooltip = fillPattern(tooltipText, tooltipReplaceInfo, trip.timeRange[vm.index]);
            tooltip = fillPatternWithActions(tooltip, 'onTooltipAction', null);

        }
        return tooltip;
    }

    function calculatePolygonTooltip(trip) {
        let tooltip = '';
        if (vm.staticSettings.displayTooltip) {
            let tooltipReplaceInfo;
            let tooltipText = vm.staticSettings.polygonTooltipPattern;
            if (vm.staticSettings.usePolygonTooltipFunction && angular.isDefined(vm.staticSettings.polygonTooltipFunction)) {
                try {
                    tooltipText = vm.staticSettings.polygonTooltipFunction(vm.ctx.data, trip.timeRange[vm.index], trip.dSIndex);
                } catch (e) {
                    tooltipText = null;
                }
            }
            tooltipText = vm.utils.createLabelFromDatasource(trip.dataSource, tooltipText);
            tooltipReplaceInfo = processPattern(tooltipText, vm.ctx.datasources, trip.dSIndex);
            tooltip = fillPattern(tooltipText, tooltipReplaceInfo, trip.timeRange[vm.index]);
            tooltip = fillPatternWithActions(tooltip, 'onTooltipAction', null);

        }
        return tooltip;
    }

    function calculatePointTooltip(trip, index) {
        let tooltip = '';
        if (vm.staticSettings.displayTooltip) {
            let tooltipReplaceInfo;
            let tooltipText = vm.staticSettings.tooltipPattern;
            if (vm.staticSettings.useTooltipFunction && angular.isDefined(vm.staticSettings.tooltipFunction)) {
                try {
                    tooltipText = vm.staticSettings.tooltipFunction(vm.ctx.data, trip.timeRange[index], trip.dSIndex);
                } catch (e) {
                    tooltipText = null;
                }
            }
            tooltipText = vm.utils.createLabelFromDatasource(trip.dataSource, tooltipText);
            tooltipReplaceInfo = processPattern(tooltipText, vm.ctx.datasources, trip.dSIndex);
            tooltip = fillPattern(tooltipText, tooltipReplaceInfo, trip.timeRange[index]);
            tooltip = fillPatternWithActions(tooltip, 'onTooltipAction', null);

        }
        return tooltip;
    }

    function calculateColor(trip) {
        let color = vm.staticSettings.pathColor;
        let colorFn;
        if (vm.staticSettings.usePathColorFunction && angular.isDefined(vm.staticSettings.colorFunction)) {
            try {
                colorFn = vm.staticSettings.colorFunction(vm.ctx.data, trip.timeRange[vm.index], trip.dSIndex);
            } catch (e) {
                colorFn = null;
            }
        }
        if (colorFn && colorFn !== color && trip.polyline) {
            trip.polyline.setStyle({color: colorFn});
        }
        return colorFn || color;
    }

    function calculatePolygonColor(trip) {
        let color = vm.staticSettings.polygonColor;
        let colorFn;
        if (vm.staticSettings.usePolygonColorFunction && angular.isDefined(vm.staticSettings.polygonColorFunction)) {
            try {
                colorFn = vm.staticSettings.polygonColorFunction(vm.ctx.data, trip.timeRange[vm.index], trip.dSIndex);
            } catch (e) {
                colorFn = null;
            }
        }
        if (colorFn && colorFn !== color && trip.polygon) {
            trip.polygon.setStyle({fillColor: colorFn});
        }
        return colorFn || color;
    }

    function calculateIcon(trip) {
        let icon = vm.staticSettings.icon;
        if (vm.staticSettings.useMarkerImageFunction && angular.isDefined(vm.staticSettings.markerImageFunction)) {
            let rawIcon;
            try {
                rawIcon = vm.staticSettings.markerImageFunction(vm.ctx.data, vm.staticSettings.markerImages, trip.timeRange[vm.index], trip.dSIndex);
            } catch (e) {
                rawIcon = null;
            }
            if (rawIcon) {
                vm.utils.loadImageAspect(rawIcon).then(
                    (aspect) => {
                        if (aspect) {
                            let width;
                            let height;
                            if (aspect > 1) {
                                width = rawIcon.size;
                                height = rawIcon.size / aspect;
                            } else {
                                width = rawIcon.size * aspect;
                                height = rawIcon.size;
                            }
                            icon = L.icon({
                                iconUrl: rawIcon,
                                iconSize: [width, height],
                                iconAnchor: [width / 2, height / 2]
                            });
                        }
                        if (trip.marker) {
                            trip.marker.setIcon(icon);
                        }
                    }
                )
            }
        }
        return icon;
    }

    function createUpdatePath(apply) {
        if (vm.trips && vm.map) {
            vm.trips.forEach(function (trip) {
                if (trip.marker) {
                    trip.marker.remove();
                    delete trip.marker;
                }
                if (trip.polyline) {
                    trip.polyline.remove();
                    delete trip.polyline;
                }
                if (trip.polylineDecorator) {
                    trip.polylineDecorator.remove();
                    delete trip.polylineDecorator;
                }
                if (trip.polygon) {
                    trip.polygon.remove();
                    delete trip.polygon;
                }
                if (trip.points && trip.points.length) {
                    trip.points.forEach(function (point) {
                        point.remove();
                    });
                    delete trip.points;
                }
            });
            vm.initBounds = true;
        }
        let normalizedTimeRange = createNormalizedTime(vm.data, vm.staticSettings.normalizationStep);
        createNormalizedTrips(normalizedTimeRange, vm.datasources);
        createTripsOnMap(apply);
        if (vm.initBounds && !vm.initTrips) {
            vm.trips.forEach(function (trip) {
                vm.map.extendBounds(vm.map.bounds, trip.polyline);
                vm.initBounds = !vm.datasources.every(
                    function (ds) {
                        return ds.dataReceived === true;
                    });
                vm.initTrips = vm.trips.every(function (trip) {
                    return angular.isDefined(trip.marker) && angular.isDefined(trip.polyline);
                });
            });

            vm.map.fitBounds(vm.map.bounds);
        }
    }

    function fillPattern(pattern, replaceInfo, currentNormalizedValue) {
        let text = angular.copy(pattern);
        let reg = /\$\{([^}]*)\}/g;
        if (replaceInfo) {
            for (let v = 0; v < replaceInfo.variables.length; v++) {
                let variableInfo = replaceInfo.variables[v];
                let label = reg.exec(pattern)[1].split(":")[0];
                let txtVal = '';
                if (label.length > -1 && angular.isDefined(currentNormalizedValue[label])) {
                    let varData = currentNormalizedValue[label];
                    if (isNumber(varData)) {
                        txtVal = padValue(varData, variableInfo.valDec, 0);
                    } else {
                        txtVal = varData;
                    }
                }
                text = text.split(variableInfo.variable).join(txtVal);
            }
        }
        return text;
    }

    function createNormalizedTime(data, step) {
        if (!step) step = 1000;
        let max_time = null;
        let min_time = null;
        let normalizedArray = [];
        if (data && data.length > 0) {
            vm.data.forEach(function (data) {
                if (data.data.length > 0) {
                    data.data.forEach(function (sData) {
                        if (max_time === null) {
                            max_time = sData[0];
                        } else if (max_time < sData[0]) {
                            max_time = sData[0]
                        }
                        if (min_time === null) {
                            min_time = sData[0];
                        } else if (min_time > sData[0]) {
                            min_time = sData[0];
                        }
                    })
                }
            });
            for (let i = min_time; i < max_time; i += step) {
                normalizedArray.push({ts: i, formattedTs: $filter('date')(i, 'medium')});

            }
            if (normalizedArray[normalizedArray.length - 1] && normalizedArray[normalizedArray.length - 1].ts !== max_time) {
                normalizedArray.push({ts: max_time, formattedTs: $filter('date')(max_time, 'medium')});
            }
        }
        vm.maxTime = normalizedArray.length - 1;
        //vm.minTime = vm.maxTime > 1 ? 1 : 0;
        if (vm.index < vm.minTime) {
            vm.index = vm.minTime;
        } else if (vm.index > vm.maxTime) {
            vm.index = vm.maxTime;
        }
        return normalizedArray;
    }

    function createNormalizedTrips(timeRange, dataSources) {
        vm.trips = [];
        if (timeRange && timeRange.length > 0 && dataSources && dataSources.length > 0 && vm.data && vm.data.length > 0) {
            dataSources.forEach(function (dS, index) {
                vm.trips.push({
                    dataSource: dS,
                    dSIndex: index,
                    timeRange: angular.copy(timeRange)
                })
            });

            vm.data.forEach(function (data) {
                let ds = data.datasource;
                let tripIndex = vm.trips.findIndex(function (el) {
                    return el.dataSource.entityId === ds.entityId;
                });

                if (tripIndex > -1) {
                    createNormalizedValue(data.data, data.dataKey.label, vm.trips[tripIndex].timeRange);
                }
            })
        }

        createNormalizedLatLngs();
    }

    function createNormalizedValue(dataArray, dataKey, timeRangeArray) {
        timeRangeArray.forEach(function (timeStamp) {
            let targetTDiff = null;
            let value = null;
            for (let i = 0; i < dataArray.length; i++) {
                let tDiff = dataArray[i][0] - timeStamp.ts;
                if (targetTDiff === null || (tDiff <= 0 && targetTDiff < tDiff)) {
                    targetTDiff = tDiff;
                    value = dataArray[i][1];

                }
            }
            if (value !== null) timeStamp[dataKey] = value;
        });
    }

    function createNormalizedLatLngs() {
        vm.trips.forEach(function (el) {
            el.latLngs = [];
            el.timeRange.forEach(function (data) {
                let lat = data[vm.staticSettings.latKeyName];
                let lng = data[vm.staticSettings.lngKeyName];
                if (lat && lng && vm.map) {
                    data.latLng = vm.map.createLatLng(lat, lng);
                }
                el.latLngs.push(data.latLng);
            });
            addAngleForTrip(el);
        })
    }

    function addAngleForTrip(trip) {
        if (trip.timeRange && trip.timeRange.length > 0) {
            trip.timeRange.forEach(function (point, index) {
                let nextPoint, prevPoint;
                nextPoint = index === (trip.timeRange.length - 1) ? trip.timeRange[index] : trip.timeRange[index + 1];
                prevPoint = index === 0 ? trip.timeRange[0] : trip.timeRange[index - 1];
                let nextLatLng = {
                    lat: nextPoint[vm.staticSettings.latKeyName],
                    lng: nextPoint[vm.staticSettings.lngKeyName]
                };
                let prevLatLng = {
                    lat: prevPoint[vm.staticSettings.latKeyName],
                    lng: prevPoint[vm.staticSettings.lngKeyName]
                };
                if (nextLatLng.lat === prevLatLng.lat && nextLatLng.lng === prevLatLng.lng) {
                    if (angular.isNumber(prevPoint.h)) {
                        point.h = prevPoint.h;
                    } else {
                        point.h = vm.staticSettings.rotationAngle;
                    }
                } else {
                    point.h = findAngle(prevLatLng.lat, prevLatLng.lng, nextLatLng.lat, nextLatLng.lng);
                    point.h += vm.staticSettings.rotationAngle;
                }
            });
        }
    }

    function createPointPopup(point, index, trip) {
        let popup = L.popup();
        popup.setContent(calculatePointTooltip(trip, index));
        point.bindPopup(popup, {autoClose: vm.staticSettings.autocloseTooltip, closeOnClick: false});
        return popup;
    }

    function createTripsOnMap(apply) {
        if (vm.trips.length > 0) {
            vm.trips.forEach(function (trip) {
                configureTripSettings(trip, vm.index, apply);
                if (trip.timeRange.length > 0 && trip.latLngs.every(el => angular.isDefined(el))) {
                    if (vm.staticSettings.showPoints) {
                        trip.points = [];
                        trip.timeRange.forEach(function (tRange, index) {
                            if (tRange && tRange.latLng
                                && (!vm.staticSettings.usePointAsAnchor || vm.staticSettings.pointAsAnchorFunction(vm.ctx.data, tRange, trip.dSIndex))) {
                                let point = L.circleMarker(tRange.latLng, {
                                    color: trip.settings.pointColor,
                                    radius: trip.settings.pointSize
                                }).addTo(vm.map.map);
                                if (vm.staticSettings.pointTooltipOnRightPanel) {
                                    point.popup = createPointPopup(point, index, trip);
                                } else {
                                    point.on('click', function () {
                                        showHidePointTooltip(calculatePointTooltip(trip, index), index);
                                    });
                                }
                                if (vm.staticSettings.usePointAsAnchor) tRange.hasAnchor = true;
                                trip.points.push(point);
                            }
                        });
                    }

                    if (angular.isUndefined(trip.marker)) {
                        trip.polyline = vm.map.createPolyline(trip.latLngs, trip.settings);
                        if (vm.staticSettings.usePolylineDecorator) {
                            trip.polylineDecorator = L.polylineDecorator(trip.polyline, {
                                patterns: [
                                    {
                                        offset: vm.staticSettings.decoratorOffset,
                                        endOffset: vm.staticSettings.endDecoratorOffset,
                                        repeat: vm.staticSettings.decoratorRepeat,
                                        symbol: L.Symbol[vm.staticSettings.decoratorSymbol]({
                                            pixelSize: vm.staticSettings.decoratorSymbolSize,
                                            polygon: false,
                                            pathOptions: {
                                                color: vm.staticSettings.useDecoratorCustomColor ? vm.staticSettings.decoratorCustomColor : trip.settings.color,
                                                stroke: true}
                                        })
                                    }
                                ],
                                interactive: false,
                            }).addTo(vm.map.map);
                        }
                    }


                    if (trip.timeRange && trip.timeRange.length && angular.isUndefined(trip.marker)) {
                        trip.marker = L.marker(trip.timeRange[vm.index].latLng);
                        trip.marker.setZIndexOffset(1000);
                        trip.marker.setIcon(vm.staticSettings.icon);
                        trip.marker.setRotationOrigin('center center');
                        trip.marker.on('click', function () {
                            showHideTooltip(trip);
                        });
                        trip.marker.addTo(vm.map.map);
                        moveMarker(trip);
                    }
                }

                if (vm.staticSettings.showPolygon && angular.isDefined(trip.timeRange[vm.index][vm.staticSettings.polKeyName])) {
                    let polygonSettings = {
                        fill: true,
                        fillColor: trip.settings.polygonColor,
                        color: trip.settings.polygonStrokeColor,
                        weight: trip.settings.polygonStrokeWeight,
                        fillOpacity: trip.settings.polygonOpacity,
                        opacity: trip.settings.polygonStrokeOpacity
                    };
                    let polygonLatLngsRaw = mapPolygonArray(angular.fromJson(trip.timeRange[vm.index][vm.staticSettings.polKeyName]));
                    trip.polygon = L.polygon(polygonLatLngsRaw, polygonSettings).addTo(vm.map.map);
                    trip.polygon.on('click',function(){showHidePolygonTooltip(trip)});
                }
            });
        }
    }

    function mapPolygonArray(rawArray) {
        return rawArray.map(function (el) {
            if (el.length === 2) {
                if (!angular.isNumber(el[0]) && !angular.isNumber(el[1])) {
                    return el.map(function (subEl) {
                        return mapPolygonArray(subEl);
                    })
                } else {
                    return vm.map.createLatLng(el[0], el[1]);
                }
            } else if (el.length > 2) {
                return mapPolygonArray(el);
            } else {
                return vm.map.createLatLng(false);
            }
        });
    }

    function moveMarker(trip) {
        if (angular.isDefined(trip.timeRange[vm.index].latLng)) {
            if (angular.isDefined(trip.marker)) {
                trip.markerAngleIsSet = true;
                trip.marker.setLatLng(trip.timeRange[vm.index].latLng);
                trip.marker.setRotationAngle(trip.timeRange[vm.index].h);
                trip.marker.update();
            } else {
                if (trip.timeRange && trip.timeRange.length) {
                    trip.marker = L.marker(trip.timeRange[vm.index].latLng);
                    trip.marker.setZIndexOffset(1000);
                    trip.marker.setIcon(vm.staticSettings.icon);
                    trip.marker.setRotationOrigin('center center');
                    trip.marker.on('click', function () {
                        showHideTooltip(trip);
                    });
                    trip.marker.addTo(vm.map.map);
                    trip.marker.update();
                }
            }
        }
        configureTripSettings(trip);
    }


    function showHideTooltip(trip) {
        if (vm.staticSettings.displayTooltip) {
            if (vm.staticSettings.showTooltip && trip && (vm.activeTripIndex !== trip.dSIndex || vm.staticSettings.tooltipMarker !== 'marker')) {
                vm.staticSettings.showTooltip = true;
                vm.staticSettings.tooltipMarker = 'marker';
            } else {
                vm.staticSettings.showTooltip = !vm.staticSettings.showTooltip;
            }
        }
        if (trip && vm.activeTripIndex !== trip.dSIndex) vm.activeTripIndex = trip.dSIndex;
        if (vm.trips.length) {
            vm.mainTooltip = vm.trips[vm.activeTripIndex].settings.tooltipText;
        }
    }

    function showHidePointTooltip(text, index) {
        if (vm.staticSettings.displayTooltip) {
            if (vm.staticSettings.tooltipMarker && vm.staticSettings.tooltipMarker.includes('point')) {
               if (vm.staticSettings.tooltipMarker === 'point' + index) {
                   vm.staticSettings.showTooltip = !vm.staticSettings.showTooltip;
               } else {
                   vm.staticSettings.showTooltip = true;
                   vm.mainTooltip = $sce.trustAsHtml(text);
                   vm.staticSettings.tooltipMarker = 'point' + index;
               }
            } else {
                vm.staticSettings.showTooltip = true;
                vm.mainTooltip = $sce.trustAsHtml(text);
                vm.staticSettings.tooltipMarker = 'point' + index;
            }
        }
    }

    function showHidePolygonTooltip(trip) {
        if (vm.staticSettings.displayTooltip) {
            if (vm.staticSettings.showTooltip && trip && (vm.activeTripIndex !== trip.dSIndex || vm.staticSettings.tooltipMarker !== 'polygon')) {
                vm.staticSettings.showTooltip = true;
                vm.staticSettings.tooltipMarker = 'polygon';
            } else {
                vm.staticSettings.showTooltip = !vm.staticSettings.showTooltip;
            }
        }
        if (trip && vm.activeTripIndex !== trip.dSIndex) vm.activeTripIndex = trip.dSIndex;
        vm.mainTooltip = vm.trips[vm.activeTripIndex].settings.polygonTooltipText;
    }
}