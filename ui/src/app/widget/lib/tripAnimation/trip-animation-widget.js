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
//import tinycolor from 'tinycolor2';
import MultiOptionsPolyline from '../../../../vendor/leaflet-multi-options-polyline/Leaflet.MultiOptionsPolyline';
import GeometryUtil from '../../../../vendor/leaflet-geometryutil/leaflet-geometryutil';
import tinycolor from "tinycolor2";
import {fillPatternWithActions, isNumber, padValue, processPattern} from "../widget-utils";
//import {fillPatternWithActions, isNumber, padValue, processPattern, fillPattern} from "../widget-utils";

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
function tripAnimationController($document, $scope, $http, $timeout, $filter, $log) {
	let vm = this;
	//const varsRegex = /\$\{([^\}]*)\}/g;
	//let icon;

	vm.markers = [];
	vm.index = 0;
	vm.dsIndex = 0;
	vm.isPlaying = false;
	vm.minTime = 0;
	vm.maxTime = 0;
	vm.isPLaying = false;
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

	L.MultiOptionsPolyline = MultiOptionsPolyline;
	L.GeometryUtil = GeometryUtil;
	L.multiOptionsPolyline = function (latlngs, options) {
		return new MultiOptionsPolyline(latlngs, options);
	};

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
			createUpdatePath();
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
		if (play && vm.isPLaying) return;
		if (play || vm.isPLaying) vm.isPLaying = true;
		if (vm.isPLaying) {
			if (vm.index + 1 > vm.maxTime) return;
			vm.index++;
			vm.trips.forEach(function (trip) {
				moveMarker(trip);
			});
			vm.timeout = $timeout(function () {
				vm.playMove();
			}, 1000 / vm.speed)
		}
	};


	vm.stopPlay = function () {
		vm.isPLaying = false;
		$timeout.cancel(vm.timeout);
	};

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
		vm.ctx.$element = angular.element("#heat-map", vm.ctx.$container);
		//vm.map = L.map(vm.ctx.$element[0]).setView([0, 0], 2);
		vm.map = new TbOpenStreetMap(vm.ctx.$element, vm.utils, initCallback, 2, null, null, vm.staticSettings.mapProvider);
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
		staticSettings.mapProvider = vm.ctx.settings.mapProvider || "OpenStreetMap.Mapnik";
		staticSettings.latKeyName = vm.ctx.settings.latKeyName || "latitude";
		staticSettings.lngKeyName = vm.ctx.settings.lngKeyName || "longitude";
		staticSettings.rotationAngle = vm.ctx.settings.rotationAngle || 0;
		staticSettings.displayTooltip = vm.ctx.settings.showTooltip || false;
		staticSettings.showTooltip = false;
		staticSettings.label = vm.ctx.settings.label || "${entityName}";
		staticSettings.useLabelFunction = vm.ctx.settings.useLabelFunction || false;
		staticSettings.showLabel = vm.ctx.settings.showLabel || false;
		staticSettings.useTooltipFunction = vm.ctx.settings.useTooltipFunction || false;
		staticSettings.tooltipPattern = vm.ctx.settings.tooltipPattern || "<b>${entityName}</b><br/><br/><b>Latitude:</b> ${latitude:7}<br/><b>Longitude:</b> ${longitude:7}<br/><b>Start Time:</b> ${maxTime}<br/><b>End Time:</b> ${minTime}";
		staticSettings.tooltipOpacity = vm.ctx.settings.tooltipOpacity || 1;
		staticSettings.tooltipColor = vm.ctx.settings.tooltipColor ? tinycolor(vm.ctx.settings.tooltipColor).toHexString() : "#ffffff";
		staticSettings.tooltipFontColor = vm.ctx.settings.tooltipFontColor ? tinycolor(vm.ctx.settings.tooltipFontColor).toHexString() : "#000000";
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
			iconUrl: "data:image/png;base64,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",
			iconSize: [30, 30],
			iconAnchor: [15, 15]
		});
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

		if (staticSettings.useLabelFunction && angular.isDefined(vm.ctx.settings.labelFunction)) {
			staticSettings.labelFunction = new Function('data, dsData, dsIndex', vm.ctx.settings.labelFunction);
		}

		if (staticSettings.useTooltipFunction && angular.isDefined(vm.ctx.settings.tooltipFunction)) {
			staticSettings.tooltipFunction = new Function('data, dsData, dsIndex', vm.ctx.settings.tooltipFunction);
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

	function configureTripSettings(trip) {
		trip.settings = {};
		trip.settings.color = calculateColor(trip);
		trip.settings.strokeWeight = vm.staticSettings.pathWeight;
		trip.settings.strokeOpacity = vm.staticSettings.pathOpacity;
		trip.settings.pointColor = vm.staticSettings.pointColor;
		trip.settings.pointSize = vm.staticSettings.pointSize;
		trip.settings.labelText = calculateLabel(trip);
		trip.settings.tooltipText = calculateTooltip(trip);
		trip.settings.icon = calculateIcon(trip);
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

	function createUpdatePath() {
		if (vm.trips && vm.map) {
			vm.trips.forEach(function (trip) {
				if (trip.marker) {
					trip.marker.remove();
				}
				if (trip.polyline) {
					trip.polyline.remove();
				}
				if (trip.points && trip.points.length) {
					trip.points.forEach(function (point) {
						point.remove();
					})
				}
			})
		}
		let normalizedTimeRange = createNormalizedTime(vm.data, 1000);
		createNormalizedTrips(normalizedTimeRange, vm.datasources);
		createTripsOnMap();
		vm.trips.forEach(function (trip) {
			vm.map.extendBounds(vm.map.bounds, trip.polyline);
			vm.map.fitBounds(vm.map.bounds);
		})

	}

	function fillPattern(pattern, replaceInfo, currentNormalizedValue) {
		let text = angular.copy(pattern);
		let reg = /\$\{([^\}]*)\}/g;
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
				normalizedArray.push({ts: i})
			}
			if (normalizedArray[normalizedArray.length - 1] && normalizedArray[normalizedArray.length - 1].ts !== max_time) normalizedArray.push({ts: max_time});
		}
		vm.maxTime = normalizedArray.length - 1;
		vm.minTime = 0;
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
			addAngleForTip(el);
		})
	}

	function addAngleForTip(trip) {
		if (trip.timeRange && trip.timeRange.length > 0) {
			trip.timeRange.forEach(function (point, index) {
				let nextPoint, prevPoint;
				nextPoint = index === (trip.timeRange.length - 1) ? trip.timeRange[index] : trip.timeRange[index + 1];
				prevPoint = index === 0 ? trip.timeRange[0] : trip.timeRange[index - 1];
				point.h = findAngle(prevPoint[vm.staticSettings.latKeyName], prevPoint[vm.staticSettings.lngKeyName], nextPoint[vm.staticSettings.latKeyName], nextPoint[vm.staticSettings.lngKeyName]);
				point.h += vm.staticSettings.rotationAngle;
			});
		}
	}

	function createTripsOnMap() {
		if (vm.trips.length > 0) {
			vm.trips.forEach(function (trip) {
				if (trip.timeRange.length > 0 && trip.latLngs.every(el => angular.isDefined(el))) {
					configureTripSettings(trip, vm.index);
					if (vm.staticSettings.showPoints) {
						trip.points = [];
						trip.latLngs.forEach(function (latLng) {
							let point = L.circleMarker(latLng, {
								color: trip.settings.pointColor,
								radius: trip.settings.pointSize
							}).addTo(vm.map.map);
							trip.points.push(point);
						});
					}

					if (angular.isUndefined(trip.marker)) {
						trip.polyline = vm.map.createPolyline(trip.latLngs, trip.settings);
					}

					if (trip.timeRange && trip.timeRange.length && angular.isUndefined(trip.marker)) {
						trip.marker = L.marker(trip.timeRange[vm.index].latLng).addTo(vm.map.map);
						trip.marker.setZIndexOffset(1000);
						trip.marker.setIcon(vm.staticSettings.icon);
						trip.marker.setRotationOrigin('center center');
						// trip.marker.addTo(vm.map.map);
						trip.marker.on('click', function () {
							showHideTooltip(trip);
						});
						moveMarker(trip);
					}
				}
			});
		}
	}

	function moveMarker(trip) {
		if (angular.isDefined(trip.marker)) {
			trip.marker.setLatLng(trip.timeRange[vm.index].latLng);
			trip.marker.setRotationAngle(trip.timeRange[vm.index].h + vm.staticSettings.rotationAngle);
			trip.marker.update();
		} else {
			if (trip.timeRange && trip.timeRange.length) {
				trip.marker = L.marker(trip.timeRange[vm.index].latLng);
				trip.marker.setZIndexOffset(1000);
				trip.marker.setIcon(vm.staticSettings.icon);
				trip.marker.setRotationOrigin('center center');
				trip.marker.addTo(vm.map.map);
				trip.marker.on('click', function () {
					showHideTooltip(trip);
				});
				trip.marker.update();
			}

		}
		configureTripSettings(trip);
	}

	function showHideTooltip(trip) {
		if (vm.staticSettings.displayTooltip)  {
			if (vm.staticSettings.showTooltip && trip && vm.activeTripIndex !== trip.dSIndex) {
				vm.staticSettings.showTooltip = true;
			} else {
				vm.staticSettings.showTooltip = !vm.staticSettings.showTooltip;
			}
		}
		if (trip && vm.activeTripIndex !== trip.dSIndex) vm.activeTripIndex = trip.dSIndex;
	}

	$log.log(vm);
}