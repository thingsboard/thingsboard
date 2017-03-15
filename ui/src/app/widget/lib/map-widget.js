/*
 * Copyright © 2016-2017 The Thingsboard Authors
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

import tinycolor from 'tinycolor2';

import TbGoogleMap from './google-map';
import TbOpenStreetMap from './openstreet-map';

export default class TbMapWidget {
    constructor(mapProvider, drawRoutes, ctx) {

        var tbMap = this;
        this.ctx = ctx;

        this.drawRoutes = drawRoutes;
        this.markers = [];
        if (this.drawRoutes) {
            this.polylines = [];
        }
        this.locationsSettings = [];
        this.varsRegex = /\$\{([^\}]*)\}/g;

        var settings = ctx.settings;

        if (settings.defaultZoomLevel) {
            if (settings.defaultZoomLevel > 0 && settings.defaultZoomLevel < 21) {
                this.defaultZoomLevel = Math.floor(settings.defaultZoomLevel);
            }
        }

        this.dontFitMapBounds = settings.fitMapBounds === false;

        function procesTooltipPattern(pattern) {
            var match = tbMap.varsRegex.exec(pattern);
            var replaceInfo = {};
            replaceInfo.variables = [];
            while (match !== null) {
                var variableInfo = {};
                variableInfo.dataKeyIndex = -1;
                var variable = match[0];
                var label = match[1];
                var valDec = 2;
                var splitVals = label.split(':');
                if (splitVals.length > 1) {
                    label = splitVals[0];
                    valDec = parseFloat(splitVals[1]);
                }
                variableInfo.variable = variable;
                variableInfo.valDec = valDec;

                if (label.startsWith('#')) {
                    var keyIndexStr = label.substring(1);
                    var n = Math.floor(Number(keyIndexStr));
                    if (String(n) === keyIndexStr && n >= 0) {
                        variableInfo.dataKeyIndex = n;
                    }
                }
                if (variableInfo.dataKeyIndex === -1) {
                    var offset = 0;
                    for (var i=0;i<ctx.datasources.length;i++) {
                        var datasource = ctx.datasources[i];
                        for (var k = 0; k < datasource.dataKeys.length; k++) {
                            var dataKey = datasource.dataKeys[k];
                            if (dataKey.label === label) {
                                variableInfo.dataKeyIndex = offset + k;
                                break;
                            }
                        }
                        offset += datasource.dataKeys.length;
                    }
                }
                replaceInfo.variables.push(variableInfo);
                match = tbMap.varsRegex.exec(pattern);
            }
            return replaceInfo;
        }

        var configuredLocationsSettings = drawRoutes ? settings.routesSettings : settings.markersSettings;
        if (!configuredLocationsSettings) {
            configuredLocationsSettings = [];
        }

        for (var i=0;i<configuredLocationsSettings.length;i++) {
            this.locationsSettings[i] = {
                latKeyName: "lat",
                lngKeyName: "lng",
                showLabel: true,
                label: "",
                color: "#FE7569",
                useColorFunction: false,
                colorFunction: null,
                markerImage: null,
                markerImageSize: 34,
                useMarkerImage: false,
                useMarkerImageFunction: false,
                markerImageFunction: null,
                markerImages: [],
                tooltipPattern: "<b>Latitude:</b> ${lat:7}<br/><b>Longitude:</b> ${lng:7}"
            };

            if (drawRoutes) {
                this.locationsSettings[i].strokeWeight = 2;
                this.locationsSettings[i].strokeOpacity = 1.0;
            }

            if (configuredLocationsSettings[i]) {
                this.locationsSettings[i].latKeyName = configuredLocationsSettings[i].latKeyName || this.locationsSettings[i].latKeyName;
                this.locationsSettings[i].lngKeyName = configuredLocationsSettings[i].lngKeyName || this.locationsSettings[i].lngKeyName;

                this.locationsSettings[i].tooltipPattern = configuredLocationsSettings[i].tooltipPattern || "<b>Latitude:</b> ${"+this.locationsSettings[i].latKeyName+":7}<br/><b>Longitude:</b> ${"+this.locationsSettings[i].lngKeyName+":7}";

                this.locationsSettings[i].tooltipReplaceInfo = procesTooltipPattern(this.locationsSettings[i].tooltipPattern);

                this.locationsSettings[i].showLabel = configuredLocationsSettings[i].showLabel !== false;
                this.locationsSettings[i].label = configuredLocationsSettings[i].label || this.locationsSettings[i].label;
                this.locationsSettings[i].color = configuredLocationsSettings[i].color ? tinycolor(configuredLocationsSettings[i].color).toHexString() : this.locationsSettings[i].color;

                this.locationsSettings[i].useColorFunction = configuredLocationsSettings[i].useColorFunction === true;
                if (angular.isDefined(configuredLocationsSettings[i].colorFunction) && configuredLocationsSettings[i].colorFunction.length > 0) {
                    try {
                        this.locationsSettings[i].colorFunction = new Function('data', configuredLocationsSettings[i].colorFunction);
                    } catch (e) {
                        this.locationsSettings[i].colorFunction = null;
                    }
                }

                this.locationsSettings[i].useMarkerImageFunction = configuredLocationsSettings[i].useMarkerImageFunction === true;
                if (angular.isDefined(configuredLocationsSettings[i].markerImageFunction) && configuredLocationsSettings[i].markerImageFunction.length > 0) {
                    try {
                        this.locationsSettings[i].markerImageFunction = new Function('data, images', configuredLocationsSettings[i].markerImageFunction);
                    } catch (e) {
                        this.locationsSettings[i].markerImageFunction = null;
                    }
                }

                this.locationsSettings[i].markerImages = configuredLocationsSettings[i].markerImages || [];

                if (!this.locationsSettings[i].useMarkerImageFunction &&
                    angular.isDefined(configuredLocationsSettings[i].markerImage) &&
                    configuredLocationsSettings[i].markerImage.length > 0) {
                    this.locationsSettings[i].markerImage = configuredLocationsSettings[i].markerImage;
                    this.locationsSettings[i].useMarkerImage = true;
                    this.locationsSettings[i].markerImageSize = configuredLocationsSettings[i].markerImageSize || 34;
                }

                if (drawRoutes) {
                    this.locationsSettings[i].strokeWeight = configuredLocationsSettings[i].strokeWeight || this.locationsSettings[i].strokeWeight;
                    this.locationsSettings[i].strokeOpacity = angular.isDefined(configuredLocationsSettings[i].strokeOpacity) ? configuredLocationsSettings[i].strokeOpacity : this.locationsSettings[i].strokeOpacity;
                }
            }
        }

        var minZoomLevel = this.drawRoutes ? 18 : 15;

        var initCallback = function() {
              tbMap.update();
              tbMap.resize();
        };

        if (mapProvider === 'google-map') {
            this.map = new TbGoogleMap(ctx.$container, initCallback, this.defaultZoomLevel, this.dontFitMapBounds, minZoomLevel, settings.gmApiKey, settings.gmDefaultMapType);
        } else if (mapProvider === 'openstreet-map') {
            this.map = new TbOpenStreetMap(ctx.$container, initCallback, this.defaultZoomLevel, this.dontFitMapBounds, minZoomLevel);
        }

    }

    update() {

        var tbMap = this;

        function isNumber(n) {
            return !isNaN(parseFloat(n)) && isFinite(n);
        }

        function padValue(val, dec, int) {
            var i = 0;
            var s, strVal, n;

            val = parseFloat(val);
            n = (val < 0);
            val = Math.abs(val);

            if (dec > 0) {
                strVal = val.toFixed(dec).toString().split('.');
                s = int - strVal[0].length;

                for (; i < s; ++i) {
                    strVal[0] = '0' + strVal[0];
                }

                strVal = (n ? '-' : '') + strVal[0] + '.' + strVal[1];
            }

            else {
                strVal = Math.round(val).toString();
                s = int - strVal.length;

                for (; i < s; ++i) {
                    strVal = '0' + strVal;
                }

                strVal = (n ? '-' : '') + strVal;
            }

            return strVal;
        }

        function arraysEqual(a, b) {
            if (a === b) return true;
            if (a === null || b === null) return false;
            if (a.length != b.length) return false;

            for (var i = 0; i < a.length; ++i) {
                if (!a[i].equals(b[i])) return false;
            }
            return true;
        }

        function calculateLocationColor(settings, dataMap) {
            if (settings.useColorFunction && settings.colorFunction) {
                var color = '#FE7569';
                try {
                    color = settings.colorFunction(dataMap);
                } catch (e) {
                    color = '#FE7569';
                }
                return tinycolor(color).toHexString();
            } else {
                return settings.color;
            }
        }

        function updateLocationColor(location, dataMap) {
            var color = calculateLocationColor(location.settings, dataMap);
            if (!location.settings.calculatedColor || location.settings.calculatedColor !== color) {
                if (!location.settings.useMarkerImage && !location.settings.useMarkerImageFunction) {
                    tbMap.map.updateMarkerColor(location.marker, color);
                }
                if (location.polyline) {
                    tbMap.map.updatePolylineColor(location.polyline, location.settings, color);
                }
                location.settings.calculatedColor = color;
            }
        }

        function calculateLocationMarkerImage(settings, dataMap) {
            if (settings.useMarkerImageFunction && settings.markerImageFunction) {
                var image = null;
                try {
                    image = settings.markerImageFunction(dataMap, settings.markerImages);
                } catch (e) {
                    image = null;
                }
                return image;
            } else {
                return null;
            }
        }

        function updateLocationMarkerImage(location, dataMap) {
            var image = calculateLocationMarkerImage(location.settings, dataMap);
            if (image != null && (!location.settings.calculatedImage || !angular.equals(location.settings.calculatedImage, image))) {
                tbMap.map.updateMarkerImage(location.marker, location.settings, image.url, image.size);
                location.settings.calculatedImage = image;
            }
        }

        function updateLocationStyle(location, dataMap) {
            updateLocationColor(location, dataMap);
            updateLocationMarkerImage(location, dataMap);
        }

        function updateLocation(location, data, dataMap) {
            var locationChanged = false;
            if (location.latIndex > -1 && location.lngIndex > -1) {
                var latData = data[location.latIndex].data;
                var lngData = data[location.lngIndex].data;
                var lat, lng, latLng;
                if (latData.length > 0 && lngData.length > 0) {
                    if (tbMap.drawRoutes) {
                        // Create or update route
                        var latLngs = [];
                        for (var i = 0; i < latData.length; i++) {
                            lat = latData[i][1];
                            lng = lngData[i][1];
                            latLng = tbMap.map.createLatLng(lat, lng);
                            if (i == 0 || !latLngs[latLngs.length-1].equals(latLng)) {
                                latLngs.push(latLng);
                            }
                        }
                        if (latLngs.length > 0) {
                            var markerLocation = latLngs[latLngs.length-1];
                            if (!location.marker) {
                                location.marker = tbMap.map.createMarker(markerLocation, location.settings);
                            } else {
                                tbMap.map.setMarkerPosition(location.marker, markerLocation);
                            }
                        }
                        if (!location.polyline) {
                            location.polyline = tbMap.map.createPolyline(latLngs, location.settings);
                            tbMap.polylines.push(location.polyline);
                            locationChanged = true;
                        } else {
                            var prevPath = tbMap.map.getPolylineLatLngs(location.polyline);
                            if (!prevPath || !arraysEqual(prevPath, latLngs)) {
                                tbMap.map.setPolylineLatLngs(location.polyline, latLngs);
                                locationChanged = true;
                            }
                        }
                    } else {
                        // Create or update marker
                        lat = latData[latData.length-1][1];
                        lng = lngData[lngData.length-1][1];
                        latLng = tbMap.map.createLatLng(lat, lng);
                        if (!location.marker) {
                            location.marker = tbMap.map.createMarker(latLng, location.settings);
                            tbMap.markers.push(location.marker);
                            locationChanged = true;
                        } else {
                            var prevPosition = tbMap.map.getMarkerPosition(location.marker);
                            if (!prevPosition.equals(latLng)) {
                                tbMap.map.setMarkerPosition(location.marker, latLng);
                                locationChanged = true;
                            }
                        }
                    }
                    updateLocationStyle(location, dataMap);
                }
            }
            return locationChanged;
        }

        function toLabelValueMap(data) {
            var dataMap = {};
            for (var i = 0; i < data.length; i++) {
                var dataKey = data[i].dataKey;
                var label = dataKey.label;
                var keyData = data[i].data;
                var val = null;
                if (keyData.length > 0) {
                    val = keyData[keyData.length-1][1];
                }
                dataMap[label] = val;
            }
            return dataMap;
        }

        function loadLocations(data) {
            var bounds = tbMap.map.createBounds();
            tbMap.locations = [];
            var dataMap = toLabelValueMap(data);
            for (var l in tbMap.locationsSettings) {
                var locationSettings = tbMap.locationsSettings[l];
                var latIndex = -1;
                var lngIndex = -1;
                for (var i = 0; i < data.length; i++) {
                    var dataKey = data[i].dataKey;
                    if (dataKey.label === locationSettings.latKeyName) {
                        latIndex = i;
                    } else if (dataKey.label === locationSettings.lngKeyName) {
                        lngIndex = i;
                    }
                }
                if (latIndex > -1 && lngIndex > -1) {
                    var location = {
                        latIndex: latIndex,
                        lngIndex: lngIndex,
                        settings: locationSettings
                    };
                    tbMap.locations.push(location);
                    updateLocation(location, data, dataMap);
                    if (location.polyline) {
                        tbMap.map.extendBounds(bounds, location.polyline);
                    } else if (location.marker) {
                        tbMap.map.extendBoundsWithMarker(bounds, location.marker);
                    }
                }
            }
            tbMap.map.fitBounds(bounds);
        }

        function updateLocations(data) {
            var locationsChanged = false;
            var bounds = tbMap.map.createBounds();
            var dataMap = toLabelValueMap(data);
            for (var p in tbMap.locations) {
                var location = tbMap.locations[p];
                locationsChanged |= updateLocation(location, data, dataMap);
                if (location.polyline) {
                    tbMap.map.extendBounds(bounds, location.polyline);
                } else if (location.marker) {
                    tbMap.map.extendBoundsWithMarker(bounds, location.marker);
                }
            }
            if (locationsChanged) {
                tbMap.map.fitBounds(bounds);
            }
        }

        if (this.map && this.map.inited()) {
            if (this.ctx.data) {
                if (!this.locations) {
                    loadLocations(this.ctx.data);
                } else {
                    updateLocations(this.ctx.data);
                }
            }
            var tooltips = this.map.getTooltips();
            for (var t in tooltips) {
                var tooltip = tooltips[t];
                var text = tooltip.pattern;
                var replaceInfo = tooltip.replaceInfo;
                for (var v in replaceInfo.variables) {
                    var variableInfo = replaceInfo.variables[v];
                    var txtVal = '';
                    if (variableInfo.dataKeyIndex > -1) {
                        var varData = this.ctx.data[variableInfo.dataKeyIndex].data;
                        if (varData.length > 0) {
                            var val = varData[varData.length - 1][1];
                            if (isNumber(val)) {
                                txtVal = padValue(val, variableInfo.valDec, 0);
                            } else {
                                txtVal = val;
                            }
                        }
                    }
                    text = text.split(variableInfo.variable).join(txtVal);
                }
                tooltip.popup.setContent(text);
            }
        }
    }

    resize() {
        if (this.map && this.map.inited()) {
            this.map.invalidateSize();
            if (this.locations && this.locations.size > 0) {
                var bounds = this.map.createBounds();
                for (var m in this.markers) {
                    this.map.extendBoundsWithMarker(bounds, this.markers[m]);
                }
                if (this.polylines) {
                    for (var p in this.polylines) {
                        this.map.extendBounds(bounds, this.polylines[p]);
                    }
                }
                this.map.fitBounds(bounds);
            }
        }
    }

}
