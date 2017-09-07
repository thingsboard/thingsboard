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
import TbImageMap from './image-map';

import {processPattern, arraysEqual, toLabelValueMap, fillPattern, fillPatternWithActions} from './widget-utils';

export default class TbMapWidgetV2 {
    constructor(mapProvider, drawRoutes, ctx, useDynamicLocations, $element) {
        var tbMap = this;
        this.ctx = ctx;
        this.mapProvider = mapProvider;
        if (!$element) {
            $element = ctx.$container;
        }
        this.utils = ctx.$scope.$injector.get('utils');
        this.drawRoutes = drawRoutes;
        this.markers = [];
        if (this.drawRoutes) {
            this.polylines = [];
        }

        this.locationSettings = {};

        var settings = ctx.settings;

        this.callbacks = {};
        this.callbacks.onLocationClick = function(){};

        if (settings.defaultZoomLevel) {
            if (settings.defaultZoomLevel > 0 && settings.defaultZoomLevel < 21) {
                this.defaultZoomLevel = Math.floor(settings.defaultZoomLevel);
            }
        }

        this.dontFitMapBounds = settings.fitMapBounds === false;

        if (!useDynamicLocations) {
            this.subscription = this.ctx.defaultSubscription;
        }

        this.configureLocationsSettings();

        var minZoomLevel = this.drawRoutes ? 18 : 15;

        var initCallback = function() {
            tbMap.update();
            tbMap.resize();
        };

        this.ctx.$scope.onTooltipAction = function(event, actionName, dsIndex) {
            tbMap.onTooltipAction(event, actionName, dsIndex);
        };
        this.tooltipActionsMap = {};
        var descriptors = this.ctx.actionsApi.getActionDescriptors('tooltipAction');
        descriptors.forEach(function (descriptor) {
            tbMap.tooltipActionsMap[descriptor.name] = descriptor;
        });

        if (mapProvider === 'google-map') {
            this.map = new TbGoogleMap($element, initCallback, this.defaultZoomLevel, this.dontFitMapBounds, minZoomLevel, settings.gmApiKey, settings.gmDefaultMapType);
        } else if (mapProvider === 'openstreet-map') {
            this.map = new TbOpenStreetMap($element, initCallback, this.defaultZoomLevel, this.dontFitMapBounds, minZoomLevel);
        } else if (mapProvider === 'image-map') {
            this.map = new TbImageMap(this.ctx, $element, initCallback,
                settings.mapImageUrl,
                settings.posFunction,
                settings.imageEntityAlias,
                settings.imageUrlAttribute);
        }
    }

    setCallbacks(callbacks) {
        Object.assign(this.callbacks, callbacks);
    }

    clearLocations() {
        if (this.locations) {
            var tbMap = this;
            this.locations.forEach(function(location) {
                if (location.marker) {
                    tbMap.map.removeMarker(location.marker);
                }
                if (location.polyline) {
                    tbMap.map.removePolyline(location.polyline);
                }
            });
            this.locations = null;
            this.markers = [];
            if (this.drawRoutes) {
                this.polylines = [];
            }
        }
    }

    setSubscription(subscription) {
        this.subscription = subscription;
        this.clearLocations();
    }

    configureLocationsSettings() {

        if (this.mapProvider  == 'image-map') {
            this.locationSettings.latKeyName = this.ctx.settings.xPosKeyName || 'xPos';
            this.locationSettings.lngKeyName = this.ctx.settings.yPosKeyName || 'yPos';
            this.locationSettings.markerOffsetX = angular.isDefined(this.ctx.settings.markerOffsetX) ? this.ctx.settings.markerOffsetX : 0.5;
            this.locationSettings.markerOffsetY = angular.isDefined(this.ctx.settings.markerOffsetY) ? this.ctx.settings.markerOffsetY : 1;
        } else {
            this.locationSettings.latKeyName = this.ctx.settings.latKeyName || 'latitude';
            this.locationSettings.lngKeyName = this.ctx.settings.lngKeyName || 'longitude';
        }

        this.locationSettings.tooltipPattern = this.ctx.settings.tooltipPattern || "<b>${entityName}</b><br/><br/><b>Latitude:</b> ${"+this.locationSettings.latKeyName+":7}<br/><b>Longitude:</b> ${"+this.locationSettings.lngKeyName+":7}";

        this.locationSettings.showLabel = this.ctx.settings.showLabel !== false;
        this.locationSettings.displayTooltip = this.ctx.settings.showTooltip !== false;
        this.locationSettings.labelColor = this.ctx.widgetConfig.color || '#000000',
        this.locationSettings.label = this.ctx.settings.label || "${entityName}";
        this.locationSettings.color = this.ctx.settings.color ? tinycolor(this.ctx.settings.color).toHexString() : "#FE7569";

        this.locationSettings.useColorFunction = this.ctx.settings.useColorFunction === true;
        if (angular.isDefined(this.ctx.settings.colorFunction) && this.ctx.settings.colorFunction.length > 0) {
            try {
                this.locationSettings.colorFunction = new Function('data, dsData, dsIndex', this.ctx.settings.colorFunction);
            } catch (e) {
                this.locationSettings.colorFunction = null;
            }
        }

        this.locationSettings.useMarkerImageFunction = this.ctx.settings.useMarkerImageFunction === true;
        if (angular.isDefined(this.ctx.settings.markerImageFunction) && this.ctx.settings.markerImageFunction.length > 0) {
            try {
                this.locationSettings.markerImageFunction = new Function('data, images, dsData, dsIndex', this.ctx.settings.markerImageFunction);
            } catch (e) {
                this.locationSettings.markerImageFunction = null;
            }
        }

        this.locationSettings.markerImages = this.ctx.settings.markerImages || [];

        if (!this.locationSettings.useMarkerImageFunction &&
            angular.isDefined(this.ctx.settings.markerImage) &&
            this.ctx.settings.markerImage.length > 0) {
            this.locationSettings.markerImage = this.ctx.settings.markerImage;
            this.locationSettings.useMarkerImage = true;
            this.locationSettings.markerImageSize = this.ctx.settings.markerImageSize || 34;
        }

        if (this.drawRoutes) {
            this.locationSettings.strokeWeight = this.ctx.settings.strokeWeight || 2;
            this.locationSettings.strokeOpacity = this.ctx.settings.strokeOpacity || 1.0;
        }
    }

    onTooltipAction(event, actionName, dsIndex) {
        var descriptor = this.tooltipActionsMap[actionName];
        if (descriptor) {
            var datasource = this.subscription.datasources[dsIndex];
            var entityId = {};
            entityId.id = datasource.entityId;
            entityId.entityType = datasource.entityType;
            var entityName = datasource.entityName;
            this.ctx.actionsApi.handleWidgetAction(event, descriptor, entityId, entityName);
        }
    }

    update() {

        var tbMap = this;

        function updateLocationLabel(location) {
            if (location.settings.showLabel && location.settings.labelReplaceInfo.variables.length) {
                location.settings.labelText = fillPattern(location.settings.label,
                    location.settings.labelReplaceInfo, tbMap.subscription.data);
                tbMap.map.updateMarkerLabel(location.marker, location.settings);
            }
        }

        function calculateLocationColor(location, dataMap) {
            if (location.settings.useColorFunction && location.settings.colorFunction) {
                var color;
                try {
                    color = location.settings.colorFunction(dataMap.dataMap, dataMap.dsDataMap, location.dsIndex);
                } catch (e) {/**/}
                if (!color) {
                    color = '#FE7569';
                }
                return tinycolor(color).toHexString();
            } else {
                return location.settings.color;
            }
        }

        function updateLocationColor(location, color, image) {
            if (!location.settings.calculatedColor || location.settings.calculatedColor !== color) {
                if (!location.settings.useMarkerImage && !image) {
                    tbMap.map.updateMarkerColor(location.marker, color);
                }
                if (location.polyline) {
                    tbMap.map.updatePolylineColor(location.polyline, location.settings, color);
                }
                location.settings.calculatedColor = color;
            }
        }

        function calculateLocationMarkerImage(location, dataMap) {
            if (location.settings.useMarkerImageFunction && location.settings.markerImageFunction) {
                var image = null;
                try {
                    image = location.settings.markerImageFunction(dataMap.dataMap, location.settings.markerImages, dataMap.dsDataMap, location.dsIndex);
                } catch (e) {
                    image = null;
                }
                return image;
            } else {
                return null;
            }
        }

        function updateLocationMarkerImage(location, image) {
            if (image && (!location.settings.calculatedImage || !angular.equals(location.settings.calculatedImage, image))) {
                tbMap.map.updateMarkerImage(location.marker, location.settings, image.url, image.size);
                location.settings.calculatedImage = image;
            }
        }

        function updateLocationStyle(location, dataMap) {
            updateLocationLabel(location);
            var color = calculateLocationColor(location, dataMap);
            var image = calculateLocationMarkerImage(location, dataMap);
            updateLocationColor(location, color, image);
            updateLocationMarkerImage(location, image);
        }

        function locationRowClick($event, location) {
            var descriptors = tbMap.ctx.actionsApi.getActionDescriptors('markerClick');
            if (descriptors.length) {
                var datasource = tbMap.subscription.datasources[location.dsIndex];
                var entityId = {};
                entityId.id = datasource.entityId;
                entityId.entityType = datasource.entityType;
                var entityName = datasource.entityName;
                tbMap.ctx.actionsApi.handleWidgetAction($event, descriptors[0], entityId, entityName);
            }
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
                            if (i == 0 || !latLngs[latLngs.length - 1].equals(latLng)) {
                                latLngs.push(latLng);
                            }
                        }
                        if (latLngs.length > 0) {
                            var markerLocation = latLngs[latLngs.length - 1];
                            if (!location.marker) {
                                location.marker = tbMap.map.createMarker(markerLocation, location.settings,
                                    function (event) {
                                        tbMap.callbacks.onLocationClick(location);
                                        locationRowClick(event, location);
                                    }, [location.dsIndex]
                                );
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
                        lat = latData[latData.length - 1][1];
                        lng = lngData[lngData.length - 1][1];
                        latLng = tbMap.map.createLatLng(lat, lng);
                        if (!location.marker) {
                            location.marker = tbMap.map.createMarker(latLng, location.settings,
                                function (event) {
                                    tbMap.callbacks.onLocationClick(location);
                                    locationRowClick(event, location);
                                }, [location.dsIndex]);
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

        function loadLocations(data, datasources) {
            var bounds = tbMap.map.createBounds();
            tbMap.locations = [];
            var dataMap = toLabelValueMap(data, datasources);
            var currentDatasource = null;
            var currentDatasourceIndex = -1;
            var latIndex = -1;
            var lngIndex = -1;

            for (var i=0;i<data.length;i++) {
                var dataKeyData = data[i];
                var dataKey = dataKeyData.dataKey;
                if (dataKeyData.datasource != currentDatasource) {
                    currentDatasource = dataKeyData.datasource;
                    currentDatasourceIndex++;
                    latIndex = -1;
                    lngIndex = -1;
                }
                var nameToCheck;
                if (dataKey.locationAttrName) {
                    nameToCheck = dataKey.locationAttrName;
                } else {
                    nameToCheck = dataKey.label;
                }
                if (nameToCheck === tbMap.locationSettings.latKeyName) {
                    latIndex = i;
                } else if (nameToCheck === tbMap.locationSettings.lngKeyName) {
                    lngIndex = i;
                }
                if (latIndex > -1 && lngIndex > -1) {
                    var location = {
                        latIndex: latIndex,
                        lngIndex: lngIndex,
                        dsIndex: currentDatasourceIndex,
                        settings: angular.copy(tbMap.locationSettings)
                    };
                    if (location.settings.showLabel) {
                        location.settings.label = tbMap.utils.createLabelFromDatasource(currentDatasource, location.settings.label);
                        location.settings.labelReplaceInfo = processPattern(location.settings.label, datasources, currentDatasourceIndex);
                        location.settings.labelText = location.settings.label;
                    }
                    if (location.settings.displayTooltip) {
                        location.settings.tooltipPattern = tbMap.utils.createLabelFromDatasource(currentDatasource, location.settings.tooltipPattern);
                        location.settings.tooltipReplaceInfo = processPattern(location.settings.tooltipPattern, datasources, currentDatasourceIndex);
                    }

                    tbMap.locations.push(location);
                    updateLocation(location, data, dataMap);
                    if (location.polyline) {
                        tbMap.map.extendBounds(bounds, location.polyline);
                    } else if (location.marker) {
                        tbMap.map.extendBoundsWithMarker(bounds, location.marker);
                    }
                    latIndex = -1;
                    lngIndex = -1;
                }

            }
            tbMap.map.fitBounds(bounds);
        }

        function updateLocations(data, datasources) {
            var locationsChanged = false;
            var bounds = tbMap.map.createBounds();
            var dataMap = toLabelValueMap(data, datasources);
            for (var p = 0; p < tbMap.locations.length; p++) {
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

        if (this.map && this.map.inited() && this.subscription) {
            if (this.subscription.data) {
                if (!this.locations) {
                    loadLocations(this.subscription.data, this.subscription.datasources);
                } else {
                    updateLocations(this.subscription.data, this.subscription.datasources);
                }
                var tooltips = this.map.getTooltips();
                for (var t=0; t < tooltips.length; t++) {
                    var tooltip = tooltips[t];
                    var text = fillPattern(tooltip.pattern, tooltip.replaceInfo, this.subscription.data);
                    text = fillPatternWithActions(text, 'onTooltipAction', tooltip.markerArgs);
                    tooltip.popup.setContent(text);
                }
            }
        }
    }

    resize() {
        if (this.map && this.map.inited()) {
            this.map.invalidateSize();
            if (this.locations && this.locations.length > 0) {
                var bounds = this.map.createBounds();
                for (var m = 0; m < this.markers.length; m++) {
                    this.map.extendBoundsWithMarker(bounds, this.markers[m]);
                }
                if (this.polylines) {
                    for (var p = 0; p < this.polylines.length; p++) {
                        this.map.extendBounds(bounds, this.polylines[p]);
                    }
                }
                this.map.fitBounds(bounds);
            }
        }
    }

    static settingsSchema(mapProvider, drawRoutes) {
        var schema;
        if (mapProvider === 'google-map') {
            schema = angular.copy(googleMapSettingsSchema);
        } else if (mapProvider === 'openstreet-map') {
            schema = angular.copy(openstreetMapSettingsSchema);
        } else if (mapProvider === 'image-map') {
            return imageMapSettingsSchema;
        }
        angular.merge(schema.schema.properties, commonMapSettingsSchema.schema.properties);
        schema.schema.required = schema.schema.required.concat(commonMapSettingsSchema.schema.required);
        schema.form = schema.form.concat(commonMapSettingsSchema.form);
        if (drawRoutes) {
            angular.merge(schema.schema.properties, routeMapSettingsSchema.schema.properties);
            schema.schema.required = schema.schema.required.concat(routeMapSettingsSchema.schema.required);
            schema.form = schema.form.concat(routeMapSettingsSchema.form);
        }
        return schema;
    }

    static dataKeySettingsSchema(/*mapProvider*/) {
        return {};
    }

    static actionSources() {
        return {
            'markerClick': {
                name: 'widget-action.marker-click',
                multiple: false
            },
            'tooltipAction': {
                name: 'widget-action.tooltip-tag-action',
                multiple: true
            }
        };
    }

}

const googleMapSettingsSchema =
    {
        "schema":{
            "title":"Google Map Configuration",
            "type":"object",
            "properties":{
                "gmApiKey":{
                    "title":"Google Maps API Key",
                    "type":"string"
                },
                "gmDefaultMapType":{
                    "title":"Default map type",
                    "type":"string",
                    "default":"roadmap"
                }
            },
            "required":[
                "gmApiKey"
            ]
        },
        "form":[
            "gmApiKey",
            {
                "key":"gmDefaultMapType",
                "type":"rc-select",
                "multiple":false,
                "items":[
                    {
                        "value":"roadmap",
                        "label":"Roadmap"
                    },
                    {
                        "value":"satellite",
                        "label":"Satellite"
                    },
                    {
                        "value":"hybrid",
                        "label":"Hybrid"
                    },
                    {
                        "value":"terrain",
                        "label":"Terrain"
                    }
                ]
            }
        ]
    };

const openstreetMapSettingsSchema =
    {
        "schema":{
            "title":"Openstreet Map Configuration",
            "type":"object",
            "properties":{
            },
            "required":[
            ]
        },
        "form":[
        ]
    };

const commonMapSettingsSchema =
    {
        "schema":{
            "title":"Map Configuration",
            "type":"object",
            "properties":{
                "defaultZoomLevel":{
                    "title":"Default map zoom level (1 - 20)",
                    "type":"number"
                },
                "fitMapBounds":{
                    "title":"Fit map bounds to cover all markers",
                    "type":"boolean",
                    "default":true
                },
                "latKeyName":{
                    "title":"Latitude key name",
                    "type":"string",
                    "default":"latitude"
                },
                "lngKeyName":{
                    "title":"Longitude key name",
                    "type":"string",
                    "default":"longitude"
                },
                "showLabel":{
                    "title":"Show label",
                    "type":"boolean",
                    "default":true
                },
                "label":{
                    "title":"Label (pattern examples: '${entityName}', '${entityName}: (Text ${keyName} units.)' )",
                    "type":"string",
                    "default":"${entityName}"
                },
                "showTooltip": {
                    "title": "Show tooltip",
                    "type":"boolean",
                    "default":true
                },
                "tooltipPattern":{
                    "title":"Tooltip (for ex. 'Text ${keyName} units.' or <link-act name='my-action'>Link text</link-act>')",
                    "type":"string",
                    "default":"<b>${entityName}</b><br/><br/><b>Latitude:</b> ${latitude:7}<br/><b>Longitude:</b> ${longitude:7}"
                },
                "color":{
                    "title":"Color",
                    "type":"string"
                },
                "useColorFunction":{
                    "title":"Use color function",
                    "type":"boolean",
                    "default":false
                },
                "colorFunction":{
                    "title":"Color function: f(data, dsData, dsIndex)",
                    "type":"string"
                },
                "markerImage":{
                    "title":"Custom marker image",
                    "type":"string"
                },
                "markerImageSize":{
                    "title":"Custom marker image size (px)",
                    "type":"number",
                    "default":34
                },
                "useMarkerImageFunction":{
                    "title":"Use marker image function",
                    "type":"boolean",
                    "default":false
                },
                "markerImageFunction":{
                    "title":"Marker image function: f(data, images, dsData, dsIndex)",
                    "type":"string"
                },
                "markerImages":{
                    "title":"Marker images",
                    "type":"array",
                    "items":{
                        "title":"Marker image",
                        "type":"string"
                    }
                }
            },
            "required":[]
        },
        "form":[
            "defaultZoomLevel",
            "fitMapBounds",
            "latKeyName",
            "lngKeyName",
            "showLabel",
            "label",
            "showTooltip",
            {
                "key": "tooltipPattern",
                "type": "textarea"
            },
            {
                "key":"color",
                "type":"color"
            },
            "useColorFunction",
            {
                "key":"colorFunction",
                "type":"javascript"
            },
            {
                "key":"markerImage",
                "type":"image"
            },
            "markerImageSize",
            "useMarkerImageFunction",
            {
                "key":"markerImageFunction",
                "type":"javascript"
            },
            {
                "key":"markerImages",
                "items":[
                    {
                        "key":"markerImages[]",
                        "type":"image"
                    }
                ]
            }
        ]
};

const routeMapSettingsSchema =
    {
        "schema":{
            "title":"Route Map Configuration",
            "type":"object",
            "properties":{
                "strokeWeight": {
                    "title": "Stroke weight",
                    "type": "number",
                    "default": 2
                },
                "strokeOpacity": {
                    "title": "Stroke opacity",
                    "type": "number",
                    "default": 1.0
                }
            },
            "required":[
            ]
        },
        "form":[
            "strokeWeight",
            "strokeOpacity"
        ]
    };

const imageMapSettingsSchema =
{
    "schema":{
        "title":"Image Map Configuration",
        "type":"object",
        "properties":{
            "mapImageUrl": {
                "title": "Image map background",
                "type": "string",
                "default": "data:image/svg+xml;base64,PHN2ZyBpZD0ic3ZnMiIgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIGhlaWdodD0iMTAwIiB3aWR0aD0iMTAwIiB2ZXJzaW9uPSIxLjEiIHhtbG5zOmNjPSJodHRwOi8vY3JlYXRpdmVjb21tb25zLm9yZy9ucyMiIHhtbG5zOmRjPSJodHRwOi8vcHVybC5vcmcvZGMvZWxlbWVudHMvMS4xLyIgdmlld0JveD0iMCAwIDEwMCAxMDAiPgogPGcgaWQ9ImxheWVyMSIgdHJhbnNmb3JtPSJ0cmFuc2xhdGUoMCAtOTUyLjM2KSI+CiAgPHJlY3QgaWQ9InJlY3Q0Njg0IiBzdHJva2UtbGluZWpvaW49InJvdW5kIiBoZWlnaHQ9Ijk5LjAxIiB3aWR0aD0iOTkuMDEiIHN0cm9rZT0iIzAwMCIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIiB5PSI5NTIuODYiIHg9Ii40OTUwNSIgc3Ryb2tlLXdpZHRoPSIuOTkwMTAiIGZpbGw9IiNlZWUiLz4KICA8dGV4dCBpZD0idGV4dDQ2ODYiIHN0eWxlPSJ3b3JkLXNwYWNpbmc6MHB4O2xldHRlci1zcGFjaW5nOjBweDt0ZXh0LWFuY2hvcjptaWRkbGU7dGV4dC1hbGlnbjpjZW50ZXIiIGZvbnQtd2VpZ2h0PSJib2xkIiB4bWw6c3BhY2U9InByZXNlcnZlIiBmb250LXNpemU9IjEwcHgiIGxpbmUtaGVpZ2h0PSIxMjUlIiB5PSI5NzAuNzI4MDkiIHg9IjQ5LjM5NjQ3NyIgZm9udC1mYW1pbHk9IlJvYm90byIgZmlsbD0iIzY2NjY2NiI+PHRzcGFuIGlkPSJ0c3BhbjQ2OTAiIHg9IjUwLjY0NjQ3NyIgeT0iOTcwLjcyODA5Ij5JbWFnZSBiYWNrZ3JvdW5kIDwvdHNwYW4+PHRzcGFuIGlkPSJ0c3BhbjQ2OTIiIHg9IjQ5LjM5NjQ3NyIgeT0iOTgzLjIyODA5Ij5pcyBub3QgY29uZmlndXJlZDwvdHNwYW4+PC90ZXh0PgogIDxyZWN0IGlkPSJyZWN0NDY5NCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCIgaGVpZ2h0PSIxOS4zNiIgd2lkdGg9IjY5LjM2IiBzdHJva2U9IiMwMDAiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgeT0iOTkyLjY4IiB4PSIxNS4zMiIgc3Ryb2tlLXdpZHRoPSIuNjM5ODYiIGZpbGw9Im5vbmUiLz4KIDwvZz4KPC9zdmc+Cg=="
            },
            "imageEntityAlias": {
                "title": "Image URL source entity alias",
                "type": "string",
                "default": ""
            },
            "imageUrlAttribute": {
                "title": "Image URL source entity attribute",
                "type": "string",
                "default": ""
            },
            "xPosKeyName":{
                "title":"X position key name",
                "type":"string",
                "default":"xPos"
            },
            "yPosKeyName":{
                "title":"Y position key name",
                "type":"string",
                "default":"yPos"
            },
            "showLabel":{
                "title":"Show label",
                "type":"boolean",
                "default":true
            },
            "label":{
                "title":"Label (pattern examples: '${entityName}', '${entityName}: (Text ${keyName} units.)' )",
                "type":"string",
                "default":"${entityName}"
            },
            "showTooltip": {
                "title": "Show tooltip",
                "type":"boolean",
                "default":true
            },
            "tooltipPattern":{
                "title":"Tooltip (for ex. 'Text ${keyName} units.' or <link-act name='my-action'>Link text</link-act>')",
                "type":"string",
                "default":"<b>${entityName}</b><br/><br/><b>X Pos:</b> ${xPos:2}<br/><b>Y Pos:</b> ${yPos:2}"
            },
            "color":{
                "title":"Color",
                "type":"string"
            },
            "posFunction":{
                "title":"Position conversion function: f(origXPos, origYPos), should return x,y coordinates as double from 0 to 1 each",
                "type":"string",
                "default": "return {x: origXPos, y: origYPos};"
            },
            "markerOffsetX": {
                "title": "Marker X offset relative to position",
                "type": "number",
                "default": 0.5
            },
            "markerOffsetY": {
                "title": "Marker Y offset relative to position",
                "type": "number",
                "default": 1
            },
            "useColorFunction":{
                "title":"Use color function",
                "type":"boolean",
                "default":false
            },
            "colorFunction":{
                "title":"Color function: f(data, dsData, dsIndex)",
                "type":"string"
            },
            "markerImage":{
                "title":"Custom marker image",
                "type":"string"
            },
            "markerImageSize":{
                "title":"Custom marker image size (px)",
                "type":"number",
                "default":34
            },
            "useMarkerImageFunction":{
                "title":"Use marker image function",
                "type":"boolean",
                "default":false
            },
            "markerImageFunction":{
                "title":"Marker image function: f(data, images, dsData, dsIndex)",
                "type":"string"
            },
            "markerImages":{
                "title":"Marker images",
                "type":"array",
                "items":{
                    "title":"Marker image",
                    "type":"string"
                }
            }
        },
        "required":[]
    },
    "form":[
        {
            "key": "mapImageUrl",
            "type": "image"
        },
        "imageEntityAlias",
        "imageUrlAttribute",
        "xPosKeyName",
        "yPosKeyName",
        "showLabel",
        "label",
        "showTooltip",
        {
            "key": "tooltipPattern",
            "type": "textarea"
        },
        {
            "key":"color",
            "type":"color"
        },
        {
            "key":"posFunction",
            "type":"javascript"
        },
        "markerOffsetX",
        "markerOffsetY",
        "useColorFunction",
        {
            "key":"colorFunction",
            "type":"javascript"
        },
        {
            "key":"markerImage",
            "type":"image"
        },
        "markerImageSize",
        "useMarkerImageFunction",
        {
            "key":"markerImageFunction",
            "type":"javascript"
        },
        {
            "key":"markerImages",
            "items":[
                {
                    "key":"markerImages[]",
                    "type":"image"
                }
            ]
        }
    ]
};