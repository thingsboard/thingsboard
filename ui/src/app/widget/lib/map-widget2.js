/*
 * Copyright © 2016-2020 The Thingsboard Authors
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
import addEntityPanelTemplate from './add-entity-panel.tpl.html';
import './add-entity-panel.scss';

export default class TbMapWidgetV2 {
	constructor(mapProvider, drawRoutes, ctx, useDynamicLocations, $element, isEdit) {
		var tbMap = this;
		this.ctx = ctx;
		this.mapProvider = mapProvider;
		if (!$element) {
			$element = ctx.$container;
		}
		this.utils = ctx.$scope.$injector.get('utils');
		this.drawRoutes = drawRoutes;
		this.isEdit = isEdit ? isEdit : false;
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
			if (settings.defaultZoomLevel >= 0 && settings.defaultZoomLevel < 21) {
				this.defaultZoomLevel = Math.floor(settings.defaultZoomLevel);
			}
		}

		if (angular.isUndefined(settings.defaultCenterPosition)) {
            settings.defaultCenterPosition = [0,0];
		} else if (angular.isString(settings.defaultCenterPosition)) {
            settings.defaultCenterPosition = settings.defaultCenterPosition.split(',').map(x => +x);
		}

		this.dontFitMapBounds = settings.fitMapBounds === false;

		if (!useDynamicLocations) {
			this.subscription = this.ctx.defaultSubscription;
		}

		this.configureLocationsSettings();

		var minZoomLevel = this.drawRoutes ? 18 : 15;

		let markerClusteringSetting = {
			isMarketCluster: false
		};

		if (settings.useClusterMarkers === true){
			if (mapProvider === 'google-map' || mapProvider === 'tencent-map') {
				markerClusteringSetting = {
					isMarketCluster: true,
					zoomOnClick: settings.zoomOnClick,
					averageCenter: true
				};
				if(angular.isDefined(settings.maxZoom) && settings.maxZoom >= 0 && settings.maxZoom < 19){
					markerClusteringSetting.maxZoom = Math.floor(settings.maxZoom);
				}
				if(angular.isDefined(settings.gridSize) && settings.gridSize > 0){
					markerClusteringSetting.gridSize = Math.floor(settings.gridSize);
				}
				if(angular.isDefined(settings.minimumClusterSize) && settings.minimumClusterSize > 1){
					markerClusteringSetting.minimumClusterSize = Math.ceil(settings.minimumClusterSize);
				}
			} else if(mapProvider === 'openstreet-map' || mapProvider === 'here') {
				markerClusteringSetting = {
					isMarketCluster: true,
					zoomToBoundsOnClick: settings.zoomOnClick,
					showCoverageOnHover: settings.showCoverageOnHover,
					removeOutsideVisibleBounds: settings.removeOutsideVisibleBounds,
					animate: settings.animate,
					chunkedLoading: settings.chunkedLoading
				};
				if(angular.isDefined(settings.maxClusterRadius) && settings.maxClusterRadius > 0){
					markerClusteringSetting.maxClusterRadius = Math.floor(settings.maxClusterRadius);
				}
				if(angular.isDefined(settings.maxZoom) && settings.maxZoom >= 0 && settings.maxZoom < 19){
					markerClusteringSetting.disableClusteringAtZoom = Math.floor(settings.maxZoom);
				}
			}
		}




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

        let openStreetMapProvider = {};
		if (mapProvider === 'google-map') {
			this.map = new TbGoogleMap($element, this.utils, initCallback, this.defaultZoomLevel, this.dontFitMapBounds, settings.disableScrollZooming, minZoomLevel, settings.gmApiKey, settings.gmDefaultMapType, settings.defaultCenterPosition, markerClusteringSetting);
		} else if (mapProvider === 'openstreet-map') {
			if (settings.useCustomProvider && settings.customProviderTileUrl) {
				openStreetMapProvider.name = settings.customProviderTileUrl;
				openStreetMapProvider.isCustom = true;
			} else {
				openStreetMapProvider.name = settings.mapProvider;
			}
			this.map = new TbOpenStreetMap($element, this.utils, initCallback, this.defaultZoomLevel, this.dontFitMapBounds, settings.disableScrollZooming, minZoomLevel, openStreetMapProvider, null,settings.defaultCenterPosition, markerClusteringSetting);
		} else if (mapProvider === 'here') {
			openStreetMapProvider.name = settings.mapProvider;
			this.map = new TbOpenStreetMap($element, this.utils, initCallback, this.defaultZoomLevel, this.dontFitMapBounds, settings.disableScrollZooming, minZoomLevel, openStreetMapProvider, settings.credentials, settings.defaultCenterPosition, markerClusteringSetting);
		} else if (mapProvider === 'image-map') {
			this.map = new TbImageMap(this.ctx, $element, this.utils, initCallback,
				settings.mapImageUrl,
				settings.disableScrollZooming,
				settings.posFunction,
				settings.imageEntityAlias,
				settings.imageUrlAttribute,
                settings.useDefaultCenterPosition ? settings.defaultCenterPosition: null);
		} else if (mapProvider === 'tencent-map') {
			this.map = new TbTencentMap($element, this.utils, initCallback, this.defaultZoomLevel, this.dontFitMapBounds, settings.disableScrollZooming, minZoomLevel, settings.tmApiKey, settings.tmDefaultMapType, settings.defaultCenterPosition, markerClusteringSetting);
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
        this.locationSettings.useDefaultCenterPosition = this.ctx.settings.useDefaultCenterPosition === true;
        this.locationSettings.displayTooltipAction = this.ctx.settings.showTooltipAction && this.ctx.settings.showTooltipAction.length ? this.ctx.settings.showTooltipAction : "click";
		this.locationSettings.autocloseTooltip = this.ctx.settings.autocloseTooltip !== false;
		this.locationSettings.showPolygon = this.ctx.settings.showPolygon === true;
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
			var entityLabel = datasource.entityLabel;
			this.ctx.actionsApi.handleWidgetAction(event, descriptor, entityId, entityName, null, entityLabel);
		}
	}

	selectEntity($event) {
		var tbMap = this;

		function setDefaultPosition(entity) {
			let position = tbMap.map.getCenter();
			if (tbMap.mapProvider === "image-map") {
				position = tbMap.map.latLngToPoint(position);
				position.lat = position.x / tbMap.map.width;
				position.lng = position.y / tbMap.map.height;
			}

			tbMap.saveMarkerLocation(
				entity,
				locationsWithoutMarker[entitiesWithoutPosition.indexOf(entity)],
				position
			);
		}

		const element = angular.element($event.target);
		const $mdPanel = this.ctx.$scope.$injector.get('$mdPanel');
		const $document = this.ctx.$scope.$injector.get('$document');
		let position = $mdPanel.newPanelPosition()
			.relativeTo(element)
			.addPanelPosition($mdPanel.xPosition.ALIGN_END, $mdPanel.yPosition.BELOW);

		let locationsWithoutMarker = this.locations.filter((location) => !location.marker);
		let entitiesWithoutPosition = [];
		for (let i = 0; i < locationsWithoutMarker.length; i++) {
			entitiesWithoutPosition.push(this.subscription.datasources[locationsWithoutMarker[i].dsIndex]);
		}

		if(entitiesWithoutPosition.length === 1){
			setDefaultPosition(entitiesWithoutPosition[0]);
		} else {
			let config = {
				attachTo: angular.element($document[0].body),
				controller: addEntityPanelController,
				controllerAs: 'vm',
				templateUrl: addEntityPanelTemplate,
				panelClass: 'tb-add-entity-panel',
				position: position,
				fullscreen: false,
				locals: {
					'entities': entitiesWithoutPosition,
					'onClose': setDefaultPosition
				},
				openFrom: $event,
				clickOutsideToClose: true,
				escapeToClose: true,
				focusOnOpen: false
			};
			$mdPanel.open(config);
		}
	}

	saveMarkerLocation(datasource, location, coordinate) {
		var tbMap = this;

		const types = tbMap.ctx.$scope.$injector.get('types');
		const $q = tbMap.ctx.$scope.$injector.get('$q');
		const attributeService = tbMap.ctx.$scope.$injector.get('attributeService');

		let attributesLocation = [];
		let timeseriesLocation = [];
		let promises = [];

		let dataKeys = datasource.dataKeys;
		for (let i = 0; i < dataKeys.length; i++) {
			if (dataKeys[i].name === location.settings.latKeyName || dataKeys[i].name === location.settings.lngKeyName) {
				let newLocation = {
					key: dataKeys[i].name,
					value: dataKeys[i].name === location.settings.latKeyName ? coordinate.lat : coordinate.lng
				};
				if (dataKeys[i].type === types.dataKeyType.attribute) {
					attributesLocation.push(newLocation);
				} else if (dataKeys[i].type === types.dataKeyType.timeseries) {
					timeseriesLocation.push(newLocation);
				}
			}
		}

		if (attributesLocation.length > 0) {
			promises.push(attributeService.saveEntityAttributes(
				datasource.entityType,
				datasource.entityId,
				types.attributesScope.server.value,
				attributesLocation,
				{
					ignoreLoading: true
				}
			))
		}
		if (timeseriesLocation.length > 0) {
			promises.push(attributeService.saveEntityTimeseries(
				datasource.entityType,
				datasource.entityId,
				"scope",
				timeseriesLocation,
				{
					ignoreLoading: true
				}
			))
		}
		return $q.all([promises]);
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
					}, [location.dsIndex],
					function (event) {
						markerDragend(event, location)
					});
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

        function markerDragend($event, location) {
			if (location.settings.drraggable) {
				let position = tbMap.map.getMarkerPosition(location.marker);
				if (tbMap.mapProvider === "image-map") {
					position.lat = position.x;
					position.lng = position.y;
					delete position.x;
					delete position.y;
				} else if (tbMap.mapProvider === "google-map") {
					position = position.toJSON();
				}

				tbMap.saveMarkerLocation(tbMap.subscription.datasources[location.dsIndex], location, position);
			}
		}

		function locationRowClick($event, location) {
			var descriptors = tbMap.ctx.actionsApi.getActionDescriptors('markerClick');
			if (descriptors.length) {
				var datasource = tbMap.subscription.datasources[location.dsIndex];
				var entityId = {};
				entityId.id = datasource.entityId;
				entityId.entityType = datasource.entityType;
				var entityName = datasource.entityName;
				var entityLabel = datasource.entityLabel;
				tbMap.ctx.actionsApi.handleWidgetAction($event, descriptors[0], entityId, entityName, null, entityLabel);
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
				var entityLabel = datasource.entityLabel;
				tbMap.ctx.actionsApi.handleWidgetAction($event, descriptors[0], entityId, entityName, null, entityLabel);
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
            var bounds = tbMap.locationSettings.useDefaultCenterPosition ? tbMap.map.createBounds().extend(tbMap.map.map.getCenter()) : tbMap.map.createBounds();
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
					location.settings.drraggable = tbMap.isEdit;
					tbMap.locations.push(location);
					updateLocation(location, data, dataMap);
					if (!tbMap.locationSettings.useDefaultCenterPosition) {
                        if (location.polyline) {
                            tbMap.map.extendBounds(bounds, location.polyline);
                        } else if (location.marker) {
                            tbMap.map.extendBoundsWithMarker(bounds, location.marker);
                        }
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
					if (!tbMap.locationSettings.useDefaultCenterPosition) {
                        tbMap.map.extendBounds(bounds, location.polygon);
                    }
				}
			});

			tbMap.map.fitBounds(bounds, tbMap.isEdit && tbMap.markers.length === 1);
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
			var bounds = tbMap.locationSettings.useDefaultCenterPosition ? tbMap.map.createBounds().extend(tbMap.map.map.getCenter()) : tbMap.map.createBounds();
			var dataMap = toLabelValueMap(data, datasources);
			for (var p = 0; p < tbMap.locations.length; p++) {
				var location = tbMap.locations[p];
				locationsChanged |= updateLocation(location, data, dataMap);
                createUpdatePolygon(location, dataMap);
				if (!tbMap.locationSettings.useDefaultCenterPosition) {
                    if (location.polyline) {
                        tbMap.map.extendBounds(bounds, location.polyline);
                    } else if (location.marker) {
                        tbMap.map.extendBoundsWithMarker(bounds, location.marker);
                    } else if (location.polygon) {
                        tbMap.map.extendBounds(bounds, location.polygon);
                    }
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

				if(!tbMap.isEdit && tbMap.markers.length !== 1 || tbMap.polylines || tbMap.polygons) {
					tbMap.map.fitBounds(bounds);
				}
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
				var bounds = this.locationSettings.useDefaultCenterPosition ? map.createBounds().extend(map.map.getCenter()) : map.createBounds();
				if (!this.locationSettings.useDefaultCenterPosition) {
                    if (this.markers && this.markers.length > 0) {
                        this.markers.forEach(function (marker) {
                            map.extendBoundsWithMarker(bounds, marker);
                        });
                    }
                    if (this.polylines && this.polylines.length > 0) {
                        this.polylines.forEach(function (polyline) {
                            map.extendBounds(bounds, polyline);
                        })
                    }
                    if (this.polygons && this.polygons.length > 0) {
                        this.polygons.forEach(function (polygon) {
                            map.extendBounds(bounds, polygon);
                        })
                    }
                }
				if((!map.isEdit && map.markers && map.markers.length !== 1) ||
					(this.polylines && this.polylines.length > 0) ||
					(this.polygons && this.polygons.length > 0)) {
					map.fitBounds(bounds);
				}
			}
		}
	}

	static settingsSchema(mapProvider, drawRoutes) {
		var schema;
		if (mapProvider === 'google-map') {
			schema = angular.copy(googleMapSettingsSchema);
			schema.groupInfoes=[{
				"formIndex":0,
				"GroupTitle":"Google Map Settings"
			}];
		} else if (mapProvider === 'openstreet-map') {
			schema = angular.copy(openstreetMapSettingsSchema);
			schema.groupInfoes=[{
				"formIndex":0,
				"GroupTitle":"Openstreet Map Settings"
			}];
		} else if (mapProvider === 'image-map') {
			return imageMapSettingsSchema;
		} else if (mapProvider === 'tencent-map') {
			schema = angular.copy(tencentMapSettingsSchema);
			schema.groupInfoes=[{
				"formIndex":0,
				"GroupTitle":"Tencent Map Settings"
			}];
		} else if (mapProvider === 'here') {
			schema = angular.copy(hereMapSettingsSchema);
			schema.groupInfoes=[{
				"formIndex":0,
				"GroupTitle":"Here Map Settings"
			}];
		}
		if(!schema.groupInfoes)schema.groupInfoes=[];
		schema.form = [schema.form];

		angular.merge(schema.schema.properties, commonMapSettingsSchema.schema.properties);
		schema.schema.required = schema.schema.required.concat(commonMapSettingsSchema.schema.required);
		schema.form.push(commonMapSettingsSchema.form);//schema.form.concat(commonMapSettingsSchema.form);
		schema.groupInfoes.push({
			"formIndex":schema.groupInfoes.length,
			"GroupTitle":"Common Map Settings"
		});
		if (drawRoutes) {
			angular.merge(schema.schema.properties, routeMapSettingsSchema.schema.properties);
			schema.schema.required = schema.schema.required.concat(routeMapSettingsSchema.schema.required);
			schema.form.push(routeMapSettingsSchema.form);//schema.form = schema.form.concat(routeMapSettingsSchema.form);
			schema.groupInfoes.push({
				"formIndex":schema.groupInfoes.length,
				"GroupTitle":"Route Map Settings"
			});
		} else if (mapProvider !== 'image-map'){
			angular.merge(schema.schema.properties, markerClusteringSettingsSchema.schema.properties);
			schema.schema.required = schema.schema.required.concat(markerClusteringSettingsSchema.schema.required);
			schema.form.push(markerClusteringSettingsSchema.form);
			if (mapProvider === 'google-map' || mapProvider === 'tencent-map') {
				angular.merge(schema.schema.properties, markerClusteringSettingsSchemaGoogle.schema.properties);
				schema.schema.required = schema.schema.required.concat(markerClusteringSettingsSchemaGoogle.schema.required);
				schema.form[schema.form.length -1] = schema.form[schema.form.length -1].concat(markerClusteringSettingsSchemaGoogle.form);
			}
			if (mapProvider === 'openstreet-map' || mapProvider === 'here') {
				angular.merge(schema.schema.properties, markerClusteringSettingsSchemaLeaflet.schema.properties);
				schema.schema.required = schema.schema.required.concat(markerClusteringSettingsSchemaLeaflet.schema.required);
				schema.form[schema.form.length -1] = schema.form[schema.form.length -1].concat(markerClusteringSettingsSchemaLeaflet.form);
			}
			schema.groupInfoes.push({
				"formIndex":schema.groupInfoes.length,
				"GroupTitle":"Markers Clustering Settings"
			});
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

const hereMapSettingsSchema =
	{
		"schema": {
			"title": "HERE Map Configuration",
			"type": "object",
			"properties": {
				"mapProvider": {
					"title": "Map layer",
					"type": "string",
					"default": "HERE.normalDay"
				},
				"credentials":{
					"type": "object",
					"properties": {
						"app_id": {
							"title": "HERE app id",
							"type": "string"
						},
						"app_code": {
							"title": "HERE app code",
							"type": "string"
						}
					},
					"required": ["app_id", "app_code"]
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
						"value": "HERE.normalDay",
						"label": "HERE.normalDay (Default)"
					},
					{
						"value": "HERE.normalNight",
						"label": "HERE.normalNight"
					},
					{
						"value": "HERE.hybridDay",
						"label": "HERE.hybridDay"
					},
					{
						"value": "HERE.terrainDay",
						"label": "HERE.terrainDay"
					}
				]
			},
			"credentials"
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
				},
				"useCustomProvider": {
					"title": "Use custom provider",
					"type": "boolean",
					"default": false
				},
				"customProviderTileUrl": {
					"title": "Custom provider tile URL",
					"type": "string",
					"default": "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
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
			},
			"useCustomProvider",
			"customProviderTileUrl"
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
                "animateAddingMarkers": {
                    "title": "添加标记时显示动画",
                    "type": "boolean",
                    "default": true
                }, 
                "showCoverageOnHover": {
                    "title": "鼠标移到标记聚类上显示覆盖范围",
                    "type": "boolean",
                    "default": true
                }, 
                "zoomToBoundsOnClick": {
                    "title": "点击标记聚类时放大到覆盖范围边界",
                    "type": "boolean",
                    "default": false
                }, 
                "removeOutsideVisibleBounds": {
                    "title": "移除可见范围之外的聚类(标记很多时可提升显示性能)",
                    "type": "boolean",
                    "default": false
                }, 
                "spiderfyOnMaxZoom": {
                    "title": "缩放级别到最大时，点击聚类后也蜘蛛化散开布局",
                    "type": "boolean",
                    "default": false
                }, 
                "maxClusterRadius": {
                    "title": "标记聚类半径(像素)",
                    "type": "number",
                    "default": 80
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
            "animateAddingMarkers",
            "showCoverageOnHover",
            "zoomToBoundsOnClick",
            "removeOutsideVisibleBounds",
            "spiderfyOnMaxZoom",
            "maxClusterRadius",              
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
/*@ngInject*/
function addEntityPanelController(mdPanelRef, entities) {
	var vm = this;
	vm.entities = entities;
	vm.selectEntity = selectEntity;

	function selectEntity(entity) {
		mdPanelRef.close().then(() => {
			this.onClose(entity);
		});
	}
}
