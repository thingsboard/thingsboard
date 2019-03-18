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
import tinycolor from 'tinycolor2';

import TbGoogleMap from './google-map';
import TbOpenStreetMap from './openstreet-map';
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
		this.polygons = [];
		if (this.drawRoutes) {
			this.polylines = [];
		}

		this.locationSettings = {};

		var settings = ctx.settings;

		this.callbacks = {};
		this.callbacks.onLocationClick = function () {
		};

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


		var initCallback = function () {
			tbMap.update();
			tbMap.resize();
		};

		this.ctx.$scope.onTooltipAction = function (event, actionName, dsIndex) {
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
			this.map = new TbOpenStreetMap($element, this.utils, initCallback, this.defaultZoomLevel, this.dontFitMapBounds, minZoomLevel, settings.mapProvider);
		} else if (mapProvider === 'image-map') {
			this.map = new TbImageMap(this.ctx, $element, this.utils, initCallback,
				settings.mapImageUrl,
				settings.posFunction,
				settings.imageEntityAlias,
				settings.imageUrlAttribute);
		} else if (mapProvider === 'tencent-map') {
			this.map = new TbTencentMap($element, this.utils, initCallback, this.defaultZoomLevel, this.dontFitMapBounds, minZoomLevel, settings.tmApiKey, settings.tmDefaultMapType);
		}


		tbMap.initBounds = true;
	}

	setCallbacks(callbacks) {
		Object.assign(this.callbacks, callbacks);
	}

	clearLocations() {
		if (this.locations) {
			var tbMap = this;
			this.locations.forEach(function (location) {
				if (location.marker) {
					tbMap.map.removeMarker(location.marker);
				}
				if (location.polyline) {
					tbMap.map.removePolyline(location.polyline);
				}
				if (location.polygon) {
					tbMap.map.removePolygon(location.polygon);
				}
			});
			this.locations = null;
			this.markers = [];
			this.polygons = [];
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

		if (this.mapProvider == 'image-map') {
			this.locationSettings.latKeyName = this.ctx.settings.xPosKeyName || 'xPos';
			this.locationSettings.lngKeyName = this.ctx.settings.yPosKeyName || 'yPos';
			this.locationSettings.markerOffsetX = angular.isDefined(this.ctx.settings.markerOffsetX) ? this.ctx.settings.markerOffsetX : 0.5;
			this.locationSettings.markerOffsetY = angular.isDefined(this.ctx.settings.markerOffsetY) ? this.ctx.settings.markerOffsetY : 1;
		} else {
			this.locationSettings.latKeyName = this.ctx.settings.latKeyName || 'latitude';
			this.locationSettings.lngKeyName = this.ctx.settings.lngKeyName || 'longitude';
			this.locationSettings.polygonKeyName = this.ctx.settings.polygonKeyName || 'coordinates';
		}

		this.locationSettings.tooltipPattern = this.ctx.settings.tooltipPattern || "<b>${entityName}</b><br/><br/><b>Latitude:</b> ${" + this.locationSettings.latKeyName + ":7}<br/><b>Longitude:</b> ${" + this.locationSettings.lngKeyName + ":7}";

		this.locationSettings.showLabel = this.ctx.settings.showLabel !== false;
		this.locationSettings.displayTooltip = this.ctx.settings.showTooltip !== false;
		this.locationSettings.autocloseTooltip = this.ctx.settings.autocloseTooltip !== false;
		this.locationSettings.showPolygon = this.ctx.settings.showPolygon !== false;
		this.locationSettings.labelColor = this.ctx.widgetConfig.color || '#000000';
		this.locationSettings.label = this.ctx.settings.label || "${entityName}";
		this.locationSettings.color = this.ctx.settings.color ? tinycolor(this.ctx.settings.color).toHexString() : "#FE7569";
		this.locationSettings.polygonColor = this.ctx.settings.polygonColor ? tinycolor(this.ctx.settings.polygonColor).toHexString() : "#0000ff";
		this.locationSettings.polygonStrokeColor = this.ctx.settings.polygonStrokeColor ? tinycolor(this.ctx.settings.polygonStrokeColor).toHexString() : "#fe0001";
		this.locationSettings.polygonOpacity = angular.isDefined(this.ctx.settings.polygonOpacity) ? this.ctx.settings.polygonOpacity : 0.5;
		this.locationSettings.polygonStrokeOpacity = angular.isDefined(this.ctx.settings.polygonStrokeOpacity) ? this.ctx.settings.polygonStrokeOpacity : 1;
		this.locationSettings.polygonStrokeWeight = angular.isDefined(this.ctx.settings.polygonStrokeWeight) ? this.ctx.settings.polygonStrokeWeight : 1;

		this.locationSettings.useLabelFunction = this.ctx.settings.useLabelFunction === true;
		if (angular.isDefined(this.ctx.settings.labelFunction) && this.ctx.settings.labelFunction.length > 0) {
			try {
				this.locationSettings.labelFunction = new Function('data, dsData, dsIndex', this.ctx.settings.labelFunction);
			} catch (e) {
				this.locationSettings.labelFunction = null;
			}
		}

		this.locationSettings.useTooltipFunction = this.ctx.settings.useTooltipFunction === true;
		if (angular.isDefined(this.ctx.settings.tooltipFunction) && this.ctx.settings.tooltipFunction.length > 0) {
			try {
				this.locationSettings.tooltipFunction = new Function('data, dsData, dsIndex', this.ctx.settings.tooltipFunction);
			} catch (e) {
				this.locationSettings.tooltipFunction = null;
			}
		}

		this.locationSettings.useColorFunction = this.ctx.settings.useColorFunction === true;
		if (angular.isDefined(this.ctx.settings.colorFunction) && this.ctx.settings.colorFunction.length > 0) {
			try {
				this.locationSettings.colorFunction = new Function('data, dsData, dsIndex', this.ctx.settings.colorFunction);
			} catch (e) {
				this.locationSettings.colorFunction = null;
			}
		}
		this.locationSettings.usePolygonColorFunction = this.ctx.settings.usePolygonColorFunction === true;
		if (angular.isDefined(this.ctx.settings.polygonColorFunction) && this.ctx.settings.polygonColorFunction.length > 0) {
			try {
				this.locationSettings.polygonColorFunction = new Function('data, dsData, dsIndex', this.ctx.settings.polygonColorFunction);
			} catch (e) {
				this.locationSettings.polygonColorFunction = null;
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


		function updateLocationLabel(location, dataMap) {
			if (location.settings.showLabel) {
				if (location.settings.useLabelFunction && location.settings.labelFunction) {
					try {
						location.settings.label = location.settings.labelFunction(dataMap.dataMap, dataMap.dsDataMap, location.dsIndex);
					} catch (e) {
						location.settings.label = null;
					}
					if (location.settings.label) {
						var datasources = tbMap.subscription.datasources;
						location.settings.label = tbMap.utils.createLabelFromDatasource(datasources[location.dsIndex], location.settings.label);
						location.settings.labelReplaceInfo = processPattern(location.settings.label, datasources, location.dsIndex);
						location.settings.labelText = location.settings.label;
					}
				}
				if (location.settings.labelReplaceInfo.variables.length) {
					location.settings.labelText = fillPattern(location.settings.label,
						location.settings.labelReplaceInfo, tbMap.subscription.data);
				}
				tbMap.map.updateMarkerLabel(location.marker, location.settings);
			}
		}


		function calculateLocationColor(location, dataMap) {
			if (location.settings.useColorFunction && location.settings.colorFunction) {
				var color;
				try {
					color = location.settings.colorFunction(dataMap.dataMap, dataMap.dsDataMap, location.dsIndex);
				} catch (e) {/**/
				}
				if (!color) {
					color = '#FE7569';
				}
				return tinycolor(color).toHexString();
			} else {
				return location.settings.color;
			}
		}

		function calculateLocationPolygonColor(location, dataMap) {
			if (location.settings.usePolygonColorFunction && location.settings.polygonColorFunction) {
				var color;
				try {
					color = location.settings.polygonColorFunction(dataMap.dataMap, dataMap.dsDataMap, location.dsIndex);
				} catch (e) {/**/
				}
				if (!color) {
					color = '#007800';
				}
				return tinycolor(color).toHexString();
			} else {
				return location.settings.polygonColor;
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

		function updateLocationPolygonColor(location, color) {
			if (location.polygon && color) {
				location.settings.calculatedPolygonColor = color;
				tbMap.map.updatePolygonColor(location.polygon, location.settings, color);
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
			updateLocationLabel(location, dataMap);
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
				location.marker = tbMap.map.createMarker(markerLocation, location.dsIndex, location.settings,
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
		function locationPolygonClick($event, location) {
			var descriptors = tbMap.ctx.actionsApi.getActionDescriptors('polygonClick');
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

		function createUpdatePolygon(location, dataMap) {
			if (location.settings.showPolygon && dataMap.dsDataMap[location.dsIndex][location.settings.polygonKeyName] !== null) {
				let polygonLatLngsRaw = angular.fromJson(dataMap.dsDataMap[location.dsIndex][location.settings.polygonKeyName]);
				let polygonLatLngs = !polygonLatLngsRaw || mapPolygonArray(polygonLatLngsRaw);
				if (!location.polygon && polygonLatLngs.length > 0) {
					location.polygon = tbMap.map.createPolygon(polygonLatLngs, location.settings, location, function (event) {
						tbMap.callbacks.onLocationClick(location);
						locationPolygonClick(event, location);
					}, [location.dsIndex]);
					tbMap.polygons.push(location.polygon);
					if (location.settings.usePolygonColorFunction) updateLocationPolygonColor(location, calculateLocationPolygonColor(location, dataMap));
				} else if (polygonLatLngs.length > 0) {
					let prevPolygonArr = tbMap.map.getPolygonLatLngs(location.polygon);
					if (!prevPolygonArr || !arraysEqual(prevPolygonArr, polygonLatLngs)) {
						tbMap.map.setPolygonLatLngs(location.polygon, polygonLatLngs);
					}
					if (location.settings.usePolygonColorFunction) updateLocationPolygonColor(location, calculateLocationPolygonColor(location, dataMap));
				}
			}
		}

		function loadLocations(data, datasources) {
			var bounds = tbMap.map.createBounds();
			tbMap.locations = [];
			var dataMap = toLabelValueMap(data, datasources);
			var currentDatasource = null;
			var currentDatasourceIndex = -1;
			var latIndex = -1;
			var lngIndex = -1;
			for (var i = 0; i < data.length; i++) {
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
			data.forEach(function (dataEl, index) {
				let nameToCheck;
				if (dataEl.dataKey.locationAttrName) {
					nameToCheck = dataEl.dataKey.locationAttrName;
				} else {
					nameToCheck = dataEl.dataKey.label;
				}
				if (nameToCheck === tbMap.locationSettings.polygonKeyName) {
					let location = {
						polIndex: index,
						settings: angular.copy(tbMap.locationSettings)
					};
					location.dsIndex = datasources.findIndex(function (ds) {
						return dataEl.datasource.entityId === ds.entityId;
					});
					tbMap.locations.push(location);
					createUpdatePolygon(location, dataMap);
					tbMap.map.extendBounds(bounds, location.polygon);
				}
			});

			tbMap.map.fitBounds(bounds);
		}

		function mapPolygonArray (rawArray) {
			let latLngArray = rawArray.map(function (el) {
				if (el.length === 2) {
					if (!angular.isNumber(el[0]) && !angular.isNumber(el[1])) {
						return el.map(function (subEl) {
							return mapPolygonArray(subEl);
						})
					} else {
						return tbMap.map.createLatLng(el[0], el[1]);
					}
				} else if (el.length > 2) {
					return mapPolygonArray(el);
				} else {
					return tbMap.map.createLatLng(false);
				}
			});
			return latLngArray;
		}

		function updateLocations(data, datasources) {
			var locationsChanged = false;
			var bounds = tbMap.map.createBounds();
			var dataMap = toLabelValueMap(data, datasources);
			for (var p = 0; p < tbMap.locations.length; p++) {
				var location = tbMap.locations[p];
				locationsChanged |= updateLocation(location, data, dataMap);
				createUpdatePolygon(location, dataMap);
				if (location.polyline) {
					tbMap.map.extendBounds(bounds, location.polyline);
				} else if (location.marker) {
					tbMap.map.extendBoundsWithMarker(bounds, location.marker);
				} else if (location.polygon) {
					tbMap.map.extendBounds(bounds, location.polygon);
				}
			}
			if (locationsChanged && tbMap.initBounds) {
				let dataReceived = datasources.every(
					function (ds) {
						return ds.dataReceived === true;
					});
				let dataValid = dataReceived && dataMap.dsDataMap.every(function (ds) {
					return !(!ds[tbMap.locationSettings.latKeyName] && !ds[tbMap.locationSettings.lngKeyName]);
				});
				tbMap.initBounds = !dataValid;
				tbMap.map.fitBounds(bounds);
			}
		}

		function createTooltipContent(tooltip, data, datasources) {
			var content;
			var settings = tooltip.locationSettings;
			if (settings.useTooltipFunction && settings.tooltipFunction) {
				var dataMap = toLabelValueMap(data, datasources);
				try {
					settings.tooltipPattern = settings.tooltipFunction(dataMap.dataMap, dataMap.dsDataMap, tooltip.dsIndex);
				} catch (e) {
					settings.tooltipPattern = null;
				}
				if (settings.tooltipPattern) {
					settings.tooltipPattern = tbMap.utils.createLabelFromDatasource(datasources[tooltip.dsIndex], settings.tooltipPattern);
					settings.tooltipReplaceInfo = processPattern(settings.tooltipPattern, datasources, tooltip.dsIndex);
				}
			}
			content = fillPattern(settings.tooltipPattern, settings.tooltipReplaceInfo, data);
			return fillPatternWithActions(content, 'onTooltipAction', tooltip.markerArgs);
		}

		if (this.map && this.map.inited() && this.subscription) {
			if (this.subscription.data) {
				if (!this.locations) {
					loadLocations(this.subscription.data, this.subscription.datasources);
				} else {
					updateLocations(this.subscription.data, this.subscription.datasources);
				}
				var tooltips = this.map.getTooltips();
				for (var t = 0; t < tooltips.length; t++) {
					var tooltip = tooltips[t];
					var text = createTooltipContent(tooltip, this.subscription.data, this.subscription.datasources);
					tooltip.popup.setContent(text);
				}
			}
		}
	}

	resize() {
		if (this.map && this.map.inited()) {
			let map = this.map;
			if (this.locations && this.locations.length > 0) {
				map.invalidateSize();
				var bounds = map.createBounds();
				if (this.markers && this.markers.length>0) {
					this.markers.forEach(function (marker) {
						map.extendBoundsWithMarker(bounds, marker);
					});
				}
				if (this.polylines && this.polylines.length>0) {
					this.polylines.forEach(function (polyline) {
						map.extendBounds(bounds, polyline);
					})
				}
				if (this.polygons && this.polygons.length > 0) {
					this.polygons.forEach(function (polygon) {
						map.extendBounds(bounds, polygon);
					})
				}
				map.fitBounds(bounds);
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
			'polygonClick': {
				name: 'widget-action.polygon-click',
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
		"schema": {
			"title": "Google Map Configuration",
			"type": "object",
			"properties": {
				"gmApiKey": {
					"title": "Google Maps API Key",
					"type": "string"
				},
				"gmDefaultMapType": {
					"title": "Default map type",
					"type": "string",
					"default": "roadmap"
				}
			},
			"required": [
				"gmApiKey"
			]
		},
		"form": [
			"gmApiKey",
			{
				"key": "gmDefaultMapType",
				"type": "rc-select",
				"multiple": false,
				"items": [
					{
						"value": "roadmap",
						"label": "Roadmap"
					},
					{
						"value": "satellite",
						"label": "Satellite"
					},
					{
						"value": "hybrid",
						"label": "Hybrid"
					},
					{
						"value": "terrain",
						"label": "Terrain"
					}
				]
			}
		]
	};

const tencentMapSettingsSchema =
	{
		"schema": {
			"title": "Tencent Map Configuration",
			"type": "object",
			"properties": {
				"tmApiKey": {
					"title": "Tencent Maps API Key",
					"type": "string"
				},
				"tmDefaultMapType": {
					"title": "Default map type",
					"type": "string",
					"default": "roadmap"
				}
			},
			"required": [
				"tmApiKey"
			]
		},
		"form": [
			"tmApiKey",
			{
				"key": "tmDefaultMapType",
				"type": "rc-select",
				"multiple": false,
				"items": [
					{
						"value": "roadmap",
						"label": "Roadmap"
					},
					{
						"value": "satellite",
						"label": "Satellite"
					},
					{
						"value": "hybrid",
						"label": "Hybrid"
					},
				]
			}
		]
	};

const openstreetMapSettingsSchema =
	{
		"schema": {
			"title": "Openstreet Map Configuration",
			"type": "object",
			"properties": {
				"mapProvider": {
					"title": "Map provider",
					"type": "string",
					"default": "OpenStreetMap.Mapnik"
				}
			},
			"required": []
		},
		"form": [
			{
				"key": "mapProvider",
				"type": "rc-select",
				"multiple": false,
				"items": [
					{
						"value": "OpenStreetMap.Mapnik",
						"label": "OpenStreetMap.Mapnik (Default)"
					},
					{
						"value": "OpenStreetMap.BlackAndWhite",
						"label": "OpenStreetMap.BlackAndWhite"
					},
					{
						"value": "OpenStreetMap.HOT",
						"label": "OpenStreetMap.HOT"
					},
					{
						"value": "Esri.WorldStreetMap",
						"label": "Esri.WorldStreetMap"
					},
					{
						"value": "Esri.WorldTopoMap",
						"label": "Esri.WorldTopoMap"
					},
					{
						"value": "CartoDB.Positron",
						"label": "CartoDB.Positron"
					},
					{
						"value": "CartoDB.DarkMatter",
						"label": "CartoDB.DarkMatter"
					}
				]
			}
		]
	};

const commonMapSettingsSchema =
	{
		"schema": {
			"title": "Map Configuration",
			"type": "object",
			"properties": {
				"defaultZoomLevel": {
					"title": "Default map zoom level (1 - 20)",
					"type": "number"
				},
				"fitMapBounds": {
					"title": "Fit map bounds to cover all markers",
					"type": "boolean",
					"default": true
				},
				"latKeyName": {
					"title": "Latitude key name",
					"type": "string",
					"default": "latitude"
				},
				"lngKeyName": {
					"title": "Longitude key name",
					"type": "string",
					"default": "longitude"
				},
				"showLabel": {
					"title": "Show label",
					"type": "boolean",
					"default": true
				},
				"label": {
					"title": "Label (pattern examples: '${entityName}', '${entityName}: (Text ${keyName} units.)' )",
					"type": "string",
					"default": "${entityName}"
				},
				"useLabelFunction": {
					"title": "Use label function",
					"type": "boolean",
					"default": false
				},
				"labelFunction": {
					"title": "Label function: f(data, dsData, dsIndex)",
					"type": "string"
				},
				"showTooltip": {
					"title": "Show tooltip",
					"type": "boolean",
					"default": true
				},
				"autocloseTooltip": {
					"title": "Auto-close tooltips",
					"type": "boolean",
					"default": true
				},
				"tooltipPattern": {
					"title": "Tooltip (for ex. 'Text ${keyName} units.' or <link-act name='my-action'>Link text</link-act>')",
					"type": "string",
					"default": "<b>${entityName}</b><br/><br/><b>Latitude:</b> ${latitude:7}<br/><b>Longitude:</b> ${longitude:7}"
				},
				"useTooltipFunction": {
					"title": "Use tooltip function",
					"type": "boolean",
					"default": false
				},
				"tooltipFunction": {
					"title": "Tooltip function: f(data, dsData, dsIndex)",
					"type": "string"
				},
				"color": {
					"title": "Color",
					"type": "string"
				},
				"useColorFunction": {
					"title": "Use color function",
					"type": "boolean",
					"default": false
				},
				"colorFunction": {
					"title": "Color function: f(data, dsData, dsIndex)",
					"type": "string"
				},
				"showPolygon": {
					"title": "Show polygon",
					"type": "boolean",
					"default": false
				},
				"polygonKeyName": {
					"title": "Polygon key name",
					"type": "string",
					"default": "coordinates"
				},
				"polygonColor": {
					"title": "Polygon color",
					"type": "string"
				},
				"polygonOpacity": {
					"title": "Polygon opacity",
					"type": "number",
					"default": 0.5
				},
				"polygonStrokeColor": {
					"title": "Stroke color",
					"type": "string"
				},
				"polygonStrokeOpacity": {
					"title": "Stroke opacity",
					"type": "number",
					"default": 1
				},
				"polygonStrokeWeight": {
					"title": "Stroke weight",
					"type": "number",
					"default": 1
				},
				"usePolygonColorFunction": {
					"title": "Use polygon color function",
					"type": "boolean",
					"default": false
				},
				"polygonColorFunction": {
					"title": "Polygon Color function: f(data, dsData, dsIndex)",
					"type": "string"
				},
				"markerImage": {
					"title": "Custom marker image",
					"type": "string"
				},
				"markerImageSize": {
					"title": "Custom marker image size (px)",
					"type": "number",
					"default": 34
				},
				"useMarkerImageFunction": {
					"title": "Use marker image function",
					"type": "boolean",
					"default": false
				},
				"markerImageFunction": {
					"title": "Marker image function: f(data, images, dsData, dsIndex)",
					"type": "string"
				},
				"markerImages": {
					"title": "Marker images",
					"type": "array",
					"items": {
						"title": "Marker image",
						"type": "string"
					}
				}
			},
			"required": []
		},
		"form": [
			"defaultZoomLevel",
			"fitMapBounds",
			"latKeyName",
			"lngKeyName",
			"showLabel",
			"label",
			"useLabelFunction",
			{
				"key": "labelFunction",
				"type": "javascript"
			},
			"showTooltip",
			"autocloseTooltip",
			{
				"key": "tooltipPattern",
				"type": "textarea"
			},
			"useTooltipFunction",
			{
				"key": "tooltipFunction",
				"type": "javascript"
			},
			{
				"key": "color",
				"type": "color"
			},
			"useColorFunction",
			{
				"key": "colorFunction",
				"type": "javascript"
			}, "showPolygon", "polygonKeyName",
			{
				"key": "polygonColor",
				"type": "color"
			},
			"polygonOpacity",
			{
				"key": "polygonStrokeColor",
				"type": "color"
			},
			"polygonStrokeOpacity","polygonStrokeWeight","usePolygonColorFunction",
			{
				"key": "polygonColorFunction",
				"type": "javascript"
			},
			{
				"key": "markerImage",
				"type": "image"
			},
			"markerImageSize",
			"useMarkerImageFunction",
			{
				"key": "markerImageFunction",
				"type": "javascript"
			},
			{
				"key": "markerImages",
				"items": [
					{
						"key": "markerImages[]",
						"type": "image"
					}
				]
			}
		]
	};

const routeMapSettingsSchema =
	{
		"schema": {
			"title": "Route Map Configuration",
			"type": "object",
			"properties": {
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
			"required": []
		},
		"form": [
			"strokeWeight",
			"strokeOpacity"
		]
	};

const imageMapSettingsSchema =
	{
		"schema": {
			"title": "Image Map Configuration",
			"type": "object",
			"properties": {
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
				"xPosKeyName": {
					"title": "X position key name",
					"type": "string",
					"default": "xPos"
				},
				"yPosKeyName": {
					"title": "Y position key name",
					"type": "string",
					"default": "yPos"
				},
				"showLabel": {
					"title": "Show label",
					"type": "boolean",
					"default": true
				},
				"label": {
					"title": "Label (pattern examples: '${entityName}', '${entityName}: (Text ${keyName} units.)' )",
					"type": "string",
					"default": "${entityName}"
				},
				"useLabelFunction": {
					"title": "Use label function",
					"type": "boolean",
					"default": false
				},
				"labelFunction": {
					"title": "Label function: f(data, dsData, dsIndex)",
					"type": "string"
				},
				"showTooltip": {
					"title": "Show tooltip",
					"type": "boolean",
					"default": true
				},
				"autocloseTooltip": {
					"title": "Auto-close tooltips",
					"type": "boolean",
					"default": true
				},
				"tooltipPattern": {
					"title": "Tooltip (for ex. 'Text ${keyName} units.' or <link-act name='my-action'>Link text</link-act>')",
					"type": "string",
					"default": "<b>${entityName}</b><br/><br/><b>X Pos:</b> ${xPos:2}<br/><b>Y Pos:</b> ${yPos:2}"
				},
				"useTooltipFunction": {
					"title": "Use tooltip function",
					"type": "boolean",
					"default": false
				},
				"tooltipFunction": {
					"title": "Tooltip function: f(data, dsData, dsIndex)",
					"type": "string"
				},
				"color": {
					"title": "Color",
					"type": "string"
				},
				"posFunction": {
					"title": "Position conversion function: f(origXPos, origYPos), should return x,y coordinates as double from 0 to 1 each",
					"type": "string",
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
				"useColorFunction": {
					"title": "Use color function",
					"type": "boolean",
					"default": false
				},
				"colorFunction": {
					"title": "Color function: f(data, dsData, dsIndex)",
					"type": "string"
				},
				"markerImage": {
					"title": "Custom marker image",
					"type": "string"
				},
				"markerImageSize": {
					"title": "Custom marker image size (px)",
					"type": "number",
					"default": 34
				},
				"useMarkerImageFunction": {
					"title": "Use marker image function",
					"type": "boolean",
					"default": false
				},
				"markerImageFunction": {
					"title": "Marker image function: f(data, images, dsData, dsIndex)",
					"type": "string"
				},
				"markerImages": {
					"title": "Marker images",
					"type": "array",
					"items": {
						"title": "Marker image",
						"type": "string"
					}
				}
			},
			"required": []
		},
		"form": [
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
			"useLabelFunction",
			{
				"key": "labelFunction",
				"type": "javascript"
			},
			"showTooltip",
			"autocloseTooltip",
			{
				"key": "tooltipPattern",
				"type": "textarea"
			},
			"useTooltipFunction",
			{
				"key": "tooltipFunction",
				"type": "javascript"
			},
			{
				"key": "color",
				"type": "color"
			},
			{
				"key": "posFunction",
				"type": "javascript"
			},
			"markerOffsetX",
			"markerOffsetY",
			"useColorFunction",
			{
				"key": "colorFunction",
				"type": "javascript"
			},
			{
				"key": "markerImage",
				"type": "image"
			},
			"markerImageSize",
			"useMarkerImageFunction",
			{
				"key": "markerImageFunction",
				"type": "javascript"
			},
			{
				"key": "markerImages",
				"items": [
					{
						"key": "markerImages[]",
						"type": "image"
					}
				]
			}
		]
	};