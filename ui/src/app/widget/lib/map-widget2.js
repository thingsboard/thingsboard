/*
 * Copyright © 2016-2018 The Thingsboard Authors
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
import TbOpenStreetMapLocal from './openstreet-map-local';
import TbImageMap from './image-map';
import TbTencentMap from './tencent-map';

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
            this.map = new TbGoogleMap($element, this.utils, initCallback, this.defaultZoomLevel, this.dontFitMapBounds, minZoomLevel, settings.gmApiKey, settings.gmDefaultMapType);
        } else if (mapProvider === 'openstreet-map') {
            this.map = new TbOpenStreetMap($element, this.utils,  initCallback, this.defaultZoomLevel, this.dontFitMapBounds, minZoomLevel, settings.mapProvider);
        } else if (mapProvider === 'openstreet-map-local') {
            this.map = new TbOpenStreetMapLocal($element, this.utils, initCallback, this.defaultZoomLevel, this.dontFitMapBounds, minZoomLevel, settings.mapProvider);
        } else if (mapProvider === 'image-map') {
            this.map = new TbImageMap(this.ctx, $element, this.utils, initCallback,
                settings.mapImageUrl,
                settings.posFunction,
                settings.imageEntityAlias,
                settings.imageUrlAttribute);
        } else if (mapProvider === 'tencent-map') {
            this.map = new TbTencentMap($element,this.utils, initCallback, this.defaultZoomLevel, this.dontFitMapBounds, minZoomLevel, settings.tmApiKey, settings.tmDefaultMapType);
        }
        if (this.mapProvider === 'openstreet-map-local'){
            //创建图层控制
            tbMap.map.makeToolBar(this.ctx);  
            tbMap.map.showLayerControl(this.ctx.settings.showLayerControl);
            tbMap.map.showDrawToolBar(this.ctx.settings.showDrawToolBar);   
            //创建地理围栏
            if (this.locationSettings.geoJsonLayers) {
                var thisMap = this.map;
                this.locationSettings.geoJsonLayers.forEach(function (geoJsonLayer) {
                    thisMap.createDefence(angular.fromJson(geoJsonLayer));
                });
            }              
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
        this.locationSettings.geoJsonLayers = angular.fromJson(this.ctx.settings.geoJsonLayers) || [];

        // var tbMap = this;
        if (this.map && this.mapProvider === 'openstreet-map-local') {
            this.map.showLayerControl(this.locationSettings.showLayerControl);
            this.map.showDrawToolBar(this.locationSettings.showDrawToolBar);
        }        

        if (this.mapProvider  == 'image-map') {
            this.locationSettings.latKeyName = this.ctx.settings.xPosKeyName || 'xPos';
            this.locationSettings.lngKeyName = this.ctx.settings.yPosKeyName || 'yPos';
            this.locationSettings.markerOffsetX = angular.isDefined(this.ctx.settings.markerOffsetX) ? this.ctx.settings.markerOffsetX : 0.5;
            this.locationSettings.markerOffsetY = angular.isDefined(this.ctx.settings.markerOffsetY) ? this.ctx.settings.markerOffsetY : 1;
        } else {
            this.locationSettings.latKeyName = this.ctx.settings.latKeyName || 'latitude';
            this.locationSettings.lngKeyName = this.ctx.settings.lngKeyName || 'longitude';
        }

        this.locationSettings.tooltipPattern = this.ctx.settings.tooltipPattern || "<b>${entityName}</b><br/><br/><b>纬度:</b> ${"+this.locationSettings.latKeyName+":7}<br/><b>经度:</b> ${"+this.locationSettings.lngKeyName+":7}";

        this.locationSettings.showLabel = this.ctx.settings.showLabel !== false;
        this.locationSettings.displayTooltip = this.ctx.settings.showTooltip !== false;
        this.locationSettings.autocloseTooltip = this.ctx.settings.autocloseTooltip !== false;
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
            this.locationSettings.useMarkerImage = true;
            var url = this.ctx.settings.markerImage;
            var size = this.ctx.settings.markerImageSize || 34;
            this.locationSettings.currentImage = {
                url: url,
                size: size
            };
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

        function updateLocationMarkerIcon(location, image) {
            if (image && (!location.settings.currentImage || !angular.equals(location.settings.currentImage, image))) {
                location.settings.currentImage = image;
                tbMap.map.updateMarkerIcon(location.marker, location.settings);
            }
        }

        function updateLocationStyle(location, dataMap) {
            updateLocationLabel(location);
            var color = calculateLocationColor(location, dataMap);
            var image = calculateLocationMarkerImage(location, dataMap);
            updateLocationColor(location, color, image);
            updateLocationMarkerIcon(location, image);
        }

        function createOrUpdateLocationMarker(location, markerLocation, dataMap) {
            var changed = false;
            if (!location.marker) {
                var image = calculateLocationMarkerImage(location, dataMap);
                if (image && (!location.settings.currentImage || !angular.equals(location.settings.currentImage, image))) {
                    location.settings.currentImage = image;
                }
                location.marker = tbMap.map.createMarker(markerLocation, location.settings,
                    function (event) {
                        tbMap.callbacks.onLocationClick(location);
                        locationRowClick(event, location);
                    }, [location.dsIndex]);
                tbMap.markers.push(location.marker);
                changed = true;
            } else {
                var prevPosition = tbMap.map.getMarkerPosition(location.marker);
                if (!prevPosition.equals(markerLocation)) {
                    tbMap.map.setMarkerPosition(location.marker, markerLocation);
                    changed = true;
                }
            }
            return changed;
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
                            if (angular.isDefined(lat) && lat != null && angular.isDefined(lng) && lng != null) {
                                latLng = tbMap.map.createLatLng(lat, lng);
                                if (i == 0 || !latLngs[latLngs.length - 1].equals(latLng)) {
                                    latLngs.push(latLng);
                                }
                            }
                        }
                        if (latLngs.length > 0) {
                            var markerLocation = latLngs[latLngs.length - 1];
                            createOrUpdateLocationMarker(location, markerLocation, dataMap);
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
                        if (angular.isDefined(lat) && lat != null && angular.isDefined(lng) && lng != null) {
                            latLng = tbMap.map.createLatLng(lat, lng);
                            if (createOrUpdateLocationMarker(location, latLng, dataMap)) {
                                locationChanged = true;
                            }
                        }
                    }
                    if (location.marker) {
                        updateLocationStyle(location, dataMap);
                    }
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
        } else if (mapProvider === 'openstreet-map-local') {
            schema = angular.copy(openstreetMapLocalSettingsSchema);
        } else if (mapProvider === 'image-map') {
            return imageMapSettingsSchema;
        } else if (mapProvider === 'tencent-map') {
            schema = angular.copy(tencentMapSettingsSchema);
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
            "title":"谷歌地图配置",
            "type":"object",
            "properties":{
                "gmApiKey":{
                    "title":"Google Maps API Key",
                    "type":"string"
                },
                "gmDefaultMapType":{
                    "title":"缺省地图类型",
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
                        "label":"街道图"
                    },
                    {
                        "value":"satellite",
                        "label":"卫星图"
                    },
                    {
                        "value":"hybrid",
                        "label":"混合图"
                    },
                    {
                        "value":"terrain",
                        "label":"地形图"
                    }
                ]
            }
        ]
    };
    
const tencentMapSettingsSchema =
    {
        "schema":{
            "title":"腾讯地图配置",
            "type":"object",
            "properties":{
                "tmApiKey":{
                    "title":"Tencent Maps API Key",
                    "type":"string"
                },
                "tmDefaultMapType":{
                    "title":"缺省的地图类型",
                    "type":"string",
                    "default":"roadmap"
                }
            },
            "required":[
                "tmApiKey"
            ]
        },
        "form":[
            "tmApiKey",
            {
                "key":"tmDefaultMapType",
                "type":"rc-select",
                "multiple":false,
                "items":[
                    {
                        "value":"roadmap",
                        "label":"街道图"
                    },
                    {
                        "value":"satellite",
                        "label":"卫星图"
                    },
                    {
                        "value":"hybrid",
                        "label":"混合图"
                    },
                ]
            }
        ]
    };
    
const openstreetMapSettingsSchema =
    {
        "schema":{
            "title":"Openstreet 地图配置",
            "type":"object",
            "properties":{
                "mapProvider":{
                    "title":"地图提供者",
                    "type":"string",
                    "default":"OpenStreetMap.Mapnik"
                }
            },
            "required":[
            ]
        },
        "form":[
            {
                "key":"mapProvider",
                "type":"rc-select",
                "multiple":false,
                "items":[
                    {
                        "value":"OpenStreetMap.Mapnik",
                        "label":"Mapnik (缺省)"
                    },
                    {
                        "value":"OpenStreetMap.BlackAndWhite",
                        "label":"黑白"
                    },
                    {
                        "value":"OpenStreetMap.HOT",
                        "label":"OpenStreetMap.HOT"
                    },
                    {
                        "value":"Esri.WorldStreetMap",
                        "label":"Esri.世界街道图"
                    },
                    {
                        "value":"Esri.WorldTopoMap",
                        "label":"Esri.世界拓扑图"
                    },
                    {
                        "value":"CartoDB.Positron",
                        "label":"CartoDB.正电子"
                    },
                    {
                        "value":"CartoDB.DarkMatter",
                        "label":"CartoDB.暗物质"
                    }
                ]
            }
        ]
    };

const openstreetMapLocalSettingsSchema =
    {
        "schema":{
            "title":"本地地图配置",
            "type":"object",
            "properties":{
                "mapProvider":{
                    "title":"地图服务提供者",
                    "type":"string",
                    "default":"OpenStreetMapLocal.Bright"
                },
                "showLayerControl": {
                    "title": "地图上显示图层控制",
                    "type": "boolean",
                    "default": false
                }, 
                "showDrawToolBar": {
                    "title": "地图上显示标图工具条",
                    "type": "boolean",
                    "default": false
                },                              
                "geoJsonLayers": {
                    "title": "添加GeoJson多边形",
                    "type": "array",
                    "items": {
                        "title": "多边形",
                        "type": "string"
                    }
                }   
            },
            "required":[
            ]
        },
        "form":[
            {
                "key":"mapProvider",
                "type":"rc-select",
                "multiple":false,
                "items":[
                    {
                        "value":"OpenStreetMapLocal.Bright",
                        "label":"明亮风格(缺省)"
                    }, 
                    {
                        "value":"OpenStreetMapLocal.Klokantech",
                        "label":"低亮风格"
                    },
                    {
                        "value":"OpenStreetMapLocal.DarkMatter",
                        "label":"黑夜风格"
                    },
                    {
                        "value":"OpenStreetMapLocal.Positron",
                        "label":"青灰风格"
                    }
                ]
            },
            "showLayerControl",
            "showDrawToolBar",
            {
                "key": "geoJsonLayers",
                "items": [
                    {
                        "key": "geoJsonLayers[]",
                        "type": "textarea"
                    }
                ]
            } 
        ]
    };    

const commonMapSettingsSchema =
    {
        "schema":{
            "title":"地图配置",
            "type":"object",
            "properties":{
                "defaultZoomLevel":{
                    "title":"缺省缩放级别 (1 - 20)",
                    "type":"number"
                },
                "fitMapBounds":{
                    "title":"自动缩放地图以包含所有标记",
                    "type":"boolean",
                    "default":true
                },
                "latKeyName":{
                    "title":"纬度属性名称",
                    "type":"string",
                    "default":"latitude"
                },
                "lngKeyName":{
                    "title":"经度属性名称",
                    "type":"string",
                    "default":"longitude"
                },
                "showLabel":{
                    "title":"显示标注",
                    "type":"boolean",
                    "default":true
                },
                "label":{
                    "title":"标注 (实例: '${entityName}', '${entityName}: (文本 ${keyName} 单位.)' )",
                    "type":"string",
                    "default":"<div style=\"position: relative; white-space: nowrap; text-align: center; font-size: 14px; top: 5px;\">     <span style=\"border: 2px solid #000; border-radius: 10px; color: #000; background-color: #fff; padding-left: 5px; padding-right: 5px; padding-top: 5px; padding-bottom: 3px;\">${entityName}</span></div>"
                },
                "showTooltip": {
                    "title": "显示信息提示框",
                    "type":"boolean",
                    "default":true
                },
                "autocloseTooltip": {
                    "title": "自动关闭信息提示框",
                    "type":"boolean",
                    "default":true
                },
                "tooltipPattern":{
                    "title":"信息提示框 (例如. '文本 ${keyName} 单位.' 或 <link-act name='my-action'>详细信息</link-act>')",
                    "type":"string",
                    "default":"<b>${entityName}</b><br/><br/><b>纬度:</b> ${latitude:7}<br/><b>经度:</b> ${longitude:7}"
                },
                "color":{
                    "title":"颜色",
                    "type":"string"
                },
                "useColorFunction":{
                    "title":"使用颜色函数",
                    "type":"boolean",
                    "default":false
                },
                "colorFunction":{
                    "title":"颜色函数: f(data, dsData, dsIndex)",
                    "type":"string"
                },
                "markerImage":{
                    "title":"自定义图标",
                    "type":"string"
                },
                "markerImageSize":{
                    "title":"自定义图标大小 (像素)",
                    "type":"number",
                    "default":34
                },
                "useMarkerImageFunction":{
                    "title":"使用自定义图标函数",
                    "type":"boolean",
                    "default":false
                },
                "markerImageFunction":{
                    "title":"图标函数: f(data, images, dsData, dsIndex)",
                    "type":"string"
                },
                "markerImages":{
                    "title":"图标",
                    "type":"array",
                    "items":{
                        "title":"图像",
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
            "autocloseTooltip",
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
            "title":"路径图配置",
            "type":"object",
            "properties":{
                "strokeWeight": {
                    "title": "线粗细",
                    "type": "number",
                    "default": 2
                },
                "strokeOpacity": {
                    "title": "线透明度",
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
                "title": "背景图",
                "type": "string",
                "default": "data:image/svg+xml;base64,PHN2ZyBpZD0ic3ZnMiIgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIGhlaWdodD0iMTAwIiB3aWR0aD0iMTAwIiB2ZXJzaW9uPSIxLjEiIHhtbG5zOmNjPSJodHRwOi8vY3JlYXRpdmVjb21tb25zLm9yZy9ucyMiIHhtbG5zOmRjPSJodHRwOi8vcHVybC5vcmcvZGMvZWxlbWVudHMvMS4xLyIgdmlld0JveD0iMCAwIDEwMCAxMDAiPgogPGcgaWQ9ImxheWVyMSIgdHJhbnNmb3JtPSJ0cmFuc2xhdGUoMCAtOTUyLjM2KSI+CiAgPHJlY3QgaWQ9InJlY3Q0Njg0IiBzdHJva2UtbGluZWpvaW49InJvdW5kIiBoZWlnaHQ9Ijk5LjAxIiB3aWR0aD0iOTkuMDEiIHN0cm9rZT0iIzAwMCIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIiB5PSI5NTIuODYiIHg9Ii40OTUwNSIgc3Ryb2tlLXdpZHRoPSIuOTkwMTAiIGZpbGw9IiNlZWUiLz4KICA8dGV4dCBpZD0idGV4dDQ2ODYiIHN0eWxlPSJ3b3JkLXNwYWNpbmc6MHB4O2xldHRlci1zcGFjaW5nOjBweDt0ZXh0LWFuY2hvcjptaWRkbGU7dGV4dC1hbGlnbjpjZW50ZXIiIGZvbnQtd2VpZ2h0PSJib2xkIiB4bWw6c3BhY2U9InByZXNlcnZlIiBmb250LXNpemU9IjEwcHgiIGxpbmUtaGVpZ2h0PSIxMjUlIiB5PSI5NzAuNzI4MDkiIHg9IjQ5LjM5NjQ3NyIgZm9udC1mYW1pbHk9IlJvYm90byIgZmlsbD0iIzY2NjY2NiI+PHRzcGFuIGlkPSJ0c3BhbjQ2OTAiIHg9IjUwLjY0NjQ3NyIgeT0iOTcwLjcyODA5Ij5JbWFnZSBiYWNrZ3JvdW5kIDwvdHNwYW4+PHRzcGFuIGlkPSJ0c3BhbjQ2OTIiIHg9IjQ5LjM5NjQ3NyIgeT0iOTgzLjIyODA5Ij5pcyBub3QgY29uZmlndXJlZDwvdHNwYW4+PC90ZXh0PgogIDxyZWN0IGlkPSJyZWN0NDY5NCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCIgaGVpZ2h0PSIxOS4zNiIgd2lkdGg9IjY5LjM2IiBzdHJva2U9IiMwMDAiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgeT0iOTkyLjY4IiB4PSIxNS4zMiIgc3Ryb2tlLXdpZHRoPSIuNjM5ODYiIGZpbGw9Im5vbmUiLz4KIDwvZz4KPC9zdmc+Cg=="
            },
            "imageEntityAlias": {
                "title": "图片 URL 源实体别名",//"Image URL source entity alias",
                "type": "string",
                "default": ""
            },
            "imageUrlAttribute": {
                "title": "图片 URL 源实体属性",//"Image URL source entity attribute",
                "type": "string",
                "default": ""
            },
            "xPosKeyName":{
                "title":"X 坐标属性名称",
                "type":"string",
                "default":"xPos"
            },
            "yPosKeyName":{
                "title":"Y 坐标属性名称",
                "type":"string",
                "default":"yPos"
            },
            "showLabel":{
                "title":"显示标注",
                "type":"boolean",
                "default":true
            },
            "label":{
                "title":"标注 (范例: '${entityName}', '${entityName}: (文本 ${keyName} 单位.)' )",
                "type":"string",
                "default":"${entityName}"
            },
            "showTooltip": {
                "title": "显示信息提示框",
                "type":"boolean",
                "default":true
            },
            "autocloseTooltip": {
                "title": "自动关闭信息提示框",
                "type":"boolean",
                "default":true
            },
            "tooltipPattern":{
                "title":"信息提示框 (例如. '文本 ${keyName} 单位.' 或 <link-act name='my-action'>详细</link-act>')",
                "type":"string",
                "default":"<b>${entityName}</b><br/><br/><b>X 坐标:</b> ${xPos:2}<br/><b>Y 坐标:</b> ${yPos:2}"
            },
            "color":{
                "title":"Color",
                "type":"string"
            },
            "posFunction":{
                "title":"位置转换函数: f(origXPos, origYPos), 应该返回0到1之间的x,y坐标的双精度数字",
                "type":"string",
                "default": "return {x: origXPos, y: origYPos};"
            },
            "markerOffsetX": {
                "title": "图标在 X 方向的偏移量",
                "type": "number",
                "default": 0.5
            },
            "markerOffsetY": {
                "title": "图标在 Y 方向的偏移量",
                "type": "number",
                "default": 1
            },
            "useColorFunction":{
                "title":"使用颜色函数",
                "type":"boolean",
                "default":false
            },
            "colorFunction":{
                "title":"C颜色函数: f(data, dsData, dsIndex)",
                "type":"string"
            },
            "markerImage":{
                "title":"自定义图标",
                "type":"string"
            },
            "markerImageSize":{
                "title":"自定义图标大小 (像素为单位)",
                "type":"number",
                "default":34
            },
            "useMarkerImageFunction":{
                "title":"使用颜色图标函数",
                "type":"boolean",
                "default":false
            },
            "markerImageFunction":{
                "title":"颜色图标函数: f(data, images, dsData, dsIndex)",
                "type":"string"
            },
            "markerImages":{
                "title":"图标",
                "type":"array",
                "items":{
                    "title":"图像",
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
        "autocloseTooltip",
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